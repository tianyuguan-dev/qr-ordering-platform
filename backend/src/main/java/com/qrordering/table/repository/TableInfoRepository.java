package com.qrordering.table.repository;

import com.qrordering.table.entity.TableInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableInfoRepository extends JpaRepository<TableInfo, Long> {

    List<TableInfo> findByTenantIdOrderByTableNumberAsc(String tenantId);

    Optional<TableInfo> findByTenantIdAndTableNumber(String tenantId, String tableNumber);

    boolean existsByTenantIdAndTableNumber(String tenantId, String tableNumber);

    boolean existsByTenantIdAndTableNumberAndIdNot(String tenantId, String tableNumber, Long excludeId);
}
