package com.qrordering.restaurant.service;

import com.qrordering.common.exception.BusinessException;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.restaurant.converter.RestaurantConverter;
import com.qrordering.restaurant.dto.request.CreateRestaurantRequest;
import com.qrordering.restaurant.dto.request.UpdateRestaurantRequest;
import com.qrordering.restaurant.dto.response.RestaurantResponse;
import com.qrordering.restaurant.entity.Restaurant;
import com.qrordering.restaurant.enums.RestaurantStatus;
import com.qrordering.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RestaurantService.
 * Mocks the repository and converter; verifies routing logic and error handling.
 * Skipped on JDK 25+ (Mockito/ByteBuddy not yet compatible).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantService")
@EnabledForJreRange(max = JRE.JAVA_22, disabledReason = "Mockito/ByteBuddy not yet compatible with JDK 25+")
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantConverter restaurantConverter;

    private RestaurantService restaurantService;

    @BeforeEach
    void setUp() {
        restaurantService = new RestaurantService(restaurantRepository, restaurantConverter);
    }

    private Restaurant stubRestaurant(String id, String name, RestaurantStatus status) {
        return Restaurant.builder()
                .id(id)
                .name(name)
                .status(status)
                .build();
    }

    private RestaurantResponse stubResponse(String id, String name) {
        return RestaurantResponse.builder()
                .id(id)
                .name(name)
                .build();
    }

    // ------------------------------------------------------------------ createRestaurant

    @Nested
    @DisplayName("createRestaurant")
    class CreateRestaurant {

        @Test
        @DisplayName("success: saves entity and returns converter response")
        void success_savesAndReturnsResponse() {
            CreateRestaurantRequest req = CreateRestaurantRequest.builder()
                    .name("New Ramen Bar")
                    .build();
            Restaurant saved = stubRestaurant("REST_20240101_AABBCC001122", "New Ramen Bar", RestaurantStatus.ACTIVE);
            RestaurantResponse expected = stubResponse(saved.getId(), saved.getName());

            when(restaurantRepository.existsByNameIgnoreCase("New Ramen Bar")).thenReturn(false);
            when(restaurantRepository.save(any(Restaurant.class))).thenReturn(saved);
            when(restaurantConverter.toResponse(saved)).thenReturn(expected);

            RestaurantResponse result = restaurantService.createRestaurant(req);

            assertThat(result.getName()).isEqualTo("New Ramen Bar");
            assertThat(result.getId()).startsWith("REST_");
            verify(restaurantRepository).save(any(Restaurant.class));
        }

        @Test
        @DisplayName("duplicate name throws BusinessException without saving")
        void duplicateName_throwsBusinessException() {
            when(restaurantRepository.existsByNameIgnoreCase("Taken Name")).thenReturn(true);

            assertThatThrownBy(() -> restaurantService.createRestaurant(
                    CreateRestaurantRequest.builder().name("Taken Name").build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Taken Name");

            verify(restaurantRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ getAllRestaurants

    @Nested
    @DisplayName("getAllRestaurants")
    class GetAllRestaurants {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("no filter: delegates to findAll")
        void noFilter_usesFindAll() {
            Page<Restaurant> page = new PageImpl<>(List.of());
            when(restaurantRepository.findAll(pageable)).thenReturn(page);

            restaurantService.getAllRestaurants(null, null, pageable);

            verify(restaurantRepository).findAll(pageable);
            verify(restaurantRepository, never()).findByStatus(any(), any());
        }

        @Test
        @DisplayName("statusCode only: delegates to findByStatus")
        void statusCodeOnly_usesFindByStatus() {
            Page<Restaurant> page = new PageImpl<>(List.of());
            when(restaurantRepository.findByStatus(RestaurantStatus.ACTIVE, pageable)).thenReturn(page);

            restaurantService.getAllRestaurants(1, null, pageable);

            verify(restaurantRepository).findByStatus(RestaurantStatus.ACTIVE, pageable);
        }

        @Test
        @DisplayName("name only: delegates to findByNameContainingIgnoreCase")
        void nameOnly_usesFindByName() {
            Page<Restaurant> page = new PageImpl<>(List.of());
            when(restaurantRepository.findByNameContainingIgnoreCase("ramen", pageable)).thenReturn(page);

            restaurantService.getAllRestaurants(null, "ramen", pageable);

            verify(restaurantRepository).findByNameContainingIgnoreCase("ramen", pageable);
        }

        @Test
        @DisplayName("status + name: delegates to findByStatusAndNameContainingIgnoreCase")
        void statusAndName_usesFindByStatusAndName() {
            Page<Restaurant> page = new PageImpl<>(List.of());
            when(restaurantRepository.findByStatusAndNameContainingIgnoreCase(
                    RestaurantStatus.ACTIVE, "ramen", pageable)).thenReturn(page);

            restaurantService.getAllRestaurants(1, "ramen", pageable);

            verify(restaurantRepository).findByStatusAndNameContainingIgnoreCase(
                    RestaurantStatus.ACTIVE, "ramen", pageable);
        }

        @Test
        @DisplayName("whitespace-only name treated as no name filter")
        void whitespaceOnlyName_usesFindAll() {
            Page<Restaurant> page = new PageImpl<>(List.of());
            when(restaurantRepository.findAll(pageable)).thenReturn(page);

            restaurantService.getAllRestaurants(null, "   ", pageable);

            verify(restaurantRepository).findAll(pageable);
        }
    }

    // ------------------------------------------------------------------ getRestaurant

    @Nested
    @DisplayName("getRestaurant")
    class GetRestaurant {

        @Test
        @DisplayName("found: returns converter response")
        void found_returnsResponse() {
            Restaurant r = stubRestaurant("REST-001", "Sushi Place", RestaurantStatus.ACTIVE);
            RestaurantResponse resp = stubResponse("REST-001", "Sushi Place");
            when(restaurantRepository.findById("REST-001")).thenReturn(Optional.of(r));
            when(restaurantConverter.toResponse(r)).thenReturn(resp);

            assertThat(restaurantService.getRestaurant("REST-001").getName()).isEqualTo("Sushi Place");
        }

        @Test
        @DisplayName("not found: throws ResourceNotFoundException")
        void notFound_throwsException() {
            when(restaurantRepository.findById("MISSING")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.getRestaurant("MISSING"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ deleteRestaurant

    @Nested
    @DisplayName("deleteRestaurant")
    class DeleteRestaurant {

        @Test
        @DisplayName("exists: calls deleteById")
        void exists_callsDeleteById() {
            when(restaurantRepository.existsById("REST-001")).thenReturn(true);

            restaurantService.deleteRestaurant("REST-001");

            verify(restaurantRepository).deleteById("REST-001");
        }

        @Test
        @DisplayName("not found: throws ResourceNotFoundException without deleting")
        void notFound_throwsException() {
            when(restaurantRepository.existsById("GHOST")).thenReturn(false);

            assertThatThrownBy(() -> restaurantService.deleteRestaurant("GHOST"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(restaurantRepository, never()).deleteById(any());
        }
    }

    // ------------------------------------------------------------------ updateRestaurant

    @Nested
    @DisplayName("updateRestaurant")
    class UpdateRestaurant {

        @Test
        @DisplayName("updates only provided fields; skips null fields")
        void updatesProvidedFieldsOnly() {
            Restaurant existing = stubRestaurant("REST-001", "Old Name", RestaurantStatus.ACTIVE);
            existing.setAddress("Old Address");
            when(restaurantRepository.findById("REST-001")).thenReturn(Optional.of(existing));
            when(restaurantRepository.existsByNameIgnoreCase("New Name")).thenReturn(false);
            when(restaurantRepository.save(existing)).thenReturn(existing);
            when(restaurantConverter.toResponse(existing)).thenReturn(stubResponse("REST-001", "New Name"));

            UpdateRestaurantRequest req = UpdateRestaurantRequest.builder()
                    .name("New Name")
                    .build();

            restaurantService.updateRestaurant("REST-001", req);

            assertThat(existing.getName()).isEqualTo("New Name");
            assertThat(existing.getAddress()).isEqualTo("Old Address"); // unchanged
        }

        @Test
        @DisplayName("updating to duplicate name throws BusinessException")
        void duplicateName_throwsBusinessException() {
            Restaurant existing = stubRestaurant("REST-001", "Original", RestaurantStatus.ACTIVE);
            when(restaurantRepository.findById("REST-001")).thenReturn(Optional.of(existing));
            when(restaurantRepository.existsByNameIgnoreCase("Taken")).thenReturn(true);

            assertThatThrownBy(() -> restaurantService.updateRestaurant(
                    "REST-001", UpdateRestaurantRequest.builder().name("Taken").build()))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
