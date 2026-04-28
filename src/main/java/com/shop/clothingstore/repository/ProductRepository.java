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
     * Tránh N+1 khi dùng filter API
     * =====================================================
     */
    @Override
    @EntityGraph(attributePaths = {
        "subCategory",
        "subCategory.category",
        "images"
    })
    @NonNull
    Page<Product> findAll(@Nullable Specification<Product> spec, @NonNull Pageable pageable);

    // =====================================================
    // RECOMMENDATION QUERIES
    // =====================================================
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
        LEFT JOIN p.productVariants v
        WHERE p.active = true
        GROUP BY p
        ORDER BY COALESCE(SUM(v.sold), 0) DESC
    """)
    List<Product> findBestSellers(Pageable pageable);

    // =====================================================
    // ANALYTICS: alias findBestSellers — dùng chung 1 query
    // =====================================================
    default List<Product> findTopSellingProducts(Pageable pageable) {
        return findBestSellers(pageable);
    }

}
