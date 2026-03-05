package com.qrordering.menu.service;

import com.qrordering.auth.enums.UserRole;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.security.RestaurantUserDetails;
import com.qrordering.common.exception.BusinessException;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.menu.converter.CategoryConverter;
import com.qrordering.menu.dto.request.CreateCategoryRequest;
import com.qrordering.menu.dto.request.UpdateCategoryRequest;
import com.qrordering.menu.dto.response.CategoryResponse;
import com.qrordering.menu.entity.Category;
import com.qrordering.menu.repository.CategoryRepository;
import com.qrordering.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryConverter categoryConverter;
    private final RestaurantRepository restaurantRepository;

    /**
     * Resolve tenant id: "me" -> from JWT (RESTAURANT_ADMIN); otherwise require PLATFORM_ADMIN and validate restaurant exists.
     */
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
    public List<CategoryResponse> listForRestaurant(String restaurantId) {
        String tenantId = resolveTenantId(restaurantId);
        return categoryRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenantId)
                .stream()
                .map(categoryConverter::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(String restaurantId, CreateCategoryRequest request) {
        String tenantId = resolveTenantId(restaurantId);
        if (categoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.getName().trim())) {
            throw new BusinessException("Category with name '" + request.getName() + "' already exists");
        }
        Category cat = Category.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        return categoryConverter.toResponse(categoryRepository.save(cat));
    }

    @Transactional
    public CategoryResponse update(String restaurantId, Long id, UpdateCategoryRequest request) {
        String tenantId = resolveTenantId(restaurantId);
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", String.valueOf(id)));
        if (!cat.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Category", String.valueOf(id));
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            if (categoryRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, request.getName().trim(), id)) {
                throw new BusinessException("Category with name '" + request.getName() + "' already exists");
            }
            cat.setName(request.getName().trim());
        }
        if (request.getSortOrder() != null) {
            cat.setSortOrder(request.getSortOrder());
        }
        return categoryConverter.toResponse(categoryRepository.save(cat));
    }

    @Transactional
    public void delete(String restaurantId, Long id) {
        String tenantId = resolveTenantId(restaurantId);
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", String.valueOf(id)));
        if (!cat.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Category", String.valueOf(id));
        }
        categoryRepository.delete(cat);
    }
}
