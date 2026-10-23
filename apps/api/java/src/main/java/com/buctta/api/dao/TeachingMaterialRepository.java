package com.buctta.api.dao;

import com.buctta.api.entities.TeachingMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingMaterialRepository extends JpaRepository<TeachingMaterial, Long>,
        JpaSpecificationExecutor<TeachingMaterial> {

    Page<TeachingMaterial> findByTeacherId(Long teacherId, Pageable pageable);

    Page<TeachingMaterial> findByCourseId(Long courseId, Pageable pageable);

    Page<TeachingMaterial> findByIsPublicTrue(Pageable pageable);

    List<TeachingMaterial> findByCourseIdAndIsPublicTrue(Long courseId);

    void deleteByCourseId(Long courseId);
}
