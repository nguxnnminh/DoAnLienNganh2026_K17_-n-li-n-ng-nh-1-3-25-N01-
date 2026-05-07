package com.shop.clothingstore.config;

import java.math.BigDecimal;

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
@Profile("!production")
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

            /* ── CATEGORIES ─────────────────────────────────── */
            Category top = findOrCreateCategory(categoryRepo, "Top", "top");
            Category bottom = findOrCreateCategory(categoryRepo, "Bottom", "bottom");
            Category accessories = findOrCreateCategory(categoryRepo, "Accessories", "accessories");

            /* ── SUB-CATEGORIES ─────────────────────────────── */
            SubCategory tee = findOrCreateSubCategory(subCategoryRepo, "Tee", "tee", top);
            SubCategory hoodie = findOrCreateSubCategory(subCategoryRepo, "Hoodie", "hoodie", top);
            SubCategory shirt = findOrCreateSubCategory(subCategoryRepo, "Shirt", "shirt", top);
            SubCategory pants = findOrCreateSubCategory(subCategoryRepo, "Pants", "pants", bottom);
            SubCategory shorts = findOrCreateSubCategory(subCategoryRepo, "Shorts", "shorts", bottom);
            SubCategory jeans = findOrCreateSubCategory(subCategoryRepo, "Jeans", "jeans", bottom);
            SubCategory bag = findOrCreateSubCategory(subCategoryRepo, "Bag", "bag", accessories);
            SubCategory shoes = findOrCreateSubCategory(subCategoryRepo, "Shoes", "shoes", accessories);
            SubCategory cap = findOrCreateSubCategory(subCategoryRepo, "Cap", "cap", accessories);

            /* ── PRODUCTS ────────────────────────────────────── */
            // TEE — 4 sản phẩm, giá phổ thông ~ 120k–220k
            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Essential Tee", "Áo thun cotton basic, mặc mọi lúc mọi nơi", tee,
                    new String[]{"XS", "S", "M", "L", "XL"},
                    new String[]{"White", "Black", "Gray"},
                    new BigDecimal("129000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Graphic Tee", "Áo thun in họa tiết độc quyền", tee,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"Black", "Navy"},
                    new BigDecimal("159000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Oversized Tee", "Áo thun form rộng streetwear", tee,
                    new String[]{"S", "M", "L", "XL", "XXL"},
                    new String[]{"White", "Black", "Beige", "Olive"},
                    new BigDecimal("189000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Vintage Tee", "Áo thun phong cách retro wash", tee,
                    new String[]{"S", "M", "L"},
                    new String[]{"Sand", "Washed Black"},
                    new BigDecimal("219000"));

            // HOODIE — 2 sản phẩm, giá mid-range 350k–550k
            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Fear Hoodie", "Hoodie oversize streetwear nặng 420gsm", hoodie,
                    new String[]{"S", "M", "L", "XL", "XXL"},
                    new String[]{"Black", "Charcoal", "Cream"},
                    new BigDecimal("459000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Minimal Hoodie", "Hoodie basic tối giản, chất cotton fleece", hoodie,
                    new String[]{"XS", "S", "M", "L", "XL"},
                    new String[]{"White", "Gray", "Black"},
                    new BigDecimal("349000"));

            // SHIRT — 2 sản phẩm
            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Oxford Shirt", "Áo sơ mi Oxford phong cách preppy", shirt,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"White", "Light Blue", "Pink"},
                    new BigDecimal("299000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Flannel Shirt", "Áo sơ mi flannel kẻ ô mùa thu", shirt,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"Red Plaid", "Blue Plaid"},
                    new BigDecimal("339000"));

            // PANTS — 3 sản phẩm, giá 250k–450k
            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Cargo Pants", "Quần cargo nhiều túi phong cách street", pants,
                    new String[]{"28", "30", "32", "34", "36"},
                    new String[]{"Khaki", "Black", "Olive"},
                    new BigDecimal("389000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Jogger Pants", "Quần jogger thể thao co giãn 4 chiều", pants,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"Black", "Gray", "Navy"},
                    new BigDecimal("259000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Wide Leg Pants", "Quần ống rộng high-waist hiện đại", pants,
                    new String[]{"XS", "S", "M", "L"},
                    new String[]{"Black", "Camel", "Brown"},
                    new BigDecimal("429000"));

            // SHORTS — 2 sản phẩm
            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Summer Shorts", "Quần short mùa hè thoáng mát", shorts,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"White", "Navy", "Khaki", "Black"},
                    new BigDecimal("189000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Mesh Shorts", "Quần short lưới thể thao", shorts,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"Black", "Gray"},
                    new BigDecimal("149000"));

            // JEANS — 2 sản phẩm, giá premium
            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Slim Jeans", "Quần jeans slim fit classic", jeans,
                    new String[]{"28", "30", "32", "34"},
                    new String[]{"Indigo", "Black", "Light Wash"},
                    new BigDecimal("549000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Baggy Jeans", "Quần jeans baggy vintage 90s", jeans,
                    new String[]{"28", "30", "32", "34", "36"},
                    new String[]{"Light Wash", "Dark Wash"},
                    new BigDecimal("599000"));

            // BAG — 1 sản phẩm, giá cao
            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Tote Bag", "Túi tote canvas tái chế, bền đẹp", bag,
                    new String[]{"One Size"},
                    new String[]{"Natural", "Black", "Army Green"},
                    new BigDecimal("249000"));

            // SHOES — 2 sản phẩm, giá cao nhất
            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Street Sneaker", "Giày sneaker chunky street style", shoes,
                    new String[]{"39", "40", "41", "42", "43", "44"},
                    new String[]{"White", "Black", "Gray"},
                    new BigDecimal("890000"));

            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Combat Boot", "Boot cổ thấp phong cách quân đội", shoes,
                    new String[]{"39", "40", "41", "42", "43"},
                    new String[]{"Black", "Brown"},
                    new BigDecimal("1190000"));

            // CAP — 1 sản phẩm
            createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "5-Panel Cap", "Mũ 5 tấm logo thêu minimal", cap,
                    new String[]{"One Size"},
                    new String[]{"Black", "White", "Olive", "Navy"},
                    new BigDecimal("179000"));

            /* ── USERS ──────────────────────────────────────── */
            if (userRepo.findByEmail("user@test.com").isEmpty()) {
                User u = new User();
                u.setEmail("user@test.com");
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
                String adminPass = System.getenv("DEV_ADMIN_PASSWORD");
                if (adminPass == null || adminPass.isBlank()) {
                    throw new IllegalStateException(
                        "DEV_ADMIN_PASSWORD environment variable is required. " +
                        "Set it before starting the application (e.g. export DEV_ADMIN_PASSWORD=YourPassword123!)");
                }
                User a = new User();
                a.setEmail("admin@test.com");
                a.setPassword(passwordEncoder.encode(adminPass));
                a.setRole(Role.ADMIN);
                userRepo.save(a);
            }

            System.out.println(">>> SAMPLE DATA READY");
        };
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Category findOrCreateCategory(CategoryRepository repo, String name, String slug) {
        return repo.findBySlug(slug).orElseGet(() -> {
            Category c = new Category();
            c.setName(name);
            c.setSlug(slug);
            return repo.save(c);
        });
    }

    private SubCategory findOrCreateSubCategory(SubCategoryRepository repo,
            String name, String slug, Category category) {
        return repo.findBySlug(slug).orElseGet(() -> {
            SubCategory sc = new SubCategory();
            sc.setName(name);
            sc.setSlug(slug);
            sc.setCategory(category);
            return repo.save(sc);
        });
    }

    /**
     * Creates a product with all size × color combinations if it doesn't exist
     * yet. minPrice is set correctly after saving all variants.
     */
    private void createProductWithVariants(
            ProductRepository productRepo,
            ProductVariantRepository variantRepo,
            ProductImageRepository imageRepo,
            String name, String description,
            SubCategory subCategory,
            String[] sizes, String[] colors,
            BigDecimal basePrice) {

        String baseSlug = toSlug(name);
        if (productRepo.findBySlug(baseSlug).isPresent()) {
            return;
        }

        String slug = generateUniqueSlug(productRepo, baseSlug);

        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(description);
        product.setActive(true);
        product.setSubCategory(subCategory);
        productRepo.save(product);

        // Create size × color variants with slight price variation by size
        int variantCount = 0;
        BigDecimal minCreated = null;
        for (String size : sizes) {
            BigDecimal price = adjustPriceForSize(basePrice, size);
            if (minCreated == null || price.compareTo(minCreated) < 0) {
                minCreated = price;
            }
            for (String color : colors) {
                ProductVariant v = new ProductVariant();
                v.setProduct(product);
                v.setSize(size);
                v.setColor(color);
                v.setPrice(price);
                v.setStock(10 + variantCount % 5);
                v.setSold(variantCount % 8);
                variantRepo.save(v);
                variantCount++;
            }
        }

        // Set denormalized minPrice
        product.setMinPrice(minCreated != null ? minCreated : basePrice);
        productRepo.save(product);

        // Sample image
        if (imageRepo.findByProduct(product).isEmpty()) {
            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setImageUrl("/images/sample.jpg");
            img.setPrimaryImage(true);
            imageRepo.save(img);
        }
    }

    /**
     * XL/XXL/Large numeric sizes carry a small 10k surcharge.
     */
    private BigDecimal adjustPriceForSize(BigDecimal base, String size) {
        String s = size.toUpperCase();
        if (s.equals("XXL") || s.equals("XXXL")) {
            return base.add(new BigDecimal("20000"));
        }
        if (s.equals("XL")) {
            return base.add(new BigDecimal("10000"));
        }
        return base;
    }

    private String toSlug(String input) {
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
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
