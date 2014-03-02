package com.itsaverse.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.itsaverse.app.utils.DataUtils;
import com.itsaverse.app.utils.RecursiveFileObserver;

import java.io.IOException;
import java.io.OutputStream;

public class OverlayControlService extends Service {

    public static final String EXTRA_TURN_OFF = "TurnOff";
    public static final String BROADCAST_ALIVE = "com.itsaverse.app.alive";

    public static boolean isAlive() {
        return sIsAlive;
    }

    private static final String TAG = "OverlayControlService";
    private static final int NOTIFICATION_ID = 89384;
    private static boolean sIsAlive = false;

    private Binder mBinder;
    private Handler mHandler;
    private NotificationManager mNotificationManager;
    private RecursiveFileObserver mScreenshotObserver;

    private boolean mIsInitialized = false;

    @Override
    public void onCreate() {
        mBinder = new OverlayControlBinder();
        mHandler = new Handler(Looper.getMainLooper());

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Intent deathIntent = new Intent();
        deathIntent.setAction(BROADCAST_ALIVE);
        deathIntent.putExtra(BROADCAST_ALIVE, false);
        sendBroadcast(deathIntent);

        sIsAlive = false;

        Log.d(TAG, "Stopping OverlayControlService");
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

    private void processStart(Intent intent) {
        if (!mIsInitialized) {
            DataUtils.copyDataIfRequired(this);

            mScreenshotObserver = new RecursiveFileObserver(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .getAbsolutePath()) {

                @Override
                public void onEvent(int event, String path) {
                    if (event == FileObserver.CREATE) {
                        Log.d(TAG, "A FILE HAS BEEN OBSERVED!!!!");
                        respondToScreenshot(path);
                    }
                }
            };
            mScreenshotObserver.startWatching();

            mIsInitialized = true;
            Log.d(TAG, "RUNNING OVERLAY CONTROL SERVICE");

            startForeground(NOTIFICATION_ID, getControllerNotification());

        } else if (intent != null) {
            // Process new intent data

            boolean turnOff = intent.getBooleanExtra(EXTRA_TURN_OFF, false);

            if (turnOff) {
                Log.d(TAG, "Turning off...");

                stopForeground(true);
                stopSelf();
            }
        }
    }

    private void respondToScreenshot(String path) {
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("New Screenshot Detected!")
                .setContentText("Tap to view the new screenshot");

        Intent imageIntent = new Intent(this, ImageViewer.class);
        imageIntent.setDataAndType(Uri.parse("file://" + path), "image/*");

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, imageIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        //mNotificationManager.notify(NOTIFICATION_ID, builder.getNotification());
    }

    private Notification getControllerNotification() {
        Intent turnOffIntent = new Intent(this, OverlayControlService.class);
        turnOffIntent.putExtra(EXTRA_TURN_OFF, true);
        PendingIntent turnOffPendingIntent = PendingIntent.getService(this, 0, turnOffIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.ongoing_notification_title))
                .setContentText(getString(R.string.ongoing_notification_desc))
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.addAction(R.drawable.notification_turn_off_icon,
                    getString(R.string.ongoing_notification_turn_off), turnOffPendingIntent);
        }

        return builder.getNotification();
    }

    private void getScreenshotUsingRoot() {

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "ATTEMPTING TO TAKE A SCREENSHOT WITH ROOT ACCESS");

                // image naming and path  to include sd card  appending name you choose for file
                String mPath = Environment.getExternalStorageDirectory().toString() + "/" + "AnnoverseScreenshot.jpg";

                try {
                    Process sh = Runtime.getRuntime().exec("su", null, null);
                    OutputStream  os = sh.getOutputStream();
                    os.write(("/system/bin/screencap -p " + mPath).getBytes("ASCII"));
                    os.flush();
                } catch (IOException e) {
                    Log.e(TAG, "SCREENSHOT DIDN'T WORK");
                    e.printStackTrace();
                }

            }
        }, 300);
    }

    public class OverlayControlBinder extends Binder {
        public OverlayControlService getService() {
            return OverlayControlService.this;
        }
    }
}
