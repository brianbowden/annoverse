package com.itsaverse.app.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.RelativeLayout;

public class DispatchRelativeLayout extends RelativeLayout {

    private OnKeyDispatchListener mKeyDispatchListener;

    public DispatchRelativeLayout(Context context) {
        super(context);
    }

    public DispatchRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DispatchRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnKeyDispatchListener(OnKeyDispatchListener listener) {
        mKeyDispatchListener = listener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mKeyDispatchListener != null) {
            return mKeyDispatchListener.onKeyDispatch(event);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    public static interface OnKeyDispatchListener {
        public boolean onKeyDispatch(KeyEvent event);
    }
}
