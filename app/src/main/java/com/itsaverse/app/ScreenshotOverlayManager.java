package com.itsaverse.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.googlecode.leptonica.android.Box;
import com.itsaverse.app.utils.DataUtils;
import com.itsaverse.app.utils.Utils;
import com.itsaverse.app.views.ClickableRectView;
import com.itsaverse.app.views.DispatchRelativeLayout;
import com.itsaverse.app.views.ScreenShotHighlightView;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ScreenshotOverlayManager {

    private final String TAG = "ScreenshotOverlay";

    private Context mContext;
    private DispatchRelativeLayout mScreenshotLayout;
    private ScreenShotHighlightView mBaseImage;
    private ScreenShotHighlightView mOverlayImage;
    private ClickableRectView mClickableOverlay;
    private RelativeLayout mPassageLayout;
    private WebView mPassageWebview;
    private ProgressBar mPassageLoadingIndicator;

    private boolean mIsPassageViewerExpanded;
    private String mPassageQuery;

    public ScreenshotOverlayManager(Context context) {
        mContext = context;
    }

    public void displayScreenshotOverlay() {
        final WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        Resources resources = mContext.getResources();
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

        mScreenshotLayout = (DispatchRelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.activity_image_viewer, null);
        mBaseImage = (ScreenShotHighlightView) mScreenshotLayout.findViewById(R.id.image_viewer_image);
        mOverlayImage = (ScreenShotHighlightView) mScreenshotLayout.findViewById(R.id.image_viewer_overlay_image);
        mClickableOverlay = (ClickableRectView) mScreenshotLayout.findViewById(R.id.image_viewer_click_overlay);
        mPassageLayout = (RelativeLayout) mScreenshotLayout.findViewById(R.id.image_viewer_passage_layout);
        mPassageWebview = (WebView) mScreenshotLayout.findViewById(R.id.image_viewer_passage_webview);
        mPassageLoadingIndicator = (ProgressBar) mScreenshotLayout.findViewById(R.id.image_viewer_passage_loading_indicator);

        mScreenshotLayout.setOnKeyDispatchListener(new DispatchRelativeLayout.OnKeyDispatchListener() {
            @Override
            public boolean onKeyDispatch(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        (event.getKeyCode() == KeyEvent.KEYCODE_BACK ||
                         event.getKeyCode() == KeyEvent.KEYCODE_HOME ||
                         event.getKeyCode() == KeyEvent.KEYCODE_MENU)) {

                    if (mContext instanceof OverlayControlService) {
                        ((OverlayControlService)mContext).stopScreenshotOverlay();
                    }
                    return false;
                }

                return true;
            }
        });

        mClickableOverlay.setOnDoubleClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //finish();
                //overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        mClickableOverlay.setOnOuterClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePassageViewer();
            }
        });

        if (OverlayControlService.getScreenshot() != null) {
            mBaseImage.setImageBitmap(OverlayControlService.getScreenshot());
            mOverlayImage.setImageBitmap(OverlayControlService.getScreenshot());

            List<ScreenShotHighlightView.ClickableBox> clickableBoxes = new ArrayList<ScreenShotHighlightView.ClickableBox>();
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
                        clickableBoxes.add(new ScreenShotHighlightView.ClickableBox(box, verseClickListener));
                    }
                }
            }

            mOverlayImage.linkClickOverlay(mClickableOverlay);
            mOverlayImage.setClickableBoxes(clickableBoxes);

        } else {
            Utils.makeCenterToast(mContext, "No screenshot to display!");
        }

        windowManager.addView(mScreenshotLayout, lp);


    }

    public void hideScreenshotOverlay() {
        final WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.removeView(mScreenshotLayout);
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
                Utils.makeCenterToast(mContext, "Unable to look up passage!");
                Log.e(TAG, "Unable to look up passage: " + retrofitError != null ? retrofitError.getMessage() : "");
                mPassageLoadingIndicator.setVisibility(View.INVISIBLE);
            }
        });
    }
}
