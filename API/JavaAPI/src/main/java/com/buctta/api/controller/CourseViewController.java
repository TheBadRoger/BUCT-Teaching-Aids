package com.buctta.api.controller;

import com.buctta.api.entities.CourseList;
import com.buctta.api.service.CourseViewService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程访问量统计控制器
 * 提供热门课程查询和访问量记录接口
 */
@Slf4j
@RestController
@RequestMapping("/api/course/view")
public class CourseViewController {

    @Resource
    private CourseViewService courseViewService;

    /**
     * 获取热门课程列表（前N个）
     *
     * @param limit 返回的课程数量，默认10
     * @return 按访问量降序排列的课程列表
     */
    @GetMapping("/popular")
    public ApiResponse<List<CourseList>> getPopularCourses(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit <= 0 || limit > 100) {
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
            }

            List<CourseList> popularCourses = courseViewService.getPopularCourses(limit);
            return ApiResponse.ok(popularCourses);

        }
        catch (Exception e) {
            log.error("Error fetching popular courses", e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取指定课程的访问量
     *
     * @param courseId 课程ID
     * @return 访问量数据
     */
    @GetMapping("/{courseId}/count")
    public ApiResponse<Long> getCourseViewCount(@PathVariable Long courseId) {
        try {
            if (courseId == null || courseId <= 0) {
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
            }

            Long viewCount = courseViewService.getCourseViewCount(courseId);
            return ApiResponse.ok(viewCount);

        }
        catch (Exception e) {
            log.error("Error fetching view count for courseId: {}", courseId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 记录课程访问
     * 当用户访问课程时调用此接口
     *
     * @param courseId 课程ID
     * @return 成功或失败信息
     */
    @PostMapping("/{courseId}/record")
    public ApiResponse<String> recordCourseView(@PathVariable Long courseId) {
        try {
            if (courseId == null || courseId <= 0) {
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
            }

            courseViewService.recordCourseView(courseId);
            return ApiResponse.ok("View recorded successfully");

        }
        catch (Exception e) {
            log.error("Error recording view for courseId: {}", courseId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 获取前10个热门课程（便捷接口）
     *
     * @return 前10热门课程
     */
    @GetMapping("/top10")
    public ApiResponse<List<CourseList>> getTop10PopularCourses() {
        try {
            List<CourseList> topCourses = courseViewService.getPopularCourses(10);
            return ApiResponse.ok(topCourses);

        }
        catch (Exception e) {
            log.error("Error fetching top 10 popular courses", e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 手动触发Redis到MySQL的数据同步
     * 仅管理员可以调用（可以加权限检查）
     */
    @PostMapping("/admin/sync")
    public ApiResponse<String> syncViewCountsManually() {
        try {
            // 这里可以添加权限检查：权限检查代码
            courseViewService.syncViewCountsToDatabase();
            return ApiResponse.ok("Data synced successfully");

        }
        catch (Exception e) {
            log.error("Error manually syncing view counts", e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 手动触发缓存重建
     * 仅管理员可以调用（可以加权限检查）
     * 用于在Redis数据异常时恢复
     */
    @PostMapping("/admin/rebuild-cache")
    public ApiResponse<String> rebuildCacheManually() {
        try {
            // 这里可以添加权限检查：权限检查代码
            courseViewService.rebuildCourseViewCache();
            return ApiResponse.ok("Cache rebuilt successfully");

        }
        catch (Exception e) {
            log.error("Error manually rebuilding cache", e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /**
     * 清除指定课程的访问记录
     * 仅管理员可以调用（可以加权限检查）
     *
     * @param courseId 课程ID
     */
    @DeleteMapping("/admin/clear/{courseId}")
    public ApiResponse<String> clearCourseViewCount(@PathVariable Long courseId) {
        try {
            if (courseId == null || courseId <= 0) {
                return ApiResponse.fail(BusinessStatus.PARAM_FORMAT_ERROR);
            }

            // 这里可以添加权限检查：权限检查代码
            courseViewService.clearCourseViewCount(courseId);
            return ApiResponse.ok("View count cleared successfully");

        }
        catch (Exception e) {
            log.error("Error clearing view count for courseId: {}", courseId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
}

