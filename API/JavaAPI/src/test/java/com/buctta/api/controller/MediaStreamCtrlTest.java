package com.buctta.api.controller;

import com.buctta.api.config.MediaProperties;
import com.buctta.api.entities.MediaFile;
import com.buctta.api.entities.MediaKind;
import com.buctta.api.serviceimp.IMPL_MediaStorageService;
import com.buctta.api.serviceimp.IMPL_MediaUploadService;
import com.buctta.api.support.FakeRepositories;
import com.buctta.api.support.TestInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 媒体读取接口：Range 分片（视频拖动播放）与响应头行为。
 */
class MediaStreamCtrlTest {

    /** 20 字节的 mp4 文件头 + 填充，内容按字节值可预测 */
    private static final byte[] MP4 = {0x00, 0x00, 0x00, 0x20, 'f', 't', 'y', 'p',
            'i', 's', 'o', 'm', 0x00, 0x00, 0x02, 0x00,
            0x10, 0x11, 0x12, 0x13};

    @TempDir
    Path tempDir;

    private MockMvc mockMvc;
    private FakeRepositories.FakeMediaFileRepository mediaFiles;
    private IMPL_MediaStorageService storage;
    private Long mediaId;

    @BeforeEach
    void setUp() throws Exception {
        MediaProperties properties = TestInjector.mediaProperties(tempDir);
        storage = TestInjector.storageService(properties);
        mediaFiles = new FakeRepositories.FakeMediaFileRepository();

        IMPL_MediaUploadService uploadService = new IMPL_MediaUploadService();
        TestInjector.injectOrdered(uploadService, storage, mediaFiles,
                new FakeRepositories.FakeCourseReposit(),
                new FakeRepositories.FakeOrganizationRepository(),
                properties);

        MediaStreamCtrl controller = new MediaStreamCtrl();
        TestInjector.injectOrdered(controller, uploadService, storage);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile file = new MockMultipartFile("file", "lesson.mp4", "video/mp4", MP4);
        var stored = storage.store(file, com.buctta.api.service.MediaPurpose.VIDEO);
        MediaFile saved = mediaFiles.save(storage.toEntity(stored));
        mediaId = saved.getId();
    }

    @Test
    void fullContentWithoutRange() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/media/{id}/content", mediaId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "video/mp4"))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(MP4.length)))
                .andReturn();

        assertArrayEquals(MP4, result.getResponse().getContentAsByteArray(),
                "无 Range 时应返回完整文件内容");
    }

    @Test
    void partialContentWithRange() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/media/{id}/content", mediaId)
                        .header(HttpHeaders.RANGE, "bytes=0-3"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE,
                        "bytes 0-3/" + MP4.length))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "4"))
                .andReturn();

        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x20},
                result.getResponse().getContentAsByteArray(),
                "206 响应应只包含请求区间的内容");
    }

    @Test
    void suffixAndOpenEndedRange() throws Exception {
        MvcResult suffix = mockMvc.perform(get("/api/media/{id}/content", mediaId)
                        .header(HttpHeaders.RANGE, "bytes=-4"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE,
                        "bytes 16-19/" + MP4.length))
                .andReturn();
        assertArrayEquals(new byte[]{0x10, 0x11, 0x12, 0x13},
                suffix.getResponse().getContentAsByteArray());

        MvcResult openEnded = mockMvc.perform(get("/api/media/{id}/content", mediaId)
                        .header(HttpHeaders.RANGE, "bytes=16-"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "4"))
                .andReturn();
        assertArrayEquals(new byte[]{0x10, 0x11, 0x12, 0x13},
                openEnded.getResponse().getContentAsByteArray());
    }

    @Test
    void unsatisfiableRangeReturns416() throws Exception {
        mockMvc.perform(get("/api/media/{id}/content", mediaId)
                        .header(HttpHeaders.RANGE, "bytes=9999-10000"))
                .andExpect(status().isRequestedRangeNotSatisfiable())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes */" + MP4.length));
    }

    @Test
    void headReturnsMetadataOnly() throws Exception {
        MvcResult result = mockMvc.perform(head("/api/media/{id}/content", mediaId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(MP4.length)))
                .andReturn();

        assertEquals(0, result.getResponse().getContentAsByteArray().length,
                "HEAD 不应返回内容体");
    }

    @Test
    void infoReturnsUsableUrl() throws Exception {
        mockMvc.perform(get("/api/media/{id}", mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.fileId").value(mediaId))
                .andExpect(jsonPath("$.data.kind").value("VIDEO"))
                .andExpect(jsonPath("$.data.contentType").value("video/mp4"))
                .andExpect(jsonPath("$.data.url").value("/api/media/" + mediaId + "/content"));
    }

    @Test
    void missingMediaIsHandled() throws Exception {
        mockMvc.perform(get("/api/media/{id}", 424242L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4042));

        // 记录在库但磁盘文件被删掉的情况
        MediaFile orphan = mediaFiles.save(new MediaFile("2026/01/missing.mp4",
                "missing.mp4", "video/mp4", 20L, MediaKind.VIDEO));
        mockMvc.perform(get("/api/media/{id}/content", orphan.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesRecordAndFile() throws Exception {
        MediaFile media = mediaFiles.findById(mediaId).orElseThrow();
        Path onDisk = storage.resolve(media.getStorageKey());

        mockMvc.perform(delete("/api/media/{id}", mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));

        assertEquals(0, mediaFiles.count(), "记录应被删除");
        org.junit.jupiter.api.Assertions.assertFalse(java.nio.file.Files.exists(onDisk),
                "磁盘文件应被删除");

        mockMvc.perform(delete("/api/media/{id}", mediaId))
                .andExpect(jsonPath("$.code").value(4042));
    }
}
