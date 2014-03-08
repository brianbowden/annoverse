package com.itsaverse.app;

import android.app.Application;

import com.itsaverse.app.utils.DimHelper;

public class ItsaverseApplication extends Application {

    @Override
    public void onCreate() {
        DimHelper.init(this);
    }

}
