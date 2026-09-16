package com.buctta.api.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ByteRangeTest {

    private static final long SIZE = 1000L;

    @Test
    void normalRange() {
        ByteRange range = ByteRange.parse("bytes=0-499", SIZE);
        assertEquals(0L, range.start());
        assertEquals(499L, range.end());
        assertEquals(500L, range.length());
        assertEquals("bytes 0-499/1000", range.contentRange(SIZE));
    }

    @Test
    void openEndedRange() {
        ByteRange range = ByteRange.parse("bytes=500-", SIZE);
        assertEquals(500L, range.start());
        assertEquals(999L, range.end());
        assertEquals(500L, range.length());
    }

    @Test
    void suffixRange() {
        ByteRange range = ByteRange.parse("bytes=-100", SIZE);
        assertEquals(900L, range.start());
        assertEquals(999L, range.end());
        assertEquals(100L, range.length());
    }

    @Test
    void suffixLargerThanFile() {
        ByteRange range = ByteRange.parse("bytes=-5000", SIZE);
        assertEquals(0L, range.start());
        assertEquals(999L, range.end());
        assertEquals(1000L, range.length());
    }

    @Test
    void endBeyondFileIsClamped() {
        ByteRange range = ByteRange.parse("bytes=990-99999", SIZE);
        assertEquals(990L, range.start());
        assertEquals(999L, range.end());
        assertEquals(10L, range.length());
    }

    @Test
    void unsatisfiableRanges() {
        assertNull(ByteRange.parse("bytes=1000-", SIZE), "起点等于文件大小应不可满足");
        assertNull(ByteRange.parse("bytes=2000-3000", SIZE), "起点越界应不可满足");
        assertNull(ByteRange.parse("bytes=500-100", SIZE), "end 小于 start 应不可满足");
        assertNull(ByteRange.parse("bytes=0-99,200-299", SIZE), "多区间暂不支持");
        assertNull(ByteRange.parse("bytes=abc-def", SIZE), "非法数字");
        assertNull(ByteRange.parse("items=0-10", SIZE), "非 bytes 单位");
        assertNull(ByteRange.parse("bytes=", SIZE), "空区间");
        assertNull(ByteRange.parse("bytes=-", SIZE), "仅一个横线");
    }

    @Test
    void degenerateInputs() {
        assertNull(ByteRange.parse("bytes=0-10", 0L), "空文件不可满足");
        assertNull(ByteRange.parse(null, SIZE), "没有 Range 头");
    }
}
