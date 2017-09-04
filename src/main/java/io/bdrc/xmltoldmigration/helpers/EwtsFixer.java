package io.bdrc.xmltoldmigration.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import io.bdrc.xmltoldmigration.MigrationHelpers;

// a bit of code to 

public class EwtsFixer {
    public static final Map<String,Map<String,String>> replacements = new HashMap<>();
    static {
        init();
    }
    
    public static void init() {
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("ewts-fixes.txt");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;

        try {
            while((line = in.readLine()) != null) {
                final int colonIndex = line.indexOf(':');
                final String resId = line.substring(0, colonIndex);
                final String[] data = line.substring(colonIndex+3).split(":::");
                final Map<String,String> repForId = replacements.computeIfAbsent(resId, x -> new HashMap<>());
                repForId.put(data[0], data[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String getFixedStr(final String resId, final String src) {
        final String shortResId = resId.replaceAll("_\\d\\d\\d\\d$", "");
        final Map<String,String> data = replacements.get(shortResId);
        if (data == null)
            return null;
        return data.get(src);
    }
    
    public static String guessLang(String src) {
        int c = src.charAt(0);
        if (c >= 0x0F00 && c <= 0x0FFF)
            return "bo";
        if (c >= 0x2E00)
            return "zh";
        return "en";
    }
}
