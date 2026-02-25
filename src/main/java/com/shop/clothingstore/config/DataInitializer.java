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

            System.out.println(">>> INIT SAMPLE DATA");

            /* ======================
             * CATEGORY
             * ====================== */
            Category top = categoryRepo.findBySlug("top").orElseGet(() -> {
                Category c = new Category();
                c.setName("Top");
                c.setSlug("top");
                return categoryRepo.save(c);
            });

            Category bottom = categoryRepo.findBySlug("bottom").orElseGet(() -> {
                Category c = new Category();
                c.setName("Bottom");
                c.setSlug("bottom");
                return categoryRepo.save(c);
            });

            Category accessories = categoryRepo.findBySlug("accessories").orElseGet(() -> {
                Category c = new Category();
                c.setName("Accessories");
                c.setSlug("accessories");
                return categoryRepo.save(c);
            });
            /* ======================
 * SUB CATEGORY
 * ====================== */

// TOP
            SubCategory tee = createSubCategory(subCategoryRepo, "Tee", "tee", top);
            SubCategory hoodie = createSubCategory(subCategoryRepo, "Hoodie", "hoodie", top);
            SubCategory shirt = createSubCategory(subCategoryRepo, "Shirt", "shirt", top);

// BOTTOM
            SubCategory pants = createSubCategory(subCategoryRepo, "Pants", "pants", bottom);
            SubCategory shorts = createSubCategory(subCategoryRepo, "Shorts", "shorts", bottom);
            SubCategory jeans = createSubCategory(subCategoryRepo, "Jeans", "jeans", bottom);

// ACCESSORIES
            SubCategory bag = createSubCategory(subCategoryRepo, "Bag", "bag", accessories);
            SubCategory shoes = createSubCategory(subCategoryRepo, "Shoes", "shoes", accessories);
            SubCategory cap = createSubCategory(subCategoryRepo, "Cap", "cap", accessories);

            /* ======================
             * PRODUCT
             * ====================== */
            Product essentialTee = createProductIfNotExists(
                    productRepo,
                    "Essential Tee",
                    "Áo thun cotton basic",
                    tee
            );

            Product fearHoodie = createProductIfNotExists(
                    productRepo,
                    "Fear Hoodie",
                    "Hoodie oversize streetwear",
                    hoodie
            );

            Product cargoPants = createProductIfNotExists(
                    productRepo,
                    "Cargo Pants",
                    "Quần cargo phong cách street",
                    pants
            );

            Product leatherBag = createProductIfNotExists(
                    productRepo,
                    "Leather Bag",
                    "Túi da tối giản",
                    bag
            );

            /* ======================
             * VARIANTS
             * ====================== */
            if (variantRepo.findByProduct(essentialTee).isEmpty()) {

                ProductVariant v1 = new ProductVariant();
                v1.setProduct(essentialTee);
                v1.setColor("Black");
                v1.setSize("M");
                v1.setPrice(250000);
                v1.setStock(10);
                v1.setSold(0);

                ProductVariant v2 = new ProductVariant();
                v2.setProduct(essentialTee);
                v2.setColor("White");
                v2.setSize("L");
                v2.setPrice(260000);
                v2.setStock(3);
                v2.setSold(0);

                variantRepo.saveAll(List.of(v1, v2));
            }

            if (variantRepo.findByProduct(fearHoodie).isEmpty()) {

                ProductVariant v1 = new ProductVariant();
                v1.setProduct(fearHoodie);
                v1.setColor("Black");
                v1.setSize("M");
                v1.setPrice(550000);
                v1.setStock(5);
                v1.setSold(0);

                variantRepo.save(v1);
            }

            /* ======================
             * IMAGES
             * ====================== */
            if (imageRepo.findByProduct(essentialTee).isEmpty()) {

                ProductImage img1 = new ProductImage();
                img1.setProduct(essentialTee);
                img1.setImageUrl("/images/tee-black.jpg");
                img1.setPrimaryImage(true);

                ProductImage img2 = new ProductImage();
                img2.setProduct(essentialTee);
                img2.setImageUrl("/images/tee-white.jpg");
                img2.setPrimaryImage(false);

                imageRepo.saveAll(List.of(img1, img2));
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
                u.setAddress("Hà Nội");

                userRepo.save(u);
            }

            if (userRepo.findByEmail("admin@test.com").isEmpty()) {

                User a = new User();
                a.setEmail("admin@test.com");
                a.setPassword(passwordEncoder.encode("123456"));
                a.setRole(Role.ADMIN);

                userRepo.save(a);
            }

            System.out.println(">>> SAMPLE DATA READY");
        };
    }

    /* ======================
     * HELPER METHODS
     * ====================== */
    private SubCategory createSubCategory(
            SubCategoryRepository repo,
            String name,
            String slug,
            Category category
    ) {
        return repo.findBySlug(slug).orElseGet(() -> {
            SubCategory sc = new SubCategory();
            sc.setName(name);
            sc.setSlug(slug);
            sc.setCategory(category);
            return repo.save(sc);
        });
    }

    private Product createProductIfNotExists(
            ProductRepository repo,
            String name,
            String description,
            SubCategory subCategory
    ) {

        String baseSlug = toSlug(name);

        return repo.findBySlug(baseSlug).orElseGet(() -> {

            Product p = new Product();
            p.setName(name);
            p.setSlug(baseSlug); // ❗ Không generate unique ở initializer
            p.setDescription(description);
            p.setActive(true);
            p.setSubCategory(subCategory);

            return repo.save(p);
        });
    }

    private String toSlug(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    private String generateUniqueSlug(ProductRepository repo, String baseSlug) {

        String slug = baseSlug;
        int index = 1;

        while (repo.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + index++;
        }

        return slug;
    }
}
