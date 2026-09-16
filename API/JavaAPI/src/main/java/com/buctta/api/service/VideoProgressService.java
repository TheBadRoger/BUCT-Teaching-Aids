package com.buctta.api.service;

import java.util.List;

/**
 * 逐视频学习记录（学习轨迹）。
 * <p>
 * 与 {@link StudentCourseService} 的分工：那边维护**课程级**续播指针（最近看到哪个视频、哪一秒），
 * 本服务维护**视频级**明细（每个视频看到哪、看了多久、是否看完），二者在同一事务内更新。
 */
public interface VideoProgressService {

    /**
     * 上报某视频的播放位置。
     *
     * @param studentId   学生 ID
     * @param courseId    课程 ID
     * @param videoId     视频 ID
     * @param lastPosition 当前播放位置（秒），非负
     * @param durationSeconds 视频总时长（秒），可为空；为空时无法判定"看完"
     * @return 上报后的该视频记录
     */
    ReportResult report(Long studentId, Long courseId, Long videoId,
                        Integer lastPosition, Integer durationSeconds);

    /** 某学生在某视频上的续播位置；无记录返回 null */
    StudentVideoProgressView resumePoint(Long studentId, Long videoId);

    /** 某学生在某门课下的逐视频轨迹，按视频 ID 升序 */
    List<StudentVideoProgressView> courseTrail(Long studentId, Long courseId);

    /** 某学生的完整学习记录摘要（跨课程） */
    LearningRecordSummary summary(Long studentId);

    /**
     * 单条视频轨迹视图
     *
     * @param watchedSeconds 累计有效观看秒数
     * @param completed      是否看完
     */
    record StudentVideoProgressView(Long videoId,
                                    Long courseId,
                                    Integer watchedSeconds,
                                    Integer lastPosition,
                                    Boolean completed,
                                    String firstWatchedAt,
                                    String updatedTime) {
    }

    /**
     * 学习记录摘要
     *
     * @param totalVideos     有记录的视频数
     * @param completedVideos 看完的视频数
     * @param totalWatchedSeconds 累计观看秒数
     */
    record LearningRecordSummary(long totalVideos, long completedVideos, long totalWatchedSeconds) {
    }

    /**
     * 上报结果
     *
     * @param errorCode 失败标识：PARAM_MISSING / VIDEO_NOT_IN_COURSE / SAVE_FAILED
     */
    record ReportResult(boolean success, StudentVideoProgressView progress,
                        String errorCode, String message) {
        public static ReportResult ok(StudentVideoProgressView progress) {
            return new ReportResult(true, progress, null, "学习记录已更新");
        }

        public static ReportResult fail(String errorCode, String message) {
            return new ReportResult(false, null, errorCode, message);
        }
    }
}
