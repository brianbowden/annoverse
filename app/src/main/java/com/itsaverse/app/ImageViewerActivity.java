package com.itsaverse.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageViewerActivity extends Activity {

    private ImageView mImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        mImage = (ImageView) findViewById(R.id.image_viewer_image);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (OverlayControlService.getScreenshot() != null) {
            mImage.setImageBitmap(OverlayControlService.getScreenshot());
        } else {
            Toast.makeText(this, "No screenshot to display!", Toast.LENGTH_LONG);
        }

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
