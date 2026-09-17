package com.buctta.api.serviceimp;

import com.buctta.api.entities.Course;
import com.buctta.api.entities.CourseVideo;
import com.buctta.api.service.VideoProgressService;
import com.buctta.api.support.FakeRepositories;
import com.buctta.api.support.TestInjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 逐视频学习记录（学习轨迹）。
 */
class VideoProgressServiceTest {

    private static final Long STUDENT_ID = 12L;

    private IMPL_VideoProgressService service;
    private FakeRepositories.FakeStudentVideoProgressRepository progressRepo;
    private FakeRepositories.FakeCourseVideoRepository courseVideos;
    /** 共用一个课程仓储，否则每次新建都会从 ID 1 重新发号导致课程串号 */
    private FakeRepositories.FakeCourseReposit courses;

    private Long courseId;
    private Long otherCourseId;
    private Long videoId;
    private Long secondVideoId;

    @BeforeEach
    void setUp() {
        progressRepo = new FakeRepositories.FakeStudentVideoProgressRepository();
        courseVideos = new FakeRepositories.FakeCourseVideoRepository();
        courses = new FakeRepositories.FakeCourseReposit();
        service = new IMPL_VideoProgressService();
        TestInjector.injectOrdered(service, progressRepo, courseVideos);

        courseId = createCourse("数据结构", "CS101");
        otherCourseId = createCourse("操作系统", "CS102");
        videoId = createVideo(courseId, "第一讲", null);
        secondVideoId = createVideo(courseId, "第二讲", null);
    }

    @Test
    void report_createsRecordWithPositionAndWatchedSeconds() {
        VideoProgressService.ReportResult result = service.report(
                STUDENT_ID, courseId, videoId, 120, 600);

        assertTrue(result.success());
        assertEquals(120, result.progress().lastPosition());
        assertEquals(120, result.progress().watchedSeconds());
        assertEquals(false, result.progress().completed());
        assertEquals(1, progressRepo.count(), "同一视频只应有一条记录");
    }

    @Test
    void report_twice_updatesSameRecordInsteadOfDuplicating() {
        service.report(STUDENT_ID, courseId, videoId, 100, 600);
        service.report(STUDENT_ID, courseId, videoId, 200, 600);

        assertEquals(1, progressRepo.count());
        assertEquals(200, service.resumePoint(STUDENT_ID, videoId).lastPosition());
    }

    @Test
    void watchedSeconds_neverDecreasesWhenRewinding() {
        service.report(STUDENT_ID, courseId, videoId, 300, 600);
        // 用户往回拖到 50 秒
        VideoProgressService.ReportResult result = service.report(
                STUDENT_ID, courseId, videoId, 50, 600);

        assertEquals(50, result.progress().lastPosition(), "续播位置应回到真实位置");
        assertEquals(300, result.progress().watchedSeconds(),
                "累计观看秒数取到达过的最大位置，不应因回拖而减少");
    }

    @Test
    void report_marksCompletedNearTheEnd() {
        service.report(STUDENT_ID, courseId, videoId, 500, 600);
        assertFalse(service.resumePoint(STUDENT_ID, videoId).completed(), "约 83% 不应算看完");

        VideoProgressService.ReportResult result = service.report(
                STUDENT_ID, courseId, videoId, 590, 600);
        assertTrue(result.progress().completed(), "约 98% 应判定看完");
    }

    @Test
    void completedFlag_isStickyAcrossLaterRewatch() {
        service.report(STUDENT_ID, courseId, videoId, 600, 600);
        assertTrue(service.resumePoint(STUDENT_ID, videoId).completed());

        // 重看时从中间开始，不应把"已看完"改回未完成
        service.report(STUDENT_ID, courseId, videoId, 30, 600);

        assertTrue(service.resumePoint(STUDENT_ID, videoId).completed(),
                "重看不应取消已完成的标记");
    }

    @Test
    void durationFallsBackToVideoMetadata() {
        Long videoWithDuration = createVideo(courseId, "带时长", 600);

        // 上报不带时长，改由视频元数据提供
        VideoProgressService.ReportResult result = service.report(
                STUDENT_ID, courseId, videoWithDuration, 600, null);

        assertTrue(result.progress().completed(), "应使用视频元数据中的时长判定完成");
    }

    @Test
    void withoutDuration_completionStaysFalse() {
        VideoProgressService.ReportResult result = service.report(
                STUDENT_ID, courseId, videoId, 99999, null);

        assertFalse(result.progress().completed(), "无时长信息时无法判定看完");
    }

    @Test
    void report_rejectsVideoFromAnotherCourse() {
        Long foreignVideo = createVideo(otherCourseId, "别的课的视频", null);

        VideoProgressService.ReportResult result = service.report(
                STUDENT_ID, courseId, foreignVideo, 10, 600);

        assertFalse(result.success());
        assertEquals("VIDEO_NOT_IN_COURSE", result.errorCode());
        assertEquals(0, progressRepo.count(), "校验失败不应落库");
    }

    @Test
    void report_rejectsMissingOrNegativeParams() {
        assertFalse(service.report(null, courseId, videoId, 10, 600).success());
        assertFalse(service.report(STUDENT_ID, null, videoId, 10, 600).success());
        assertFalse(service.report(STUDENT_ID, courseId, null, 10, 600).success());
        assertFalse(service.report(STUDENT_ID, courseId, videoId, -1, 600).success());
        assertFalse(service.report(STUDENT_ID, courseId, videoId, null, 600).success());
    }

    @Test
    void courseTrail_returnsOnlyThatCourseRecords() {
        service.report(STUDENT_ID, courseId, videoId, 100, 600);
        service.report(STUDENT_ID, courseId, secondVideoId, 700, 600);
        Long foreignVideo = createVideo(otherCourseId, "别的课", null);
        service.report(STUDENT_ID, otherCourseId, foreignVideo, 50, 600);

        List<VideoProgressService.StudentVideoProgressView> trail =
                service.courseTrail(STUDENT_ID, courseId);

        assertEquals(2, trail.size(), "只应返回该课程下的视频轨迹");
        assertTrue(trail.stream().anyMatch(v -> v.videoId().equals(videoId)));
        assertTrue(trail.stream().anyMatch(v -> v.videoId().equals(secondVideoId)));
    }

    @Test
    void summary_aggregatesAcrossCourses() {
        service.report(STUDENT_ID, courseId, videoId, 600, 600);       // 看完
        service.report(STUDENT_ID, courseId, secondVideoId, 100, 600); // 未看完
        Long foreignVideo = createVideo(otherCourseId, "别的课", null);
        service.report(STUDENT_ID, otherCourseId, foreignVideo, 600, 600); // 看完

        VideoProgressService.LearningRecordSummary summary = service.summary(STUDENT_ID);

        assertEquals(3, summary.totalVideos(), "应跨课程统计有记录的视频数");
        assertEquals(2, summary.completedVideos());
        assertEquals(1300, summary.totalWatchedSeconds());
    }

    @Test
    void summary_isolatedPerStudent() {
        service.report(STUDENT_ID, courseId, videoId, 600, 600);

        VideoProgressService.LearningRecordSummary other = service.summary(999L);

        assertEquals(0, other.totalVideos());
        assertEquals(0, other.totalWatchedSeconds());
    }

    @Test
    void resumePoint_returnsNullWhenNoRecord() {
        assertNull(service.resumePoint(STUDENT_ID, videoId));
        assertNull(service.resumePoint(STUDENT_ID, null));
        assertNull(service.resumePoint(null, videoId));
    }

    @Test
    void emptyInputs_returnEmptyRatherThanThrowing() {
        assertEquals(0, service.courseTrail(STUDENT_ID, null).size());
        assertEquals(0, service.courseTrail(null, courseId).size());
        assertEquals(0, service.summary(null).totalVideos());
    }

    /* ------------------------------------------------------------ */

    private Long createCourse(String name, String number) {
        Course course = new Course();
        course.setCourseName(name);
        course.setCourseNumber(number);
        return courses.save(course).getId();
    }

    private Long createVideo(Long courseId, String title, Integer durationSeconds) {
        CourseVideo video = new CourseVideo(courseId, title, 1L);
        video.setSortOrder(0);
        video.setDurationSeconds(durationSeconds);
        return courseVideos.save(video).getId();
    }
}
