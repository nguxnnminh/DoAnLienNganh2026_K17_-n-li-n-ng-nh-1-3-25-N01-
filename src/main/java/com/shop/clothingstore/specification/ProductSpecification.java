package com.shop.clothingstore.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.entity.Product;

import jakarta.persistence.criteria.Predicate;

public class ProductSpecification {

    public static Specification<Product> filter(ProductFilterDTO filter) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // ==================================================
            // SEARCH BY NAME — parameterized, no SQL injection
            // ==================================================
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + filter.getKeyword().toLowerCase() + "%"
                        )
                );
            }

            // ==================================================
            // FILTER CATEGORY
            // ==================================================
            if (filter.getCategoryId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("subCategory").get("category").get("id"),
                                filter.getCategoryId()
                        )
                );
            }

            // ==================================================
            // FILTER SUB CATEGORY
            // ==================================================
            if (filter.getSubCategoryId() != null) {
                predicates.add(
                        cb.equal(
                                root.get("subCategory").get("id"),
                                filter.getSubCategoryId()
                        )
                );
            }

            // ==================================================
            // FILTER PRICE — uses denormalized minPrice column.
            // Previously used a variant JOIN which caused:
            //   - Cartesian product (N rows per product before DISTINCT)
            //   - Wrong pagination total counts
            //   - Unnecessary query complexity
            // minPrice is kept in sync via Product.refreshMinPrice().
            // ==================================================
            if (filter.getMinPrice() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.<java.math.BigDecimal>get("minPrice"),
                                filter.getMinPrice()
                        )
                );
            }

            if (filter.getMaxPrice() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.<java.math.BigDecimal>get("minPrice"),
                                filter.getMaxPrice()
                        )
                );
            }

            // ==================================================
            // ACTIVE FILTER — skipped for admin queries (onlyActive = false)
            // ==================================================
            if (filter.isOnlyActive()) {
                predicates.add(cb.isTrue(root.get("active")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
