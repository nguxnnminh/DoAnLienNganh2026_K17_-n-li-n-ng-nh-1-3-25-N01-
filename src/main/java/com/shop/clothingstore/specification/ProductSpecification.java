package com.shop.clothingstore.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductVariant;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class ProductSpecification {

    public static Specification<Product> filter(ProductFilterDTO filter) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            Join<Product, ProductVariant> variantJoin = null;

            // ==================================================
            // SEARCH BY NAME
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
                                root.get("subCategory")
                                        .get("category")
                                        .get("id"),
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
            // FILTER PRICE (JOIN ONLY WHEN NEEDED)
            // ==================================================
            if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {

                variantJoin = root.join("productVariants", JoinType.LEFT);

                if (filter.getMinPrice() != null) {

                    predicates.add(
                            cb.greaterThanOrEqualTo(
                                    variantJoin.get("price"),
                                    filter.getMinPrice()
                            )
                    );
                }

                if (filter.getMaxPrice() != null) {

                    predicates.add(
                            cb.lessThanOrEqualTo(
                                    variantJoin.get("price"),
                                    filter.getMaxPrice()
                            )
                    );
                }

                if (query != null) {
                    query.distinct(true);
                }
            }

            // ==================================================
            // ONLY ACTIVE PRODUCT
            // ==================================================
            predicates.add(cb.isTrue(root.get("active")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
