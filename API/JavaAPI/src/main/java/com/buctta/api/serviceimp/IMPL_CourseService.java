package com.buctta.api.serviceimp;

import com.buctta.api.dao.CourseReposit;
import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class IMPL_CourseService implements CourseService {

    @Resource
    private CourseReposit courseReposit;

    @Override
    public CourseResult addCourse(Course course) {
        if (courseReposit.findCourseByCourseNumber(course.getCourseNumber()) != null) {
            return CourseResult.fail("COURSE_NUMBER_EXISTS", "课程编号已存在: " + course.getCourseNumber());
        }
        try {
            Course savedCourse = courseReposit.save(course);
            return CourseResult.success(savedCourse, "课程添加成功");
        }
        catch (Exception e) {
            return CourseResult.fail("SAVE_FAILED", "保存课程失败: " + e.getMessage());
        }
    }

    @Override
    public Page<Course> searchCourses(String courseName, String courseNumber,
                                      String teachingTeachers, String courseStatus,
                                      String courseTags, String startDate, Pageable pageable) {
        Specification<Course> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 课程名称模糊查询
            if (courseName != null && !courseName.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("courseName"), "%" + courseName + "%"));
            }

            // 课程编号精确查询
            if (courseNumber != null && !courseNumber.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("courseNumber"), courseNumber));
            }

            // 授课老师模糊查询
            if (teachingTeachers != null && !teachingTeachers.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("teachingTeachers"), "%" + teachingTeachers + "%"));
            }

            // 课程状态精确查询
            if (courseStatus != null && !courseStatus.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("courseStatus"), courseStatus));
            }

            // 课程标签模糊查询
            if (courseTags != null && !courseTags.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("courseTags"), "%" + courseTags + "%"));
            }

            // 开课日期查询
            if (startDate != null && !startDate.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("startDate"), startDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return courseReposit.findAll(specification, pageable);
    }

    @Override
    public CourseResult updateCourse(Long id, Course courseDetails) {
        Course existingCourse = courseReposit.findById(id).orElse(null);
        if (existingCourse == null) {
            return CourseResult.fail("COURSE_NOT_FOUND", "课程不存在，ID: " + id);
        }

        // 检查课程编号是否重复（排除自己）
        Course courseWithSameNumber = courseReposit.findCourseByCourseNumber(courseDetails.getCourseNumber());
        if (courseWithSameNumber != null && courseWithSameNumber.getId() != id) {
            return CourseResult.fail("COURSE_NUMBER_EXISTS", "课程编号已存在: " + courseDetails.getCourseNumber());
        }

        // 更新字段
        if (courseDetails.getCourseName() != null) {
            existingCourse.setCourseName(courseDetails.getCourseName());
        }
        if (courseDetails.getCourseNumber() != null) {
            existingCourse.setCourseNumber(courseDetails.getCourseNumber());
        }
        if (courseDetails.getCourseIntroduction() != null) {
            existingCourse.setCourseIntroduction(courseDetails.getCourseIntroduction());
        }
        if (courseDetails.getStartDate() != null) {
            existingCourse.setStartDate(courseDetails.getStartDate());
        }
        if (courseDetails.getTeachingObjectives() != null) {
            existingCourse.setTeachingObjectives(courseDetails.getTeachingObjectives());
        }
        if (courseDetails.getDuration() != null) {
            existingCourse.setDuration(courseDetails.getDuration());
        }
        if (courseDetails.getTeachingTeachers() != null) {
            existingCourse.setTeachingTeachers(courseDetails.getTeachingTeachers());
        }
        if (courseDetails.getTeachingClasses() != null) {
            existingCourse.setTeachingClasses(courseDetails.getTeachingClasses());
        }
        if (courseDetails.getTargetAudience() != null) {
            existingCourse.setTargetAudience(courseDetails.getTargetAudience());
        }
        if (courseDetails.getClassAddress() != null) {
            existingCourse.setClassAddress(courseDetails.getClassAddress());
        }
        if (courseDetails.getCoursePrice() != null) {
            existingCourse.setCoursePrice(courseDetails.getCoursePrice());
        }
        if (courseDetails.getCourseStatus() != null) {
            existingCourse.setCourseStatus(courseDetails.getCourseStatus());
        }
        if (courseDetails.getCourseTags() != null) {
            existingCourse.setCourseTags(courseDetails.getCourseTags());
        }
        if (courseDetails.getCourseOutline() != null) {
            existingCourse.setCourseOutline(courseDetails.getCourseOutline());
        }
        if (courseDetails.getCourseImage() != null) {
            existingCourse.setCourseImage(courseDetails.getCourseImage());
        }

        try {
            Course updatedCourse = courseReposit.save(existingCourse);
            return CourseResult.success(updatedCourse, "课程更新成功");
        }
        catch (Exception e) {
            return CourseResult.fail("UPDATE_FAILED", "更新课程失败: " + e.getMessage());
        }
    }

    @Override
    public Course getCourseById(Long id) {
        return courseReposit.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在，ID: " + id));
    }

    @Override
    public CourseResult deleteCourse(Long id) {
        try {
            if (!courseReposit.existsById(id)) {
                return CourseResult.fail("COURSE_NOT_FOUND", "课程不存在，ID: " + id);
            }
            courseReposit.deleteById(id);
            return CourseResult.success(null, "课程删除成功");
        }
        catch (Exception e) {
            return CourseResult.fail("DELETE_FAILED", "删除课程失败: " + e.getMessage());
        }
    }
    @Override
    public CourseResult deleteCourses(List<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return CourseResult.fail("INVALID_IDS", "课程ID列表不能为空");
            }
            courseReposit.deleteAllByIdIn(ids);
            return CourseResult.success(null, "批量删除成功");
        } catch (Exception e) {
            return CourseResult.fail("DELETE_FAILED", "批量删除失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportCoursesToExcel(List<Course> courses) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("课程列表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("课程名称");
        header.createCell(2).setCellValue("课程编号");
        header.createCell(3).setCellValue("授课教师");
        header.createCell(4).setCellValue("状态");
        int rowIdx = 1;
        for (Course c : courses) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getCourseName());
            row.createCell(2).setCellValue(c.getCourseNumber());
            row.createCell(3).setCellValue(c.getTeachingTeachers());
            row.createCell(4).setCellValue(c.getCourseStatus());
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        return bos.toByteArray();
    }
    @Override
    public List<Course> getAllCourses() {
        return courseReposit.findAll();
    }

    @Override
    public CourseResult setPublished(Long id, Boolean published, String teacherName) {
        if (id == null) {
            return CourseResult.fail("PARAM_MISSING", "课程 ID 不能为空");
        }
        Course course = courseReposit.findById(id).orElse(null);
        if (course == null) {
            return CourseResult.fail("COURSE_NOT_FOUND", "课程不存在，ID: " + id);
        }

        // 弱归属校验：teachingTeachers 是逗号分隔的姓名文本，没有教师外键可依赖。
        // teacherName == null 表示管理员调用，跳过校验；空串表示调用者没有教师身份，
        // 必须照常走校验并被拒绝——否则任何登录用户都能发布任意课程。
        if (teacherName != null && !belongsToTeacher(course, teacherName)) {
            return CourseResult.fail("NO_PERMISSION",
                    "只能发布自己授课的课程（当前课程授课教师：" + course.getTeachingTeachers() + "）");
        }

        course.setPublished(published == null || published);
        try {
            return CourseResult.success(courseReposit.save(course),
                    course.getPublished() ? "课程已发布" : "课程已取消发布");
        }
        catch (Exception e) {
            return CourseResult.fail("UPDATE_FAILED", "更新发布状态失败: " + e.getMessage());
        }
    }

    /**
     * 判断课程是否由指定教师授课。
     * <p>
     * {@code teachingTeachers} 支持中英文逗号、顿号、分号分隔，且可能是"张三老师"这类带后缀的写法，
     * 因此做包含匹配而不是精确相等。
     */
    private boolean belongsToTeacher(Course course, String teacherName) {
        String teachers = course.getTeachingTeachers();
        if (teachers == null || teachers.isBlank()) {
            // 未填写授课教师时不拦截，避免把正常流程卡死
            return true;
        }
        if (teacherName == null || teacherName.isBlank()) {
            // 课程有授课教师，但调用者没有可用的教师姓名 → 无从匹配，拒绝
            return false;
        }
        String target = teacherName.trim();
        for (String part : teachers.split("[,，、;；]")) {
            if (part.trim().contains(target)) {
                return true;
            }
        }
        return false;
    }
}