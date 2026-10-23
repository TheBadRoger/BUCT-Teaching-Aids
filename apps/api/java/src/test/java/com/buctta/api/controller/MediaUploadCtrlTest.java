package com.buctta.api.controller;

import com.buctta.api.config.MediaProperties;
import com.buctta.api.entities.Course;
import com.buctta.api.entities.MediaFile;
import com.buctta.api.entities.Organization;
import com.buctta.api.serviceimp.IMPL_MediaStorageService;
import com.buctta.api.serviceimp.IMPL_MediaUploadService;
import com.buctta.api.support.FakeRepositories;
import com.buctta.api.support.TestInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 上传接口的端到端行为（不含 Spring 容器，直接用内存仓储验证业务字段回填）。
 */
class MediaUploadCtrlTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 'J', 'F', 'I', 'F'};
    private static final byte[] MP4 = {0x00, 0x00, 0x00, 0x20, 'f', 't', 'y', 'p',
            'i', 's', 'o', 'm', 0x00, 0x00, 0x02, 0x00};

    @TempDir
    Path tempDir;

    private MockMvc mockMvc;
    private FakeRepositories.FakeMediaFileRepository mediaFiles;
    private FakeRepositories.FakeCourseReposit courses;
    private FakeRepositories.FakeOrganizationRepository organizations;

    @BeforeEach
    void setUp() {
        MediaProperties properties = TestInjector.mediaProperties(tempDir);
        IMPL_MediaStorageService storage = TestInjector.storageService(properties);
        mediaFiles = new FakeRepositories.FakeMediaFileRepository();
        courses = new FakeRepositories.FakeCourseReposit();
        organizations = new FakeRepositories.FakeOrganizationRepository();

        IMPL_MediaUploadService service = new IMPL_MediaUploadService();
        TestInjector.injectOrdered(service, storage, mediaFiles, courses, organizations, properties);

        MediaUploadCtrl controller = new MediaUploadCtrl();
        TestInjector.injectOrdered(controller, service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void uploadCourseCoverFillsCourseImage() throws Exception {
        Course course = new Course();
        course.setCourseName("数据结构");
        course.setCourseNumber("CS101");
        courses.save(course);

        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", JPEG);
        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("purpose", "COURSE_COVER")
                        .param("ownerId", String.valueOf(course.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.purpose").value("COURSE_COVER"))
                .andExpect(jsonPath("$.data.targetField").value("courseImage"))
                .andExpect(jsonPath("$.data.file.kind").value("IMAGE"))
                .andExpect(jsonPath("$.data.file.contentType").value("image/jpeg"));

        // 业务字段确实被写成了新的访问地址
        Course updated = courses.findById(course.getId()).orElseThrow();
        assertEquals("/api/media/" + mediaFiles.findAll().get(0).getId() + "/content",
                updated.getCourseImage());
    }

    @Test
    void uploadOrganizationImagesFillMatchingFields() throws Exception {
        Organization org = new Organization();
        org.setName("化学工程学院");
        organizations.save(org);
        String ownerId = String.valueOf(org.getId());

        performOrgUpload("ORG_BANNER", "banner.png", ownerId,
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        performOrgUpload("ORG_LOGO", "logo.gif", ownerId, "GIF89a".getBytes(StandardCharsets.UTF_8));
        performOrgUpload("ORG_HONOR_CERT", "cert.jpg", ownerId, JPEG);

        Organization updated = organizations.findById(org.getId()).orElseThrow();
        assertTrue(updated.getBannerUrl().startsWith("/api/media/"), "bannerUrl 应被回填");
        assertTrue(updated.getLogo().startsWith("/api/media/"), "logo 应被回填");
        assertTrue(updated.getHonorCertUrl().startsWith("/api/media/"), "honorCertUrl 应被回填");
    }

    @Test
    void invalidPurposeIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", JPEG);

        mockMvc.perform(multipart("/api/media/upload").file(file).param("purpose", "NOT_A_PURPOSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));

        mockMvc.perform(multipart("/api/media/upload").file(file))
                .andExpect(status().isBadRequest());

        assertEquals(0, mediaFiles.count(), "非法请求不应落库");
    }

    @Test
    void wrongKindIsRejectedWithBusinessCode() throws Exception {
        Course course = new Course();
        course.setCourseName("操作系统");
        course.setCourseNumber("CS102");
        courses.save(course);

        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", MP4);
        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("purpose", "COURSE_COVER")
                        .param("ownerId", String.valueOf(course.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4005));

        assertEquals(0, mediaFiles.count(), "校验失败不应落库");
    }

    @Test
    void nonNumericOwnerIdIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", JPEG);
        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("purpose", "COURSE_COVER")
                        .param("ownerId", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4002));
    }

    @Test
    void missingOwnerIsRejectedAndFileCleanedUp() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", JPEG);
        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("purpose", "COURSE_COVER")
                        .param("ownerId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4042));

        assertEquals(0, mediaFiles.count(), "回填失败不应落库");
        Path mediaRoot = tempDir.resolve("media");
        long leftover = java.nio.file.Files.walk(mediaRoot)
                .filter(java.nio.file.Files::isRegularFile)
                .count();
        assertEquals(0, leftover, "回填失败应清理已落盘文件");
    }

    @Test
    void missingOwnerIdIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", JPEG);
        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("purpose", "ORG_LOGO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));

        assertEquals(0, mediaFiles.count());
    }

    @Test
    void videoPurposeOnlyStoresFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "lesson.mp4", "video/mp4", MP4);
        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("purpose", "VIDEO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.targetField").isEmpty())
                .andExpect(jsonPath("$.data.file.kind").value("VIDEO"));

        assertEquals(1, mediaFiles.count());
        MediaFile saved = mediaFiles.findAll().get(0);
        assertEquals("video/mp4", saved.getContentType());
    }

    private void performOrgUpload(String purpose, String filename, String ownerId, byte[] content)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/*", content);
        mockMvc.perform(multipart("/api/media/upload")
                        .file(file)
                        .param("purpose", purpose)
                        .param("ownerId", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000));
    }
}
