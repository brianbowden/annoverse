package com.itsaverse.app.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ScreenShotImageView extends ImageView {

    private int mVerticalOffset = 0;
    private int mDheight = 0;

    public ScreenShotImageView(Context context) {
        super(context);
    }

    public ScreenShotImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScreenShotImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        mDheight = drawable.getIntrinsicHeight();
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Adjust vertical alignment to align with nav/status bars
        // The logic below was adapted from the
        // ImageView source code for CENTER_CROP.

        int vHeight = this.getMeasuredHeight();
        int existingOffset = Math.round((mDheight - vHeight) / 2.0f);

        Resources resources = getContext().getResources();
        int statusBarId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (statusBarId > 0) {
            mVerticalOffset = -resources.getDimensionPixelSize(statusBarId);
        }

        canvas.translate(0, existingOffset + mVerticalOffset);

        super.onDraw(canvas);
    }
}
