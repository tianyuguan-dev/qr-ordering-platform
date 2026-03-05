package com.qrordering.table.service;

import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.common.exception.BusinessException;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.table.converter.TableInfoConverter;
import com.qrordering.table.dto.request.CreateTableInfoRequest;
import com.qrordering.table.dto.request.UpdateTableInfoRequest;
import com.qrordering.table.dto.response.TableInfoResponse;
import com.qrordering.table.entity.TableInfo;
import com.qrordering.table.enums.TableStatus;
import com.qrordering.table.repository.TableInfoRepository;
import com.qrordering.restaurant.repository.RestaurantRepository;
import com.qrordering.config.CustomerBaseUrlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TableInfoService {

    private final TableInfoRepository tableInfoRepository;
    private final TableInfoConverter tableInfoConverter;
    private final RestaurantRepository restaurantRepository;
    private final CustomerBaseUrlProperties customerBaseUrl;

    /** Build the customer order page URL for this table (scan QR → open this URL). */
    private String buildOrderPageUrl(String tenantId, Long tableId, String tableNumber) {
        String base = customerBaseUrl.getCustomerBaseUrl();
        if (base == null || base.isBlank()) return null;
        String path = "/t/" + tenantId + "/menu?tableId=" + tableId + "&tableNumber=" + java.net.URLEncoder.encode(tableNumber, java.nio.charset.StandardCharsets.UTF_8);
        return base.replaceAll("/$", "") + path;
    }

    private String resolveTenantId(String restaurantId) {
        if ("me".equalsIgnoreCase(restaurantId)) {
            var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!(principal instanceof RestaurantUserDetails r) || r.getRole() != UserRole.RESTAURANT_ADMIN) {
                throw new org.springframework.security.access.AccessDeniedException("Only restaurant admin can use /me");
            }
            return r.getTenantId();
        }
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof PlatformAdminDetails)) {
            throw new org.springframework.security.access.AccessDeniedException("Only platform admin can manage another restaurant's tables");
        }
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
        return restaurantId;
    }

    @Transactional(readOnly = true)
    public List<TableInfoResponse> listForRestaurant(String restaurantId) {
        String tenantId = resolveTenantId(restaurantId);
        return tableInfoRepository.findByTenantIdOrderByTableNumberAsc(tenantId)
                .stream()
                .map(tableInfoConverter::toResponse)
                .toList();
    }

    @Transactional
    public TableInfoResponse create(String restaurantId, CreateTableInfoRequest request) {
        String tenantId = resolveTenantId(restaurantId);
        String num = request.getTableNumber().trim();
        if (tableInfoRepository.existsByTenantIdAndTableNumber(tenantId, num)) {
            throw new BusinessException("Table number '" + num + "' already exists");
        }
        TableInfo t = TableInfo.builder()
                .tenantId(tenantId)
                .tableNumber(num)
                .seats(request.getSeats() != null ? request.getSeats() : 4)
                .status(TableStatus.AVAILABLE)
                .build();
        t = tableInfoRepository.save(t);
        t.setQrCodeUrl(buildOrderPageUrl(tenantId, t.getId(), t.getTableNumber()));
        t = tableInfoRepository.save(t);
        return tableInfoConverter.toResponse(t);
    }

    @Transactional
    public TableInfoResponse update(String restaurantId, Long id, UpdateTableInfoRequest request) {
        String tenantId = resolveTenantId(restaurantId);
        TableInfo t = tableInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table", String.valueOf(id)));
        if (!t.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Table", String.valueOf(id));
        }
        if (request.getTableNumber() != null && !request.getTableNumber().isBlank()) {
            if (tableInfoRepository.existsByTenantIdAndTableNumberAndIdNot(tenantId, request.getTableNumber().trim(), id)) {
                throw new BusinessException("Table number '" + request.getTableNumber() + "' already exists");
            }
            t.setTableNumber(request.getTableNumber().trim());
        }
        if (request.getSeats() != null) t.setSeats(request.getSeats());
        if (request.getStatus() != null) t.setStatus(TableStatus.fromCode(request.getStatus()));
        t.setQrCodeUrl(buildOrderPageUrl(tenantId, t.getId(), t.getTableNumber()));
        return tableInfoConverter.toResponse(tableInfoRepository.save(t));
    }

    @Transactional
    public void delete(String restaurantId, Long id) {
        String tenantId = resolveTenantId(restaurantId);
        TableInfo t = tableInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table", String.valueOf(id)));
        if (!t.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Table", String.valueOf(id));
        }
        tableInfoRepository.delete(t);
    }
}
