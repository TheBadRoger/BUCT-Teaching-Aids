package com.buctta.api.serviceimp;

import com.buctta.api.dao.OrganizationRepository;
import com.buctta.api.entities.Organization;
import com.buctta.api.service.OrganizationService;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IMPL_OrganizationService implements OrganizationService {

    @Resource
    private OrganizationRepository organizationRepository;

    @Override
    public OrganizationResult addOrganization(Organization organization) {
        if (organizationRepository.findByName(organization.getName()).isPresent()) {
            return OrganizationResult.fail("ORGANIZATION_NAME_EXISTS",
                    "机构名称已存在: " + organization.getName());
        }
        try {
            Organization saved = organizationRepository.save(organization);
            return OrganizationResult.success(saved, "机构添加成功");
        } catch (Exception e) {
            return OrganizationResult.fail("SAVE_FAILED", "保存机构失败: " + e.getMessage());
        }
    }

    @Override
    public OrganizationResult updateOrganization(Long id, Organization details) {
        Organization existing = organizationRepository.findById(id).orElse(null);
        if (existing == null) {
            return OrganizationResult.fail("ORGANIZATION_NOT_FOUND", "机构不存在，ID: " + id);
        }

        if (details.getName() != null &&
                organizationRepository.existsByNameAndIdNot(details.getName(), id)) {
            return OrganizationResult.fail("ORGANIZATION_NAME_EXISTS",
                    "该名称已被其他机构使用: " + details.getName());
        }

        if (details.getName() != null) {
            existing.setName(details.getName());
        }
        if (details.getLogo() != null) {
            existing.setLogo(details.getLogo());
        }
        if (details.getBannerUrl() != null) {
            existing.setBannerUrl(details.getBannerUrl());
        }
        if (details.getInfo() != null) {
            existing.setInfo(details.getInfo());
        }
        if (details.getHonorCertUrl() != null) {
            existing.setHonorCertUrl(details.getHonorCertUrl());
        }

        try {
            Organization updated = organizationRepository.save(existing);
            return OrganizationResult.success(updated, "机构更新成功");
        } catch (Exception e) {
            return OrganizationResult.fail("UPDATE_FAILED", "更新机构失败: " + e.getMessage());
        }
    }

    @Override
    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("机构不存在，ID: " + id));
    }

    @Override
    public OrganizationResult deleteOrganization(Long id) {
        try {
            if (!organizationRepository.existsById(id)) {
                return OrganizationResult.fail("ORGANIZATION_NOT_FOUND",
                        "机构不存在，ID: " + id);
            }
            organizationRepository.deleteById(id);
            return OrganizationResult.success(null, "机构删除成功");
        } catch (Exception e) {
            return OrganizationResult.fail("DELETE_FAILED", "删除机构失败: " + e.getMessage());
        }
    }

    @Override
    public Page<Organization> searchOrganizations(String name, Pageable pageable) {
        Specification<Organization> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return organizationRepository.findAll(specification, pageable);
    }
}