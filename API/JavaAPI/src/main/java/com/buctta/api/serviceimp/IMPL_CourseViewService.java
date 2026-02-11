package com.buctta.api.serviceimp;

import com.buctta.api.dao.CourseReposit;
import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseViewService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 课程访问量统计服务实现
 * 使用Redis Sorted Set存储访问量，支持高并发
 * 定时同步到MySQL数据库进行持久化
 */
@Slf4j
@Service
public class IMPL_CourseViewService implements CourseViewService {

    // Redis key前缀
    private static final String COURSE_VIEW_KEY = "course:view:";
    // Redis Sorted Set key（用于排序）
    private static final String POPULAR_COURSES_KEY = "popular:courses";
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CourseReposit courseReposit;

    /**
     * 记录课程访问
     * 使用Redis的Sorted Set和普通String同时记录，提高并发性能
     */
    @Override
    public void recordCourseView(Long courseId) {
        try {
            if (courseId == null || courseId <= 0) {
                log.warn("Invalid courseId: {}", courseId);
                return;
            }

            // 1. 使用Sorted Set记录访问量（score为访问数，member为courseId）
            // ZINCRBY 命令原子性地增加score值
            stringRedisTemplate.opsForZSet().incrementScore(POPULAR_COURSES_KEY, courseId.toString(), 1);

            // 2. 记录单个课程的访问计数（用于快速查询）
            String courseViewCountKey = COURSE_VIEW_KEY + courseId;
            stringRedisTemplate.opsForValue().increment(courseViewCountKey);

            log.debug("Recorded view for courseId: {}", courseId);
        }
        catch (Exception e) {
            log.error("Error recording course view for courseId: {}", courseId, e);
        }
    }

    /**
     * 获取热门课程（前N个）
     * 直接从Redis读取，性能最优
     */
    @Override
    public List<Course> getPopularCourses(int limit) {
        try {
            if (limit <= 0) {
                limit = 10;
            }

            // 从Redis的Sorted Set中获取前limit个，按score降序排列
            Set<ZSetOperations.TypedTuple<String>> topCourseIds = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(POPULAR_COURSES_KEY, 0, limit - 1);

            if (topCourseIds == null || topCourseIds.isEmpty()) {
                log.debug("No popular courses found in Redis");
                return new ArrayList<>();
            }

            List<Course> popularCourses = new ArrayList<>();

            // 遍历courseId，从数据库获取完整的课程信息
            for (ZSetOperations.TypedTuple<String> tuple : topCourseIds) {
                String courseIdStr = tuple.getValue();
                Double score = tuple.getScore();

                if (courseIdStr == null) {
                    log.warn("CourseId is null in Redis");
                    continue;
                }

                try {
                    Long courseId = Long.parseLong(courseIdStr);
                    Optional<Course> course = courseReposit.findCourseById(courseId);

                    if (course.isPresent()) {
                        Course courseEntity = course.get();
                        // 更新实体中的访问量（从Redis中获取最新值）
                        courseEntity.setViewCount(score != null ? score.longValue() : 0L);
                        popularCourses.add(courseEntity);
                    }
                }
                catch (NumberFormatException e) {
                    log.warn("Invalid courseId format in Redis: {}", courseIdStr, e);
                }
            }

            return popularCourses;
        }
        catch (Exception e) {
            log.error("Error getting popular courses", e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取指定课程的访问量
     */
    @Override
    public Long getCourseViewCount(Long courseId) {
        try {
            if (courseId == null || courseId <= 0) {
                return 0L;
            }

            // 优先从Sorted Set获取
            Double score = stringRedisTemplate.opsForZSet().score(POPULAR_COURSES_KEY, courseId.toString());
            if (score != null) {
                return score.longValue();
            }

            // 备选：从单个计数key获取
            String courseViewCountKey = COURSE_VIEW_KEY + courseId;
            String count = stringRedisTemplate.opsForValue().get(courseViewCountKey);
            if (count != null) {
                return Long.parseLong(count);
            }

            return 0L;
        }
        catch (Exception e) {
            log.error("Error getting view count for courseId: {}", courseId, e);
            return 0L;
        }
    }

    /**
     * 将Redis中的访问数据同步到MySQL
     * 这个方法应该由定时任务定期调用（如每小时一次）
     */
    @Override
    public void syncViewCountsToDatabase() {
        try {
            log.info("Starting to sync view counts from Redis to MySQL...");
            long startTime = System.currentTimeMillis();

            // 获取所有课程的访问数据（不限制数量）
            Set<ZSetOperations.TypedTuple<String>> allCourseData = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(POPULAR_COURSES_KEY, 0, -1);

            if (allCourseData == null || allCourseData.isEmpty()) {
                log.info("No course view data to sync");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            // 批量更新数据库
            for (ZSetOperations.TypedTuple<String> tuple : allCourseData) {
                String courseIdStr = tuple.getValue();
                Double score = tuple.getScore();

                if (courseIdStr == null) {
                    log.warn("CourseId is null in Redis");
                    failCount++;
                    continue;
                }

                try {
                    Long courseId = Long.parseLong(courseIdStr);
                    Long viewCount = score != null ? score.longValue() : 0L;

                    Optional<Course> courseOptional = courseReposit.findCourseById(courseId);
                    if (courseOptional.isPresent()) {
                        Course course = courseOptional.get();
                        // 更新访问量到数据库
                        course.setViewCount(viewCount);
                        courseReposit.save(course);
                        successCount++;
                        log.debug("Synced view count for courseId: {}, viewCount: {}", courseId, viewCount);
                    }
                }
                catch (Exception e) {
                    log.warn("Error syncing view count for courseId: {}", courseIdStr, e);
                    failCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Sync completed. Total: {}, Success: {}, Failed: {}, Time: {}ms",
                    allCourseData.size(), successCount, failCount, duration);

        }
        catch (Exception e) {
            log.error("Error syncing view counts to database", e);
        }
    }

    /**
     * 清除指定课程的访问记录
     */
    @Override
    public void clearCourseViewCount(Long courseId) {
        try {
            if (courseId == null || courseId <= 0) {
                return;
            }

            // 从Sorted Set中移除
            stringRedisTemplate.opsForZSet().remove(POPULAR_COURSES_KEY, courseId.toString());

            // 删除单个计数key
            String courseViewCountKey = COURSE_VIEW_KEY + courseId;
            stringRedisTemplate.delete(courseViewCountKey);

            log.info("Cleared view count for courseId: {}", courseId);
        }
        catch (Exception e) {
            log.error("Error clearing view count for courseId: {}", courseId, e);
        }
    }

    /**
     * 重新构建Redis缓存
     * 从数据库读取所有课程的访问量到Redis中
     * 应该在Redis数据丢失时调用
     */
    @Override
    public void rebuildCourseViewCache() {
        try {
            log.info("Starting to rebuild course view cache...");
            long startTime = System.currentTimeMillis();

            // 清空原有数据
            stringRedisTemplate.delete(POPULAR_COURSES_KEY);

            // 从数据库获取所有课程
            List<Course> allCourses = courseReposit.findAll();

            int successCount = 0;
            for (Course course : allCourses) {
                try {
                    long viewCount = course.getViewCount() != null ? course.getViewCount() : 0L;
                    // 加载到Redis Sorted Set
                    stringRedisTemplate.opsForZSet().add(POPULAR_COURSES_KEY, String.valueOf(course.getId()), (double) viewCount);
                    successCount++;
                }
                catch (Exception e) {
                    log.warn("Error rebuilding cache for courseId: {}", course.getId(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache rebuild completed. Total courses: {}, Success: {}, Time: {}ms",
                    allCourses.size(), successCount, duration);

        }
        catch (Exception e) {
            log.error("Error rebuilding course view cache", e);
        }
    }
}

