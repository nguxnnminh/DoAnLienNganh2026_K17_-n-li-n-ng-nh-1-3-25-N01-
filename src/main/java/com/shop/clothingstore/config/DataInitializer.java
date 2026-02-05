package com.shop.clothingstore.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductImage;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.CategoryRepository;
import com.shop.clothingstore.repository.ProductImageRepository;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.repository.UserRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            CategoryRepository categoryRepo,
            SubCategoryRepository subCategoryRepo,
            ProductRepository productRepo,
            ProductVariantRepository variantRepo,
            ProductImageRepository imageRepo,
            UserRepository userRepo,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            /* ======================
             * CATEGORY
             * ====================== */
            Category top = categoryRepo.findAll().stream()
                    .filter(c -> c.getName().equalsIgnoreCase("Top"))
                    .findFirst()
                    .orElseGet(() -> {
                        Category c = new Category();
                        c.setName("Top");
                        return categoryRepo.save(c);
                    });

            Category bottom = categoryRepo.findAll().stream()
                    .filter(c -> c.getName().equalsIgnoreCase("Bottom"))
                    .findFirst()
                    .orElseGet(() -> {
                        Category c = new Category();
                        c.setName("Bottom");
                        return categoryRepo.save(c);
                    });

            /* ======================
             * SUB CATEGORY
             * ====================== */
            SubCategory tee = subCategoryRepo.findAll().stream()
                    .filter(sc -> sc.getName().equalsIgnoreCase("Tee"))
                    .findFirst()
                    .orElseGet(() -> {
                        SubCategory sc = new SubCategory();
                        sc.setName("Tee");
                        sc.setCategory(top);
                        return subCategoryRepo.save(sc);
                    });

            SubCategory hoodie = subCategoryRepo.findAll().stream()
                    .filter(sc -> sc.getName().equalsIgnoreCase("Hoodie"))
                    .findFirst()
                    .orElseGet(() -> {
                        SubCategory sc = new SubCategory();
                        sc.setName("Hoodie");
                        sc.setCategory(top);
                        return subCategoryRepo.save(sc);
                    });

            /* ======================
             * PRODUCT
             * ====================== */
            Product teeProduct = productRepo.findAll().stream()
                    .filter(p -> p.getName().equalsIgnoreCase("Essential Tee"))
                    .findFirst()
                    .orElseGet(() -> {
                        Product p = new Product();
                        p.setName("Essential Tee");
                        p.setDescription("Minimal cotton tee");
                        p.setActive(true);
                        p.setSubCategory(tee);
                        return productRepo.save(p);
                    });

            Product hoodieProduct = productRepo.findAll().stream()
                    .filter(p -> p.getName().equalsIgnoreCase("Fear Hoodie"))
                    .findFirst()
                    .orElseGet(() -> {
                        Product p = new Product();
                        p.setName("Fear Hoodie");
                        p.setDescription("Oversize hoodie streetwear");
                        p.setActive(true);
                        p.setSubCategory(hoodie);
                        return productRepo.save(p);
                    });

            /* ======================
             * VARIANT (STOCK TEST)
             * ====================== */
            if (variantRepo.findAll().stream()
                    .noneMatch(v -> v.getProduct().getId().equals(teeProduct.getId()))) {

                ProductVariant v1 = new ProductVariant();
                v1.setProduct(teeProduct);
                v1.setColor("Black");
                v1.setSize("M");
                v1.setPrice(250000);
                v1.setStock(10);
                variantRepo.save(v1);

                ProductVariant v2 = new ProductVariant();
                v2.setProduct(teeProduct);
                v2.setColor("White");
                v2.setSize("L");
                v2.setPrice(260000);
                v2.setStock(2); // LOW STOCK
                variantRepo.save(v2);
            }

            if (variantRepo.findAll().stream()
                    .noneMatch(v -> v.getProduct().getId().equals(hoodieProduct.getId()))) {

                ProductVariant v1 = new ProductVariant();
                v1.setProduct(hoodieProduct);
                v1.setColor("Black");
                v1.setSize("M");
                v1.setPrice(550000);
                v1.setStock(5);
                variantRepo.save(v1);

                ProductVariant v2 = new ProductVariant();
                v2.setProduct(hoodieProduct);
                v2.setColor("Grey");
                v2.setSize("L");
                v2.setPrice(580000);
                v2.setStock(1); // VERY LOW
                variantRepo.save(v2);
            }

            /* ======================
             * PRODUCT IMAGE
             * ====================== */
            if (imageRepo.findAll().isEmpty()) {
                ProductImage i1 = new ProductImage();
                i1.setProduct(teeProduct);
                i1.setImageUrl("/images/tee-1.jpg");
                i1.setPrimaryImage(true);

                ProductImage i2 = new ProductImage();
                i2.setProduct(teeProduct);
                i2.setImageUrl("/images/tee-2.jpg");
                i2.setPrimaryImage(false);

                imageRepo.saveAll(List.of(i1, i2));
            }

            /* ======================
             * USERS
             * ====================== */
            if (userRepo.findByEmail("user@test.com").isEmpty()) {
                User u = new User();
                u.setEmail("user@test.com");
                u.setPassword(passwordEncoder.encode("123456"));
                u.setRole(Role.USER);
                u.setFullName("Test User");
                u.setPhone("0123456789");
                u.setAddress("Ha Noi");
                userRepo.save(u);
            }

            if (userRepo.findByEmail("admin@test.com").isEmpty()) {
                User a = new User();
                a.setEmail("admin@test.com");
                a.setPassword(passwordEncoder.encode("123456"));
                a.setRole(Role.ADMIN);
                userRepo.save(a);
            }

            System.out.println(">>> DATA INITIALIZED OK");
        };
    }
}
