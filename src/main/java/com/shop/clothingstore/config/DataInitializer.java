package com.shop.clothingstore.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.entity.Coupon;
import com.shop.clothingstore.entity.Order;
import com.shop.clothingstore.entity.OrderItem;
import com.shop.clothingstore.entity.OrderStatus;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductImage;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.Review;
import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.entity.UserCoupon;
import com.shop.clothingstore.entity.WishlistItem;
import com.shop.clothingstore.repository.CategoryRepository;
import com.shop.clothingstore.repository.CouponRepository;
import com.shop.clothingstore.repository.OrderItemRepository;
import com.shop.clothingstore.repository.OrderRepository;
import com.shop.clothingstore.repository.ProductImageRepository;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.ProductVariantRepository;
import com.shop.clothingstore.repository.ReviewRepository;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.repository.UserCouponRepository;
import com.shop.clothingstore.repository.UserRepository;
import com.shop.clothingstore.repository.WishlistItemRepository;

@Configuration
@Profile("!production")
@SuppressWarnings("unused")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initData(
            CategoryRepository categoryRepo,
            SubCategoryRepository subCategoryRepo,
            ProductRepository productRepo,
            ProductVariantRepository variantRepo,
            ProductImageRepository imageRepo,
            UserRepository userRepo,
            PasswordEncoder passwordEncoder,
            OrderRepository orderRepo,
            OrderItemRepository orderItemRepo,
            ReviewRepository reviewRepo,
            CouponRepository couponRepo,
            UserCouponRepository userCouponRepo,
            WishlistItemRepository wishlistRepo
    ) {
        return args -> {

            log.info("Initializing sample data");

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
            Product essentialTee = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Essential Tee", "Áo thun cotton basic, mặc mọi lúc mọi nơi", tee,
                    new String[]{"XS", "S", "M", "L", "XL"},
                    new String[]{"White", "Black", "Gray"},
                    new BigDecimal("129000"));

            Product graphicTee = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Graphic Tee", "Áo thun in họa tiết độc quyền", tee,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"Black", "Navy"},
                    new BigDecimal("159000"));

            Product oversizedTee = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Oversized Tee", "Áo thun form rộng streetwear", tee,
                    new String[]{"S", "M", "L", "XL", "XXL"},
                    new String[]{"White", "Black", "Beige", "Olive"},
                    new BigDecimal("189000"));

            Product vintageTee = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Vintage Tee", "Áo thun phong cách retro wash", tee,
                    new String[]{"S", "M", "L"},
                    new String[]{"Sand", "Washed Black"},
                    new BigDecimal("219000"));

            Product fearHoodie = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Fear Hoodie", "Hoodie oversize streetwear nặng 420gsm", hoodie,
                    new String[]{"S", "M", "L", "XL", "XXL"},
                    new String[]{"Black", "Charcoal", "Cream"},
                    new BigDecimal("459000"));

            Product minimalHoodie = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Minimal Hoodie", "Hoodie basic tối giản, chất cotton fleece", hoodie,
                    new String[]{"XS", "S", "M", "L", "XL"},
                    new String[]{"White", "Gray", "Black"},
                    new BigDecimal("349000"));

            Product oxfordShirt = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Oxford Shirt", "Áo sơ mi Oxford phong cách preppy", shirt,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"White", "Light Blue", "Pink"},
                    new BigDecimal("299000"));

            Product flannelShirt = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Flannel Shirt", "Áo sơ mi flannel kẻ ô mùa thu", shirt,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"Red Plaid", "Blue Plaid"},
                    new BigDecimal("339000"));

            Product cargoPants = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Cargo Pants", "Quần cargo nhiều túi phong cách street", pants,
                    new String[]{"28", "30", "32", "34", "36"},
                    new String[]{"Khaki", "Black", "Olive"},
                    new BigDecimal("389000"));

            Product joggerPants = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Jogger Pants", "Quần jogger thể thao co giãn 4 chiều", pants,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"Black", "Gray", "Navy"},
                    new BigDecimal("259000"));

            Product wideLegPants = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Wide Leg Pants", "Quần ống rộng high-waist hiện đại", pants,
                    new String[]{"XS", "S", "M", "L"},
                    new String[]{"Black", "Camel", "Brown"},
                    new BigDecimal("429000"));

            Product summerShorts = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Summer Shorts", "Quần short mùa hè thoáng mát", shorts,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"White", "Navy", "Khaki", "Black"},
                    new BigDecimal("189000"));

            Product meshShorts = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Mesh Shorts", "Quần short lưới thể thao", shorts,
                    new String[]{"S", "M", "L", "XL"},
                    new String[]{"Black", "Gray"},
                    new BigDecimal("149000"));

            Product slimJeans = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Slim Jeans", "Quần jeans slim fit classic", jeans,
                    new String[]{"28", "30", "32", "34"},
                    new String[]{"Indigo", "Black", "Light Wash"},
                    new BigDecimal("549000"));

            Product baggyJeans = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Baggy Jeans", "Quần jeans baggy vintage 90s", jeans,
                    new String[]{"28", "30", "32", "34", "36"},
                    new String[]{"Light Wash", "Dark Wash"},
                    new BigDecimal("599000"));

            Product toteBag = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Tote Bag", "Túi tote canvas tái chế, bền đẹp", bag,
                    new String[]{"One Size"},
                    new String[]{"Natural", "Black", "Army Green"},
                    new BigDecimal("249000"));

            Product streetSneaker = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Street Sneaker", "Giày sneaker chunky street style", shoes,
                    new String[]{"39", "40", "41", "42", "43", "44"},
                    new String[]{"White", "Black", "Gray"},
                    new BigDecimal("890000"));

            Product combatBoot = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "Combat Boot", "Boot cổ thấp phong cách quân đội", shoes,
                    new String[]{"39", "40", "41", "42", "43"},
                    new String[]{"Black", "Brown"},
                    new BigDecimal("1190000"));

            Product fivePanelCap = createProductWithVariants(productRepo, variantRepo, imageRepo,
                    "5-Panel Cap", "Mũ 5 tấm logo thêu minimal", cap,
                    new String[]{"One Size"},
                    new String[]{"Black", "White", "Olive", "Navy"},
                    new BigDecimal("179000"));

            // Collect all products for wishlist seeding
            List<Product> allProducts = List.of(
                    essentialTee, graphicTee, oversizedTee, vintageTee,
                    fearHoodie, minimalHoodie, oxfordShirt, flannelShirt,
                    cargoPants, joggerPants, wideLegPants, summerShorts,
                    meshShorts, slimJeans, baggyJeans, toteBag,
                    streetSneaker, combatBoot, fivePanelCap
            );
            // Filter out nulls (products that already existed and returned null)
            List<Product> products = allProducts.stream()
                    .filter(p -> p != null)
                    .toList();

            /* ── USERS ──────────────────────────────────────── */
            User testUser = findOrCreateUser(userRepo, passwordEncoder,
                    "user@test.com", "User@Dev2024!", Role.USER,
                    "Nguyễn Văn An", "0901234567", "123 Nguyễn Huệ, Q.1, TP.HCM");

            User user2 = findOrCreateUser(userRepo, passwordEncoder,
                    "lan@test.com", "User@Dev2024!", Role.USER,
                    "Trần Thị Lan", "0912345678", "456 Lê Lợi, Q.3, TP.HCM");

            User user3 = findOrCreateUser(userRepo, passwordEncoder,
                    "minh@test.com", "User@Dev2024!", Role.USER,
                    "Lê Hoàng Minh", "0923456789", "789 Điện Biên Phủ, Q.Bình Thạnh, TP.HCM");

            User user4 = findOrCreateUser(userRepo, passwordEncoder,
                    "huong@test.com", "User@Dev2024!", Role.USER,
                    "Phạm Thu Hương", "0934567890", "12 Trần Hưng Đạo, Q.5, TP.HCM");

            User user5 = findOrCreateUser(userRepo, passwordEncoder,
                    "duc@test.com", "User@Dev2024!", Role.USER,
                    "Võ Đức Thắng", "0945678901", "34 Hai Bà Trưng, Q.1, TP.HCM");

            // Admin
            if (userRepo.findByEmail("admin@test.com").isEmpty()) {
                String adminPass = System.getenv("DEV_ADMIN_PASSWORD");
                if (adminPass == null || adminPass.isBlank()) {
                    throw new IllegalStateException(
                            "DEV_ADMIN_PASSWORD environment variable is required. "
                            + "Set it before starting the application (e.g. export DEV_ADMIN_PASSWORD=YourPassword123!)");
                }
                User a = new User();
                a.setEmail("admin@test.com");
                a.setPassword(passwordEncoder.encode(adminPass));
                a.setRole(Role.ADMIN);
                a.setFullName("Admin");
                a.setPhone("0999999999");
                userRepo.save(a);
            }

            /* ── COUPONS ─────────────────────────────────────── */
            Coupon welcome10 = findOrCreateCoupon(couponRepo,
                    "WELCOME10", "Giảm 10% cho đơn hàng đầu tiên",
                    Coupon.DiscountType.PERCENTAGE, new BigDecimal("10"),
                    new BigDecimal("100000"), null, 100, false);

            Coupon summer50k = findOrCreateCoupon(couponRepo,
                    "SUMMER50K", "Giảm 50.000đ cho đơn từ 300k",
                    Coupon.DiscountType.FIXED, new BigDecimal("50000"),
                    new BigDecimal("300000"), null, 50, false);

            Coupon flash20 = findOrCreateCoupon(couponRepo,
                    "FLASH20", "Flash sale cuối tuần - giảm 20%",
                    Coupon.DiscountType.PERCENTAGE, new BigDecimal("20"),
                    new BigDecimal("200000"),
                    LocalDateTime.now().plusDays(7), 30, false);

            Coupon vip100k = findOrCreateCoupon(couponRepo,
                    "VIP100K", "Ưu đãi VIP - giảm 100.000đ",
                    Coupon.DiscountType.FIXED, new BigDecimal("100000"),
                    new BigDecimal("500000"), null, 20, true);

            Coupon newuser15 = findOrCreateCoupon(couponRepo,
                    "NEWUSER15", "Tân thành viên - giảm 15%",
                    Coupon.DiscountType.PERCENTAGE, new BigDecimal("15"),
                    new BigDecimal("150000"), null, 200, true);

            // Assign user-specific coupons
            assignCouponToUser(userCouponRepo, testUser, vip100k);
            assignCouponToUser(userCouponRepo, testUser, newuser15);
            assignCouponToUser(userCouponRepo, user2, newuser15);
            assignCouponToUser(userCouponRepo, user3, vip100k);
            assignCouponToUser(userCouponRepo, user4, newuser15);

            /* ── ORDERS + REVIEWS ────────────────────────────── */
            if (products.size() >= 5) {
                Product p0 = products.get(0); // Essential Tee
                Product p1 = products.get(1); // Graphic Tee
                Product p2 = products.get(4); // Fear Hoodie
                Product p3 = products.get(8); // Cargo Pants
                Product p4 = products.get(13); // Slim Jeans
                Product p5 = products.get(16); // Street Sneaker
                Product p6 = products.get(5);  // Minimal Hoodie
                Product p7 = products.get(9);  // Jogger Pants

                // ---- testUser orders ----
                Order order1 = createOrder(orderRepo, orderItemRepo,
                        testUser, OrderStatus.COMPLETED,
                        "Nguyễn Văn An", "0901234567", "123 Nguyễn Huệ, Q.1, TP.HCM",
                        LocalDateTime.now().minusDays(30),
                        List.of(
                                newItem(p0, variantRepo, "M", "White", 2),
                                newItem(p1, variantRepo, "L", "Black", 1)
                        ));

                Order order2 = createOrder(orderRepo, orderItemRepo,
                        testUser, OrderStatus.COMPLETED,
                        "Nguyễn Văn An", "0901234567", "123 Nguyễn Huệ, Q.1, TP.HCM",
                        LocalDateTime.now().minusDays(15),
                        List.of(
                                newItem(p2, variantRepo, "L", "Black", 1),
                                newItem(p3, variantRepo, "32", "Khaki", 1)
                        ));

                Order order3 = createOrder(orderRepo, orderItemRepo,
                        testUser, OrderStatus.SHIPPING,
                        "Nguyễn Văn An", "0901234567", "123 Nguyễn Huệ, Q.1, TP.HCM",
                        LocalDateTime.now().minusDays(3),
                        List.of(
                                newItem(p4, variantRepo, "30", "Indigo", 1)
                        ));

                Order order4 = createOrder(orderRepo, orderItemRepo,
                        testUser, OrderStatus.PENDING,
                        "Nguyễn Văn An", "0901234567", "123 Nguyễn Huệ, Q.1, TP.HCM",
                        LocalDateTime.now().minusHours(2),
                        List.of(
                                newItem(p5, variantRepo, "42", "White", 1),
                                newItem(p6, variantRepo, "M", "Gray", 1)
                        ));

                Order order5 = createOrder(orderRepo, orderItemRepo,
                        testUser, OrderStatus.CANCELLED,
                        "Nguyễn Văn An", "0901234567", "123 Nguyễn Huệ, Q.1, TP.HCM",
                        LocalDateTime.now().minusDays(45),
                        List.of(
                                newItem(p7, variantRepo, "M", "Black", 2)
                        ));

                // ---- user2 orders ----
                Order order6 = createOrder(orderRepo, orderItemRepo,
                        user2, OrderStatus.COMPLETED,
                        "Trần Thị Lan", "0912345678", "456 Lê Lợi, Q.3, TP.HCM",
                        LocalDateTime.now().minusDays(20),
                        List.of(
                                newItem(p0, variantRepo, "S", "Black", 3),
                                newItem(p6, variantRepo, "S", "White", 1)
                        ));

                Order order7 = createOrder(orderRepo, orderItemRepo,
                        user2, OrderStatus.PROCESSING,
                        "Trần Thị Lan", "0912345678", "456 Lê Lợi, Q.3, TP.HCM",
                        LocalDateTime.now().minusDays(1),
                        List.of(
                                newItem(p4, variantRepo, "28", "Black", 1)
                        ));

                // ---- user3 orders ----
                Order order8 = createOrder(orderRepo, orderItemRepo,
                        user3, OrderStatus.COMPLETED,
                        "Lê Hoàng Minh", "0923456789", "789 Điện Biên Phủ, Q.Bình Thạnh, TP.HCM",
                        LocalDateTime.now().minusDays(10),
                        List.of(
                                newItem(p5, variantRepo, "41", "Black", 1),
                                newItem(p3, variantRepo, "30", "Olive", 1),
                                newItem(p1, variantRepo, "M", "Navy", 2)
                        ));

                Order order9 = createOrder(orderRepo, orderItemRepo,
                        user3, OrderStatus.COMPLETED,
                        "Lê Hoàng Minh", "0923456789", "789 Điện Biên Phủ, Q.Bình Thạnh, TP.HCM",
                        LocalDateTime.now().minusDays(5),
                        List.of(
                                newItem(p2, variantRepo, "M", "Charcoal", 1)
                        ));

                // ---- user4 orders ----
                Order order10 = createOrder(orderRepo, orderItemRepo,
                        user4, OrderStatus.SHIPPING,
                        "Phạm Thu Hương", "0934567890", "12 Trần Hưng Đạo, Q.5, TP.HCM",
                        LocalDateTime.now().minusDays(2),
                        List.of(
                                newItem(p0, variantRepo, "XS", "White", 2),
                                newItem(products.get(10), variantRepo, "S", "Camel", 1)
                        ));

                // ---- user5 orders ----
                Order order11 = createOrder(orderRepo, orderItemRepo,
                        user5, OrderStatus.COMPLETED,
                        "Võ Đức Thắng", "0945678901", "34 Hai Bà Trưng, Q.1, TP.HCM",
                        LocalDateTime.now().minusDays(8),
                        List.of(
                                newItem(p5, variantRepo, "43", "White", 1),
                                newItem(p3, variantRepo, "34", "Black", 1)
                        ));

                Order order12 = createOrder(orderRepo, orderItemRepo,
                        user5, OrderStatus.PENDING,
                        "Võ Đức Thắng", "0945678901", "34 Hai Bà Trưng, Q.1, TP.HCM",
                        LocalDateTime.now().minusMinutes(30),
                        List.of(
                                newItem(products.get(14), variantRepo, "30", "Light Wash", 1)
                        ));

                /* ── REVIEWS (chỉ cho COMPLETED orders) ─────── */
                // Reviews for order1 items (Essential Tee, Graphic Tee)
                createReviewForOrderItem(reviewRepo, order1, 0, testUser, p0.getId(), 5,
                        "Áo chất lượng tốt, cotton mềm mại, mặc rất thoải mái. Size đúng như mô tả, màu sắc đẹp. Sẽ mua thêm!");
                createReviewForOrderItem(reviewRepo, order1, 1, testUser, p1.getId(), 4,
                        "Họa tiết in sắc nét, không bị nhòe sau nhiều lần giặt. Form áo vừa vặn, chỉ hơi tiếc màu hơi khác ảnh một chút.");

                // Reviews for order2 items (Fear Hoodie, Cargo Pants)
                createReviewForOrderItem(reviewRepo, order2, 0, testUser, p2.getId(), 5,
                        "Hoodie xịn thật sự! Vải dày 420gsm, mặc mùa đông rất ấm. Form oversize chuẩn, mặc cùng jogger rất ngầu.");
                createReviewForOrderItem(reviewRepo, order2, 1, testUser, p3.getId(), 4,
                        "Quần cargo nhiều túi tiện lợi, chất vải bền. Màu khaki nhìn rất đẹp ngoài thực tế. Giao hàng nhanh.");

                // Reviews for order6 items (lan@test)
                createReviewForOrderItem(reviewRepo, order6, 0, user2, p0.getId(), 5,
                        "Mua cho cả nhà mặc, ai cũng thích. Vải cotton 100% thoáng mát, giặt không co. Giá hợp lý.");
                createReviewForOrderItem(reviewRepo, order6, 1, user2, p6.getId(), 4,
                        "Hoodie trắng rất dễ phối đồ, chất fleece mềm. Mình mua size S vừa đẹp.");

                // Reviews for order8 items (minh@test - Street Sneaker, Cargo, Graphic Tee)
                createReviewForOrderItem(reviewRepo, order8, 0, user3, p5.getId(), 5,
                        "Giày đẹp xuất sắc! Đế chunky rất trendy, đi vào form chân rất chắc chắn. Nhận được hàng đúng size, đóng gói cẩn thận.");
                createReviewForOrderItem(reviewRepo, order8, 1, user3, p3.getId(), 3,
                        "Quần ổn nhưng màu olive hơi nhạt hơn ảnh. Size đúng, chất vải tốt. Túi nhiều tiện dụng.");
                createReviewForOrderItem(reviewRepo, order8, 2, user3, p1.getId(), 5,
                        "Mua 2 cái cho 2 màu, cả 2 đều đẹp. Họa tiết unique, không thấy ai mặc giống mình bao giờ. Recommend mạnh!");

                // Reviews for order9 items (minh - Fear Hoodie)
                createReviewForOrderItem(reviewRepo, order9, 0, user3, p2.getId(), 4,
                        "Hoodie màu charcoal rất sang, không bị nhàm. Chất vải nặng tay, mặc vào ấm lắm. Chỉ hơi khó giặt bằng tay.");

                // Reviews for order11 items (duc - Street Sneaker, Cargo Pants)
                createReviewForOrderItem(reviewRepo, order11, 0, user5, p5.getId(), 5,
                        "Sản phẩm xịn hơn mình mong đợi. Giày đẹp, chất liệu tốt. Đi cả ngày không đau chân. Đóng gói rất cẩn thận.");
                createReviewForOrderItem(reviewRepo, order11, 1, user5, p3.getId(), 4,
                        "Quần cargo đúng chuẩn street style. Màu đen rất dễ phối. Nhiều túi tiện để điện thoại, ví, đồ linh tinh.");
            }

            /* ── WISHLISTS ───────────────────────────────────── */
            if (products.size() >= 10 && testUser != null) {
                addToWishlist(wishlistRepo, testUser, products.get(4));   // Fear Hoodie
                addToWishlist(wishlistRepo, testUser, products.get(13));  // Slim Jeans
                addToWishlist(wishlistRepo, testUser, products.get(16));  // Street Sneaker
                addToWishlist(wishlistRepo, testUser, products.get(18));  // 5-Panel Cap
                addToWishlist(wishlistRepo, testUser, products.get(14));  // Baggy Jeans

                addToWishlist(wishlistRepo, user2, products.get(0));      // Essential Tee
                addToWishlist(wishlistRepo, user2, products.get(5));      // Minimal Hoodie
                addToWishlist(wishlistRepo, user2, products.get(10));     // Wide Leg Pants

                addToWishlist(wishlistRepo, user3, products.get(16));     // Street Sneaker
                addToWishlist(wishlistRepo, user3, products.get(4));      // Fear Hoodie
                addToWishlist(wishlistRepo, user3, products.get(6));      // Oxford Shirt
            }

            log.info("Sample data ready");
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

    private Product createProductWithVariants(
            ProductRepository productRepo,
            ProductVariantRepository variantRepo,
            ProductImageRepository imageRepo,
            String name, String description,
            SubCategory subCategory,
            String[] sizes, String[] colors,
            BigDecimal basePrice) {

        String baseSlug = toSlug(name);
        if (productRepo.findBySlug(baseSlug).isPresent()) {
            return productRepo.findBySlug(baseSlug).get();
        }

        String slug = generateUniqueSlug(productRepo, baseSlug);

        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(description);
        product.setActive(true);
        product.setSubCategory(subCategory);
        productRepo.save(product);

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

        product.setMinPrice(minCreated != null ? minCreated : basePrice);
        productRepo.save(product);

        if (imageRepo.findByProduct(product).isEmpty()) {
            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setImageUrl("/images/sample.jpg");
            img.setPrimaryImage(true);
            imageRepo.save(img);
        }

        return product;
    }

    private User findOrCreateUser(UserRepository repo, PasswordEncoder encoder,
            String email, String password, Role role,
            String fullName, String phone, String address) {
        return repo.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            String pass = System.getenv("DEV_USER_PASSWORD");
            if (pass == null || pass.isBlank()) {
                pass = password;
            }
            u.setPassword(encoder.encode(pass));
            u.setRole(role);
            u.setFullName(fullName);
            u.setPhone(phone);
            u.setAddress(address);
            return repo.save(u);
        });
    }

    private Coupon findOrCreateCoupon(CouponRepository repo,
            String code, String description,
            Coupon.DiscountType type, BigDecimal value,
            BigDecimal minOrder, LocalDateTime expiry,
            int usageLimit, boolean userSpecific) {
        return repo.findByCodeAndActiveTrue(code).orElseGet(() -> {
            Coupon c = new Coupon();
            c.setCode(code);
            c.setDescription(description);
            c.setDiscountType(type);
            c.setDiscountValue(value);
            c.setMinOrderAmount(minOrder);
            c.setExpiryDate(expiry);
            c.setUsageLimit(usageLimit);
            c.setUsageCount(0);
            c.setActive(true);
            c.setUserSpecific(userSpecific);
            return repo.save(c);
        });
    }

    private void assignCouponToUser(UserCouponRepository repo, User user, Coupon coupon) {
        if (user == null || coupon == null) {
            return;
        }
        if (!repo.existsByUserAndCoupon(user, coupon)) {
            UserCoupon uc = new UserCoupon();
            uc.setUser(user);
            uc.setCoupon(coupon);
            uc.setUsed(false);
            repo.save(uc);
        }
    }

    private OrderItem newItem(Product product, ProductVariantRepository variantRepo,
            String size, String color, int qty) {
        if (product == null) {
            return null;
        }
        ProductVariant variant = variantRepo.findByProductAndSizeAndColor(product, size, color)
                .orElse(variantRepo.findByProduct(product).stream().findFirst().orElse(null));
        if (variant == null) {
            return null;
        }

        OrderItem item = new OrderItem();
        item.setProductName(product.getName());
        item.setSize(size);
        item.setColor(color);
        item.setPrice(variant.getPrice());
        item.setQuantity(qty);
        item.setVariantId(variant.getId());
        return item;
    }

    private Order createOrder(OrderRepository orderRepo, OrderItemRepository orderItemRepo,
            User user, OrderStatus status,
            String customerName, String phone, String address,
            LocalDateTime createdAt,
            List<OrderItem> rawItems) {

        if (user == null) {
            return null;
        }

        // Filter out null items
        List<OrderItem> items = rawItems.stream().filter(i -> i != null).toList();
        if (items.isEmpty()) {
            return null;
        }

        BigDecimal total = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setActor(user);
        order.setStatus(status);
        order.setCustomerName(customerName);
        order.setPhone(phone);
        order.setAddress(address);
        order.setTotal(total);
        order.setShippingFee(new BigDecimal("30000"));
        Order saved = orderRepo.save(order);

        for (OrderItem item : items) {
            item.setOrder(saved);
            orderItemRepo.save(item);
        }

        return saved;
    }

    private void createReviewForOrderItem(ReviewRepository reviewRepo,
            Order order, int itemIndex, User user, Long productId,
            int rating, String comment) {
        if (order == null || order.getItems() == null) {
            return;
        }
        List<OrderItem> items = order.getItems();
        if (itemIndex >= items.size()) {
            return;
        }
        OrderItem oi = items.get(itemIndex);
        if (reviewRepo.findByOrderItem_Id(oi.getId()).isPresent()) {
            return;
        }

        Review r = new Review();
        r.setActor(user);
        r.setOrderItem(oi);
        r.setItemId(productId);
        r.setRating(rating);
        r.setComment(comment);
        reviewRepo.save(r);
    }

    private void addToWishlist(WishlistItemRepository repo, User user, Product product) {
        if (user == null || product == null) {
            return;
        }
        if (!repo.existsByUserAndProduct(user, product)) {
            WishlistItem w = new WishlistItem();
            w.setUser(user);
            w.setProduct(product);
            repo.save(w);
        }
    }

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
