package com.buctta.api.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import java.util.Arrays;

class TextUtilsTest {

    @Test
    void splitByLine_withSeparator_returnsNonBlankParts() {
        String text = "========== 第 1/2 个文件 ==========\nA\n========== 第 2/2 个文件 ==========\nB\n";
        String[] parts = TextUtils.spiltByLine(text, "^========== 第 \\d+/\\d+ 个文件 ==========\\R");

        assertArrayEquals(new String[]{"A", "B"}, Arrays.stream(parts).map(String::trim).toArray(String[]::new));
    }

    @Test
    void splitByLine_withNullText_returnsEmptyArray() {
        assertArrayEquals(new String[0], TextUtils.spiltByLine(null, "x"));
    }

    @Test
    void extractInfo_returnsAllCapturedValues() {
        String text = "姓名：张三\n姓名：李四\n";
        String[] values = TextUtils.ExtractInfo(text, "^姓名：(.+)$");

        assertArrayEquals(new String[]{"张三", "李四"}, values);
    }
}
