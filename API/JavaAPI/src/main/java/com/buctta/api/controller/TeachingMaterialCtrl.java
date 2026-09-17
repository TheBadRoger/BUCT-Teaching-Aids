package com.buctta.api.controller;

import com.buctta.api.entities.TeachingMaterial;
import com.buctta.api.service.TeachingMaterialService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import com.buctta.api.utils.MediaUrls;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 教参（教学参考资料）接口。
 * <p>
 * 独立于笔记：笔记是学生个人产出，教参是教师按课程发布的资料。
 */
@Slf4j
@RestController
@RequestMapping("/api/teaching-materials")
public class TeachingMaterialCtrl {

    @Resource
    private TeachingMaterialService teachingMaterialService;

    /** 新建教参 */
    @PostMapping("/create")
    public ApiResponse<TeachingMaterial> create(@RequestBody TeachingMaterial material) {
        ApiResponse<TeachingMaterial> invalid = rejectUnsafeAttachment(material.getAttachmentUrl());
        if (invalid != null) {
            return invalid;
        }
        if (material.getTitle() == null || material.getTitle().isBlank()) {
            return ApiResponse.fail(BusinessStatus.PARAM_MISSING, "title 不能为空");
        }
        try {
            return ApiResponse.ok(teachingMaterialService.create(material));
        }
        catch (RuntimeException e) {
            log.error("创建教参失败 title={}", material.getTitle(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, "创建教参失败: " + e.getMessage());
        }
    }

    /**
     * 按 ID 读取教参。
     * <p>
     * {@code countView} 默认 false：GET 带副作用容易被预取/重试放大浏览量，
     * 因此由调用方在真实阅读场景显式传 {@code countView=true}。
     */
    @GetMapping("/{id}")
    public ApiResponse<TeachingMaterial> getById(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "false") boolean countView) {
        try {
            TeachingMaterial material = countView
                    ? teachingMaterialService.viewAndCount(id)
                    : teachingMaterialService.getById(id);
            if (material == null) {
                return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND, "教参不存在，ID: " + id);
            }
            return ApiResponse.ok(material);
        }
        catch (RuntimeException e) {
            log.error("读取教参失败 id={}", id, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 按发布教师分页 */
    @GetMapping("/teacher/{teacherId}")
    public ApiResponse<Page<TeachingMaterial>> listByTeacher(
            @PathVariable Long teacherId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ApiResponse.ok(teachingMaterialService.listByTeacher(teacherId, pageable(page, size)));
        }
        catch (RuntimeException e) {
            log.error("查询教师教参失败 teacherId={}", teacherId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 按课程分页 */
    @GetMapping("/course/{courseId}")
    public ApiResponse<Page<TeachingMaterial>> listByCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ApiResponse.ok(teachingMaterialService.listByCourse(courseId, pageable(page, size)));
        }
        catch (RuntimeException e) {
            log.error("查询课程教参失败 courseId={}", courseId, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 公开教参分页，供学生浏览 */
    @GetMapping("/public")
    public ApiResponse<Page<TeachingMaterial>> listPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ApiResponse.ok(teachingMaterialService.listPublic(pageable(page, size)));
        }
        catch (RuntimeException e) {
            log.error("查询公开教参失败", e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /** 更新教参，仅覆盖非 null 字段 */
    @PutMapping("/update/{id}")
    public ApiResponse<TeachingMaterial> update(@PathVariable Long id,
                                                @RequestBody TeachingMaterial details) {
        ApiResponse<TeachingMaterial> invalid = rejectUnsafeAttachment(details.getAttachmentUrl());
        if (invalid != null) {
            return invalid;
        }
        try {
            TeachingMaterial updated = teachingMaterialService.update(id, details);
            if (updated == null) {
                return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND, "教参不存在，ID: " + id);
            }
            return ApiResponse.ok(updated);
        }
        catch (RuntimeException e) {
            log.error("更新教参失败 id={}", id, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR, "更新教参失败: " + e.getMessage());
        }
    }

    /** 删除教参 */
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        try {
            if (!teachingMaterialService.delete(id)) {
                return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND, "教参不存在，ID: " + id);
            }
            return ApiResponse.ok("教参已删除，ID: " + id);
        }
        catch (RuntimeException e) {
            log.error("删除教参失败 id={}", id, e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }

    /* ------------------------------------------------------------ */

    private Pageable pageable(int page, int size) {
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    /** 附件地址会进 href/src，非法协议直接以 4006 拒绝，而不是留给服务层抛异常 */
    private ApiResponse<TeachingMaterial> rejectUnsafeAttachment(String url) {
        if (MediaUrls.isSafe(url)) {
            return null;
        }
        return ApiResponse.fail(BusinessStatus.INVALID_MEDIA_URL,
                MediaUrls.rejectMessage("attachmentUrl"));
    }
}
