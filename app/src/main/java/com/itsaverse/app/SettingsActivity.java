package com.itsaverse.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private SettingsFragment mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSettingsFragment = new SettingsFragment(this);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mSettingsFragment)
                    .commit();
        };

        Intent serviceIntent = new Intent(this, OverlayControlService.class);
        startService(serviceIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mSettingsFragment != null && mSettingsFragment.isResumed() && mSettingsFragment.isVisible()) {
            mSettingsFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends Fragment {

        private static final int REQUEST_MEDIA_PROJECTION = 1;

        private SettingsActivity mActivity;
        private Button mStopStartButton;
        private Button mLoadLastButton;
        private Button mCaptureScreenButton;

        // Temp stuff
        private int mScreenDensity;
        private MediaProjectionManager mMediaProjectionManager;
        private MediaProjection mMediaProjection;
        private SurfaceView mSurfaceView;
        private Surface mSurface;
        private VirtualDisplay mVirtualDisplay;
        private boolean mIsProjecting;

        private int mResultCode;
        private Intent mResultData;

        private BroadcastReceiver mServiceAliveReceiver;

        private boolean mIsServiceAlive;

        public SettingsFragment(SettingsActivity activity) {
            mActivity = activity;

            mServiceAliveReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent != null) {
                        mIsServiceAlive = intent.getBooleanExtra(OverlayControlService.BROADCAST_ALIVE, false);
                        setStopStartButton(mIsServiceAlive);
                    }
                }
            };
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

            mStopStartButton = (Button) rootView.findViewById(R.id.settings_stop_button);
            mLoadLastButton = (Button) rootView.findViewById(R.id.settings_load_last_button);
            mCaptureScreenButton = (Button) rootView.findViewById(R.id.settings_capture_screen_button);
            mSurfaceView = (SurfaceView) rootView.findViewById(R.id.settings_screen_capture_surface);
            mSurface = mSurfaceView.getHolder().getSurface();

            mStopStartButton.setOnClickListener(v -> {
                Intent serviceIntent = new Intent(mActivity, OverlayControlService.class);

                if (mIsServiceAlive) {
                    mActivity.stopService(serviceIntent);
                } else {
                    mActivity.startService(serviceIntent);
                }
            });

            mLoadLastButton.setOnClickListener(v -> {
                Intent serviceIntent = new Intent(mActivity, OverlayControlService.class);
                serviceIntent.putExtra(OverlayControlService.EXTRA_LOAD_LAST, true);
                mActivity.startService(serviceIntent);
            });

            mCaptureScreenButton.setOnClickListener(v -> {
                if (mIsProjecting) {
                    stopScreenCapture();
                } else {
                    startScreenCapture();
                }
            });

            initScreenCaptureConfig();

            return rootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_MEDIA_PROJECTION) {
                if (resultCode == Activity.RESULT_OK) {
                    if (getActivity() != null) {
                        mResultCode = resultCode;
                        mResultData = data;
                        startScreenCapture();
                    }
                } else {
                    Toast.makeText(getActivity(), "Unable to start screen capture", Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            mIsServiceAlive = OverlayControlService.isAlive();
            setStopStartButton(mIsServiceAlive);
            mActivity.registerReceiver(mServiceAliveReceiver,
                    new IntentFilter(OverlayControlService.BROADCAST_ALIVE));
        }

        @Override
        public void onPause() {
            super.onPause();
            mActivity.unregisterReceiver(mServiceAliveReceiver);
        }

        public void setStopStartButton(boolean isStarted) {
            mStopStartButton.setText(getString(isStarted ? R.string.settings_turn_off : R.string.settings_turn_on));
        }

        private void initScreenCaptureConfig() {
            DisplayMetrics metrics = new DisplayMetrics();
            mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mScreenDensity = metrics.densityDpi;
            mMediaProjectionManager = (MediaProjectionManager)
                    mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }

        private void initVirtualDisplay() {
            if (mMediaProjection != null) {
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCap",
                        mSurfaceView.getWidth(), mSurfaceView.getHeight(), mScreenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mSurface, null, null);
                mCaptureScreenButton.setText("Stop Capturing Screen");
                mIsProjecting = true;
            }
        }

        private void initMediaProjection() {
            mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
        }

        private void startScreenCapture() {
            if (mSurface == null || mActivity == null) return;

            if (mMediaProjection != null) {
                initVirtualDisplay();
            } else if (mResultCode != 0 && mResultData != null) {
                initMediaProjection();
                initVirtualDisplay();
            } else {
                mActivity.startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
                        REQUEST_MEDIA_PROJECTION);
            }
        }

        private void stopScreenCapture() {
            if (mVirtualDisplay == null) return;
            mVirtualDisplay.release();
            mVirtualDisplay = null;
            mCaptureScreenButton.setText("Capture Screen");
            mIsProjecting = false;
        }
    }

}
