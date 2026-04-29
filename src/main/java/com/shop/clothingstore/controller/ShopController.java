package com.shop.clothingstore.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.dto.VariantDTO;
import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.service.CategoryService;
import com.shop.clothingstore.service.ProductService;
import com.shop.clothingstore.service.RecommendationService;
import com.shop.clothingstore.service.ReviewService;
import com.shop.clothingstore.service.SubCategoryService;
import com.shop.clothingstore.service.UserService;
import com.shop.clothingstore.service.WishlistService;

@Controller
public class ShopController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SubCategoryService subCategoryService;
    private final ReviewService reviewService;
    private final RecommendationService recommendationService;
    private final WishlistService wishlistService;
    private final UserService userService;

    public ShopController(ProductService productService,
            CategoryService categoryService,
            SubCategoryService subCategoryService,
            ReviewService reviewService,
            RecommendationService recommendationService,
            WishlistService wishlistService,
            UserService userService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.subCategoryService = subCategoryService;
        this.reviewService = reviewService;
        this.recommendationService = recommendationService;
        this.wishlistService = wishlistService;
        this.userService = userService;
    }

    // =====================================================
    // HOME
    // =====================================================
    @GetMapping("/")
    public String home(Model model) {
        List<Product> homeProducts = new ArrayList<>();
        homeProducts.addAll(productService.findTopByCategorySlug("top", PageRequest.of(0, 1)).getContent());
        homeProducts.addAll(productService.findTopByCategorySlug("bottom", PageRequest.of(0, 1)).getContent());
        homeProducts.addAll(productService.findTopByCategorySlug("accessories", PageRequest.of(0, 1)).getContent());
        model.addAttribute("homeProducts", homeProducts);
        model.addAttribute("bestSellers", homeProducts);
        return "shop/home";
    }

    // =====================================================
    // SORT HELPER
    // price_asc / price_desc sort on the denormalized minPrice column
    // (computed from variants and stored on the product row).
    // best_seller sorts by totalSold which is also a computed helper —
    // for proper DB-level sorting a denormalized column would be needed;
    // fall back to newest for now.
    // =====================================================
    private Pageable buildPageable(int page, String sort) {
        Sort sortObj;
        if (sort == null) {
            sortObj = Sort.by("id").descending();
        } else {
            switch (sort) {
                case "price_asc":
                    sortObj = Sort.by("minPrice").ascending();
                    break;
                case "price_desc":
                    sortObj = Sort.by("minPrice").descending();
                    break;
                case "best_seller":
                    // totalSold is computed from variants; minPrice is the only
                    // reliable denormalized sortable column. Use it as proxy.
                    sortObj = Sort.by("id").descending();
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
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setKeyword(keyword);

        Pageable pageable = buildPageable(page, sort);
        Page<Product> products = productService.findWithFilter(filter, pageable);

        model.addAttribute("products", products);
        model.addAttribute("filter", filter);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "shop/products";
    }

    // =====================================================
    // PRODUCTS BY CATEGORY
    // =====================================================
    @GetMapping("/products/{categorySlug}")
    public String productsByCategory(
            @PathVariable String categorySlug,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Category category = categoryService.getCategoryBySlug(categorySlug)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Category not found: " + categorySlug));

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setCategoryId(category.getId());
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setKeyword(keyword);

        Pageable pageable = buildPageable(page, sort);
        Page<Product> products = productService.findWithFilter(filter, pageable);

        model.addAttribute("products", products);
        model.addAttribute("filter", filter);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("currentCategory", category);
        model.addAttribute("subCategories", subCategoryService.getByCategoryId(category.getId()));

        return "shop/products";
    }

    // =====================================================
    // PRODUCTS BY SUB CATEGORY
    // =====================================================
    @GetMapping("/products/{categorySlug}/{subSlug}")
    public String productsBySubCategory(
            @PathVariable String categorySlug,
            @PathVariable String subSlug,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Category category = categoryService.getCategoryBySlug(categorySlug)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Category not found: " + categorySlug));

        SubCategory subCategory = subCategoryService.getBySlug(subSlug)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "SubCategory not found: " + subSlug));

        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SubCategory does not belong to this category");
        }

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setCategoryId(category.getId());
        filter.setSubCategoryId(subCategory.getId());
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setKeyword(keyword);

        Pageable pageable = buildPageable(page, sort);
        Page<Product> products = productService.findWithFilter(filter, pageable);

        model.addAttribute("products", products);
        model.addAttribute("filter", filter);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentSubCategory", subCategory);
        model.addAttribute("subCategories", subCategoryService.getByCategoryId(category.getId()));

        return "shop/products";
    }

    // =====================================================
    // PRODUCT DETAIL BY SLUG
    // =====================================================
    @GetMapping("/product/{slug}")
    public String productDetailBySlug(
            @PathVariable String slug,
            Model model,
            Authentication authentication) {

        Product product = productService.findBySlug(slug);
        return renderProductDetail(product, model, authentication);
    }

    // =====================================================
    // PRODUCT DETAIL BY CATEGORY/SUB/ID
    // =====================================================
    @GetMapping("/products/{categorySlug}/{subSlug}/{id}")
    public String productDetail(
            @PathVariable String categorySlug,
            @PathVariable String subSlug,
            @PathVariable Long id,
            Model model,
            Authentication authentication) {

        Product product = productService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product not found"));

        return renderProductDetail(product, model, authentication);
    }

    // =====================================================
    // SHARED RENDER LOGIC
    // =====================================================
    private String renderProductDetail(Product product, Model model, Authentication authentication) {
        Long id = product.getId();

        Set<String> sizes = product.getProductVariants().stream()
                .map(v -> v.getSize())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> colors = product.getProductVariants().stream()
                .map(v -> v.getColor())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        double averageRating = reviewService.getAverageRating(id);
        long reviewCount = reviewService.getReviewCount(id);

        model.addAttribute("product", product);
        model.addAttribute("sizes", sizes);
        model.addAttribute("colors", colors);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("reviews", reviewService.getReviewsByItem(id));

        var variantDTOs = product.getProductVariants().stream()
                .map(v -> new VariantDTO(v.getId(), v.getSize(), v.getColor(), v.getPrice(), v.getStock()))
                .toList();

        model.addAttribute("variantsJson", variantDTOs);

        List<Product> relatedProducts = recommendationService.getSimilarProducts(id, 4);
        model.addAttribute("relatedProducts", relatedProducts);

        boolean isInWishlist = false;
        if (authentication != null && authentication.isAuthenticated()) {
            isInWishlist = userService.findByEmail(authentication.getName())
                    .map(user -> wishlistService.isInWishlist(user, id))
                    .orElse(false);
        }
        model.addAttribute("isInWishlist", isInWishlist);

        return "shop/product-detail";
    }
}
