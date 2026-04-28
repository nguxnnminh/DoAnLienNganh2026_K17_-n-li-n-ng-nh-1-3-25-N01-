package com.shop.clothingstore.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
@Profile("!production") // Không chạy trên production — tránh overwrite dữ liệu thật
@SuppressWarnings("unused")
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
            // ================== TEE (4 products) ==================
            Product essentialTee = createProductIfNotExists(productRepo,
                    "Essential Tee", "Áo thun cotton basic", tee);

            Product graphicTee = createProductIfNotExists(productRepo,
                    "Graphic Tee", "Áo thun in họa tiết", tee);

            Product oversizedTee = createProductIfNotExists(productRepo,
                    "Oversized Tee", "Áo thun form rộng", tee);

            Product vintageTee = createProductIfNotExists(productRepo,
                    "Vintage Tee", "Áo thun phong cách retro", tee);

            // ================== HOODIE (1 product) ==================
            Product fearHoodie = createProductIfNotExists(productRepo,
                    "Fear Hoodie", "Hoodie oversize streetwear", hoodie);

            // ================== SHIRT (0 product) ==================
            // ❗ cố tình không thêm sản phẩm cho shirt
            // ================== PANTS (3 products) ==================
            Product cargoPants = createProductIfNotExists(productRepo,
                    "Cargo Pants", "Quần cargo phong cách street", pants);

            Product joggerPants = createProductIfNotExists(productRepo,
                    "Jogger Pants", "Quần jogger thể thao", pants);

            Product widePants = createProductIfNotExists(productRepo,
                    "Wide Pants", "Quần ống rộng hiện đại", pants);

            // ================== SHORTS (1 product) ==================
            Product summerShorts = createProductIfNotExists(productRepo,
                    "Summer Shorts", "Quần short mùa hè", shorts);

            // ================== JEANS (0 product) ==================
            // ❗ cố tình không thêm sản phẩm cho jeans
            // ================== BAG (1 product) ==================
            Product leatherBag = createProductIfNotExists(productRepo,
                    "Leather Bag", "Túi da tối giản", bag);

            // ================== SHOES (2 products) ==================
            Product sneaker = createProductIfNotExists(productRepo,
                    "Street Sneaker", "Giày sneaker street style", shoes);

            Product boot = createProductIfNotExists(productRepo,
                    "Combat Boot", "Boot phong cách quân đội", shoes);

            // ================== CAP (0 product) ==================
            // ❗ cố tình không thêm sản phẩm cho cap

            /* ======================
            * AUTO CREATE VARIANTS
            * ====================== */
            List<Product> allProducts = productRepo.findAll();

            for (Product product : allProducts) {

                if (variantRepo.findByProduct(product).isEmpty()) {

                    ProductVariant v = new ProductVariant();
                    v.setProduct(product);
                    v.setColor("Black");
                    v.setSize("M");
                    v.setPrice(120000.0);
                    v.setStock(10);
                    v.setSold(0);

                    variantRepo.save(v);
                }
            }

            /* ======================
            * AUTO CREATE IMAGES
            * ====================== */
            for (Product product : productRepo.findAll()) {

                if (imageRepo.findByProduct(product).isEmpty()) {

                    ProductImage img = new ProductImage();
                    img.setProduct(product);
                    img.setImageUrl("/images/sample.jpg");
                    img.setPrimaryImage(true);

                    imageRepo.save(img);
                }
            }

            /* ======================
             * USERS
             * ====================== */
            if (userRepo.findByEmail("user@test.com").isEmpty()) {

                User u = new User();
                u.setEmail("user@test.com");
                // Lấy password từ env var DEV_USER_PASSWORD, fallback là chuỗi mạnh hơn
                String userPass = System.getenv("DEV_USER_PASSWORD");
                if (userPass == null || userPass.isBlank()) {
                    userPass = "User@Dev2024!";
                }
                u.setPassword(passwordEncoder.encode(userPass));
                u.setRole(Role.USER);
                u.setFullName("Test User");
                u.setPhone("0123456789");
                u.setAddress("Hà Nội");

                userRepo.save(u);
            }

            if (userRepo.findByEmail("admin@test.com").isEmpty()) {

                User a = new User();
                a.setEmail("admin@test.com");
                String adminPass = System.getenv("DEV_ADMIN_PASSWORD");
                if (adminPass == null || adminPass.isBlank()) {
                    adminPass = "Admin@Dev2024!";
                }
                a.setPassword(passwordEncoder.encode(adminPass));
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

            // ❗ CHỈ generate khi cần tạo mới
            String uniqueSlug = generateUniqueSlug(repo, baseSlug);

            Product p = new Product();
            p.setName(name);
            p.setSlug(uniqueSlug);
            p.setDescription(description);
            p.setActive(true);
            p.setSubCategory(subCategory);

            return repo.save(p);
        });
    }

    private String toSlug(String input) {
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.toLowerCase();
        slug = slug.replaceAll("[^a-z0-9\\s-]", "");
        slug = slug.replaceAll("\\s+", "-");
        return slug;
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
