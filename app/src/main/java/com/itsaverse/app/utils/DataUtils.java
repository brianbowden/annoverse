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
import java.util.Arrays;
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

        fullText = fullText.replace("–", "-").replace("—", "-");

        List<VerseReference> refs = new ArrayList<VerseReference>();

        Pattern regex = Pattern.compile(String.format(VerseReference.REGEX));
        Matcher matcher = regex.matcher(fullText);

        while (matcher.find()) {
            // Get rid of this ridiculous long dash
            String candidate = matcher.group();
            String book = matcher.group(3).toLowerCase();

            if (VerseReference.BOOK_VARIATIONS.contains(book)) {
                refs.add(new VerseReference(candidate, matcher.start(), matcher.end()));
            }
        }

        refs.add(new VerseReference(fullText, 0, 0));

        return refs;
    }

    public static class VerseReference {

        public static final String REGEX = "(\\d|[iI]{1,3}+(\\s))?(\\w++|(song of \\w++))(\\s)(\\d[\\d\\s,;:\\-and\\&]*)";

        public static final List<String> BOOK_VARIATIONS = Arrays.asList(
                "amos",
                "am\\.",
                "chronicles",
                "chron",
                "chr",
                "daniel",
                "dan\\.",
                "dn",
                "deuteronomy",
                "deut",
                "dt",
                "ecclesiastes",
                "eccles",
                "eccl",
                "esther",
                "est\\.",
                "exodus",
                "exod",
                "ex\\.",
                "ezekiel",
                "ezek",
                "ez",
                "ezra",
                "ezr",
                "genesis",
                "gen",
                "gn",
                "habakkuk",
                "hab",
                "hb",
                "haggai",
                "hag.",
                "hg",
                "hosea",
                "hos",
                "isaiah",
                "isa",
                "is\\.",
                "jeremiah",
                "jer",
                "job",
                "jb",
                "joel",
                "jl",
                "jonah",
                "jon.",
                "joshua",
                "josh",
                "jo",
                "judges",
                "judg",
                "jgs",
                "kings",
                "kgs",
                "lamentations",
                "lam",
                "leviticus",
                "lev",
                "lv",
                "malachi",
                "mal",
                "micah",
                "mic",
                "mi",
                "nahum",
                "nah",
                "na",
                "nehemiah",
                "neh",
                "numbers",
                "num",
                "nm",
                "obadiah",
                "obad",
                "ob",
                "proverbs",
                "prov",
                "prv",
                "psalms",
                "psalm",
                "ps",
                "pss",
                "ruth",
                "ru",
                "samuel",
                "sam",
                "sm",
                "song of solomon",
                "song of sol",
                "song of songs",
                "song of sg",
                "zechariah",
                "zech",
                "zec",
                "zephaniah",
                "zeph",
                "zep",
                "acts",
                "colossians",
                "col",
                "corinthians",
                "cor",
                "ephesians",
                "eph",
                "galatians",
                "gal",
                "hebrews",
                "heb",
                "james",
                "jas",
                "john",
                "jn",
                "jude",
                "luke",
                "lk",
                "mark",
                "mk",
                "matthew",
                "matt",
                "mt",
                "peter",
                "pet",
                "pt",
                "philemon",
                "philem",
                "phlm",
                "philippians",
                "phil",
                "revelation",
                "rev",
                "rv",
                "romans",
                "rom",
                "thessalonians",
                "thess",
                "thes",
                "timothy",
                "tim",
                "tm",
                "titus",
                "ti"
        );

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
