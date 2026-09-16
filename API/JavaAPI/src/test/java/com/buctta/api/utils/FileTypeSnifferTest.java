package com.buctta.api.utils;

import com.buctta.api.entities.MediaKind;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTypeSnifferTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 'J', 'F', 'I', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a'};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00,
            'W', 'E', 'B', 'P'};
    private static final byte[] MP4 = {0x00, 0x00, 0x00, 0x20, 'f', 't', 'y', 'p',
            'i', 's', 'o', 'm', 0x00, 0x00, 0x02, 0x00};
    private static final byte[] MOV = {0x00, 0x00, 0x00, 0x14, 'f', 't', 'y', 'p',
            'q', 't', ' ', ' ', 0x00, 0x00, 0x02, 0x00};
    private static final byte[] WEBM = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3,
            0x01, 0x00, 0x00, 0x00};
    private static final byte[] AVI = {'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00,
            'A', 'V', 'I', ' '};

    @Test
    void detectsImages() {
        assertEquals("image/jpeg", sniff(JPEG, "jpg"));
        assertEquals("image/png", sniff(PNG, "png"));
        assertEquals("image/gif", sniff(GIF, "gif"));
        assertEquals("image/webp", sniff(WEBP, "webp"));
    }

    @Test
    void detectsVideos() {
        assertEquals("video/mp4", sniff(MP4, "mp4"));
        assertEquals("video/quicktime", sniff(MOV, "mov"));
        assertEquals("video/webm", sniff(WEBM, "webm"));
        assertEquals("video/x-matroska", sniff(WEBM, "mkv"));
    }

    @Test
    void unknownFtypBrand() {
        byte[] unknownBrand = {0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p',
                'x', 'x', 'x', 'x', 0x00, 0x00, 0x00, 0x00};
        assertEquals("video/quicktime", sniff(unknownBrand, "mov"));
        assertEquals("video/mp4", sniff(unknownBrand, "mp4"));
        assertEquals("video/mp4", sniffless(unknownBrand));
    }

    @Test
    void fakeExtensionIsRejected() {
        byte[] text = "this is definitely not a video".getBytes(StandardCharsets.UTF_8);
        assertEquals(FileTypeSniffer.TYPE_UNKNOWN, sniff(text, "mp4"));
    }

    @Test
    void degenerateInputs() {
        assertEquals(FileTypeSniffer.TYPE_UNKNOWN, FileTypeSniffer
                .resolveContentType(new byte[0], 0, "mp4"));
        assertEquals(FileTypeSniffer.TYPE_UNKNOWN, FileTypeSniffer
                .resolveContentType(new byte[]{1, 2, 3}, 3, "jpg"));
    }

    @Test
    void kindMapping() {
        assertEquals(MediaKind.IMAGE, FileTypeSniffer.kindOf("image/png"));
        assertEquals(MediaKind.VIDEO, FileTypeSniffer.kindOf("video/mp4"));
        assertNull(FileTypeSniffer.kindOf(FileTypeSniffer.TYPE_UNKNOWN));
        assertNull(FileTypeSniffer.kindOf(null));
    }

    @Test
    void extensionAndPlayability() {
        assertEquals("jpg", FileTypeSniffer.extensionOf("C:\\tmp\\a\\photo.JPG"));
        assertEquals("mp4", FileTypeSniffer.extensionOf("../../etc/evil.MP4"));
        assertEquals("", FileTypeSniffer.extensionOf("noextension"));
        assertEquals("", FileTypeSniffer.extensionOf(null));

        assertEquals("video/x-msvideo", sniff(AVI, "avi"));
        assertTrue(FileTypeSniffer.isLikelyNotPlayableInBrowser("video/x-msvideo"));
        assertTrue(FileTypeSniffer.isLikelyNotPlayableInBrowser("video/x-matroska"));
        assertFalse(FileTypeSniffer.isLikelyNotPlayableInBrowser("video/mp4"));
    }

    private String sniff(byte[] data, String extension) {
        return FileTypeSniffer.resolveContentType(data, data.length, extension);
    }

    /** 无扩展名时也要能判定 */
    private String sniffless(byte[] data) {
        return FileTypeSniffer.resolveContentType(data, data.length, null);
    }
}
