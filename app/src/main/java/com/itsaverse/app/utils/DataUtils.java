package com.itsaverse.app.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataUtils {


    public static final String RELATIVE_DATA_PATH = "tessdata/eng.traineddata";
    public static final String DATA_PATH = Environment.getExternalStorageDirectory() + "/" + RELATIVE_DATA_PATH;

    private static final String TAG = "DataUtils";

    public static void copyDataIfRequired(Context context) {
        File dataFile = new File(DATA_PATH);
        if (!dataFile.exists()) {

            try {

                AssetManager assetManager = context.getAssets();
                InputStream in = assetManager.open(RELATIVE_DATA_PATH);
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                OutputStream out = new FileOutputStream(DATA_PATH);
                copyFile(in, out);
                in.close();
                out.flush();
                out.close();

            } catch (IOException e) {
                Log.e(TAG, "Failed to copy data: " + e.getMessage());
            }

        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
}
