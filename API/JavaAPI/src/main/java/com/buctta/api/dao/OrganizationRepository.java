package com.buctta.api.dao;

import com.buctta.api.entities.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long>,
        JpaSpecificationExecutor<Organization> {

    Optional<Organization> findByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}