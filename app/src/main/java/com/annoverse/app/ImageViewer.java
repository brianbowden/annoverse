package com.annoverse.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;

public class ImageViewer extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ImageView image = (ImageView) findViewById(R.id.image_viewer_image);

        if (getIntent() != null) {
            Uri uri = getIntent().getData();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(uri.getPath(), options);

            if (bitmap != null) {
                image.setImageBitmap(bitmap);
            } else {
                Log.e("IMAGEVIEWER", "BITMAP WAS NULL");
            }
        }
    }

}
