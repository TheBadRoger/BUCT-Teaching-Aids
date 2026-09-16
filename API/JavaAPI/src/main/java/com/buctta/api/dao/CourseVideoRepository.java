package com.buctta.api.dao;

import com.buctta.api.entities.CourseVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseVideoRepository extends JpaRepository<CourseVideo, Long> {

    /** 课程视频目录：按 sort_order 升序，其次按创建顺序，保证顺序稳定 */
    List<CourseVideo> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);

    long countByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);

    void deleteByVideoFileId(Long videoFileId);
}
