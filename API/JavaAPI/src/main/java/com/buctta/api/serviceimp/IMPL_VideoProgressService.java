package com.buctta.api.serviceimp;

import com.buctta.api.dao.CourseVideoRepository;
import com.buctta.api.dao.StudentVideoProgressRepository;
import com.buctta.api.entities.CourseVideo;
import com.buctta.api.entities.StudentVideoProgress;
import com.buctta.api.service.VideoProgressService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class IMPL_VideoProgressService implements VideoProgressService {

    /** 播放到该比例即视为看完，留出片尾几秒的误差 */
    private static final double COMPLETE_THRESHOLD = 0.97;

    @Resource
    private StudentVideoProgressRepository progressRepository;

    @Resource
    private CourseVideoRepository courseVideoRepository;

    @Override
    @Transactional
    public ReportResult report(Long studentId, Long courseId, Long videoId,
                               Integer lastPosition, Integer durationSeconds) {
        if (studentId == null || courseId == null || videoId == null) {
            return ReportResult.fail("PARAM_MISSING", "studentId / courseId / videoId 均不能为空");
        }
        if (lastPosition == null || lastPosition < 0) {
            return ReportResult.fail("PARAM_MISSING", "lastPosition 必须为非负整数");
        }

        // 视频必须属于该课程，避免把轨迹写到别的课的视频上
        CourseVideo video = courseVideoRepository.findById(videoId).orElse(null);
        if (video == null || !video.getCourseId().equals(courseId)) {
            return ReportResult.fail("VIDEO_NOT_IN_COURSE",
                    "视频 " + videoId + " 不属于课程 " + courseId);
        }

        try {
            StudentVideoProgress progress = progressRepository
                    .findByStudentIdAndVideoId(studentId, videoId)
                    .orElseGet(() -> new StudentVideoProgress(studentId, courseId, videoId));

            // 累计观看秒数按"到达过的最大位置"计算：来回拖动进度条不会虚增，
            // 且不必维护区间集合。注意语义是"看到过的最大位置"，不是真实去重时长。
            int previousWatched = progress.getWatchedSeconds() == null ? 0 : progress.getWatchedSeconds();
            progress.setWatchedSeconds(Math.max(previousWatched, lastPosition));
            progress.setLastPosition(lastPosition);

            // 时长优先取上报值，其次取视频元数据中记录的值
            Integer effectiveDuration = durationSeconds != null
                    ? durationSeconds
                    : video.getDurationSeconds();
            if (isCompleted(lastPosition, effectiveDuration)) {
                progress.setCompleted(true);
            }

            StudentVideoProgress saved = progressRepository.save(progress);
            return ReportResult.ok(toView(saved));
        }
        catch (RuntimeException e) {
            // 学习记录失败不应影响播放本身，由调用方决定是否提示
            log.error("保存学习记录失败 studentId={} videoId={}", studentId, videoId, e);
            return ReportResult.fail("SAVE_FAILED", "学习记录保存失败: " + e.getMessage());
        }
    }

    @Override
    public StudentVideoProgressView resumePoint(Long studentId, Long videoId) {
        if (studentId == null || videoId == null) {
            return null;
        }
        return progressRepository.findByStudentIdAndVideoId(studentId, videoId)
                .map(this::toView)
                .orElse(null);
    }

    @Override
    public List<StudentVideoProgressView> courseTrail(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) {
            return List.of();
        }
        return progressRepository.findByStudentIdAndCourseId(studentId, courseId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public LearningRecordSummary summary(Long studentId) {
        if (studentId == null) {
            return new LearningRecordSummary(0, 0, 0);
        }
        List<StudentVideoProgress> records = progressRepository.findByStudentId(studentId);
        long completed = records.stream()
                .filter(r -> Boolean.TRUE.equals(r.getCompleted()))
                .count();
        long watched = records.stream()
                .mapToLong(r -> r.getWatchedSeconds() == null ? 0 : r.getWatchedSeconds())
                .sum();
        return new LearningRecordSummary(records.size(), completed, watched);
    }

    /* ------------------------------------------------------------ */

    private boolean isCompleted(Integer lastPosition, Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0 || lastPosition == null) {
            return false;
        }
        return lastPosition >= durationSeconds * COMPLETE_THRESHOLD;
    }

    private StudentVideoProgressView toView(StudentVideoProgress progress) {
        return new StudentVideoProgressView(progress.getVideoId(),
                progress.getCourseId(),
                progress.getWatchedSeconds(),
                progress.getLastPosition(),
                progress.getCompleted(),
                progress.getFirstWatchedAt() == null ? null : progress.getFirstWatchedAt().toString(),
                progress.getUpdatedTime() == null ? null : progress.getUpdatedTime().toString());
    }
}
