package com.buctta.api.serviceimp;

import com.buctta.api.dao.TeacherReposit;
import com.buctta.api.entities.TeacherList;
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
