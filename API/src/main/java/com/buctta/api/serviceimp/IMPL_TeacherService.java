package com.buctta.api.serviceimp;

import com.buctta.api.entities.TeacherList;
import com.buctta.api.reposit.TeacherReposit;
import com.buctta.api.service.TeacherService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IMPL_TeacherService implements TeacherService {
    @Resource
    private TeacherReposit teacherReposit;
    @Override
    public TeacherList AddTeacher(TeacherList teacherList)
    {
        if (teacherReposit.findTeacherListByName(teacherList.getName())!=null)
        {
            return null;
        }
        else
        {
            TeacherList newTeacher=teacherReposit.save(teacherList);
            return newTeacher;
        }
    }
    @Override
    public Page<TeacherList> searchTeachers(String name, String organization, String jointime, String gender, String education, Pageable pageable) {
        Specification<TeacherList> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 添加姓名查询条件（模糊查询）
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }

            // 添加学院查询条件（模糊查询）
            if (organization != null && !organization.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("organization"), "%" + organization + "%"));
            }

            // 添加入职时间查询条件
            if (jointime != null && !jointime.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("jointime"), jointime));
            }

            // 添加性别查询条件
            if (gender != null && !gender.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), gender));
            }

            // 添加学历查询条件
            if (education != null && !education.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("education"), education));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return teacherReposit.findAll(specification, pageable);
    }
}
