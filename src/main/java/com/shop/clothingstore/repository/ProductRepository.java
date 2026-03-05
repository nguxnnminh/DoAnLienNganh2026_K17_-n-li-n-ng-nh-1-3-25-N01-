package com.shop.clothingstore.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    Optional<Product> findById(Long id);


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
    @EntityGraph(attributePaths = {
        "subCategory",
        "subCategory.category"
    })
    Page<Product> findAll(Pageable pageable);

}
