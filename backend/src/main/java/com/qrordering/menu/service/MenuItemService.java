package com.qrordering.menu.service;

import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.menu.converter.MenuItemConverter;
import com.qrordering.menu.dto.request.CreateMenuItemRequest;
import com.qrordering.menu.dto.request.UpdateMenuItemRequest;
import com.qrordering.menu.dto.response.MenuItemResponse;
import com.qrordering.menu.entity.MenuItem;
import com.qrordering.menu.enums.MenuItemStatus;
import com.qrordering.menu.repository.MenuItemRepository;
import com.qrordering.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemConverter menuItemConverter;
    private final RestaurantRepository restaurantRepository;

    private String resolveTenantId(String restaurantId) {
        if ("me".equalsIgnoreCase(restaurantId)) {
            var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!(principal instanceof RestaurantUserDetails r)) {
                throw new org.springframework.security.access.AccessDeniedException("Not a restaurant user");
            }
            if (r.getRole() != UserRole.RESTAURANT_ADMIN && r.getRole() != UserRole.KITCHEN) {
                throw new org.springframework.security.access.AccessDeniedException("Only restaurant admin or kitchen can use /me for menu");
            }
            return r.getTenantId();
        }
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof PlatformAdminDetails)) {
            throw new org.springframework.security.access.AccessDeniedException("Only platform admin can manage another restaurant's menu");
        }
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
        return restaurantId;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> listAllForRestaurant(String restaurantId) {
        String tenantId = resolveTenantId(restaurantId);
        return menuItemRepository.findByTenantIdOrderByCategoryIdAscNameAsc(tenantId)
                .stream()
                .map(menuItemConverter::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MenuItemResponse> listForRestaurant(String restaurantId, Long categoryId, Integer statusCode, Pageable pageable) {
        String tenantId = resolveTenantId(restaurantId);
        MenuItemStatus status = statusCode != null ? MenuItemStatus.fromCode(statusCode) : null;
        if (categoryId != null && status != null) {
            return menuItemRepository.findByTenantIdAndCategoryIdAndStatus(tenantId, categoryId, status, pageable)
                    .map(menuItemConverter::toResponse);
        }
        if (categoryId != null) {
            return menuItemRepository.findByTenantIdAndCategoryId(tenantId, categoryId, pageable)
                    .map(menuItemConverter::toResponse);
        }
        if (status != null) {
            return menuItemRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                    .map(menuItemConverter::toResponse);
        }
        return menuItemRepository.findByTenantId(tenantId, pageable)
                .map(menuItemConverter::toResponse);
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getById(String restaurantId, Long id) {
        String tenantId = resolveTenantId(restaurantId);
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", String.valueOf(id)));
        if (!item.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Menu item", String.valueOf(id));
        }
        return menuItemConverter.toResponse(item);
    }

    @Transactional
    public MenuItemResponse create(String restaurantId, CreateMenuItemRequest request) {
        String tenantId = resolveTenantId(restaurantId);
        MenuItem item = MenuItem.builder()
                .tenantId(tenantId)
                .categoryId(request.getCategoryId())
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .price(request.getPrice())
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
                .allergens(request.getAllergens() != null ? request.getAllergens().trim() : null)
                .status(MenuItemStatus.AVAILABLE)
                .build();
        return menuItemConverter.toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse update(String restaurantId, Long id, UpdateMenuItemRequest request) {
        String tenantId = resolveTenantId(restaurantId);
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", String.valueOf(id)));
        if (!item.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Menu item", String.valueOf(id));
        }
        if (request.getCategoryId() != null) item.setCategoryId(request.getCategoryId());
        if (request.getName() != null) item.setName(request.getName().trim());
        if (request.getDescription() != null) item.setDescription(request.getDescription().trim());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getImageUrl() != null) item.setImageUrl(request.getImageUrl().trim());
        if (request.getAllergens() != null) item.setAllergens(request.getAllergens().trim());
        if (request.getStatus() != null) item.setStatus(MenuItemStatus.fromCode(request.getStatus()));
        return menuItemConverter.toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public void delete(String restaurantId, Long id) {
        String tenantId = resolveTenantId(restaurantId);
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", String.valueOf(id)));
        if (!item.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Menu item", String.valueOf(id));
        }
        menuItemRepository.delete(item);
    }

    @Transactional
    public MenuItemResponse updateStatus(String restaurantId, Long id, Integer statusCode) {
        String tenantId = resolveTenantId(restaurantId);
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item", String.valueOf(id)));
        if (!item.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Menu item", String.valueOf(id));
        }
        item.setStatus(MenuItemStatus.fromCode(statusCode));
        return menuItemConverter.toResponse(menuItemRepository.save(item));
    }
}
