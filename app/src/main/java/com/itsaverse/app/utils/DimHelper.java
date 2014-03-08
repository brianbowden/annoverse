package com.itsaverse.app.utils;


import android.content.Context;
import android.util.DisplayMetrics;

public class DimHelper {

    private static DimHelper mDimHelper;
    private Context mContext;

    private DimHelper() {}

    private DimHelper(Context context) {
        this.mContext = context;
    }

    public static void init(Context context) {
        mDimHelper = new DimHelper(context);
    }

    public static DimHelper get() {
        return mDimHelper;
    }

    public int getScreenWidthDP() {
        return Math.round((float) getDisplayMetrics().widthPixels / getDisplayMetrics().density);
    }

    public int getScreenHeightDP() {
        return Math.round((float) getDisplayMetrics().heightPixels / getDisplayMetrics().density);
    }

    public int getMaxDimensionDP() {
        return Math.max(getScreenWidthDP(), getScreenHeightDP());
    }

    public int getScreenWidthPixels() {
        return getDisplayMetrics().widthPixels;
    }

    public int getScreenHeightPixels() {
        return getDisplayMetrics().heightPixels;
    }

    public int getScreenDensity() {
        return getDisplayMetrics().densityDpi;
    }

    public int convertDPToPixels(int dp) {
        return (int) (dp * getDisplayMetrics().density + 0.5f);
    }

    public int convertDPToPixels(float dp) {
        return (int) (dp * getDisplayMetrics().density + 0.5f);
    }

    public int convertPixelsToDP(int pixels) {
        return (int) (pixels / getDisplayMetrics().density + 0.5f);
    }

    private DisplayMetrics getDisplayMetrics() {
        return mContext.getResources().getDisplayMetrics();
    }
}
