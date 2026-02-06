package com.buctta.api.serviceimp;

import com.buctta.api.dao.CourseReposit;
import com.buctta.api.dao.StudentCourseReposit;
import com.buctta.api.dao.StudentReposit;
import com.buctta.api.entities.CourseList;
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
    public StudentCourse selectCourse(Long studentId, Long courseId) {
        Student student = studentRepository.findById(studentId)
                .orElse(null);
        if (student == null) {
            return null;
        }

        CourseList course = courseListRepository.findById(courseId)
                .orElse(null);
        if (course == null) {
            return null;
        }

        if (studentCourseRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            return null;
        }

        try {
            StudentCourse studentCourse = new StudentCourse(student, course);
            studentCourse.setIsViewed(false);

            return studentCourseRepository.save(studentCourse);
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public StudentCourse updateViewedStatus(Long studentId, Long courseId, Boolean isViewed) {
        StudentCourse studentCourse = studentCourseRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElse(null);

        if (studentCourse == null) {
            return null;
        }

        try {
            studentCourse.setIsViewed(isViewed);
            return studentCourseRepository.save(studentCourse);
        }
        catch (Exception e) {
            return null;
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
    public void dropCourse(Long studentId, Long courseId) {
        StudentCourseId id = new StudentCourseId(studentId, courseId);
        if (!studentCourseRepository.existsById(id)) {
            return;
        }

        try {
            studentCourseRepository.deleteById(id);
        }
        catch (Exception ignored) {
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

