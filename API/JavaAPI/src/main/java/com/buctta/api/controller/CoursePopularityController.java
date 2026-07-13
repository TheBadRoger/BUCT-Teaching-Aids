package com.buctta.api.controller;

import com.buctta.api.entities.Course;
import com.buctta.api.service.CourseViewService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import com.buctta.api.utils.CoursePopularityResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程热门排行增强控制器
 * 提供更详细的热门课程统计数据
 */
@Slf4j
@RestController
@RequestMapping("/api/course/popularity")
public class CoursePopularityController {

    private static final String POPULAR_COURSES_KEY = "popular:courses";
    @Resource
    private CourseViewService courseViewService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取热门课程排行（详细版本）
     *
     * @param limit 返回的课程数量，默认10，最大100
     * @return 包含详细统计信息的热门课程排行数据
     */
    @GetMapping("/ranking")
    public ApiResponse<CoursePopularityResponse> getCoursePopularityRanking(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit <= 0 || limit > 100) {
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
            }

            List<Course> popularCourses = courseViewService.getPopularCourses(limit);

            // 构建响应对象
            CoursePopularityResponse response = new CoursePopularityResponse();
            List<CoursePopularityResponse.CoursePopularityItem> items = new ArrayList<>();

            int ranking = 1;
            long totalViews = 0;
            long maxViews = 0;

            for (Course course : popularCourses) {
                long viewCount = course.getViewCount() != null ? course.getViewCount() : 0L;

                CoursePopularityResponse.CoursePopularityItem item = new CoursePopularityResponse.CoursePopularityItem();
                item.setRanking(ranking);
                item.setCourseId(course.getId());
                item.setCourseName(course.getCourseName());
                item.setViewCount(viewCount);
                item.setCourseStatus(course.getCourseStatus());
                item.setCourseTags(course.getCourseTags());
                item.setCourseImage(course.getCourseImage());

                items.add(item);
                totalViews += viewCount;
                if (viewCount > maxViews) {
                    maxViews = viewCount;
                }
                ranking++;
            }

            // 获取所有课程总数用于计算平均值
            Long totalCourses = stringRedisTemplate.opsForZSet().size(POPULAR_COURSES_KEY);
            if (totalCourses == null) {
                totalCourses = 0L;
            }

            long averageViews = totalCourses > 0 ? totalViews / totalCourses : 0;

            // 构建统计信息
            CoursePopularityResponse.PopularityStats stats = new CoursePopularityResponse.PopularityStats();
            stats.setTotalCourses(totalCourses.intValue());
            stats.setTotalViews(totalViews);
            stats.setAverageViews(averageViews);
            stats.setMaxViews(maxViews);

            response.setItems(items);
            response.setStats(stats);
            response.setTimestamp(System.currentTimeMillis());

            return ApiResponse.ok(response);

        }
        catch (Exception e) {
            log.error("Error fetching course popularity ranking", e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取前10热门课程简化版
     */
    @GetMapping("/top10-simple")
    public ApiResponse<List<CoursePopularityResponse.CoursePopularityItem>> getTop10PopularCoursesSimple() {
        try {
            List<Course> popularCourses = courseViewService.getPopularCourses(10);
            List<CoursePopularityResponse.CoursePopularityItem> items = new ArrayList<>();

            int ranking = 1;
            for (Course course : popularCourses) {
                Long viewCount = course.getViewCount() != null ? course.getViewCount() : 0L;

                CoursePopularityResponse.CoursePopularityItem item = new CoursePopularityResponse.CoursePopularityItem();
                item.setRanking(ranking);
                item.setCourseId(course.getId());
                item.setCourseName(course.getCourseName());
                item.setViewCount(viewCount);
                item.setCourseStatus(course.getCourseStatus());

                items.add(item);
                ranking++;
            }

            return ApiResponse.ok(items);

        }
        catch (Exception e) {
            log.error("Error fetching top 10 popular courses", e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取特定排名范围的课程
     *
     * @param start 起始排名（从1开始）
     * @param end   结束排名
     */
    @GetMapping("/range")
    public ApiResponse<List<CoursePopularityResponse.CoursePopularityItem>> getCoursesByRankingRange(
            @RequestParam(defaultValue = "1") Integer start,
            @RequestParam(defaultValue = "10") Integer end) {
        try {
            if (start < 1 || end < start || (end - start + 1) > 100) {
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
            }

            List<Course> popularCourses = courseViewService.getPopularCourses(end);

            List<CoursePopularityResponse.CoursePopularityItem> items = new ArrayList<>();
            int currentRanking = 1;

            for (Course course : popularCourses) {
                if (currentRanking >= start && currentRanking <= end) {
                    Long viewCount = course.getViewCount() != null ? course.getViewCount() : 0L;

                    CoursePopularityResponse.CoursePopularityItem item = new CoursePopularityResponse.CoursePopularityItem();
                    item.setRanking(currentRanking);
                    item.setCourseId(course.getId());
                    item.setCourseName(course.getCourseName());
                    item.setViewCount(viewCount);
                    item.setCourseStatus(course.getCourseStatus());
                    item.setCourseImage(course.getCourseImage());

                    items.add(item);
                }
                currentRanking++;
            }

            return ApiResponse.ok(items);

        }
        catch (Exception e) {
            log.error("Error fetching courses by ranking range", e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 检查课程是否在前N个热门课程中
     *
     * @param courseId 课程ID
     * @param limit    检查范围（前N个课程）
     */
    @GetMapping("/{courseId}/is-popular")
    public ApiResponse<Boolean> isCoursPopular(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (courseId == null || courseId <= 0 || limit <= 0) {
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
            }

            List<Course> popularCourses = courseViewService.getPopularCourses(limit);
            boolean isPopular = popularCourses.stream()
                    .anyMatch(course -> course.getId() == courseId);

            return ApiResponse.ok(isPopular);

        }
        catch (Exception e) {
            log.error("Error checking if course is popular", e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取课程的排名
     *
     * @param courseId 课程ID
     */
    @GetMapping("/{courseId}/ranking")
    public ApiResponse<Integer> getCourseRanking(@PathVariable Long courseId) {
        try {
            if (courseId == null || courseId <= 0) {
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
            }

            // 获取所有课程并找出排名
            List<Course> allPopularCourses = courseViewService.getPopularCourses(Integer.MAX_VALUE);

            for (int i = 0; i < allPopularCourses.size(); i++) {
                if (allPopularCourses.get(i).getId() == courseId) {
                    return ApiResponse.ok(i + 1);
                }
            }

            // 未找到表示课程没有访问量
            return ApiResponse.ok(0);

        }
        catch (Exception e) {
            log.error("Error getting course ranking for courseId: {}", courseId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
}

