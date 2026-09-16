package com.buctta.api.serviceimp;

import com.buctta.api.dao.CourseReposit;
import com.buctta.api.dao.CourseVideoRepository;
import com.buctta.api.dao.StudentCourseReposit;
import com.buctta.api.dao.StudentReposit;
import com.buctta.api.entities.Course;
import com.buctta.api.entities.CourseVideo;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.StudentCourse;
import com.buctta.api.entities.StudentCourseId;
import com.buctta.api.service.StudentCourseService;
import com.buctta.api.service.VideoProgressService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class IMPL_StudentCourseService implements StudentCourseService {

    /** 播放到该比例即视为看完，避免片尾几秒误差导致永远看不完 */
    private static final double COMPLETE_THRESHOLD = 0.97;

    @Resource
    private StudentCourseReposit studentCourseRepository;

    @Resource
    private StudentReposit studentRepository;

    @Resource
    private CourseReposit courseListRepository;

    @Resource
    private CourseVideoRepository courseVideoRepository;

    @Resource
    private VideoProgressService videoProgressService;

    @Override
    public CourseOperationResult selectCourse(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return CourseOperationResult.fail("STUDENT_NOT_FOUND", "学生不存在，ID: " + studentId);
        }

        Course course = courseListRepository.findById(courseId).orElse(null);
        if (course == null) {
            return CourseOperationResult.fail("COURSE_NOT_FOUND", "课程不存在，ID: " + courseId);
        }

        if (studentCourseRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            return CourseOperationResult.fail("ALREADY_SELECTED", "已选择该课程");
        }

        try {
            StudentCourse studentCourse = new StudentCourse(student, course);
            studentCourse.setIsViewed(false);
            StudentCourse savedCourse = studentCourseRepository.save(studentCourse);
            return CourseOperationResult.success(savedCourse, "选课成功");
        }
        catch (Exception e) {
            return CourseOperationResult.fail("SELECT_FAILED", "选课失败: " + e.getMessage());
        }
    }

    @Override
    public CourseOperationResult updateViewedStatus(Long studentId, Long courseId, Boolean isViewed) {
        StudentCourse studentCourse = studentCourseRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);

        if (studentCourse == null) {
            return CourseOperationResult.fail("NOT_FOUND", "未找到选课记录");
        }

        try {
            studentCourse.setIsViewed(isViewed);
            StudentCourse updatedCourse = studentCourseRepository.save(studentCourse);
            return CourseOperationResult.success(updatedCourse, "状态更新成功");
        }
        catch (Exception e) {
            return CourseOperationResult.fail("UPDATE_FAILED", "更新失败: " + e.getMessage());
        }
    }

    @Override
    public Page<StudentCourse> getAllCourses(Long studentId, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            return Page.empty(pageable);
        }

        try {
            return studentCourseRepository.findByStudentId(studentId, pageable);
        }
        catch (Exception e) {
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<StudentCourse> getViewedCourses(Long studentId, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            return Page.empty(pageable);
        }

        try {
            return studentCourseRepository.findByStudentIdAndIsViewed(studentId, true, pageable);
        }
        catch (Exception e) {
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<StudentCourse> getNotViewedCourses(Long studentId, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            return Page.empty(pageable);
        }

        try {
            return studentCourseRepository.findByStudentIdAndIsViewed(studentId, false, pageable);
        }
        catch (Exception e) {
            return Page.empty(pageable);
        }
    }

    @Override
    public CourseOperationResult dropCourse(Long studentId, Long courseId) {
        StudentCourseId id = new StudentCourseId(studentId, courseId);
        if (!studentCourseRepository.existsById(id)) {
            return CourseOperationResult.fail("NOT_FOUND", "未找到选课记录");
        }

        try {
            studentCourseRepository.deleteById(id);
            return CourseOperationResult.success(null, "退课成功");
        }
        catch (Exception e) {
            return CourseOperationResult.fail("DROP_FAILED", "退课失败: " + e.getMessage());
        }
    }

    @Override
    public Page<StudentCourse> searchStudentCourses(String studentName, String courseName,
                                                    Boolean isViewed, Long studentId, Pageable pageable) {
        Specification<StudentCourse> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (studentName != null && !studentName.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("student").get("name"), "%" + studentName + "%"));
            }
            if (courseName != null && !courseName.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("course").get("courseName"), "%" + courseName + "%"));
            }
            if (isViewed != null) {
                predicates.add(criteriaBuilder.equal(root.get("isviewed"), isViewed));
            }
            if (studentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("student").get("id"), studentId));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return studentCourseRepository.findAll(specification, pageable);
    }

    @Override
    public PlaybackProgressResult reportPlaybackProgress(Long studentId, Long courseId, Long videoId,
                                                         Integer positionSeconds, Integer durationSeconds) {
        if (studentId == null || courseId == null) {
            return PlaybackProgressResult.fail("PARAM_MISSING", "studentId 与 courseId 不能为空");
        }
        if (positionSeconds == null || positionSeconds < 0) {
            return PlaybackProgressResult.fail("PARAM_INVALID", "positionSeconds 必须为非负整数");
        }

        // 校验视频确实属于该课程，避免把进度写到别的课的视频上
        if (videoId != null) {
            CourseVideo video = courseVideoRepository.findById(videoId).orElse(null);
            if (video == null || !video.getCourseId().equals(courseId)) {
                return PlaybackProgressResult.fail("VIDEO_NOT_IN_COURSE",
                        "视频 " + videoId + " 不属于课程 " + courseId);
            }
        }

        StudentCourse studentCourse = studentCourseRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);
        boolean existed = studentCourse != null;

        try {
            if (studentCourse == null) {
                // 尚未选课：自动补建选课记录，保证播放进度不丢
                Student student = studentRepository.findById(studentId).orElse(null);
                if (student == null) {
                    return PlaybackProgressResult.fail("STUDENT_NOT_FOUND", "学生不存在，ID: " + studentId);
                }
                Course course = courseListRepository.findById(courseId).orElse(null);
                if (course == null) {
                    return PlaybackProgressResult.fail("COURSE_NOT_FOUND", "课程不存在，ID: " + courseId);
                }
                studentCourse = new StudentCourse(student, course);
                studentCourse.setIsViewed(false);
            }

            if (videoId != null) {
                studentCourse.setLastVideoId(videoId);
            }
            // 课程级指针始终记录真实位置：此前在标记"已看完"时把它归零，
            // 导致用户看到 97% 离开后，回来续播会从头开始。
            studentCourse.setLastPositionSeconds(positionSeconds);

            // 播放到片尾附近则认为该课程已观看完成
            if (isFinished(positionSeconds, durationSeconds)) {
                studentCourse.setIsViewed(true);
            }

            StudentCourse saved = studentCourseRepository.save(studentCourse);

            // 同步写入逐视频学习轨迹（视频级明细），失败不影响课程级进度
            if (videoId != null) {
                VideoProgressService.ReportResult trail = videoProgressService
                        .report(studentId, courseId, videoId, positionSeconds, durationSeconds);
                if (!trail.success()) {
                    log.warn("逐视频学习记录写入失败 studentId={} videoId={}: {}",
                            studentId, videoId, trail.message());
                }
            }

            return PlaybackProgressResult.success(saved, existed,
                    existed ? "进度已更新" : "已自动补建选课记录并保存进度");
        }
        catch (Exception e) {
            log.error("上报播放进度失败 studentId={} courseId={}", studentId, courseId, e);
            return PlaybackProgressResult.fail("UPDATE_FAILED", "进度保存失败: " + e.getMessage());
        }
    }

    @Override
    public PlaybackProgress getPlaybackProgress(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) {
            return null;
        }
        return studentCourseRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(sc -> new PlaybackProgress(sc.getLastVideoId(),
                        sc.getLastPositionSeconds() == null ? 0 : sc.getLastPositionSeconds(),
                        sc.getIsViewed()))
                .orElse(null);
    }

    private boolean isFinished(Integer positionSeconds, Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0 || positionSeconds == null) {
            return false;
        }
        return positionSeconds >= durationSeconds * COMPLETE_THRESHOLD;
    }
}

