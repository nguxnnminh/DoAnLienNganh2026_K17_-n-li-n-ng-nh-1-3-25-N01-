package com.shop.clothingstore.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface ProductRepository extends BaseRepository<Product, Long> {

    /*
     * =====================================================
     * PRODUCT DETAIL BY ID
     * Load đầy đủ dữ liệu tránh N+1 query
     * =====================================================
     */
    @Override
    @EntityGraph(attributePaths = {
        "productVariants",
        "images",
        "subCategory",
        "subCategory.category"
    })
    @NonNull
    Optional<Product> findById(@NonNull Long id);


    /*
     * =====================================================
     * PRODUCT DETAIL BY SLUG
     * Dùng cho trang product detail ngoài shop
     * =====================================================
     */
    @EntityGraph(attributePaths = {
        "productVariants",
        "images",
        "subCategory",
        "subCategory.category"
    })
    Optional<Product> findBySlug(String slug);


    /*
     * =====================================================
     * SIMPLE SEARCH
     * =====================================================
     */
    Optional<Product> findByName(String name);


    /*
     * =====================================================
     * TOP PRODUCTS BY CATEGORY
     * Sắp xếp theo variant bán nhiều nhất
     * =====================================================
     */
    @Query("""
    SELECT p
    FROM Product p
    JOIN p.subCategory sc
    JOIN sc.category c
    LEFT JOIN p.productVariants v
    WHERE c.slug = :slug
    GROUP BY p
    ORDER BY COALESCE(MAX(v.sold),0) DESC, p.id DESC
""")
    Page<Product> findTopByCategorySlug(
            @Param("slug") String slug,
            Pageable pageable
    );


    /*
     * =====================================================
     * PRODUCT FOR EDIT (ADMIN)
     * Load variants + images để edit
     * =====================================================
     */
    @Query("""
    SELECT DISTINCT p
    FROM Product p
    LEFT JOIN FETCH p.productVariants
    WHERE p.id = :id
""")
    Optional<Product> findProductForEdit(@Param("id") Long id);


    /*
     * =====================================================
     * ADMIN LIST WITH RELATIONS
     * Tránh N+1 khi hiển thị danh sách sản phẩm
     * =====================================================
     */
    @Override
    @EntityGraph(attributePaths = {
        "subCategory",
        "subCategory.category"
    })
    @NonNull
    Page<Product> findAll(@NonNull Pageable pageable);

    /*
     * =====================================================
     * PRODUCT LIST WITH SPECIFICATION + ENTITYGRAPH
     * Tránh N+1 khi dùng filter API (shop + admin).
     * productVariants included so product.getTotalStock()
     * doesn't trigger a lazy SELECT per product in admin.
     * Hibernate fetches variants in one batch SELECT, not N.
     * =====================================================
     */
    @Override
    @EntityGraph(attributePaths = {
        "subCategory",
        "subCategory.category",
        "images",
        "productVariants"
    })
    @NonNull
    Page<Product> findAll(@Nullable Specification<Product> spec, @NonNull Pageable pageable);

    // =====================================================
    // RECOMMENDATION QUERIES
    // EntityGraph fetches images + subCategory eagerly so the product-detail
    // template does NOT trigger lazy loads for each related product card.
    // productVariants are NOT fetched here — use product.minPrice in templates.
    // =====================================================
    @EntityGraph(attributePaths = {"images", "subCategory", "subCategory.category"})
    @Query("""
        SELECT p FROM Product p
        WHERE p.subCategory.id = :subCategoryId
          AND p.id <> :excludeId
          AND p.active = true
        ORDER BY p.id DESC
    """)
    List<Product> findSimilarBySubCategory(
            @Param("subCategoryId") Long subCategoryId,
            @Param("excludeId") Long excludeId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"images", "subCategory", "subCategory.category"})
    @Query("""
        SELECT p FROM Product p
        WHERE p.subCategory.category.id = :categoryId
          AND p.id <> :excludeId
          AND p.active = true
        ORDER BY p.id DESC
    """)
    List<Product> findSimilarByCategory(
            @Param("categoryId") Long categoryId,
            @Param("excludeId") Long excludeId,
            Pageable pageable);

    @Query("""
        SELECT DISTINCT v.product FROM ProductVariant v
        WHERE v.id = :variantId
    """)
    java.util.Optional<Product> findByVariantId(@Param("variantId") Long variantId);

    @Query("""
        SELECT p FROM Product p
        WHERE p.active = true
        ORDER BY p.totalSold DESC, p.id DESC
    """)
    List<Product> findBestSellers(Pageable pageable);


    /*
     * =====================================================
     * BEST SELLER PER CATEGORY (HOME PAGE)
     * Lấy 1 sản phẩm bán chạy nhất theo slug category
     * EntityGraph tránh N+1 cho images + subCategory
     * =====================================================
     */
    @EntityGraph(attributePaths = {"images", "subCategory", "subCategory.category"})
    @Query("""
        SELECT p FROM Product p
        WHERE p.subCategory.category.slug = :slug
          AND p.active = true
        ORDER BY p.totalSold DESC, p.id DESC
    """)
    List<Product> findBestSellerByCategorySlug(@Param("slug") String slug, Pageable pageable);

    // =====================================================
    // ANALYTICS: alias findBestSellers — dùng chung 1 query
    // =====================================================
    default List<Product> findTopSellingProducts(Pageable pageable) {
        return findBestSellers(pageable);
    }

    // =====================================================
    // FULL-TEXT SEARCH (MySQL/MariaDB FULLTEXT)
    // Trả về danh sách ID sản phẩm khớp, xếp theo độ liên quan (relevance).
    // Dùng BOOLEAN MODE để hỗ trợ tiền tố (q*). Service sẽ fallback LIKE nếu
    // index chưa tồn tại hoặc câu lệnh lỗi.
    // =====================================================
    @Query(value = """
            SELECT p.id
            FROM products p
            WHERE p.active = 1
              AND MATCH(p.name, p.description) AGAINST(:q IN BOOLEAN MODE)
            ORDER BY MATCH(p.name, p.description) AGAINST(:q IN BOOLEAN MODE) DESC, p.total_sold DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> fullTextSearchIds(@Param("q") String q, @Param("limit") int limit);

    // Tải đầy đủ sản phẩm theo danh sách ID (kèm ảnh + variants + danh mục).
    // productVariants cần cho ProductResponse.summary().getTotalStock() — tránh lazy load
    // khi map sang DTO ngoài transaction.
    @EntityGraph(attributePaths = {"images", "productVariants", "subCategory", "subCategory.category"})
    List<Product> findByIdInAndActiveTrue(List<Long> ids);

    // =====================================================
    // VIRTUAL TRY-ON: all try-on-enabled products
    // =====================================================
    @EntityGraph(attributePaths = {
        "subCategory",
        "subCategory.category",
        "images"
    })
    @Query("""
        SELECT p FROM Product p
        WHERE p.tryOnEnabled = true
          AND p.garmentProcessedUrl IS NOT NULL
          AND p.active = true
        ORDER BY p.garmentType, p.name
    """)
    List<Product> findAllTryOnEnabled();

}
