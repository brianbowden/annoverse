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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                copy(in, out);
                in.close();
                out.flush();
                out.close();

            } catch (IOException e) {
                Log.e(TAG, "Failed to copy data: " + e.getMessage());
            }

        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public static List<VerseReference> getVerseReferences(String fullText) {
        List<VerseReference> refs = new ArrayList<VerseReference>();

        Pattern regex = Pattern.compile(VerseReference.REGEX);
        Matcher matcher = regex.matcher(fullText);

        while (matcher.find()) {
            refs.add(new VerseReference(matcher.group(), matcher.start(), matcher.end()));
        }

        return refs;
    }

    public static class VerseReference {

        public static final String REGEX = "\\b(((1|2|3|i|ii|iii)\\s)?(\\w+|(song of \\w+))\\.?)(\\s)((\\s?([,-–]|," +
                "? and|to)\\s?)?(?!([12] (sam|king|chron|cor|thes|tim|pet|john)))(\\d{1,3})((:)(((,|,? " +
                "and|to)\\s?)?(\\d{1,3}(?!:))(\\s?(-|–)\\s?\\d{1,3}(?!:))?)+)?)+\\b";

        public String text;
        public int startIndex;
        public int stopIndex;

        public VerseReference(String text, int startIndex, int stopIndex) {
            this.text = text;
            this.startIndex = startIndex;
            this.stopIndex = stopIndex;
        }
    }
}
