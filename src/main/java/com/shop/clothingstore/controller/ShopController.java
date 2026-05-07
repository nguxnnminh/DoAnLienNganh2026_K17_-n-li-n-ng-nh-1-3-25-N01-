package com.shop.clothingstore.controller;

import java.math.BigDecimal;
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
import com.shop.clothingstore.entity.Review;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.exception.ProductNotFoundException;
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
        // Best sellers: 1 top-selling product per category (top / bottom / accessories)
        List<Product> bestSellers = new java.util.ArrayList<>();
        productService.findBestSellerByCategorySlug("top")        .ifPresent(bestSellers::add);
        productService.findBestSellerByCategorySlug("bottom")     .ifPresent(bestSellers::add);
        productService.findBestSellerByCategorySlug("accessories").ifPresent(bestSellers::add);
        model.addAttribute("bestSellers", bestSellers);
        return "shop/home";
    }

    // =====================================================
    // SORT HELPER
    // =====================================================
    private Pageable buildPageable(int page, String sort) {
        Sort sortObj = switch (sort != null ? sort : "newest") {
            case "price_asc"   -> Sort.by("minPrice").ascending();
            case "price_desc"  -> Sort.by("minPrice").descending();
            case "best_seller" -> Sort.by("totalSold").descending();
            default            -> Sort.by("id").descending();
        };
        return PageRequest.of(page, 12, sortObj);
    }

    // =====================================================
    // PAGINATION WINDOW HELPER
    // Computes a ±2 window around the current page so the
    // template never renders more than 7 page buttons.
    // =====================================================
    private void addPaginationWindow(Model model, Page<?> page) {
        int current     = page.getNumber();
        int total       = page.getTotalPages();
        int windowStart = Math.max(0, current - 2);
        int windowEnd   = Math.min(total - 1, current + 2);
        model.addAttribute("pageWindowStart",    windowStart);
        model.addAttribute("pageWindowEnd",      windowEnd);
        model.addAttribute("showStartEllipsis",  windowStart > 1);
        model.addAttribute("showEndEllipsis",    windowEnd < total - 2);
    }

    // =====================================================
    // ALL PRODUCTS
    // =====================================================
    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setKeyword(keyword);

        Page<Product> products = productService.findWithFilter(filter, buildPageable(page, sort));

        model.addAttribute("products",    products);
        model.addAttribute("filter",      filter);
        model.addAttribute("sort",        sort);
        model.addAttribute("categories",  categoryService.getAllCategories());
        addPaginationWindow(model, products);

        return "shop/products";
    }

    // =====================================================
    // PRODUCTS BY CATEGORY
    // =====================================================
    @GetMapping("/products/{categorySlug}")
    public String productsByCategory(
            @PathVariable String categorySlug,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Category category = categoryService.getCategoryBySlug(categorySlug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found: " + categorySlug));

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setCategoryId(category.getId());
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setKeyword(keyword);

        Page<Product> products = productService.findWithFilter(filter, buildPageable(page, sort));

        model.addAttribute("products",        products);
        model.addAttribute("filter",          filter);
        model.addAttribute("sort",            sort);
        model.addAttribute("categories",      categoryService.getAllCategories());
        model.addAttribute("currentCategory", category);
        model.addAttribute("subCategories",   subCategoryService.getByCategoryId(category.getId()));
        addPaginationWindow(model, products);

        return "shop/products";
    }

    // =====================================================
    // PRODUCTS BY SUBCATEGORY
    // =====================================================
    @GetMapping("/products/{categorySlug}/{subSlug}")
    public String productsBySubCategory(
            @PathVariable String categorySlug,
            @PathVariable String subSlug,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Category category = categoryService.getCategoryBySlug(categorySlug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found: " + categorySlug));

        SubCategory subCategory = subCategoryService.getBySlug(subSlug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SubCategory not found: " + subSlug));

        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "SubCategory does not belong to this category");
        }

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setCategoryId(category.getId());
        filter.setSubCategoryId(subCategory.getId());
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setKeyword(keyword);

        Page<Product> products = productService.findWithFilter(filter, buildPageable(page, sort));

        model.addAttribute("products",           products);
        model.addAttribute("filter",             filter);
        model.addAttribute("sort",               sort);
        model.addAttribute("categories",         categoryService.getAllCategories());
        model.addAttribute("currentCategory",    category);
        model.addAttribute("currentSubCategory", subCategory);
        model.addAttribute("subCategories",      subCategoryService.getByCategoryId(category.getId()));
        addPaginationWindow(model, products);

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

        Product product = productService.findBySlug(slug); // throws ProductNotFoundException
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
                .orElseThrow(() -> new ProductNotFoundException(id));

        return renderProductDetail(product, model, authentication);
    }

    // =====================================================
    // SHARED RENDER LOGIC
    //
    // BEFORE: 6 sequential DB queries per page view
    //   reviewService.getAverageRating()  → query 1
    //   reviewService.getReviewCount()    → query 2
    //   reviewService.getReviewsByItem()  → query 3
    //   recommendationService.get...()   → query 4 (+N+1 for images — fixed in repo)
    //   userService.findByEmail()         → query 5
    //   wishlistService.isInWishlist()    → query 6
    //
    // AFTER: 4 queries
    //   reviewService.getReviewsByItem()  → query 1  (avg+count computed in memory)
    //   recommendationService.get...()   → query 2  (images pre-fetched via EntityGraph)
    //   userService.findByEmail()         → query 3
    //   wishlistService.isInWishlist()    → query 4
    // =====================================================
    private String renderProductDetail(Product product, Model model, Authentication authentication) {
        Long id = product.getId();

        // Variant selectors
        Set<String> sizes = product.getProductVariants().stream()
                .map(v -> v.getSize())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> colors = product.getProductVariants().stream()
                .map(v -> v.getColor())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Single review query — compute avg + count from the returned list in memory
        List<Review> reviews    = reviewService.getReviewsByItem(id);
        double averageRating    = reviews.stream()
                .mapToDouble(r -> r.getRating())
                .average()
                .orElse(0.0);
        long reviewCount        = reviews.size();

        model.addAttribute("product",       product);
        model.addAttribute("sizes",         sizes);
        model.addAttribute("colors",        colors);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewCount",   reviewCount);
        model.addAttribute("reviews",       reviews);

        // Variants as JSON for the client-side size/color selector
        List<VariantDTO> variantDTOs = product.getProductVariants().stream()
                .map(v -> new VariantDTO(v.getId(), v.getSize(), v.getColor(), v.getPrice(), v.getStock()))
                .toList();
        model.addAttribute("variantsJson", variantDTOs);

        // Related products — images pre-fetched via EntityGraph (no lazy load in template)
        List<Product> relatedProducts = recommendationService.getSimilarProducts(id, 4);
        model.addAttribute("relatedProducts", relatedProducts);

        // Wishlist state — only for authenticated users
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
