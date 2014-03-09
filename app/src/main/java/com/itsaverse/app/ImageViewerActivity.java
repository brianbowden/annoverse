package com.itsaverse.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.googlecode.leptonica.android.Box;
import com.itsaverse.app.utils.DataUtils;
import com.itsaverse.app.views.ClickableRectView;
import com.itsaverse.app.views.ScreenShotImageView;
import com.itsaverse.app.views.ScreenShotImageView.ClickableBox;

import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends Activity {

    private final Context CONTEXT = this;
    private ScreenShotImageView mBaseImage;
    private ScreenShotImageView mOverlayImage;
    private ClickableRectView mClickableOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        mBaseImage = (ScreenShotImageView) findViewById(R.id.image_viewer_image);
        mOverlayImage = (ScreenShotImageView) findViewById(R.id.image_viewer_overlay_image);
        mClickableOverlay = (ClickableRectView) findViewById(R.id.image_viewer_click_overlay);

        mClickableOverlay.setOnDoubleClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (OverlayControlService.getScreenshot() != null) {
            mBaseImage.setImageBitmap(OverlayControlService.getScreenshot());
            mOverlayImage.setImageBitmap(OverlayControlService.getScreenshot());

            List<ClickableBox> clickableBoxes = new ArrayList<ClickableBox>();
            for (final DataUtils.VerseReference ref : OverlayControlService.getVerseReferences()) {
                if (ref.posBoxes != null) {

                    View.OnClickListener verseClickListener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(CONTEXT, ref.text, Toast.LENGTH_LONG).show();
                        }
                    };

                    for (Box box : ref.posBoxes) {
                        clickableBoxes.add(new ClickableBox(box, verseClickListener));
                    }
                }
            }

            mOverlayImage.linkClickOverlay(mClickableOverlay);
            mOverlayImage.setClickableBoxes(clickableBoxes);

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
