package com.buctta.api.controller;

import com.buctta.api.config.MediaProperties;
import com.buctta.api.entities.Course;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.StudentCourse;
import com.buctta.api.service.CourseVideoService;
import com.buctta.api.serviceimp.IMPL_CourseVideoService;
import com.buctta.api.serviceimp.IMPL_MediaStorageService;
import com.buctta.api.serviceimp.IMPL_StudentCourseService;
import com.buctta.api.serviceimp.IMPL_VideoProgressService;
import com.buctta.api.support.FakeRepositories;
import com.buctta.api.support.TestInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 课程视频接口：上传 / 目录 / 续播进度。
 */
class CourseVideoCtrlTest {

    private static final byte[] MP4 = {0x00, 0x00, 0x00, 0x20, 'f', 't', 'y', 'p',
            'i', 's', 'o', 'm', 0x00, 0x00, 0x02, 0x00};

    @TempDir
    Path tempDir;

    private MockMvc mockMvc;
    private Long courseId;
    private Long otherCourseId;
    private Long studentId;
    private Long videoId;
    private FakeRepositories.FakeCourseVideoRepository courseVideos;
    private FakeRepositories.FakeMediaFileRepository mediaFiles;
    private FakeRepositories.FakeCourseReposit courses;
    private FakeRepositories.FakeStudentRepository studentReposit;
    private FakeRepositories.FakeStudentCourseRepository studentCourseRepository;

    @BeforeEach
    void setUp() throws Exception {
        MediaProperties properties = TestInjector.mediaProperties(tempDir);
        IMPL_MediaStorageService storage = TestInjector.storageService(properties);
        mediaFiles = new FakeRepositories.FakeMediaFileRepository();
        courseVideos = new FakeRepositories.FakeCourseVideoRepository();
        courses = new FakeRepositories.FakeCourseReposit();
        studentReposit = new FakeRepositories.FakeStudentRepository();
        studentCourseRepository = new FakeRepositories.FakeStudentCourseRepository();

        IMPL_CourseVideoService courseVideoService = new IMPL_CourseVideoService();
        TestInjector.injectOrdered(courseVideoService, storage, mediaFiles, courseVideos, courses);

        IMPL_StudentCourseService studentCourseService = new IMPL_StudentCourseService();

        // 真实的逐视频轨迹服务：让本测试端到端覆盖"课程级指针 + 视频级明细"的联动
        IMPL_VideoProgressService videoProgressService = new IMPL_VideoProgressService();
        TestInjector.injectOrdered(videoProgressService,
                new FakeRepositories.FakeStudentVideoProgressRepository(), courseVideos);

        TestInjector.injectOrdered(studentCourseService, studentCourseRepository,
                studentReposit, courses, courseVideos, videoProgressService);

        CourseVideoCtrl controller = new CourseVideoCtrl();
        TestInjector.injectOrdered(controller, courseVideoService, studentCourseService,
                videoProgressService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        Course course = new Course();
        course.setCourseName("数据结构");
        course.setCourseNumber("CS101");
        courseId = courses.save(course).getId();

        Course other = new Course();
        other.setCourseName("操作系统");
        other.setCourseNumber("CS102");
        otherCourseId = courses.save(other).getId();

        Student student = new Student();
        student.setName("张三");
        student.setStudentNumber("2026001");
        studentId = studentReposit.save(student).getId();

        // 上传一个视频作为后续用例的素材
        CourseVideoService.CourseVideoDetail detail = courseVideoService.uploadVideo(
                courseId, "第一讲", null, null, 600,
                new MockMultipartFile("video", "l1.mp4", "video/mp4", MP4), null, false);
        videoId = detail.id();
    }

    @Test
    void uploadReturnsPlayableDetail() throws Exception {
        MockMultipartFile video = new MockMultipartFile("video", "l2.mp4", "video/mp4", MP4);
        mockMvc.perform(multipart("/api/course/video/upload")
                        .file(video)
                        .param("courseId", String.valueOf(courseId))
                        .param("title", "第二讲")
                        .param("durationSeconds", "600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.title").value("第二讲"))
                .andExpect(jsonPath("$.data.videoUrl").exists())
                .andExpect(jsonPath("$.data.playableInBrowser").value(true));
    }

    @Test
    void listReturnsAllVideosInOrder() throws Exception {
        mockMvc.perform(multipart("/api/course/video/upload")
                        .file(new MockMultipartFile("video", "l2.mp4", "video/mp4", MP4))
                        .param("courseId", String.valueOf(courseId))
                        .param("title", "第二讲"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/course/video/list").param("courseId", String.valueOf(courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("第一讲"))
                .andExpect(jsonPath("$.data[1].title").value("第二讲"));
    }

    @Test
    void listForMissingCourseReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/course/video/list").param("courseId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4042));
    }

    @Test
    void progressRoundTrip() throws Exception {
        mockMvc.perform(post("/api/course/video/progress")
                        .param("courseId", String.valueOf(courseId))
                        .param("studentId", String.valueOf(studentId))
                        .param("videoId", String.valueOf(videoId))
                        .param("positionSeconds", "120")
                        .param("durationSeconds", "600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.success").value(true));

        mockMvc.perform(get("/api/course/video/progress")
                        .param("courseId", String.valueOf(courseId))
                        .param("studentId", String.valueOf(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.videoId").value(videoId))
                .andExpect(jsonPath("$.data.positionSeconds").value(120))
                .andExpect(jsonPath("$.data.viewed").value(false));
    }

    @Test
    void finishingVideoMarksCourseViewedAndKeepsResumePoint() throws Exception {
        mockMvc.perform(post("/api/course/video/progress")
                        .param("courseId", String.valueOf(courseId))
                        .param("studentId", String.valueOf(studentId))
                        .param("videoId", String.valueOf(videoId))
                        .param("positionSeconds", "599")
                        .param("durationSeconds", "600"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/course/video/progress")
                        .param("courseId", String.valueOf(courseId))
                        .param("studentId", String.valueOf(studentId)))
                .andExpect(jsonPath("$.data.viewed").value(true))
                // 看完时保留真实位置：此前会归零，导致重看从 0 开始
                .andExpect(jsonPath("$.data.positionSeconds").value(599))
                .andExpect(jsonPath("$.data.videoId").value(videoId));
    }

    @Test
    void finishingVideoWritesPerVideoLearningRecord() throws Exception {
        mockMvc.perform(post("/api/course/video/progress")
                        .param("courseId", String.valueOf(courseId))
                        .param("studentId", String.valueOf(studentId))
                        .param("videoId", String.valueOf(videoId))
                        .param("positionSeconds", "300")
                        .param("durationSeconds", "600"))
                .andExpect(status().isOk());

        // 视频级明细应同步写入
        mockMvc.perform(get("/api/course/video/{videoId}/learning-record", videoId)
                        .param("studentId", String.valueOf(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.videoId").value(videoId))
                .andExpect(jsonPath("$.data.lastPosition").value(300))
                .andExpect(jsonPath("$.data.completed").value(false));

        // 课程级轨迹应能列出该视频
        mockMvc.perform(get("/api/course/video/learning-records")
                        .param("courseId", String.valueOf(courseId))
                        .param("studentId", String.valueOf(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // 摘要应统计到 1 个视频、300 秒
        mockMvc.perform(get("/api/course/video/learning-summary")
                        .param("studentId", String.valueOf(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalVideos").value(1))
                .andExpect(jsonPath("$.data.totalWatchedSeconds").value(300));
    }

    @Test
    void learningRecordForUntouchedVideo_returnsNullData() throws Exception {
        mockMvc.perform(get("/api/course/video/{videoId}/learning-record", videoId)
                        .param("studentId", String.valueOf(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void progressRejectsVideoFromAnotherCourse() throws Exception {
        var otherVideo = new com.buctta.api.entities.CourseVideo(otherCourseId, "别的课", 1L);
        otherVideo.setSortOrder(0);
        Long otherVideoId = courseVideos.save(otherVideo).getId();

        mockMvc.perform(post("/api/course/video/progress")
                        .param("courseId", String.valueOf(courseId))
                        .param("studentId", String.valueOf(studentId))
                        .param("videoId", String.valueOf(otherVideoId))
                        .param("positionSeconds", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5000));
    }

    @Test
    void progressWithoutIdentityIsRejected() throws Exception {
        mockMvc.perform(post("/api/course/video/progress")
                        .param("courseId", String.valueOf(courseId))
                        .param("positionSeconds", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void progressForUntouchedCourseIsNull() throws Exception {
        mockMvc.perform(get("/api/course/video/progress")
                        .param("courseId", String.valueOf(otherCourseId))
                        .param("studentId", String.valueOf(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
