package com.buctta.api.serviceimp;

import com.buctta.api.dao.NoteReposit;
import com.buctta.api.dao.StudentCourseReposit;
import com.buctta.api.dao.StudentReposit;
import com.buctta.api.dao.TeachingMaterialRepository;
import com.buctta.api.dao.TeacherReposit;
import com.buctta.api.dto.LearningStatsDTO;
import com.buctta.api.entities.Course;
import com.buctta.api.entities.Note;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.StudentCourse;
import com.buctta.api.entities.Teacher;
import com.buctta.api.service.FollowService;
import com.buctta.api.service.LearningStatsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class IMPL_LearningStatsService implements LearningStatsService {

    /** 参与聚合的笔记上限：个人笔记量远小于此，超出部分不影响趋势判断 */
    private static final int NOTE_SCAN_LIMIT = 500;

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 成长阶段定义（后端唯一口径）。
     * <p>
     * 顺序即等级高低，达标判定为「全部条件满足」。
     */
    private enum Stage {
        START("start", "启程", "加入第一门课程", 1, 0),
        STEADY("steady", "稳固", "完成 3 门课程并沉淀 3 篇笔记", 3, 3),
        DEEP("deep", "深耕", "完成 6 门课程，笔记获 10 次点赞", 6, 10),
        EXPERT("expert", "领航", "完成 10 门课程并保持 15 篇以上笔记", 10, 15);

        final String key;
        final String title;
        final String description;
        /** 需要的已完成课程数 */
        final long requiredViewed;
        /** 需要的笔记数（START 阶段为 0，点赞数另算） */
        final long requiredNotes;

        Stage(String key, String title, String description, long requiredViewed, long requiredNotes) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.requiredViewed = requiredViewed;
            this.requiredNotes = requiredNotes;
        }
    }

    @Resource
    private StudentReposit studentReposit;

    @Resource
    private TeacherReposit teacherReposit;

    @Resource
    private StudentCourseReposit studentCourseReposit;

    @Resource
    private NoteReposit noteReposit;

    @Resource
    private TeachingMaterialRepository teachingMaterialRepository;

    @Resource
    private FollowService followService;

    @Override
    public LearningStatsDTO forStudent(Long studentId) {
        if (studentId == null) {
            return null;
        }
        Student student = studentReposit.findById(studentId).orElse(null);
        if (student == null) {
            return null;
        }

        List<StudentCourse> enrollments = studentCourseReposit.findByStudentId(studentId);
        long enrolled = enrollments.size();
        long completed = enrollments.stream()
                .filter(sc -> Boolean.TRUE.equals(sc.getIsViewed()))
                .count();

        List<Note> notes = loadNotes(studentId);
        LearningStatsDTO.NoteMetrics noteMetrics = noteMetrics(notes);
        LearningStatsDTO.FollowMetrics followMetrics = followMetrics(student.getUser() == null
                ? null
                : student.getUser().getId());

        LearningStatsDTO.CourseMetrics courseMetrics = new LearningStatsDTO.CourseMetrics(
                enrolled, completed, enrolled == 0 ? 0d : (double) completed / enrolled);

        return new LearningStatsDTO(
                "STUDENT",
                studentId,
                null,
                student.getName(),
                courseMetrics,
                noteMetrics,
                followMetrics,
                growthStage(completed, noteMetrics.total(), noteMetrics.totalLikes()),
                tagDistribution(enrollments),
                activityByMonth(notes),
                notesByCourse(notes));
    }

    @Override
    public LearningStatsDTO forTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        Teacher teacher = teacherReposit.findById(teacherId).orElse(null);
        if (teacher == null) {
            return null;
        }

        long materials = teachingMaterialRepository
                .findByTeacherId(teacherId, PageRequest.of(0, 1))
                .getTotalElements();

        LearningStatsDTO.FollowMetrics followMetrics = followMetrics(teacher.getUser() == null
                ? null
                : teacher.getUser().getId());

        // 教师维度没有"已选课程"语义，课程指标置零，避免前端误用
        return new LearningStatsDTO(
                "TEACHER",
                null,
                teacherId,
                teacher.getName(),
                new LearningStatsDTO.CourseMetrics(0, 0, 0d),
                new LearningStatsDTO.NoteMetrics(0, 0, 0, 0),
                followMetrics,
                null,
                List.of(),
                List.of(),
                List.of());
    }

    /* ------------------------------------------------------------ */

    private List<Note> loadNotes(Long studentId) {
        return noteReposit.findByStudentId(studentId, PageRequest.of(0, NOTE_SCAN_LIMIT))
                .getContent();
    }

    /**
     * 笔记互动指标。
     * <p>
     * 点赞/评论走数据库聚合查询，而不是把笔记正文全量拉回内存再求和。
     * 注意：notes 受 NOTE_SCAN_LIMIT 截断，因此总数以扫描结果为准，
     * 若学生笔记量超过该上限，total 会偏小（个人笔记量远小于此，可接受）。
     */
    private LearningStatsDTO.NoteMetrics noteMetrics(List<Note> notes) {
        long total = notes.size();
        long publicCount = notes.stream()
                .filter(n -> Boolean.TRUE.equals(n.getIsPublic()))
                .count();
        long likes = 0;
        long comments = 0;
        if (!notes.isEmpty()) {
            Long studentId = notes.get(0).getStudent() == null ? null : notes.get(0).getStudent().getId();
            if (studentId != null) {
                Long[] sums = noteReposit.sumEngagementByStudentId(studentId);
                if (sums != null && sums.length >= 2) {
                    likes = sums[0] == null ? 0 : sums[0];
                    comments = sums[1] == null ? 0 : sums[1];
                }
            }
        }
        return new LearningStatsDTO.NoteMetrics(total, publicCount, likes, comments);
    }

    private LearningStatsDTO.FollowMetrics followMetrics(Long userId) {
        if (userId == null) {
            return new LearningStatsDTO.FollowMetrics(0, 0);
        }
        FollowService.FollowStats stats = followService.stats(userId);
        return new LearningStatsDTO.FollowMetrics(stats.following(), stats.followers());
    }

    /**
     * 由指标推导成长阶段。
     * <p>
     * 逐级判断"是否满足该级全部条件"，取满足的最高级；同时给出距离下一级的完成度，
     * 前端无需再自行折算。
     */
    private LearningStatsDTO.GrowthStage growthStage(long completed, long notes, long likes) {
        Stage[] stages = Stage.values();
        Stage current = stages[0];
        int achieved = 1;

        for (int i = 1; i < stages.length; i++) {
            Stage stage = stages[i];
            boolean notesOk = notes >= stage.requiredNotes;
            // 深耕（DEEP）额外要求点赞数
            boolean likesOk = stage != Stage.DEEP || likes >= 10;
            if (completed >= stage.requiredViewed && notesOk && likesOk) {
                current = stage;
                achieved = i + 1;
            }
            else {
                break;
            }
        }

        Stage next = achieved < stages.length ? stages[achieved] : null;
        double progress = next == null ? 1d : progressTo(next, completed, notes, likes);

        return new LearningStatsDTO.GrowthStage(current.key, current.title, current.description,
                achieved, next == null ? null : next.title, progress);
    }

    /** 距离下一阶段的完成度：取各项条件完成比例的最小值，任一条件未达成即无法满格 */
    private double progressTo(Stage next, long completed, long notes, long likes) {
        double byCourse = next.requiredViewed <= 0 ? 1d
                : Math.min(1d, (double) completed / next.requiredViewed);
        double byNotes = next.requiredNotes <= 0 ? 1d
                : Math.min(1d, (double) notes / next.requiredNotes);
        double byLikes = next == Stage.DEEP ? Math.min(1d, (double) likes / 10) : 1d;
        return Math.min(byCourse, Math.min(byNotes, byLikes));
    }

    /** 按已选课程的标签聚合学习广度 */
    private List<LearningStatsDTO.DistributionItem> tagDistribution(List<StudentCourse> enrollments) {
        Map<String, Long> counter = new LinkedHashMap<>();
        for (StudentCourse enrollment : enrollments) {
            Course course = enrollment.getCourse();
            if (course == null || course.getCourseTags() == null) {
                continue;
            }
            for (String tag : course.getCourseTags().split("[,，、;；]")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    counter.merge(trimmed, 1L, Long::sum);
                }
            }
        }
        return toDistribution(counter);
    }

    /** 按月份聚合笔记产出，形成活跃度趋势 */
    private List<LearningStatsDTO.ActivityPoint> activityByMonth(List<Note> notes) {
        Map<String, Long> byMonth = new TreeMap<>();
        for (Note note : notes) {
            if (note.getCreatedAt() == null) {
                continue;
            }
            byMonth.merge(note.getCreatedAt().format(MONTH), 1L, Long::sum);
        }
        List<LearningStatsDTO.ActivityPoint> points = new ArrayList<>(byMonth.size());
        byMonth.forEach((month, count) -> points.add(
                new LearningStatsDTO.ActivityPoint(month, count)));
        return points;
    }

    /** 笔记按所属课程分布 */
    private List<LearningStatsDTO.DistributionItem> notesByCourse(List<Note> notes) {
        Map<String, Long> counter = new LinkedHashMap<>();
        long uncategorized = 0;
        for (Note note : notes) {
            String courseName = note.getCourse() == null ? null : note.getCourse().getCourseName();
            if (courseName == null || courseName.isBlank()) {
                uncategorized++;
                continue;
            }
            counter.merge(courseName, 1L, Long::sum);
        }
        if (uncategorized > 0) {
            counter.put("未关联课程", uncategorized);
        }
        return toDistribution(counter);
    }

    private List<LearningStatsDTO.DistributionItem> toDistribution(Map<String, Long> counter) {
        long total = counter.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return List.of();
        }
        return counter.entrySet().stream()
                .map(e -> new LearningStatsDTO.DistributionItem(e.getKey(), e.getValue(),
                        (double) e.getValue() / total))
                .sorted(Comparator.comparingLong(LearningStatsDTO.DistributionItem::count).reversed())
                .toList();
    }
}
