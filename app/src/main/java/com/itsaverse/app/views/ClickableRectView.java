package com.itsaverse.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClickableRectView extends View {

    private static final int DOUBLE_TAP_DURATION = 500;

    private List<ClickableRect> mClickableRects;
    private OnClickListener mDoubleClickListener;
    private int mClickedRectIndex = -1;
    private Paint mOverlayPaint;
    private long lastOuterTap = -1;

    public ClickableRectView(Context context) {
        super(context);
        init();
    }

    public ClickableRectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClickableRectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mOverlayPaint = new Paint();
        mOverlayPaint.setStyle(Paint.Style.FILL);
        mOverlayPaint.setColor(Color.argb(64, 0, 0, 0));
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mClickedRectIndex > -1 && mClickableRects != null && mClickableRects.size() > mClickedRectIndex) {
            ClickableRect cRect = mClickableRects.get(mClickedRectIndex);

            canvas.drawRoundRect(cRect.getRect(), cRect.getCornerRadius(),
                    cRect.getCornerRadius(), mOverlayPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mClickableRects == null) return super.onTouchEvent(ev);

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {

            for (int i = 0; i < mClickableRects.size(); i++) {
                RectF rect = mClickableRects.get(i).getRect();
                if (rect.contains((int)ev.getX(), (int)ev.getY())) {
                    mClickedRectIndex = i;
                    invalidate();
                    return true;
                }
            }

            long currTime = new Date().getTime();
            if (lastOuterTap != -1 && currTime - lastOuterTap < DOUBLE_TAP_DURATION
                    && mDoubleClickListener != null) {

                mDoubleClickListener.onClick(this);
            }

            lastOuterTap = currTime;

        } else if (ev.getAction() == MotionEvent.ACTION_UP) {

            for (int i = 0; i < mClickableRects.size(); i++) {
                RectF rect = mClickableRects.get(i).getRect();
                if (rect.contains((int)ev.getX(), (int)ev.getY()) && i == mClickedRectIndex) {
                    mClickableRects.get(i).getClickListener().onClick(this);
                    break;
                }
            }

            mClickedRectIndex = -1;
            invalidate();

            return true;
        }

        return super.onTouchEvent(ev);
    }

    public void setClickableRects(List<ClickableRect> clickableRects) {
        mClickableRects = clickableRects;
    }

    public void addClickableRect(ClickableRect clickableRect) {
        if (mClickableRects == null) {
            mClickableRects = new ArrayList<ClickableRect>();
        }

        mClickableRects.add(clickableRect);
    }

    public void setOnDoubleClickListener(OnClickListener doubleClickListener) {
        mDoubleClickListener = doubleClickListener;
    }

    public static class ClickableRect {

        private RectF mRect;
        private int mCornerRadius;
        private OnClickListener mClickListener;

        public ClickableRect(RectF rect, int cornerRadius, OnClickListener clickListener) {
            mRect = rect;
            mCornerRadius = cornerRadius;
            mClickListener = clickListener;
        }

        public RectF getRect() {
            return mRect;
        }

        public int getCornerRadius() {
            return mCornerRadius;
        }

        public OnClickListener getClickListener() {
            return mClickListener;
        }
    }
}
