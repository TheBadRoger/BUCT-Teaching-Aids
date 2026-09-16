package com.buctta.api.controller;

import com.buctta.api.entities.Organization;
import com.buctta.api.service.OrganizationService;
import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
public class OrganizationController {

    @Resource
    private final OrganizationService organizationService;

    @PostMapping("/add")
    public ApiResponse<Organization> addOrganization(@RequestBody Organization org) {
        OrganizationService.OrganizationResult result = organizationService.addOrganization(org);
        if (result.success()) {
            return ApiResponse.ok(result.organization());
        } else {
            return fail(result);
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Organization> deleteOrganization(@PathVariable Long id) {
        OrganizationService.OrganizationResult result = organizationService.deleteOrganization(id);
        if (result.success()) {
            return ApiResponse.ok(result.organization());
        } else {
            return fail(result);
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Organization> updateOrganization(@PathVariable Long id,
                                                        @RequestBody Organization details) {
        OrganizationService.OrganizationResult result =
                organizationService.updateOrganization(id, details);
        if (result.success()) {
            return ApiResponse.ok(result.organization());
        } else {
            return fail(result);
        }
    }

    /**
     * 把服务层的失败结果映射为对应的业务状态码。
     * <p>
     * 之前这里对各接口硬编码了状态码（如 PUT 一律返回 4042），会把"名称重复""图片地址非法"
     * 这类失败误报成"机构不存在"，前端无法据此给出正确的提示。
     */
    private ApiResponse<Organization> fail(OrganizationService.OrganizationResult result) {
        BusinessStatus status = switch (result.errorCode() == null ? "" : result.errorCode()) {
            case "ORGANIZATION_NOT_FOUND" -> BusinessStatus.RESOURCE_NOT_FOUND;
            case "ORGANIZATION_NAME_EXISTS" -> BusinessStatus.ENTITY_EXISTS;
            case "INVALID_MEDIA_URL" -> BusinessStatus.INVALID_MEDIA_URL;
            default -> BusinessStatus.INTERNAL_ERROR;
        };
        return ApiResponse.fail(status, result.message());
    }

    @GetMapping("/{id}")
    public ApiResponse<Organization> getOrganizationById(@PathVariable Long id) {
        try {
            Organization org = organizationService.getOrganizationById(id);
            return ApiResponse.ok(org);
        } catch (RuntimeException e) {
            log.error("获取机构信息失败: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/search")
    public ApiResponse<Page<Organization>> searchOrganizations(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
            Page<Organization> orgPage = organizationService.searchOrganizations(name, pageable);
            return ApiResponse.ok(orgPage);
        } catch (Exception e) {
            log.error("搜索机构时发生错误: {}", e.getMessage(), e);
            return ApiResponse.fail(BusinessStatus.INTERNAL_ERROR);
        }
    }
}