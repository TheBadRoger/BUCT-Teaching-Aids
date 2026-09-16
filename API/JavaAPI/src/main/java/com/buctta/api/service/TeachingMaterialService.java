package com.buctta.api.service;

import com.buctta.api.entities.TeachingMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 教参（教学参考资料）管理。
 */
public interface TeachingMaterialService {

    /** 新建教参 */
    TeachingMaterial create(TeachingMaterial material);

    /** 读取单条；不存在返回 null */
    TeachingMaterial getById(Long id);

    /** 读取单条并累加浏览量 */
    TeachingMaterial viewAndCount(Long id);

    /** 按发布教师分页 */
    Page<TeachingMaterial> listByTeacher(Long teacherId, Pageable pageable);

    /** 按课程分页 */
    Page<TeachingMaterial> listByCourse(Long courseId, Pageable pageable);

    /** 公开教参分页，供学生浏览 */
    Page<TeachingMaterial> listPublic(Pageable pageable);

    /**
     * 更新教参，仅覆盖请求中提供的非 null 字段。
     *
     * @return 更新后的实体；不存在返回 null
     */
    TeachingMaterial update(Long id, TeachingMaterial details);

    /** 删除；不存在返回 false */
    boolean delete(Long id);
}
