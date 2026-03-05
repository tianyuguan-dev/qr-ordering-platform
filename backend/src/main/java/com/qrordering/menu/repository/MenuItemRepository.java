package com.qrordering.menu.repository;

import com.qrordering.menu.entity.MenuItem;
import com.qrordering.menu.enums.MenuItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByTenantIdOrderByCategoryIdAscNameAsc(String tenantId);

    List<MenuItem> findByTenantIdAndStatusOrderByCategoryIdAscNameAsc(String tenantId, MenuItemStatus status);

    Page<MenuItem> findByTenantId(String tenantId, Pageable pageable);

    Page<MenuItem> findByTenantIdAndCategoryId(String tenantId, Long categoryId, Pageable pageable);

    Page<MenuItem> findByTenantIdAndStatus(String tenantId, MenuItemStatus status, Pageable pageable);

    Page<MenuItem> findByTenantIdAndCategoryIdAndStatus(String tenantId, Long categoryId, MenuItemStatus status, Pageable pageable);
}
