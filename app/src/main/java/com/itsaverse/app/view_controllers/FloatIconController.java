package com.itsaverse.app.view_controllers;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.itsaverse.app.R;
import com.itsaverse.app.utils.DimHelper;

public class FloatIconController {

    public static final int FLOAT_ICON_LAYOUT_WIDTH_DP = 48;
    public static final int FLOAT_ICON_LAYOUT_HEIGHT_DP = 48;

    private Context mContext;

    private View mFloatIconLayout;
    private WindowManager.LayoutParams mFloatIconLayoutParams;

    public FloatIconController(Context context) {
        mContext = context;
    }

    public View getFloatIconLayout() {
        if (mFloatIconLayout == null) {
            mFloatIconLayout = LayoutInflater.from(mContext).inflate(R.layout.float_icon_layout, null);
        }

        return mFloatIconLayout;
    }

    public WindowManager.LayoutParams getFloatIconLayoutParams() {
        if (mFloatIconLayoutParams == null) {
            mFloatIconLayoutParams = new WindowManager.LayoutParams(
                    DimHelper.get().convertDPToPixels(FLOAT_ICON_LAYOUT_WIDTH_DP),
                    DimHelper.get().convertDPToPixels(FLOAT_ICON_LAYOUT_HEIGHT_DP),
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
        }

        return mFloatIconLayoutParams;
    }

    public void attachFloatIcon() {

    }
}
