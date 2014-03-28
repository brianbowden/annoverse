package com.itsaverse.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.itsaverse.app.utils.BitmapUtils;
import com.itsaverse.app.utils.DataUtils;
import com.itsaverse.app.utils.RecursiveFileObserver;
import com.itsaverse.app.utils.Utils;

import java.util.List;

public class OverlayControlService extends Service {

    public static final String EXTRA_TURN_OFF = "TurnOff";
    public static final String EXTRA_LOAD_LAST = "LoadLast";
    public static final String BROADCAST_ALIVE = "com.itsaverse.app.alive";

    public static Bitmap getScreenshot() {
        return sScreenshot;
    }
    public static List<DataUtils.VerseReference> getVerseReferences() {
        return sVerseRefs;
    }

    public static boolean isAlive() {
        return sIsAlive;
    }

    private static final String STORE_LAST_SCREENSHOT = "LastScreenshot";

    private static final String TAG = "OverlayControlService";
    private static final int NOTIFICATION_ID = 89384;
    private static boolean sIsAlive = false;
    private static Bitmap sScreenshot;
    private static List<DataUtils.VerseReference> sVerseRefs;

    private final Context CONTEXT = this;

    private Binder mBinder;
    private Handler mHandler;
    private NotificationManager mNotificationManager;
    private RecursiveFileObserver mScreenshotObserver;
    private TessBaseAPI mTessApi;

    private ScreenshotOverlayManager mScreenshotOverlayManager;

    private boolean mIsInitialized = false;
    private boolean mAllowHistory = false;

    // Overlay Views
    private RelativeLayout mLoadingOverlayLayout;

    @Override
    public void onCreate() {
        mBinder = new OverlayControlBinder();
        mHandler = new Handler(Looper.getMainLooper());

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mScreenshotOverlayManager = new ScreenshotOverlayManager(this);

        // Offload the Tess initialization into a non-UI thread. This is potentially hazardous, but it should be ok.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mTessApi = new TessBaseAPI();
                    mTessApi.init(Environment.getExternalStorageDirectory().getAbsolutePath(), "eng");
                } catch (Exception e) {
                    Log.e(TAG, "Unable to initialize Tess: " + (e != null ? e.getMessage() : "n/a"));
                }
            }
        }).run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Intent deathIntent = new Intent();
        deathIntent.setAction(BROADCAST_ALIVE);
        deathIntent.putExtra(BROADCAST_ALIVE, false);
        sendBroadcast(deathIntent);

        sIsAlive = false;

        if (mScreenshotObserver != null) {
            mScreenshotObserver.stopWatching();
        }

        Log.e(TAG, "Stopping OverlayControlService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        processStart(intent);

        Intent lifeIntent = new Intent();
        lifeIntent.setAction(BROADCAST_ALIVE);
        lifeIntent.putExtra(BROADCAST_ALIVE, true);
        sendBroadcast(lifeIntent);

        sIsAlive = true;

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startScreenshotOverlay() {
        /**Intent viewerIntent = new Intent(CONTEXT, ImageViewerActivity.class);
         int flags = Intent.FLAG_ACTIVITY_NEW_TASK | (mAllowHistory ? 0 : Intent.FLAG_ACTIVITY_NO_HISTORY);
         viewerIntent.setFlags(flags);
         startActivity(viewerIntent);**/

        mScreenshotOverlayManager.displayScreenshotOverlay();
    }

    public void stopScreenshotOverlay() {
        mScreenshotOverlayManager.hideScreenshotOverlay();
    }

    private void processStart(Intent intent) {
        if (!mIsInitialized) {
            mAllowHistory = false;
            DataUtils.copyDataIfRequired(this);

            mScreenshotObserver = new RecursiveFileObserver(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .getAbsolutePath()) {

                @Override
                public void onEvent(int event, String path) {
                    if (event == FileObserver.CLOSE_WRITE && path != null && path.toLowerCase().contains("screen")) {
                        Log.e(TAG, "A SCREENSHOT HAS BEEN OBSERVED");
                        respondToScreenshot(path);
                    }
                }
            };
            mScreenshotObserver.startWatching();

            mIsInitialized = true;
            Log.e(TAG, "RUNNING OVERLAY CONTROL SERVICE");

            startForeground(NOTIFICATION_ID, getControllerNotification(false));

        } else if (intent != null) {
            // Process new intent data

            boolean turnOff = intent.getBooleanExtra(EXTRA_TURN_OFF, false);
            boolean loadLast = intent.getBooleanExtra(EXTRA_LOAD_LAST, false);

            if (turnOff) {
                Log.e(TAG, "Turning off...");
                clearLoadingIndicator();
                stopForeground(true);
                stopSelf();

            } else if (loadLast) {
                Log.e(TAG, "Rescanning last screenshot...");
                mAllowHistory = true;
                mNotificationManager.notify(NOTIFICATION_ID, getControllerNotification(true));
                new OcrAsyncTask().execute(Utils.getData(CONTEXT, STORE_LAST_SCREENSHOT));
            }
        }
    }

    private void respondToScreenshot(String path) {
        mNotificationManager.notify(NOTIFICATION_ID, getControllerNotification(true));
        Utils.setData(this, STORE_LAST_SCREENSHOT, path);
        new OcrAsyncTask().execute(path);
    }

    private Notification getControllerNotification(boolean isWorking) {
        Intent turnOffIntent = new Intent(this, OverlayControlService.class);
        turnOffIntent.putExtra(EXTRA_TURN_OFF, true);
        PendingIntent turnOffPendingIntent = PendingIntent.getService(this, 0, turnOffIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String contentText = isWorking ? getString(R.string.ongoing_notification_scanning)
                : getString(R.string.ongoing_notification_desc);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.itsaverse_notif_icon)
                .setContentTitle(getString(R.string.ongoing_notification_title))
                .setContentText(contentText)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.addAction(R.drawable.notification_turn_off_icon,
                    getString(R.string.ongoing_notification_turn_off), turnOffPendingIntent);
        }

        return builder.getNotification();
    }

    private void displayLoadingIndicator() {

        final WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        Resources resources = CONTEXT.getResources();
        int navBarHeight = 0;
        int statusBarHeight = 0;
        int navBarId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int statusBarId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (navBarId > 0) {
            navBarHeight = resources.getDimensionPixelSize(navBarId);
        }
        if (statusBarId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(statusBarId);
        }

        lp.height = displayMetrics.heightPixels - navBarHeight + statusBarHeight;
        lp.width = displayMetrics.widthPixels;

        mLoadingOverlayLayout = (RelativeLayout) LayoutInflater.from(CONTEXT).inflate(R.layout.loading_indicator_layout, null);
        View loadingIndicatorImage = mLoadingOverlayLayout.findViewById(R.id.loading_indicator_image);
        loadingIndicatorImage.startAnimation(AnimationUtils.loadAnimation(CONTEXT, R.anim.continuous_rotation));

        windowManager.addView(mLoadingOverlayLayout, lp);
    }

    private void clearLoadingIndicator() {
        if (mLoadingOverlayLayout != null) {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mLoadingOverlayLayout);
        }
    }

    private class OcrAsyncTask extends AsyncTask<String, Void, List<DataUtils.VerseReference>> {

        @Override
        public void onPreExecute() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    displayLoadingIndicator();
                }
            });
        }

        @Override
        protected List<DataUtils.VerseReference> doInBackground(String... paths) {
            if (paths == null || paths.length == 0) return null;
            String path = "file://" + paths[0];

            Uri uri = Uri.parse(path);

            long startTime = 0;
            long endTime = 0;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            startTime = System.currentTimeMillis();
            sScreenshot = BitmapFactory.decodeFile(uri.getPath(), options);
            endTime = System.currentTimeMillis();

            Log.e(TAG, "Bitmap Decode: " + (endTime - startTime) + "ms");

            if (sScreenshot != null) {

                startTime = System.currentTimeMillis();
                BitmapUtils.correctBitmapOrientation(CONTEXT, sScreenshot, uri.getPath(), false);
                endTime = System.currentTimeMillis();
                Log.e(TAG, "Bitmap Orientate: " + (endTime - startTime) + "ms");

                startTime = System.currentTimeMillis();
                mTessApi.setImage(sScreenshot);
                String fullText = mTessApi.getUTF8Text();
                Pixa words = mTessApi.getWords();

                endTime = System.currentTimeMillis();
                Log.e(TAG, "Perform OCR: " + (endTime - startTime) + "ms");

                startTime = System.currentTimeMillis();
                sVerseRefs = DataUtils.getVerseReferences(fullText, words);
                endTime = System.currentTimeMillis();
                Log.e(TAG, "Perform Verse Regex: " + (endTime - startTime) + "ms");

                return sVerseRefs;

            } else {
                Log.e(TAG, "Bitmap was null");
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<DataUtils.VerseReference> result) {
            mNotificationManager.notify(NOTIFICATION_ID, getControllerNotification(false));
            clearLoadingIndicator();

            String test = "";

            for (DataUtils.VerseReference ref : result) {
                test += ref.text + "\\n";
            }

            Log.e(TAG, "Results: " + test);

            if (result != null && result.size() > 0) {
                startScreenshotOverlay();
            }
        }
    }

    public class OverlayControlBinder extends Binder {
        public OverlayControlService getService() {
            return OverlayControlService.this;
        }
    }
}
