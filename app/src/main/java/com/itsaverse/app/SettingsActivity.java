package com.itsaverse.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

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

        private SettingsActivity mActivity;
        private Button mStopStartButton;
        private Button mLoadLastButton;

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

            mStopStartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent serviceIntent = new Intent(mActivity, OverlayControlService.class);

                    if (mIsServiceAlive) {
                        mActivity.stopService(serviceIntent);
                    } else {
                        mActivity.startService(serviceIntent);
                    }
                }
            });

            mLoadLastButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent serviceIntent = new Intent(mActivity, OverlayControlService.class);
                    serviceIntent.putExtra(OverlayControlService.EXTRA_LOAD_LAST, true);
                    mActivity.startService(serviceIntent);
                }
            });

            return rootView;
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
    }

}
