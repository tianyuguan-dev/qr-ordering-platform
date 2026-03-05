package com.qrordering.menu.repository;

import com.qrordering.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByTenantIdOrderBySortOrderAscNameAsc(String tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(String tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(String tenantId, String name, Long excludeId);
}
