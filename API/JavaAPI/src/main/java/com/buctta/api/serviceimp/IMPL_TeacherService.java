package com.buctta.api.serviceimp;

import com.buctta.api.dao.TeacherReposit;
import com.buctta.api.entities.Teacher;
import com.buctta.api.service.TeacherService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMPL_TeacherService implements TeacherService {
    @Resource
    private TeacherReposit teacherReposit;

    @Override
    public TeacherResult addTeacher(Teacher teacher) {
        if (teacherReposit.findTeacherListByName(teacher.getName()) != null) {
            return TeacherResult.fail("TEACHER_EXISTS", "教师已存在: " + teacher.getName());
        }
        try {
            Teacher savedTeacher = teacherReposit.save(teacher);
            return TeacherResult.success(savedTeacher, "教师添加成功");
        }
        catch (Exception e) {
            return TeacherResult.fail("SAVE_FAILED", "保存教师失败: " + e.getMessage());
        }
    }

    @Override
    public Page<Teacher> searchTeachers(String name, String organization, String jointime, String gender, String education, Pageable pageable) {
        Specification<Teacher> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }

            if (organization != null && !organization.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("organization"), "%" + organization + "%"));
            }

            if (jointime != null && !jointime.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("jointime"), jointime));
            }
            if (gender != null && !gender.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }

            if (education != null && !education.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("education"), education));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return teacherReposit.findAll(specification, pageable);
    }
}
