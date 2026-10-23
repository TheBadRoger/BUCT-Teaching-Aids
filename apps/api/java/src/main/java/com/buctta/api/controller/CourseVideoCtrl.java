package com.buctta.api.controller;

import com.buctta.api.entities.User;
import com.buctta.api.service.CourseVideoService;
import com.buctta.api.service.MediaValidationException;
import com.buctta.api.service.StudentCourseService;
import com.buctta.api.service.VideoProgressService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 课程视频接口（一门课程挂多个视频）。
 * <p>
 * 播放地址由上传接口返回的 {@code videoUrl} 给出，指向
 * {@code /api/media/{id}/content}，服务端支持 HTTP Range，
 * 前端把该地址交给 {@code <video src>} 即可拖动进度条与断点续播。
 */
@Slf4j
@RestController
@RequestMapping("/api/course/video")
@CrossOrigin
public class CourseVideoCtrl {

    @Resource
    private CourseVideoService courseVideoService;

    @Resource
    private StudentCourseService studentCourseService;

    @Resource
    private VideoProgressService videoProgressService;

    /**
     * 上传视频并挂到课程下。
     *
     * @param courseId        课程 ID
     * @param title           视频标题，省略时取原始文件名
     * @param description     简介
     * @param sortOrder       课程内顺序，省略时追加到末尾
     * @param durationSeconds 时长（秒）
     * @param video           视频文件，表单字段名 {@code video}
     * @param cover           封面图，表单字段名 {@code cover}，可省略
     */
    @PostMapping(path = "/upload", consumes = "multipart/form-data")
    public ApiResponse<CourseVideoService.CourseVideoDetail> upload(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "video") MultipartFile video,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        try {
            boolean withCover = cover != null && !cover.isEmpty();
            CourseVideoService.CourseVideoDetail detail = courseVideoService.uploadVideo(
                    courseId, title, description, sortOrder, durationSeconds, video, cover, withCover);
            return ApiResponse.ok(detail);
        }
        catch (MediaValidationException e) {
            return ApiResponse.fail(e.status(), e.getMessage());
        }
        catch (IOException e) {
            log.error("上传课程视频失败 courseId={}", courseId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, "视频保存失败: " + e.getMessage());
        }
    }

    /** 课程视频目录，按 sortOrder 升序 */
    @GetMapping("/list")
    public ApiResponse<List<CourseVideoService.CourseVideoDetail>> list(
            @RequestParam("courseId") Long courseId) {
        try {
            return ApiResponse.ok(courseVideoService.listByCourse(courseId));
        }
        catch (MediaValidationException e) {
            return ApiResponse.fail(e.status(), e.getMessage());
        }
        catch (RuntimeException e) {
            log.error("查询课程视频失败 courseId={}", courseId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 单个视频详情 */
    @GetMapping("/{videoId}")
    public ApiResponse<CourseVideoService.CourseVideoDetail> detail(@PathVariable Long videoId) {
        try {
            return ApiResponse.ok(courseVideoService.getDetail(videoId));
        }
        catch (MediaValidationException e) {
            return ApiResponse.fail(e.status(), e.getMessage());
        }
        catch (RuntimeException e) {
            log.error("查询视频详情失败 videoId={}", videoId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 修改视频信息；传了 {@code cover} 则替换封面并清理旧封面。
     */
    @PutMapping(path = "/{videoId}", consumes = "multipart/form-data")
    public ApiResponse<CourseVideoService.CourseVideoDetail> update(
            @PathVariable Long videoId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        try {
            boolean replaceCover = cover != null && !cover.isEmpty();
            CourseVideoService.CourseVideoDetail detail = courseVideoService.updateVideo(
                    videoId, title, description, sortOrder, durationSeconds, cover, replaceCover);
            return ApiResponse.ok(detail);
        }
        catch (MediaValidationException e) {
            return ApiResponse.fail(e.status(), e.getMessage());
        }
        catch (IOException e) {
            log.error("更新课程视频失败 videoId={}", videoId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, "封面保存失败: " + e.getMessage());
        }
    }

    /** 删除视频（视频文件与封面一并清理） */
    @DeleteMapping("/{videoId}")
    public ApiResponse<String> delete(@PathVariable Long videoId) {
        try {
            courseVideoService.deleteVideo(videoId);
            return ApiResponse.ok("视频已删除，ID: " + videoId);
        }
        catch (MediaValidationException e) {
            return ApiResponse.fail(e.status(), e.getMessage());
        }
        catch (RuntimeException e) {
            log.error("删除课程视频失败 videoId={}", videoId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 上报播放进度，用于断点续播。
     * <p>
     * {@code studentId} 省略时取当前登录用户绑定的学生身份（后台管理员登录下为 null，
     * 此时必须显式传 {@code studentId}）。播放到片尾附近会自动把课程标记为已观看。
     */
    @PostMapping("/progress")
    public ApiResponse<StudentCourseService.PlaybackProgressResult> reportProgress(
            @RequestParam("courseId") Long courseId,
            @RequestParam("positionSeconds") Integer positionSeconds,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "videoId", required = false) Long videoId,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds) {
        Long resolvedStudentId = studentId != null ? studentId : currentStudentId();
        if (resolvedStudentId == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING,
                    "studentId 省略时必须处于学生登录态");
        }
        StudentCourseService.PlaybackProgressResult result = studentCourseService
                .reportPlaybackProgress(resolvedStudentId, courseId, videoId,
                        positionSeconds, durationSeconds);
        if (result.success()) {
            return ApiResponse.ok(result);
        }
        return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, result.message());
    }

    /** 查询续播位置；无记录时返回 {@code data: null} */
    @GetMapping("/progress")
    public ApiResponse<StudentCourseService.PlaybackProgress> getProgress(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        Long resolvedStudentId = studentId != null ? studentId : currentStudentId();
        if (resolvedStudentId == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING,
                    "studentId 省略时必须处于学生登录态");
        }
        return ApiResponse.ok(studentCourseService.getPlaybackProgress(resolvedStudentId, courseId));
    }

    /**
     * 单个视频的学习记录（该视频看到哪、看了多久、是否看完）。
     * 与 {@code /progress} 的区别：{@code /progress} 是课程级续播指针，
     * 本接口是视频级明细。
     */
    @GetMapping("/{videoId}/learning-record")
    public ApiResponse<VideoProgressService.StudentVideoProgressView> videoLearningRecord(
            @PathVariable Long videoId,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        Long resolvedStudentId = studentId != null ? studentId : currentStudentId();
        if (resolvedStudentId == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING,
                    "studentId 省略时必须处于学生登录态");
        }
        try {
            // 无记录时返回 null 而不是报错：未开始学习的视频属正常情况
            return ApiResponse.ok(videoProgressService.resumePoint(resolvedStudentId, videoId));
        }
        catch (RuntimeException e) {
            log.error("查询视频学习记录失败 videoId={} studentId={}", videoId, resolvedStudentId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 某学生在某门课下的逐视频学习轨迹 */
    @GetMapping("/learning-records")
    public ApiResponse<List<VideoProgressService.StudentVideoProgressView>> courseLearningRecords(
            @RequestParam("courseId") Long courseId,
            @RequestParam(value = "studentId", required = false) Long studentId) {
        Long resolvedStudentId = studentId != null ? studentId : currentStudentId();
        if (resolvedStudentId == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING,
                    "studentId 省略时必须处于学生登录态");
        }
        try {
            return ApiResponse.ok(videoProgressService.courseTrail(resolvedStudentId, courseId));
        }
        catch (RuntimeException e) {
            log.error("查询课程学习轨迹失败 courseId={} studentId={}", courseId, resolvedStudentId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 某学生的整体学习记录摘要 */
    @GetMapping("/learning-summary")
    public ApiResponse<VideoProgressService.LearningRecordSummary> learningSummary(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        Long resolvedStudentId = studentId != null ? studentId : currentStudentId();
        if (resolvedStudentId == null) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING,
                    "studentId 省略时必须处于学生登录态");
        }
        try {
            return ApiResponse.ok(videoProgressService.summary(resolvedStudentId));
        }
        catch (RuntimeException e) {
            log.error("查询学习记录摘要失败 studentId={}", resolvedStudentId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /* ------------------------------------------------------------ */

    /** 当前登录用户绑定的学生 ID；未登录 / 非学生 / 机构账号登录时返回 null */
    private Long currentStudentId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user
                && user.getStudent() != null) {
            return user.getStudent().getId();
        }
        return null;
    }
}
