package com.buctta.api.serviceimp;

import com.buctta.api.config.MediaProperties;
import com.buctta.api.entities.Course;
import com.buctta.api.entities.CourseVideo;
import com.buctta.api.service.CourseVideoService;
import com.buctta.api.service.MediaValidationException;
import com.buctta.api.support.FakeRepositories;
import com.buctta.api.support.TestInjector;
import com.buctta.api.utils.BusinessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课程多视频：目录顺序、封面处理、级联清理。
 */
class CourseVideoServiceTest {

    private static final byte[] MP4 = {0x00, 0x00, 0x00, 0x20, 'f', 't', 'y', 'p',
            'i', 's', 'o', 'm', 0x00, 0x00, 0x02, 0x00};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 'J', 'F', 'I', 'F'};

    @TempDir
    Path tempDir;

    private IMPL_CourseVideoService service;
    private IMPL_MediaStorageService storage;
    private FakeRepositories.FakeMediaFileRepository mediaFiles;
    private FakeRepositories.FakeCourseVideoRepository courseVideos;
    private FakeRepositories.FakeCourseReposit courses;
    private Long courseId;
    private Long otherCourseId;

    @BeforeEach
    void setUp() {
        MediaProperties properties = TestInjector.mediaProperties(tempDir);
        storage = TestInjector.storageService(properties);
        mediaFiles = new FakeRepositories.FakeMediaFileRepository();
        courseVideos = new FakeRepositories.FakeCourseVideoRepository();
        courses = new FakeRepositories.FakeCourseReposit();

        service = new IMPL_CourseVideoService();
        TestInjector.injectOrdered(service, storage, mediaFiles, courseVideos, courses);

        Course course = new Course();
        course.setCourseName("数据结构");
        course.setCourseNumber("CS101");
        courseId = courses.save(course).getId();

        Course other = new Course();
        other.setCourseName("操作系统");
        other.setCourseNumber("CS102");
        otherCourseId = courses.save(other).getId();
    }

    @Test
    void courseHoldsMultipleVideos() throws IOException {
        CourseVideoService.CourseVideoDetail first = upload("第一讲：绪论", null);
        CourseVideoService.CourseVideoDetail second = upload("第二讲：线性表", null);
        CourseVideoService.CourseVideoDetail third = upload("第三讲：栈与队列", null);

        assertEquals(0, first.sortOrder());
        assertEquals(1, second.sortOrder());
        assertEquals(2, third.sortOrder());

        List<CourseVideoService.CourseVideoDetail> list = service.listByCourse(courseId);
        assertEquals(3, list.size(), "同一课程下应挂载 3 个视频");
        assertEquals(List.of("第一讲：绪论", "第二讲：线性表", "第三讲：栈与队列"),
                list.stream().map(CourseVideoService.CourseVideoDetail::title).toList(),
                "目录应按 sortOrder 升序");
    }

    @Test
    void explicitSortOrderWins() throws IOException {
        upload("后置内容", 10);
        upload("前置内容", 1);

        List<CourseVideoService.CourseVideoDetail> list = service.listByCourse(courseId);
        assertEquals(List.of("前置内容", "后置内容"),
                list.stream().map(CourseVideoService.CourseVideoDetail::title).toList());
    }

    @Test
    void detailExposesPlayableUrls() throws IOException {
        CourseVideoService.CourseVideoDetail detail = upload("带封面的视频", 0, true);

        assertTrue(detail.videoUrl().matches("/api/media/\\d+/content"),
                "videoUrl 应指向媒体读取接口: " + detail.videoUrl());
        assertTrue(detail.coverUrl().matches("/api/media/\\d+/content"),
                "coverUrl 应指向媒体读取接口: " + detail.coverUrl());
        assertEquals("video/mp4", detail.videoContentType());
        assertEquals((long) MP4.length, detail.videoSize());
        assertTrue(detail.playableInBrowser(), "mp4 应可在浏览器直接播放");
        assertTrue(mediaFiles.findById(detail.coverFileId()).isPresent(), "封面应写入 media_file");
    }

    @Test
    void titleFallsBackToFilename() throws IOException {
        CourseVideoService.CourseVideoDetail detail = service.uploadVideo(courseId, "  ", null, null, null,
                multipart("第4讲-树.mp4", MP4), null, false);

        assertEquals("第4讲-树", detail.title());
    }

    @Test
    void missingCourseIsRejected() {
        MultipartFile video = multipart("a.mp4", MP4);

        MediaValidationException e = assertThrows(MediaValidationException.class,
                () -> service.uploadVideo(999999L, "x", null, null, null, video, null, false));
        assertEquals(BusinessStatus.RESOURCE_NOT_FOUND, e.status());
        assertEquals(0, mediaFiles.count(), "校验失败不应落库");
    }

    @Test
    void imageCannotBeUploadedAsVideo() {
        MultipartFile image = multipart("cover.jpg", JPEG);

        MediaValidationException e = assertThrows(MediaValidationException.class,
                () -> service.uploadVideo(courseId, "x", null, null, null, image, null, false));
        assertEquals(BusinessStatus.UPLOAD_CONTENT_TYPE_NOT_SUPPORTED, e.status());
        assertEquals(0, mediaFiles.count());
    }

    @Test
    void emptyCoverIsRejected() {
        MultipartFile video = multipart("a.mp4", MP4);
        MultipartFile emptyCover = new MockMultipartFile("cover", "c.jpg", "image/jpeg", new byte[0]);

        assertThrows(MediaValidationException.class,
                () -> service.uploadVideo(courseId, "x", null, null, null, video, emptyCover, true));
    }

    @Test
    void updateReplacesCoverAndCleansOldFile() throws IOException {
        CourseVideoService.CourseVideoDetail uploaded = upload("原视频", 0, true);
        Long oldCoverId = uploaded.coverFileId();
        Path oldCoverPath = storage.resolve(mediaFiles.findById(oldCoverId).orElseThrow()
                .getStorageKey());
        assertTrue(Files.exists(oldCoverPath));

        CourseVideoService.CourseVideoDetail updated = service.updateVideo(uploaded.id(),
                "改名后的视频", "新简介", 5, 360, multipart("new-cover.png",
                        new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}), true);

        assertEquals("改名后的视频", updated.title());
        assertEquals("新简介", updated.description());
        assertEquals(5, updated.sortOrder());
        assertEquals(360, updated.durationSeconds());
        assertFalse(oldCoverId.equals(updated.coverFileId()), "封面 ID 应被替换");
        assertTrue(mediaFiles.findById(oldCoverId).isEmpty(), "旧封面记录应被清理");
        assertFalse(Files.exists(oldCoverPath), "旧封面文件应被删除");
    }

    @Test
    void updateWithoutCoverKeepsExistingCover() throws IOException {
        CourseVideoService.CourseVideoDetail uploaded = upload("原视频", 0, true);

        CourseVideoService.CourseVideoDetail updated = service.updateVideo(uploaded.id(),
                "仅改标题", null, null, null, null, false);

        assertEquals("仅改标题", updated.title());
        assertEquals(uploaded.coverFileId(), updated.coverFileId(), "封面不应变化");
    }

    @Test
    void deleteCleansVideoAndCover() throws IOException {
        CourseVideoService.CourseVideoDetail detail = upload("待删除", 0, true);
        Path videoPath = storage.resolve(mediaFiles.findById(detail.videoFileId())
                .orElseThrow().getStorageKey());
        Path coverPath = storage.resolve(mediaFiles.findById(detail.coverFileId())
                .orElseThrow().getStorageKey());

        service.deleteVideo(detail.id());

        assertEquals(0, courseVideos.count(), "视频记录应被删除");
        assertEquals(0, mediaFiles.count(), "视频与封面文件记录都应被清理");
        assertFalse(Files.exists(videoPath), "视频文件应被删除");
        assertFalse(Files.exists(coverPath), "封面文件应被删除");
    }

    @Test
    void deleteMissingVideoIsRejected() {
        MediaValidationException e = assertThrows(MediaValidationException.class,
                () -> service.deleteVideo(123456L));
        assertEquals(BusinessStatus.RESOURCE_NOT_FOUND, e.status());
    }

    @Test
    void listBehaviour() throws IOException {
        assertEquals(List.of(), service.listByCourse(otherCourseId), "无视频的课程返回空列表");

        MediaValidationException e = assertThrows(MediaValidationException.class,
                () -> service.listByCourse(888888L));
        assertEquals(BusinessStatus.RESOURCE_NOT_FOUND, e.status());
    }

    @Test
    void getDetailReturnsEntityData() throws IOException {
        CourseVideoService.CourseVideoDetail uploaded = upload("详情视频", 3);

        CourseVideoService.CourseVideoDetail detail = service.getDetail(uploaded.id());
        assertEquals(uploaded.id(), detail.id());
        assertEquals(courseId, detail.courseId());
        assertEquals(3, detail.sortOrder());
        assertNull(detail.coverUrl(), "未上传封面时 coverUrl 为空");
    }

    /** 上传一个视频（无封面） */
    private CourseVideoService.CourseVideoDetail upload(String title, Integer sortOrder)
            throws IOException {
        return upload(title, sortOrder, false);
    }

    private CourseVideoService.CourseVideoDetail upload(String title, Integer sortOrder, boolean withCover)
            throws IOException {
        MultipartFile cover = withCover ? multipart("cover.jpg", JPEG) : null;
        return service.uploadVideo(courseId, title, null, sortOrder, null,
                multipart("lesson.mp4", MP4), cover, withCover);
    }

    private MultipartFile multipart(String filename, byte[] content) {
        String contentType = filename.endsWith(".jpg") ? "image/jpeg" : "video/mp4";
        return new MockMultipartFile("file", filename, contentType, content);
    }

    /** 断言 CourseVideo 实体确实挂到了正确的课程上 */
    @Test
    void videosAreScopedToCourse() throws IOException {
        upload("课程 A 的视频", 0);

        List<CourseVideo> forOther = courseVideos.findByCourseIdOrderBySortOrderAscIdAsc(otherCourseId);
        assertEquals(0, forOther.size(), "另一门课程不应看到该视频");
    }
}
