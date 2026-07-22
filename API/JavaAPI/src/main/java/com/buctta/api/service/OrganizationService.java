package com.buctta.api.service;

import com.buctta.api.entities.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrganizationService {

    OrganizationResult addOrganization(Organization organization);

    OrganizationResult updateOrganization(Long id, Organization details);

    Organization getOrganizationById(Long id);

    OrganizationResult deleteOrganization(Long id);

    Page<Organization> searchOrganizations(String name, Pageable pageable);

    record OrganizationResult(boolean success, Organization organization,
                              String errorCode, String message) {
        public static OrganizationResult success(Organization organization) {
            return new OrganizationResult(true, organization, null, "操作成功");
        }

        public static OrganizationResult success(Organization organization, String message) {
            return new OrganizationResult(true, organization, null, message);
        }

        public static OrganizationResult fail(String errorCode, String message) {
            return new OrganizationResult(false, null, errorCode, message);
        }
    }
}