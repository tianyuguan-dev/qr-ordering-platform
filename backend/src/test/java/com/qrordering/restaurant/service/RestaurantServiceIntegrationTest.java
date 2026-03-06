package com.qrordering.restaurant.service;

import com.qrordering.common.exception.BusinessException;
import com.qrordering.common.exception.ResourceNotFoundException;
import com.qrordering.restaurant.dto.request.CreateRestaurantRequest;
import com.qrordering.restaurant.dto.request.UpdateRestaurantRequest;
import com.qrordering.restaurant.dto.response.RestaurantResponse;
import com.qrordering.restaurant.entity.Restaurant;
import com.qrordering.restaurant.enums.RestaurantStatus;
import com.qrordering.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for RestaurantService using real PostgreSQL + Redis via Testcontainers.
 *
 * <p>Validates: Flyway migrations, JPA AttributeConverters (status stored as INTEGER),
 * business rules (duplicate name check), and full CRUD round-trip.
 *
 * <p>Each test is {@link Transactional} so changes are rolled back automatically.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "platform.bootstrap.enabled=false",
                "spring.task.scheduling.pool.size=0"  // disable scheduled tasks during test
        })
@Testcontainers(disabledWithoutDocker = true)
@Transactional
@DisplayName("RestaurantService (Integration)")
class RestaurantServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("qr_ordering_test")
            .withUsername("admin")
            .withPassword("password");

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    RestaurantRepository restaurantRepository;

    // ------------------------------------------------------------------ helpers

    private CreateRestaurantRequest createReq(String name) {
        return CreateRestaurantRequest.builder()
                .name(name)
                .address("123 Queen St, Auckland")
                .phone("+64 9 123 4567")
                .build();
    }

    // ------------------------------------------------------------------ create

    @Nested
    @DisplayName("createRestaurant")
    class Create {

        @Test
        @DisplayName("persists restaurant to DB with ACTIVE status and generated REST_ id")
        void persistsToDatabase() {
            RestaurantResponse resp = restaurantService.createRestaurant(createReq("The Test Kitchen"));

            assertThat(resp.getId()).startsWith("REST_");
            assertThat(resp.getName()).isEqualTo("The Test Kitchen");
            assertThat(resp.getStatus().getCode()).isEqualTo(1); // RestaurantStatus.ACTIVE

            // Verify directly via repository — status stored as INTEGER converter
            Restaurant fromDb = restaurantRepository.findById(resp.getId()).orElseThrow();
            assertThat(fromDb.getStatus()).isEqualTo(RestaurantStatus.ACTIVE);
        }

        @Test
        @DisplayName("duplicate name throws BusinessException and nothing is saved")
        void duplicateName_throwsAndDoesNotSave() {
            restaurantService.createRestaurant(createReq("Unique Name"));
            long countBefore = restaurantRepository.count();

            assertThatThrownBy(() -> restaurantService.createRestaurant(createReq("Unique Name")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Unique Name");

            assertThat(restaurantRepository.count()).isEqualTo(countBefore);
        }
    }

    // ------------------------------------------------------------------ read

    @Nested
    @DisplayName("getAllRestaurants")
    class GetAll {

        @Test
        @DisplayName("no filter returns all restaurants")
        void noFilter_returnsAll() {
            restaurantService.createRestaurant(createReq("Alpha"));
            restaurantService.createRestaurant(createReq("Beta"));

            Page<RestaurantResponse> page = restaurantService.getAllRestaurants(
                    null, null, PageRequest.of(0, 20));

            assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("status filter returns only matching restaurants")
        void statusFilter_returnsOnlyMatching() {
            // Create one restaurant then suspend it
            RestaurantResponse created = restaurantService.createRestaurant(createReq("Suspended One"));
            restaurantService.updateRestaurant(created.getId(),
                    UpdateRestaurantRequest.builder().status(2).build()); // SUSPENDED

            restaurantService.createRestaurant(createReq("Active One"));

            Page<RestaurantResponse> active = restaurantService.getAllRestaurants(
                    1, null, PageRequest.of(0, 20)); // status=1 ACTIVE
            Page<RestaurantResponse> suspended = restaurantService.getAllRestaurants(
                    2, null, PageRequest.of(0, 20)); // status=2 SUSPENDED

            assertThat(active.getContent())
                    .extracting(r -> r.getStatus().getCode())
                    .doesNotContain(2);
            assertThat(suspended.getContent())
                    .extracting(r -> r.getStatus().getCode())
                    .containsOnly(2);
        }

        @Test
        @DisplayName("name filter returns only matching restaurants (case-insensitive)")
        void nameFilter_caseInsensitive() {
            restaurantService.createRestaurant(createReq("Tokyo Ramen House"));
            restaurantService.createRestaurant(createReq("Pizza Palace"));

            Page<RestaurantResponse> results = restaurantService.getAllRestaurants(
                    null, "ramen", PageRequest.of(0, 20));

            assertThat(results.getContent())
                    .extracting(RestaurantResponse::getName)
                    .contains("Tokyo Ramen House")
                    .doesNotContain("Pizza Palace");
        }
    }

    // ------------------------------------------------------------------ update

    @Nested
    @DisplayName("updateRestaurant")
    class Update {

        @Test
        @DisplayName("updates name and address; other fields unchanged")
        void updatesFields() {
            RestaurantResponse created = restaurantService.createRestaurant(
                    CreateRestaurantRequest.builder()
                            .name("Old Name")
                            .address("Old Address")
                            .build());

            restaurantService.updateRestaurant(created.getId(),
                    UpdateRestaurantRequest.builder().name("New Name").build());

            Restaurant fromDb = restaurantRepository.findById(created.getId()).orElseThrow();
            assertThat(fromDb.getName()).isEqualTo("New Name");
            assertThat(fromDb.getAddress()).isEqualTo("Old Address"); // unchanged
        }
    }

    // ------------------------------------------------------------------ delete

    @Nested
    @DisplayName("deleteRestaurant")
    class Delete {

        @Test
        @DisplayName("removes restaurant from DB")
        void removesFromDb() {
            RestaurantResponse created = restaurantService.createRestaurant(createReq("To Delete"));

            restaurantService.deleteRestaurant(created.getId());

            assertThat(restaurantRepository.existsById(created.getId())).isFalse();
        }

        @Test
        @DisplayName("not found throws ResourceNotFoundException")
        void notFound_throwsException() {
            assertThatThrownBy(() -> restaurantService.deleteRestaurant("GHOST-ID"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
