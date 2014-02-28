package com.annoverse.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.annoverse.app.utils.BitmapUtils;
import com.annoverse.app.utils.DataUtils;
import com.googlecode.tesseract.android.TessBaseAPI;

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
            BitmapUtils.correctBitmapOrientation(this, bitmap, uri.getPath(), false);

            TessBaseAPI tessApi = new TessBaseAPI();
            tessApi.init(Environment.getExternalStorageDirectory().getAbsolutePath(), "eng");
            tessApi.setImage(bitmap);

            final Dialog d = new Dialog(this);
            TextView randomText = new TextView(this);
            randomText.setText(tessApi.getUTF8Text());
            randomText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    d.dismiss();
                }
            });
            d.setContentView(randomText);
            d.setCanceledOnTouchOutside(true);
            d.show();

            if (bitmap != null) {
                image.setImageBitmap(bitmap);
            } else {
                Log.e("IMAGEVIEWER", "BITMAP WAS NULL");
            }
        }
    }

}
