package com.shop.clothingstore.controller;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.CategoryRepository;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.specification.ProductSpecification;

@Controller
public class ShopController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public ShopController(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          SubCategoryRepository subCategoryRepository) {

        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    // =====================================================
    // HOME
    // =====================================================
    @GetMapping("/")
    public String home(Model model) {

        Pageable pageable = PageRequest.of(0, 8, Sort.by("id").descending());
        Page<Product> featured = productRepository.findAll(pageable);

        model.addAttribute("products", featured.getContent());

        return "shop/home";
    }

    // =====================================================
    // HELPER SORT METHOD
    // =====================================================
    private Pageable buildPageable(int page, String sort) {

        Sort sortObj = Sort.by("id").descending();

        if (sort != null) {

            switch (sort) {

                case "price_asc":
                    sortObj = Sort.by("productVariants.price").ascending();
                    break;

                case "price_desc":
                    sortObj = Sort.by("productVariants.price").descending();
                    break;

                case "best_seller":
                    sortObj = Sort.by("productVariants.sold").descending();
                    break;

                case "newest":
                default:
                    sortObj = Sort.by("id").descending();
                    break;
            }
        }

        return PageRequest.of(page, 12, sortObj);
    }

    // =====================================================
    // ALL PRODUCTS
    // =====================================================
    @GetMapping("/products")
    public String products(

            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setKeyword(keyword);

        Pageable pageable = buildPageable(page, sort);

        Page<Product> products = productRepository.findAll(
                ProductSpecification.filter(filter),
                pageable
        );

        model.addAttribute("products", products);
        model.addAttribute("filter", filter);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", categoryRepository.findAll());

        return "shop/products";
    }

    // =====================================================
    // PRODUCTS BY CATEGORY
    // =====================================================
    @GetMapping("/products/{categorySlug}")
    public String productsByCategory(

            @PathVariable String categorySlug,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setCategoryId(category.getId());
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setKeyword(keyword);

        Pageable pageable = buildPageable(page, sort);

        Page<Product> products = productRepository.findAll(
                ProductSpecification.filter(filter),
                pageable
        );

        model.addAttribute("products", products);
        model.addAttribute("filter", filter);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("currentCategory", category);
        model.addAttribute("subCategories",
                subCategoryRepository.findByCategoryId(category.getId()));

        return "shop/products";
    }

    // =====================================================
    // PRODUCTS BY SUB CATEGORY
    // =====================================================
    @GetMapping("/products/{categorySlug}/{subSlug}")
    public String productsBySubCategory(

            @PathVariable String categorySlug,
            @PathVariable String subSlug,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        SubCategory subCategory = subCategoryRepository.findBySlug(subSlug)
                .orElseThrow(() -> new RuntimeException("SubCategory not found"));

        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new RuntimeException("SubCategory not in Category");
        }

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setCategoryId(category.getId());
        filter.setSubCategoryId(subCategory.getId());
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setKeyword(keyword);

        Pageable pageable = buildPageable(page, sort);

        Page<Product> products = productRepository.findAll(
                ProductSpecification.filter(filter),
                pageable
        );

        model.addAttribute("products", products);
        model.addAttribute("filter", filter);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentSubCategory", subCategory);
        model.addAttribute("subCategories",
                subCategoryRepository.findByCategoryId(category.getId()));

        return "shop/products";
    }

    // =====================================================
    // PRODUCT DETAIL
    // =====================================================
    @GetMapping("/products/{categorySlug}/{subSlug}/{id}")
    public String productDetail(
            @PathVariable String categorySlug,
            @PathVariable String subSlug,
            @PathVariable Long id,
            Model model) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // ===== UNIQUE SIZE =====
        Set<String> sizes = product.getProductVariants()
                .stream()
                .map(v -> v.getSize())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // ===== UNIQUE COLOR =====
        Set<String> colors = product.getProductVariants()
                .stream()
                .map(v -> v.getColor())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        model.addAttribute("product", product);
        model.addAttribute("sizes", sizes);
        model.addAttribute("colors", colors);

        // ⭐ FRONTEND JS dùng
        model.addAttribute("variantsJson", product.getProductVariants());

        return "shop/product-detail";
    }

}
