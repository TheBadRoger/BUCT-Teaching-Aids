package com.buctta.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {
    public static String[] spiltByLine(String text, String sign) {
        Pattern SEP = Pattern.compile(sign, Pattern.MULTILINE);
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        // 先按正则切开，再去掉首尾可能出现的空白块
        return Arrays.stream(SEP.split(text))
                .filter(s -> !s.trim().isEmpty())
                .toArray(String[]::new);
    }

    public static String[] ExtractInfo(String text, String rgx) {
        Pattern P = Pattern.compile(rgx, Pattern.MULTILINE);

        if (text == null) return new String[0];
        Matcher m = P.matcher(text);
        List<String> list = new ArrayList<>();
        while (m.find()) {
            list.add(m.group(1).trim());
        }
        return list.toArray(new String[0]);
    }
}
