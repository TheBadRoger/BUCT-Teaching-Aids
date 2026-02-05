package com.buctta.api.serviceimp;

import com.buctta.api.entities.CourseList;
import com.buctta.api.dao.CourseReposit;
import com.buctta.api.service.CourseService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IMPL_CourseService implements CourseService {

    @Resource
    private CourseReposit courseReposit;

    @Override
    public CourseList addCourse(CourseList courseList) {
        if (courseReposit.findCourseListByCourseNumber(courseList.getCourseNumber()) != null) {
            return null;
        } else {
            CourseList newCourse = courseReposit.save(courseList);
            return newCourse;
        }
    }

    @Override
    public Page<CourseList> searchCourses(String courseName, String courseNumber,
                                          String teachingTeachers, String courseStatus,
                                          String courseTags, String startDate, Pageable pageable) {
        Specification<CourseList> specification = (root, query, criteriaBuilder) -> {
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
    public CourseList updateCourse(Long id, CourseList courseDetails) {
        CourseList existingCourse = courseReposit.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在，ID: " + id));

        // 检查课程编号是否重复（排除自己）
        CourseList courseWithSameNumber = courseReposit.findCourseListByCourseNumber(courseDetails.getCourseNumber());
        if (courseWithSameNumber != null && courseWithSameNumber.getId() != id) {
            throw new RuntimeException("课程编号已存在: " + courseDetails.getCourseNumber());
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

        return courseReposit.save(existingCourse);
    }

    @Override
    public CourseList getCourseById(Long id) {
        return courseReposit.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在，ID: " + id));
    }

    @Override
    public boolean deleteCourse(Long id) {
        try {
            if (courseReposit.existsById(id)) {
                courseReposit.deleteById(id);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("删除课程失败: " + e.getMessage());
        }
    }
}