package com.itsaverse.app.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import com.googlecode.leptonica.android.Box;
import com.googlecode.leptonica.android.Pixa;

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

    public static List<VerseReference> getVerseReferences(String utf8Text, Pixa words) {

        utf8Text = utf8Text.replace("–", "-").replace("—", "-");

        List<VerseReference> refs = new ArrayList<VerseReference>();
        List<Integer> wordPositions = getAllWordPositions(utf8Text);

        Pattern regex = Pattern.compile(String.format(VerseReference.REGEX));
        Matcher matcher = regex.matcher(utf8Text);

        while (matcher.find()) {
            // Get rid of this ridiculous long dash
            String candidate = matcher.group();
            String book = matcher.group(3).toLowerCase();

            if (VerseReference.BOOK_VARIATIONS.contains(book)) {
                VerseReference ref = new VerseReference(candidate, matcher.start(), matcher.end());
                setPositionBoxes(ref, wordPositions, words);
                refs.add(ref);
            }
        }

        return refs;
    }

    private static List<Integer> getAllWordPositions(String utf8text) {
        List<Integer> positions = new ArrayList<Integer>();

        boolean isSpace = false;
        boolean wordStarted = false;
        int wordStart = 0;

        for (int i = 0; i < utf8text.length(); i++) {
            char c = utf8text.charAt(i);
            isSpace = Character.isWhitespace(c);

            if (!isSpace && !wordStarted) {
                wordStarted = true;
                wordStart = i;

            } else if (isSpace && wordStarted) {
                positions.add(wordStart);
                wordStarted = false;
            }
        }

        return positions;
    }

    private static void setPositionBoxes(VerseReference ref, List<Integer> wordPositions, Pixa words) {
        if (ref == null || wordPositions == null || words == null) return;

        List<Box> positionBoxes = new ArrayList<Box>();

        int startX = 0;
        int startY = 0;
        int endX = 0;
        int height = 0;

        for (int position : wordPositions) {
            if (position < words.size()) {
                Box box = words.getBox(position);

                if (positionBoxes.size() == 0) {

                    startX = box.getX();
                    startY = box.getY();
                    endX = box.getX() + box.getWidth();
                    height = box.getHeight();

                } else if (positionBoxes.get(positionBoxes.size() - 1).getY() == startY) {

                    endX = box.getX();

                } else {

                    positionBoxes.add(new Box(startX, startY, endX - startX, height));

                    startX = box.getX();
                    startY = box.getY();
                    endX = box.getX() + box.getWidth();
                    height = box.getHeight();

                }

            }
        }

        positionBoxes.add(new Box(startX, startY, endX - startX, height));

        ref.setPosBoxes(positionBoxes);
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
        List<Box> posBoxes;

        public VerseReference(String text, int startIndex, int stopIndex) {
            this.text = text;
            this.startIndex = startIndex;
            this.stopIndex = stopIndex;
        }

        public void setPosBoxes(List<Box> posBoxes) {
            this.posBoxes = posBoxes;
        }
    }
}
