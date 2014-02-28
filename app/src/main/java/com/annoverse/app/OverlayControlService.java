package com.annoverse.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.annoverse.app.utils.DataUtils;
import com.annoverse.app.utils.RecursiveFileObserver;

import java.io.IOException;
import java.io.OutputStream;

public class OverlayControlService extends Service {

    private static final String TAG = "OverlayControlService";
    private static final int NOTIFICATION_ID = 89384;

    private Binder mBinder;
    private Handler mHandler;
    private RecursiveFileObserver mScreenshotObserver;

    @Override
    public void onCreate() {
        mBinder = new OverlayControlBinder();
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        DataUtils.copyDataIfRequired(this);

        mScreenshotObserver = new RecursiveFileObserver(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .getAbsolutePath()) {

            @Override
            public void onEvent(int event, String path) {
                if (event == FileObserver.CREATE) {
                    Log.e(TAG, "A FILE HAS BEEN OBSERVED!!!!");
                    respondToScreenshot(path);
                }
            }
        };
        mScreenshotObserver.startWatching();

        Log.e(TAG, "RUNNING OVERLAY CONTROL SERVICE");

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
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

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.getNotification());
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
