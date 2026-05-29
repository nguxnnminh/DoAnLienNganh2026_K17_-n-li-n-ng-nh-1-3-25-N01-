package com.shop.clothingstore.service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.shop.clothingstore.exception.ProductNotFoundException;

import com.shop.clothingstore.dto.ProductCreateDTO;
import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.dto.ProductUpdateDTO;
import com.shop.clothingstore.dto.VariantDTO;
import com.shop.clothingstore.entity.GarmentType;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductImage;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.service.base.GenericServiceBase;
import com.shop.clothingstore.service.storage.FileStorageService;
import com.shop.clothingstore.specification.ProductSpecification;

@Service
public class ProductService extends GenericServiceBase<Product, Long> {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final FileStorageService fileStorageService;

    public ProductService(ProductRepository productRepository,
            SubCategoryRepository subCategoryRepository,
            FileStorageService fileStorageService) {
        super(productRepository);
        this.productRepository = productRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.fileStorageService = fileStorageService;
    }

    // =====================================================
    // CREATE PRODUCT
    // =====================================================
    @Transactional
    @CacheEvict(value = "bestSellers", allEntries = true)
    public Product createProduct(ProductCreateDTO dto) throws IOException {

        log.info("Creating product: {}", dto.getName());

        Long subCatId = dto.getSubCategoryId();
        if (subCatId == null) {
            throw new IllegalArgumentException("SubCategoryId is required");
        }

        SubCategory subCategory = subCategoryRepository
                .findById(subCatId)
                .orElseThrow(() -> new IllegalArgumentException("SubCategory not found"));

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSubCategory(subCategory);
        // null → true (default active), false → false, true → true — no unboxing
        product.setActive(!Boolean.FALSE.equals(dto.getActive()));
        product.setSlug(generateUniqueSlug(dto.getName()));

        if (dto.getVariants() != null) {
            for (VariantDTO v : dto.getVariants()) {
                if (v.getSize() == null || v.getSize().isBlank()) {
                    continue;
                }
                ProductVariant variant = new ProductVariant();
                variant.setSize(v.getSize());
                variant.setColor(v.getColor());
                variant.setPrice(v.getPrice());
                variant.setStock(v.getStock());
                variant.setSold(0);
                product.addVariant(variant);
            }
        }

        product.refreshMinPrice();

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            saveImages(product, dto.getImages(), dto.getPrimaryImageIndex());
        }

        // Handle Try-On garment image (optional during create)
        if (dto.getGarmentImage() != null && !dto.getGarmentImage().isEmpty()) {
            String garmentUrl = fileStorageService.upload(dto.getGarmentImage(), "tryon-garments");
            product.setGarmentProcessedUrl(garmentUrl);
            product.setTryOnEnabled(true);

            GarmentType gType = GarmentType.UPPER_BODY;
            if (dto.getGarmentType() != null) {
                try {
                    gType = GarmentType.valueOf(dto.getGarmentType());
                } catch (IllegalArgumentException ignored) {}
            }
            product.setGarmentType(gType);
            log.info("Try-on enabled during creation | garment={} type={}", garmentUrl, gType);
        }

        try {
            Product saved = save(product);
            log.info("Product created | id={} | name={} | slug={}",
                    saved.getId(), saved.getName(), saved.getSlug());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Slug collision from concurrent creation — append timestamp and retry once
            product.setSlug(product.getSlug() + "-" + System.currentTimeMillis());
            Product saved = save(product);
            log.warn("Slug collision resolved | final slug={}", saved.getSlug());
            return saved;
        }
    }

    // =====================================================
    // UPDATE PRODUCT
    // =====================================================
    @Transactional
    @CacheEvict(value = {"bestSellers", "tryOnProducts"}, allEntries = true)
    public Product updateProduct(Long id, ProductUpdateDTO dto) throws IOException {

        Product product = productRepository.findProductForEdit(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Long subCatId = dto.getSubCategoryId();
        if (subCatId == null) {
            throw new IllegalArgumentException("SubCategoryId is required");
        }

        SubCategory subCategory = subCategoryRepository
                .findById(subCatId)
                .orElseThrow(() -> new IllegalArgumentException("SubCategory not found"));

        String newSlug = toSlug(dto.getName());
        if (!product.getSlug().equals(newSlug)) {
            product.setSlug(generateUniqueSlug(dto.getName()));
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSubCategory(subCategory);
        product.setActive(dto.getActive());

        updateVariants(product, dto.getVariants());
        product.refreshMinPrice();

        // BUG-01 FIX: use removeImagesByIds() which operates on the backing Set,
        // not getImages() which returns an unmodifiable copy.
        if (dto.getImagesToDelete() != null && !dto.getImagesToDelete().isEmpty()) {
            // Collect URLs before removal so we can delete files from disk
            List<String> urlsToDelete = product.getImages().stream()
                    .filter(img -> dto.getImagesToDelete().contains(img.getId()))
                    .map(ProductImage::getImageUrl)
                    .toList();
            product.removeImagesByIds(dto.getImagesToDelete());
            // Delete physical files
            for (String url : urlsToDelete) {
                try {
                    fileStorageService.delete(url);
                } catch (IOException e) {
                    log.warn("Failed to delete image file: {}", url, e);
                }
            }
        }

        if (dto.getNewImages() != null && !dto.getNewImages().isEmpty()) {
            // Reset primary flag on all existing images
            product.getImages().forEach(img -> img.setPrimaryImage(false));
            saveImages(product, dto.getNewImages(), dto.getPrimaryImageIndex());
        }

        return save(product);
    }

    // =====================================================
    // UPDATE VARIANTS — BUG-02 FIX
    // For existing variants: update in-place (do NOT call addVariant again).
    // For new variants: create and add via addVariant.
    // =====================================================
    private void updateVariants(Product product, List<VariantDTO> variantDTOs) {

        if (variantDTOs == null) {
            return;
        }

        // Build lookup map of currently persisted variants
        Map<Long, ProductVariant> existing = new HashMap<>();
        for (ProductVariant v : product.getProductVariants()) {
            if (v.getId() != null) {
                existing.put(v.getId(), v);
            }
        }

        // IDs present in the DTO submission
        Set<Long> dtoIds = variantDTOs.stream()
                .map(VariantDTO::getId)
                .filter(idVal -> idVal != null)
                .collect(Collectors.toSet());

        // Remove variants no longer in the DTO
        product.getProductVariants().removeIf(v -> !dtoIds.contains(v.getId()));

        for (VariantDTO dto : variantDTOs) {

            if (dto.getSize() == null || dto.getSize().isBlank()) {
                continue;
            }

            if (dto.getId() != null && existing.containsKey(dto.getId())) {
                // UPDATE EXISTING — modify in-place, never call addVariant again
                ProductVariant variant = existing.get(dto.getId());
                variant.setSize(dto.getSize());
                variant.setColor(dto.getColor());
                variant.setPrice(dto.getPrice());
                variant.setStock(dto.getStock());
                // sold is intentionally not reset
            } else {
                // NEW VARIANT — create and add
                ProductVariant variant = new ProductVariant();
                variant.setSold(0);
                variant.setSize(dto.getSize());
                variant.setColor(dto.getColor());
                variant.setPrice(dto.getPrice());
                variant.setStock(dto.getStock());
                product.addVariant(variant);
            }
        }
    }

    // =====================================================
    // DELETE PRODUCT
    // =====================================================
    @Transactional
    @CacheEvict(value = {"bestSellers", "tryOnProducts"}, allEntries = true)
    public void deleteProduct(Long productId) {
        log.info("Deleting product | id={}", productId);
        if (productId == null) throw new ProductNotFoundException(0L);

        // Delete all image files and garment file from disk before removing from DB
        productRepository.findById(productId).ifPresent(product -> {
            // Product images
            for (ProductImage img : product.getImages()) {
                try {
                    fileStorageService.delete(img.getImageUrl());
                } catch (IOException e) {
                    log.warn("Failed to delete image: {}", img.getImageUrl(), e);
                }
            }
            // Garment image (try-on)
            if (product.getGarmentProcessedUrl() != null) {
                try {
                    fileStorageService.delete(product.getGarmentProcessedUrl());
                } catch (IOException e) {
                    log.warn("Failed to delete garment: {}", product.getGarmentProcessedUrl(), e);
                }
            }
        });

        delete(productId);
    }

    public Product getProductForEdit(Long id) {
        return productRepository.findProductForEdit(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    // =====================================================
    // SAVE IMAGES
    // =====================================================
    private void saveImages(Product product,
            List<MultipartFile> files,
            Integer primaryIndex) throws IOException {

        if (files == null || files.isEmpty()) {
            return;
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty()) {
                continue;
            }
            String imageUrl = fileStorageService.upload(file, "products");
            ProductImage image = new ProductImage();
            image.setImageUrl(imageUrl);
            image.setPrimaryImage(primaryIndex != null && i == primaryIndex);
            product.addImage(image);
        }
    }

    // =====================================================
    // SLUG GENERATION — race condition handled by catching
    // DataIntegrityViolationException at save time.
    // =====================================================
    private String generateUniqueSlug(String name) {
        String baseSlug = toSlug(name);
        String slug = baseSlug;
        int i = 1;
        while (productRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + i++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.toLowerCase();
        slug = slug.replaceAll("[^a-z0-9\\s-]", "");
        slug = slug.replaceAll("\\s+", "-");
        return slug;
    }

    // =====================================================
    // QUERY METHODS
    // =====================================================
    public Product findBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> new ProductNotFoundException(slug));
    }

    public Page<Product> findTopByCategorySlug(String slug, Pageable pageable) {
        return productRepository.findTopByCategorySlug(slug, pageable);
    }

    @Cacheable(value = "bestSellers", key = "#pageable.pageSize")
    public List<Product> findBestSellers(Pageable pageable) {
        return productRepository.findBestSellers(pageable);
    }

    public java.util.Optional<Product> findBestSellerByCategorySlug(String slug) {
        List<Product> results = productRepository.findBestSellerByCategorySlug(
                slug, org.springframework.data.domain.PageRequest.of(0, 1));
        return results.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(results.get(0));
    }

    public Page<Product> findWithFilter(ProductFilterDTO filter, Pageable pageable) {
        java.util.Objects.requireNonNull(pageable, "Pageable must not be null");
        return productRepository.findAll(
                ProductSpecification.filter(filter),
                pageable
        );
    }

    // =====================================================
    // FULL-TEXT SEARCH (MySQL/MariaDB FULLTEXT) + AUTOCOMPLETE
    // Dùng cho ô tìm kiếm / gợi ý từ khóa. Thử MATCH AGAINST trước
    // (xếp theo relevance), nếu lỗi/không có kết quả thì fallback
    // LIKE qua Specification — đảm bảo luôn trả kết quả.
    // =====================================================
    @Transactional(readOnly = true)
    public List<Product> fullTextSearch(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String q = query.trim();
        int safeLimit = Math.max(1, Math.min(limit, 20));

        // 1. Thử FULLTEXT (BOOLEAN MODE, hỗ trợ tiền tố: "ao*")
        try {
            String booleanQuery = buildBooleanQuery(q);
            List<Long> ids = productRepository.fullTextSearchIds(booleanQuery, safeLimit);
            if (!ids.isEmpty()) {
                // Tải đầy đủ (kèm ảnh) rồi giữ đúng thứ tự relevance từ MATCH
                List<Product> loaded = productRepository.findByIdInAndActiveTrue(ids);
                Map<Long, Product> byId = loaded.stream()
                        .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
                List<Product> ordered = new java.util.ArrayList<>();
                for (Long id : ids) {
                    Product p = byId.get(id);
                    if (p != null) ordered.add(p);
                }
                if (!ordered.isEmpty()) return ordered;
            }
        } catch (Exception e) {
            log.warn("Full-text MATCH search failed, fallback to LIKE. q='{}' err={}", q, e.getMessage());
        }

        // 2. Fallback LIKE qua Specification (luôn chạy, an toàn cho tiếng Việt)
        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setKeyword(q);
        return productRepository
                .findAll(ProductSpecification.filter(filter), org.springframework.data.domain.PageRequest.of(0, safeLimit))
                .getContent();
    }

    /**
     * Chuyển câu tìm kiếm thành cú pháp BOOLEAN MODE: mỗi từ ≥1 ký tự thành tiền tố "+từ*".
     * Lọc ký tự đặc biệt của FULLTEXT để tránh lỗi cú pháp.
     */
    private String buildBooleanQuery(String q) {
        String[] tokens = q.replaceAll("[+\\-><()~*\"@]", " ").trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) {
            if (t.isBlank()) continue;
            sb.append('+').append(t).append('*').append(' ');
        }
        return sb.toString().trim();
    }
}
