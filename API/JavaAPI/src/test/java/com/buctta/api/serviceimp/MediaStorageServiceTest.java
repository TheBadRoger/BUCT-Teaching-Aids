package com.buctta.api.serviceimp;

import com.buctta.api.config.MediaProperties;
import com.buctta.api.entities.MediaKind;
import com.buctta.api.service.MediaPurpose;
import com.buctta.api.service.MediaStorageService;
import com.buctta.api.service.MediaValidationException;
import com.buctta.api.support.TestInjector;
import com.buctta.api.utils.BusinessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaStorageServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01, 0x02, 0x03};
    private static final byte[] MP4 = {0x00, 0x00, 0x00, 0x20, 'f', 't', 'y', 'p',
            'i', 's', 'o', 'm', 0x00, 0x00, 0x02, 0x00, 0x11, 0x22, 0x33, 0x44};

    @TempDir
    Path tempDir;

    private MediaProperties properties;
    private IMPL_MediaStorageService storage;

    @BeforeEach
    void setUp() {
        properties = TestInjector.mediaProperties(tempDir);
        storage = TestInjector.storageService(properties);
    }

    @Test
    void storesImage() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", JPEG);

        MediaStorageService.StoredFile stored = storage.store(file, MediaPurpose.COURSE_COVER);

        assertEquals("image/jpeg", stored.contentType());
        assertEquals(MediaKind.IMAGE, stored.kind());
        assertEquals(JPEG.length, stored.size());
        assertEquals("cover.jpg", stored.originalFilename());
        assertTrue(stored.storageKey().endsWith(".jpg"), "存储名应保留白名单扩展名");
        assertTrue(storage.exists(stored.storageKey()));

        // 内容必须逐字节一致，验证 PushbackStream 没有丢头
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        storage.writeTo(stored.storageKey(), 0, stored.size(), out);
        assertArrayEquals(JPEG, out.toByteArray());
    }

    @Test
    void storesVideoAndSlicesRange() throws IOException {
        MultipartFile file = new MockMultipartFile("video", "lesson.mp4", "video/mp4", MP4);
        MediaStorageService.StoredFile stored = storage.store(file, MediaPurpose.VIDEO);

        assertEquals("video/mp4", stored.contentType());
        assertEquals(MediaKind.VIDEO, stored.kind());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        storage.writeTo(stored.storageKey(), 4, 8, out);
        byte[] slice = out.toByteArray();
        assertEquals(8, slice.length);
        for (int i = 0; i < 8; i++) {
            assertEquals(MP4[4 + i], slice[i], "第 " + i + " 字节应来自原文件的 4+" + i);
        }

        // 请求长度超过剩余内容时按实际长度截断，不抛异常
        ByteArrayOutputStream over = new ByteArrayOutputStream();
        storage.writeTo(stored.storageKey(), 10, 9999, over);
        assertEquals(MP4.length - 10, over.toByteArray().length);
    }

    @Test
    void purposeKindMismatchIsRejected() {
        MultipartFile video = new MockMultipartFile("file", "a.mp4", "video/mp4", MP4);
        MultipartFile image = new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG);

        MediaValidationException forImage = assertThrows(MediaValidationException.class,
                () -> storage.store(video, MediaPurpose.COURSE_COVER));
        assertEquals(BusinessStatus.UPLOAD_CONTENT_TYPE_NOT_SUPPORTED, forImage.status());

        MediaValidationException forVideo = assertThrows(MediaValidationException.class,
                () -> storage.store(image, MediaPurpose.VIDEO));
        assertEquals(BusinessStatus.UPLOAD_CONTENT_TYPE_NOT_SUPPORTED, forVideo.status());
    }

    @Test
    void forgedExtensionIsRejected() {
        MultipartFile fake = new MockMultipartFile("file", "evil.mp4", "video/mp4",
                "not a video at all".getBytes(StandardCharsets.UTF_8));

        MediaValidationException e = assertThrows(MediaValidationException.class,
                () -> storage.store(fake, MediaPurpose.VIDEO));
        assertEquals(BusinessStatus.UPLOAD_CONTENT_TYPE_NOT_SUPPORTED, e.status());
    }

    @Test
    void nonWhitelistedExtensionIsRejected() {
        MultipartFile bmp = new MockMultipartFile("file", "logo.bmp", "image/bmp", JPEG);

        MediaValidationException e = assertThrows(MediaValidationException.class,
                () -> storage.store(bmp, MediaPurpose.ORG_LOGO));
        assertEquals(BusinessStatus.UPLOAD_CONTENT_TYPE_NOT_SUPPORTED, e.status());
    }

    @Test
    void emptyAndNamelessFilesAreRejected() {
        MultipartFile empty = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);
        assertEquals(BusinessStatus.RESOURCE_NOT_FOUND, assertThrows(MediaValidationException.class,
                () -> storage.store(empty, MediaPurpose.COURSE_COVER)).status());

        MultipartFile noExtension = new MockMultipartFile("file", "cover", "image/jpeg", JPEG);
        assertEquals(BusinessStatus.PARAM_FORMAT_ERROR, assertThrows(MediaValidationException.class,
                () -> storage.store(noExtension, MediaPurpose.COURSE_COVER)).status());
    }

    @Test
    void sizeLimitIsEnforcedPerPurpose() {
        properties.getUpload().setMaxImageSize(4);
        properties.getUpload().setMaxVideoSize(4);
        MultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", JPEG);

        MediaValidationException e = assertThrows(MediaValidationException.class,
                () -> storage.store(file, MediaPurpose.COURSE_COVER));
        assertEquals(BusinessStatus.UPLOAD_FILE_TOO_LARGE, e.status());
    }

    @Test
    void pathTraversalIsRejected() {
        assertThrows(MediaValidationException.class, () -> storage.resolve("../../etc/passwd"));
        assertThrows(MediaValidationException.class, () -> storage.resolve("a/../../b.mp4"));
        assertThrows(MediaValidationException.class, () -> storage.resolve("/etc/passwd"));
        assertThrows(MediaValidationException.class, () -> storage.resolve("C:\\Windows\\win.ini"));
        assertThrows(MediaValidationException.class, () -> storage.resolve("  "));
    }

    @Test
    void deleteIsIdempotent() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "cover.png", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2});
        MediaStorageService.StoredFile stored = storage.store(file, MediaPurpose.ORG_BANNER);

        Path onDisk = storage.resolve(stored.storageKey());
        assertTrue(Files.exists(onDisk));
        assertTrue(storage.delete(stored.storageKey()));
        assertFalse(Files.exists(onDisk));
        assertFalse(storage.delete(stored.storageKey()));
    }

    @Test
    void storedNameIsServerGenerated() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "../../../evil.jpg", "image/jpeg", JPEG);

        MediaStorageService.StoredFile stored = storage.store(file, MediaPurpose.COURSE_COVER);

        assertFalse(stored.storageKey().contains(".."), "存储路径不应包含 ..");
        assertTrue(stored.storageKey().matches("\\d{4}/\\d{2}/[0-9a-f]{32}\\.jpg"),
                "存储路径应为 yyyy/MM/uuid.ext: " + stored.storageKey());
        assertEquals("evil.jpg", stored.originalFilename(), "原始文件名只保留末段用于展示");
        assertNotNull(storage.resolve(stored.storageKey()));
    }
}
