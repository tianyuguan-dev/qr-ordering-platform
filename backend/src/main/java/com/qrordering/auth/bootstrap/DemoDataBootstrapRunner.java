package com.qrordering.auth.bootstrap;

import com.qrordering.auth.config.DemoDataProperties;
import com.qrordering.auth.dto.request.CreateRestaurantUserRequest;
import com.qrordering.auth.security.PlatformAdminDetails;
import com.qrordering.auth.service.RestaurantUserService;
import com.qrordering.menu.dto.request.CreateCategoryRequest;
import com.qrordering.menu.dto.request.CreateMenuItemRequest;
import com.qrordering.menu.dto.response.CategoryResponse;
import com.qrordering.menu.service.CategoryService;
import com.qrordering.menu.service.MenuItemService;
import com.qrordering.restaurant.dto.request.CreateRestaurantRequest;
import com.qrordering.restaurant.dto.response.RestaurantResponse;
import com.qrordering.restaurant.repository.RestaurantRepository;
import com.qrordering.restaurant.service.RestaurantService;
import com.qrordering.storage.service.MinioService;
import com.qrordering.table.dto.request.CreateTableInfoRequest;
import com.qrordering.table.service.TableInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds two demo restaurants (Tokyo Sushi + Sichuan Hotpot) with staff, menu items, and tables
 * on first startup. Runs only when demo.data.enabled=true and neither restaurant exists.
 * Images are uploaded to MinIO from classpath resources; if MinIO is unavailable the
 * runner still creates all other data with null image URLs.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DemoDataBootstrapRunner implements ApplicationRunner {

    private static final String SUSHI_NAME  = "Tokyo Sushi";
    private static final String HOTPOT_NAME = "Sichuan Hotpot";

    private final DemoDataProperties      demoDataProperties;
    private final RestaurantRepository    restaurantRepository;
    private final RestaurantService       restaurantService;
    private final RestaurantUserService   restaurantUserService;
    private final CategoryService         categoryService;
    private final MenuItemService         menuItemService;
    private final TableInfoService        tableInfoService;
    @Nullable
    private final MinioService            minioService;

    @Override
    public void run(ApplicationArguments args) {
        if (!demoDataProperties.isEnabled()) return;
        if (restaurantRepository.existsByNameIgnoreCase(SUSHI_NAME)
                || restaurantRepository.existsByNameIgnoreCase(HOTPOT_NAME)) {
            log.debug("Demo data already exists, skipping.");
            return;
        }

        log.info("Seeding demo data...");
        // Services check SecurityContext for authorization; inject a system identity for the duration.
        var systemAuth = new UsernamePasswordAuthenticationToken(
                new PlatformAdminDetails(0L, "system", ""), null,
                new PlatformAdminDetails(0L, "system", "").getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(systemAuth);
        try {
            seedSushi();
            seedHotpot();
        } finally {
            SecurityContextHolder.clearContext();
        }
        log.info("Demo data seeded: {} and {}", SUSHI_NAME, HOTPOT_NAME);
    }

    // ------------------------------------------------------------------ Tokyo Sushi

    private void seedSushi() {
        String logo = img("1.寿司店logo.png", "logos/sushi-logo.png");
        RestaurantResponse r = restaurantService.createRestaurant(
                CreateRestaurantRequest.builder()
                        .name(SUSHI_NAME)
                        .description("Authentic Japanese sushi crafted with the freshest ingredients")
                        .address("12 High Street, Auckland CBD")
                        .phone("+64 9 111 2233")
                        .logoUrl(logo)
                        .build());
        String rid = r.getId();

        staff(rid);

        CategoryResponse starters = cat(rid, "Starters", 1);
        CategoryResponse sushi    = cat(rid, "Sushi", 2);
        CategoryResponse desserts = cat(rid, "Desserts", 3);
        CategoryResponse drinks   = cat(rid, "Drinks", 4);

        item(rid, starters, "Seaweed Salad",  "Tender seaweed strips with a soy and rice-vinegar dressing", new BigDecimal("12.00"), "1.1-1海带丝.png",   "dishes/sushi/");
        item(rid, starters, "Wasabi Octopus", "Fresh octopus tossed in a punchy wasabi sauce",              new BigDecimal("18.00"), "1.1-2芥末章鱼.png", "dishes/sushi/");

        item(rid, sushi, "Hand Roll",     "Crispy nori cone filled with fresh fish and seasoned rice",      new BigDecimal("16.00"), "1.2-1手卷寿司.png", "dishes/sushi/");
        item(rid, sushi, "Nigiri",        "Hand-pressed sushi rice topped with a slice of premium fish",    new BigDecimal("22.00"), "1.2-2手握寿司.png", "dishes/sushi/");
        item(rid, sushi, "Gunkan",        "Battleship-shaped nigiri topped with ikura or sea urchin",       new BigDecimal("20.00"), "1.2-3军舰寿司.png", "dishes/sushi/");
        item(rid, sushi, "Cucumber Roll", "Light and refreshing roll with crisp cucumber filling",          new BigDecimal("10.00"), "1.2-4黄瓜卷.png",  "dishes/sushi/");

        item(rid, desserts, "Dorayaki",         "Fluffy pancake sandwich filled with sweet red bean paste",     new BigDecimal("8.00"),  "1.3-1铜锣烧.png",  "dishes/sushi/");
        item(rid, desserts, "Yokan",            "Traditional Japanese red bean jelly — silky and subtly sweet", new BigDecimal("9.00"),  "1.3-2羊羹.png",    "dishes/sushi/");
        item(rid, desserts, "Strawberry Mochi", "Fresh strawberry wrapped in soft, chewy mochi",                new BigDecimal("10.00"), "1.3-3草莓大福.png", "dishes/sushi/");

        item(rid, drinks, "Sake",  "Premium Japanese sake, served warm or chilled",  new BigDecimal("14.00"), "1.4-1清酒.png",  "dishes/sushi/");
        item(rid, drinks, "Vodka", "Imported vodka with a clean, crisp finish",       new BigDecimal("12.00"), "1.4-2伏特加.png", "dishes/sushi/");

        tables(rid, 8);
    }

    // ------------------------------------------------------------------ Sichuan Hotpot

    private void seedHotpot() {
        String logo = img("2.火锅店logo.png", "logos/hotpot-logo.png");
        RestaurantResponse r = restaurantService.createRestaurant(
                CreateRestaurantRequest.builder()
                        .name(HOTPOT_NAME)
                        .description("Authentic Sichuan mala hotpot with rich, aromatic broth and the freshest ingredients")
                        .address("88 Victoria Street, Auckland CBD")
                        .phone("+64 9 222 3344")
                        .logoUrl(logo)
                        .build());
        String rid = r.getId();

        staff(rid);

        CategoryResponse broth   = cat(rid, "Broth", 1);
        CategoryResponse veggies = cat(rid, "Vegetables", 2);
        CategoryResponse meat    = cat(rid, "Meat", 3);
        CategoryResponse drinks  = cat(rid, "Drinks", 4);

        item(rid, broth, "Twin Pot",    "Half spicy mala broth, half nourishing clear broth — best of both", new BigDecimal("20.00"), "2.1-1鸳鸯锅.png", "dishes/hotpot/");
        item(rid, broth, "Spicy Pot",   "Authentic Sichuan mala broth with fragrant tallow base",            new BigDecimal("16.00"), "2.1-2辣锅.png",   "dishes/hotpot/");
        item(rid, broth, "Clear Broth", "Mild chicken-bone broth — naturally sweet and clean",               new BigDecimal("14.00"), "2.1-3清汤锅.png", "dishes/hotpot/");

        item(rid, veggies, "Bamboo Shoots",        "Fresh and crunchy bamboo shoots",                        new BigDecimal("8.00"), "2.2-1竹笋.png", "dishes/hotpot/");
        item(rid, veggies, "Chrysanthemum Greens", "Tender greens with a delicate herbal flavour",           new BigDecimal("7.00"), "2.2-2茼蒿.png", "dishes/hotpot/");
        item(rid, veggies, "Shiitake Mushroom",    "Plump, meaty shiitake packed with umami",                new BigDecimal("9.00"), "2.2-3香菇.png", "dishes/hotpot/");

        item(rid, meat, "Beef Slices",     "Premium wagyu-style beef, paper-thin and ready in seconds",     new BigDecimal("22.00"), "2.3-1牛肉卷.png", "dishes/hotpot/");
        item(rid, meat, "Lamb Slices",     "Inner Mongolian lamb, thinly sliced and mild in flavour",       new BigDecimal("20.00"), "2.3-2羊肉卷.png", "dishes/hotpot/");
        item(rid, meat, "Sea Bass Slices", "Fresh sea bass fillets, silky smooth after a quick dip",        new BigDecimal("24.00"), "2.3-3斑鱼片.png", "dishes/hotpot/");

        item(rid, drinks, "Beer", "Ice-cold beer — perfect for taming the heat",                             new BigDecimal("8.00"), "2.4-1啤酒.png", "dishes/hotpot/");
        item(rid, drinks, "Cola", "Classic chilled cola — a timeless spice remedy",                         new BigDecimal("4.00"), "2.4-2可乐.png", "dishes/hotpot/");
        item(rid, drinks, "Milk", "Pure whole milk — gentle on the stomach, great with spicy food",         new BigDecimal("5.00"), "2.4-3牛奶.png", "dishes/hotpot/");

        tables(rid, 10);
    }

    // ------------------------------------------------------------------ helpers

    private void staff(String rid) {
        restaurantUserService.createUser(rid, CreateRestaurantUserRequest.builder()
                .username("owner").password("owner123").role(1).build());
        restaurantUserService.createUser(rid, CreateRestaurantUserRequest.builder()
                .username("waiter").password("waiter123").role(2).build());
        restaurantUserService.createUser(rid, CreateRestaurantUserRequest.builder()
                .username("kitchen").password("kitchen123").role(3).build());
    }

    private CategoryResponse cat(String rid, String name, int sortOrder) {
        return categoryService.create(rid,
                CreateCategoryRequest.builder().name(name).sortOrder(sortOrder).build());
    }

    private void item(String rid, CategoryResponse cat, String name, String desc,
                      BigDecimal price, String imageFile, String prefix) {
        String imageUrl = img(imageFile, prefix + imageFile);
        menuItemService.create(rid, CreateMenuItemRequest.builder()
                .categoryId(cat.getId())
                .name(name)
                .description(desc)
                .price(price)
                .imageUrl(imageUrl)
                .build());
    }

    private void tables(String rid, int count) {
        for (int i = 1; i <= count; i++) {
            tableInfoService.create(rid, CreateTableInfoRequest.builder()
                    .tableNumber(String.format("T%02d", i))
                    .seats(i <= count / 2 ? 2 : 4)
                    .build());
        }
    }

    /**
     * Upload a demo image from classpath to MinIO and return its public URL.
     * Returns null if MinIO is unavailable (so menu items are created without images).
     */
    @Nullable
    private String img(String classpathFilename, String objectName) {
        if (minioService == null) return null;
        ClassPathResource res = new ClassPathResource("demo-images/" + classpathFilename);
        if (!res.exists()) return null;
        try {
            return minioService.uploadStream(
                    res.getInputStream(), res.contentLength(), "image/png", objectName);
        } catch (Exception e) {
            log.warn("Failed to upload demo image {}: {}", classpathFilename, e.getMessage());
            return null;
        }
    }
}
