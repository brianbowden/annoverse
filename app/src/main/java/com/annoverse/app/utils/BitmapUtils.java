package com.annoverse.app.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;

/**
 * Created by bicxman on 2/27/14.
 */
public class BitmapUtils {

    public static Bitmap correctBitmapOrientation(Context ctx, Bitmap bitmap, String path, boolean fromCamera) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotate = 0;

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            if (rotate == 0 && fromCamera) {
                // Stupid (thanks to certain Android versions) hack to get orientation of a camera photo
                Cursor mediaCursor = ctx.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.ImageColumns.ORIENTATION, MediaStore.Images.ImageColumns.DATE_ADDED}, null, null, MediaStore.MediaColumns.DATE_ADDED + " DESC LIMIT 1");

                if (mediaCursor != null && mediaCursor.getCount() != 0) {
                    mediaCursor.moveToNext();
                    rotate = mediaCursor.getInt(0);
                }

                mediaCursor.close();
            }

            int centerX = bitmap.getWidth() / 2;
            int centerY = bitmap.getHeight() / 2;

            Matrix matrix = new Matrix();
            matrix.setRotate(rotate, centerX, centerY);

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        } catch (IOException e) {
            Log.e("Image Rotation Exception", "IO Error while rotating: " + e.getMessage());
            return null;
        } catch (OutOfMemoryError e) {
            if (bitmap != null) {
                bitmap.recycle();
            }
            Log.e("Image Rotation Exception", "OutOfMemoryError while Rotating: " + e.getMessage());
            return null;
        }
    }

}
