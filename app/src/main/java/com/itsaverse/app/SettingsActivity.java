package com.itsaverse.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
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

public class SettingsActivity extends FragmentActivity {

    private SettingsFragment mSettingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSettingsFragment = new SettingsFragment();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, mSettingsFragment)
                    .commit();
        }

        Intent serviceIntent = new Intent(this, OverlayControlService.class);
        startService(serviceIntent);
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

        private static final String STATE_RESULT_CODE = "result_code";
        private static final String STATE_RESULT_DATA = "result_data";
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

        public SettingsFragment() {
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
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
                mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_settings, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            mStopStartButton = (Button) view.findViewById(R.id.settings_stop_button);
            mLoadLastButton = (Button) view.findViewById(R.id.settings_load_last_button);
            mCaptureScreenButton = (Button) view.findViewById(R.id.settings_capture_screen_button);
            mSurfaceView = (SurfaceView) view.findViewById(R.id.settings_screen_capture_surface);
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
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mActivity = (SettingsActivity) getActivity();
            initScreenCaptureConfig();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if (mResultData != null) {
                outState.putInt(STATE_RESULT_CODE, mResultCode);
                outState.putParcelable(STATE_RESULT_DATA, mResultData);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_MEDIA_PROJECTION) {
                if (resultCode == Activity.RESULT_OK) {
                    if (getActivity() != null) {
                        mResultCode = resultCode;
                        mResultData = data;
                        initMediaProjection();
                        initVirtualDisplay();
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
            stopScreenCapture();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            tearDownMediaProjection();
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
                Log.i("STUFF", "Setting up a VirtualDisplay: " +
                        mSurfaceView.getWidth() + "x" + mSurfaceView.getHeight() +
                        " (" + mScreenDensity + ") Surface: " + (mSurface == null ? "null" : "not null"));
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCap",
                        mSurfaceView.getWidth(), mSurfaceView.getHeight(), mScreenDensity,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mSurface, new VirtualDisplay.Callback() {
                            @Override
                            public void onPaused() {
                                super.onPaused();
                                Log.d("SURFACE", "Paused");
                            }

                            @Override
                            public void onResumed() {
                                super.onResumed();
                                Log.d("SURFACE", "Resumed");
                            }

                            @Override
                            public void onStopped() {
                                super.onStopped();
                                Log.d("SURFACE", "Stopped");
                            }
                        }, null);
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
                startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
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

        private void tearDownMediaProjection() {
            if (mMediaProjection != null) {
                mMediaProjection.stop();
                mMediaProjection = null;
            }
        }
    }

}
