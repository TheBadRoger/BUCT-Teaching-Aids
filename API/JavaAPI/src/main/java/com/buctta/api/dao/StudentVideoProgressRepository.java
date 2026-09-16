package com.buctta.api.dao;

import com.buctta.api.entities.StudentVideoProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentVideoProgressRepository extends JpaRepository<StudentVideoProgress, Long> {

    Optional<StudentVideoProgress> findByStudentIdAndVideoId(Long studentId, Long videoId);

    /** 某学生在某门课下的逐视频记录 */
    List<StudentVideoProgress> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /** 某学生的全部逐视频记录，按视频维度统计完成数 */
    List<StudentVideoProgress> findByStudentId(Long studentId);

    long countByStudentIdAndVideoIdIn(Long studentId, List<Long> videoIds);

    long countByStudentIdAndCompletedTrue(Long studentId);

    void deleteByVideoId(Long videoId);

    void deleteByCourseId(Long courseId);
}
