package com.itsaverse.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.googlecode.leptonica.android.Box;
import com.itsaverse.app.utils.DataUtils;
import com.itsaverse.app.utils.Utils;
import com.itsaverse.app.views.ClickableRectView;
import com.itsaverse.app.views.ScreenShotImageView;
import com.itsaverse.app.views.ScreenShotImageView.ClickableBox;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ImageViewerActivity extends Activity {

    private static final String TAG = "ImageViewActivity";

    private final Context CONTEXT = this;
    private ScreenShotImageView mBaseImage;
    private ScreenShotImageView mOverlayImage;
    private ClickableRectView mClickableOverlay;
    private RelativeLayout mPassageLayout;
    private WebView mPassageWebview;
    private ProgressBar mPassageLoadingIndicator;

    private boolean mIsPassageViewerExpanded;
    private String mPassageQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        mBaseImage = (ScreenShotImageView) findViewById(R.id.image_viewer_image);
        mOverlayImage = (ScreenShotImageView) findViewById(R.id.image_viewer_overlay_image);
        mClickableOverlay = (ClickableRectView) findViewById(R.id.image_viewer_click_overlay);
        mPassageLayout = (RelativeLayout) findViewById(R.id.image_viewer_passage_layout);
        mPassageWebview = (WebView) findViewById(R.id.image_viewer_passage_webview);
        mPassageLoadingIndicator = (ProgressBar) findViewById(R.id.image_viewer_passage_loading_indicator);

        mClickableOverlay.setOnDoubleClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        mClickableOverlay.setOnOuterClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePassageViewer();
            }
        });

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onResume() {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
                            mPassageQuery = ref.text;
                            showPassageViewer(true);
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
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showPassageViewer(final boolean loadPassage) {
        if (!mIsPassageViewerExpanded) {
            mPassageLayout.setVisibility(View.VISIBLE);
            mPassageWebview.loadData(null, null, null);

            TranslateAnimation transAnim = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF,
                    0.0f,
                    Animation.RELATIVE_TO_SELF,
                    0.0f,
                    Animation.RELATIVE_TO_SELF,
                    1.0f,
                    Animation.RELATIVE_TO_SELF,
                    0.0f);
            transAnim.setDuration(300);
            transAnim.setInterpolator(new DecelerateInterpolator());
            transAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    mIsPassageViewerExpanded = true;
                    if (loadPassage) requestPassage();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            mPassageLayout.startAnimation(transAnim);

        } else {
            requestPassage();
        }
    }

    private void closePassageViewer() {
        if (!mIsPassageViewerExpanded) return;

        mPassageLoadingIndicator.setVisibility(View.INVISIBLE);

        TranslateAnimation transAnim = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF,
                0.0f,
                Animation.RELATIVE_TO_SELF,
                0.0f,
                Animation.RELATIVE_TO_SELF,
                0.0f,
                Animation.RELATIVE_TO_SELF,
                1.0f);
        transAnim.setDuration(300);
        transAnim.setInterpolator(new DecelerateInterpolator());
        transAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mPassageLayout.setVisibility(View.GONE);
                mIsPassageViewerExpanded = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        mPassageLayout.startAnimation(transAnim);
    }

    private void requestPassage() {

        /**String encodedQuery = null;

        try {
            encodedQuery = URLEncoder.encode(mPassageQuery, Charset.defaultCharset().name());

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error encoding the passage: " + e != null ? e.getMessage() : "");
            return;
        }**/

        mPassageLoadingIndicator.setVisibility(View.VISIBLE);

        VerseFetcher.requestEsvPassage(mPassageQuery, new Callback<String>() {
            @Override
            public void success(String data, Response response) {
                mPassageWebview.loadDataWithBaseURL(null, data, "text/html", "utf-8", null);
                mPassageLoadingIndicator.setVisibility(View.INVISIBLE);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Utils.makeCenterToast(CONTEXT, "Unable to look up passage!");
                Log.e(TAG, "Unable to look up passage: " + retrofitError != null ? retrofitError.getMessage() : "");
                mPassageLoadingIndicator.setVisibility(View.INVISIBLE);
            }
        });
    }
}
