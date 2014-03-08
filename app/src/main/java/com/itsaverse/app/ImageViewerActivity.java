package com.itsaverse.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import com.googlecode.leptonica.android.Box;
import com.itsaverse.app.utils.DataUtils;
import com.itsaverse.app.views.ScreenShotImageView;

import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends Activity {

    private ScreenShotImageView mBaseImage;
    private ScreenShotImageView mOverlayImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        mBaseImage = (ScreenShotImageView) findViewById(R.id.image_viewer_image);
        mOverlayImage = (ScreenShotImageView) findViewById(R.id.image_viewer_overlay_image);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (OverlayControlService.getScreenshot() != null) {
            mBaseImage.setImageBitmap(OverlayControlService.getScreenshot());
            mOverlayImage.setImageBitmap(OverlayControlService.getScreenshot());

            List<Box> boxes = new ArrayList<Box>();
            for (DataUtils.VerseReference ref : OverlayControlService.getVerseReferences()) {
                if (ref.posBoxes != null) {
                    boxes.addAll(ref.posBoxes);
                }
            }

            mOverlayImage.setClipBoxes(boxes);

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
