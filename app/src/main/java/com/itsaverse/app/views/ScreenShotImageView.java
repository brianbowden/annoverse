package com.itsaverse.app.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.googlecode.leptonica.android.Box;
import com.itsaverse.app.OverlayControlService;
import com.itsaverse.app.utils.DimHelper;

import java.util.List;

public class ScreenShotImageView extends ImageView {

    private final String TAG = "ScreenShotImageView";

    private Context mContext;
    private int mVerticalOffset = 0;
    private int mDheight = 0;
    private boolean hasVirtualButtons;
    private List<ClickableBox> mClickableBoxes;
    private ClickableRectView mClickOverlay;

    public ScreenShotImageView(Context context) {
        super(context);
        init(context);
    }

    public ScreenShotImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ScreenShotImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
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

        mVerticalOffset = hasVirtualButtons ? -(DimHelper.get().getScreenHeightPixels() - vHeight) : 0;

        canvas.translate(0, existingOffset + mVerticalOffset);

        if (mClickableBoxes != null) {
            Path clipPath = new Path();

            for (ClickableBox clickableBox : mClickableBoxes) {
                Box box = clickableBox.getBox();
                int roundedEdge = Math.round(0.3f * box.getHeight());
                int vertFix = Math.round(0.1f * box.getHeight()); // inexplicable
                RectF rect = new RectF(box.getX(), box.getY() - box.getHeight() - roundedEdge - vertFix + mVerticalOffset,
                        box.getX() + box.getWidth(), box.getY() + roundedEdge - vertFix + mVerticalOffset);

                clipPath.addRoundRect(rect, roundedEdge, roundedEdge, Path.Direction.CW);

                Log.e(TAG, "1-- Left: " + rect.left + " Top: " + rect.top +
                        " Right: " + rect.right + " Bottom: " + rect.bottom);


                if (mClickOverlay != null) {
                    rect.top = rect.top + existingOffset + mVerticalOffset;
                    rect.bottom = rect.bottom + existingOffset + mVerticalOffset;
                    mClickOverlay.addClickableRect(new ClickableRectView.ClickableRect(rect,
                            roundedEdge, clickableBox.getClickListener()));
                }
            }

            canvas.drawARGB(0, 0, 0, 0);
            canvas.clipPath(clipPath);
        }

        super.onDraw(canvas);
    }

    public void setClickableBoxes(List<ClickableBox> boxes) {
        mClickableBoxes = boxes;
    }

    public void linkClickOverlay(ClickableRectView clickOverlay) {
        mClickOverlay = clickOverlay;
    }

    private void init(Context context) {
        mContext = context;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        hasVirtualButtons = !ViewConfiguration.get(getContext()).hasPermanentMenuKey();
    }

    public static class ClickableBox {

        private Box mBox;
        private OnClickListener mClickListener;

        public ClickableBox(Box box, OnClickListener clickListener) {
            mBox = box;
            mClickListener = clickListener;
        }

        public Box getBox() {
            return mBox;
        }

        public OnClickListener getClickListener() {
            return mClickListener;
        }
    }
}
