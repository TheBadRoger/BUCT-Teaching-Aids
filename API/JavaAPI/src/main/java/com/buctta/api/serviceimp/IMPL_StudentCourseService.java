package com.buctta.api.serviceimp;

import com.buctta.api.dao.CourseReposit;
import com.buctta.api.dao.StudentCourseReposit;
import com.buctta.api.dao.StudentReposit;
import com.buctta.api.entities.Course;
import com.buctta.api.entities.Student;
import com.buctta.api.entities.StudentCourse;
import com.buctta.api.entities.StudentCourseId;
import com.buctta.api.service.StudentCourseService;
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

    @Resource
    private StudentCourseReposit studentCourseRepository;

    @Resource
    private StudentReposit studentRepository;

    @Resource
    private CourseReposit courseListRepository;

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
}

