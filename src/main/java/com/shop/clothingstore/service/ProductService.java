package com.shop.clothingstore.service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.shop.clothingstore.dto.ProductCreateDTO;
import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.dto.ProductUpdateDTO;
import com.shop.clothingstore.dto.VariantDTO;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductImage;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.service.base.GenericServiceBase;
import com.shop.clothingstore.service.storage.FileStorageService;
import com.shop.clothingstore.specification.ProductSpecification;

import jakarta.transaction.Transactional;

@Service
@SuppressWarnings("null")
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

    /*
     * =====================================================
     * CREATE PRODUCT
     * =====================================================
     */
    @Transactional
    public Product createProduct(ProductCreateDTO dto) throws IOException {

        log.info("Creating product: {}", dto.getName());

        if (dto.getSubCategoryId() == null) {
            throw new RuntimeException("SubCategoryId không được null");
        }

        SubCategory subCategory = subCategoryRepository
                .findById(dto.getSubCategoryId())
                .orElseThrow(() -> new RuntimeException("SubCategory không tồn tại"));

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSubCategory(subCategory);
        product.setActive(dto.getActive() != null ? dto.getActive() : true);
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

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            saveImages(product, dto.getImages(), dto.getPrimaryImageIndex());
        }

        Product saved = save(product);

        log.info("Product created | id={} | name={} | slug={}",
                saved.getId(), saved.getName(), saved.getSlug());

        return saved;
    }

    /*
     * =====================================================
     * UPDATE PRODUCT
     * =====================================================
     */
    @Transactional
    public Product updateProduct(Long id, ProductUpdateDTO dto) throws IOException {

        Product product = productRepository.findProductForEdit(id)
                .orElseThrow(() -> new RuntimeException("Product không tồn tại"));

        if (dto.getSubCategoryId() == null) {
            throw new RuntimeException("SubCategoryId không được null");
        }

        SubCategory subCategory = subCategoryRepository
                .findById(dto.getSubCategoryId())
                .orElseThrow(() -> new RuntimeException("SubCategory không tồn tại"));

        String newSlug = toSlug(dto.getName());

        if (!product.getSlug().equals(newSlug)) {
            product.setSlug(generateUniqueSlug(dto.getName()));
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSubCategory(subCategory);
        product.setActive(dto.getActive());

        updateVariants(product, dto.getVariants());

        if (dto.getImagesToDelete() != null && !dto.getImagesToDelete().isEmpty()) {

            product.getImages().removeIf(img
                    -> dto.getImagesToDelete().contains(img.getId()));
        }

        if (dto.getNewImages() != null && !dto.getNewImages().isEmpty()) {

            product.getImages().forEach(img -> img.setPrimaryImage(false));

            saveImages(product, dto.getNewImages(), dto.getPrimaryImageIndex());
        }
        return save(product);
    }

    /*
     * =====================================================
     * UPDATE VARIANTS
     * =====================================================
     */
    private void updateVariants(Product product, List<VariantDTO> variantDTOs) {

        Map<Long, ProductVariant> existing = new HashMap<>();

        for (ProductVariant v : product.getProductVariants()) {
            existing.put(v.getId(), v);
        }

        if (variantDTOs == null) {
            return;
        }

        // IDs từ DTO
        java.util.Set<Long> dtoIds = variantDTOs.stream()
                .map(VariantDTO::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        // Xóa variant không còn trong DTO
        product.getProductVariants().removeIf(v -> !dtoIds.contains(v.getId()));

        for (VariantDTO dto : variantDTOs) {

            if (dto.getSize() == null || dto.getSize().isBlank()) {
                continue;
            }

            ProductVariant variant;

            if (dto.getId() != null && existing.containsKey(dto.getId())) {

                // update variant cũ
                variant = existing.get(dto.getId());

            } else {

                // tạo variant mới
                variant = new ProductVariant();
                variant.setSold(0);
            }

            variant.setSize(dto.getSize());
            variant.setColor(dto.getColor());
            variant.setPrice(dto.getPrice());
            variant.setStock(dto.getStock());

            product.addVariant(variant);
        }
    }

    /*
     * =====================================================
     * DELETE PRODUCT
     * =====================================================
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product | id={}", id);
        delete(id);
    }

    /*
     * =====================================================
     * GET PRODUCT FOR EDIT
     * =====================================================
     */
    public Product getProductForEdit(Long id) {

        return productRepository.findProductForEdit(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    /*
     * =====================================================
     * SAVE IMAGES (delegate to FileStorageService)
     * =====================================================
     */
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

            // Delegate upload cho FileStorageService
            String imageUrl = fileStorageService.upload(file, "products");

            ProductImage image = new ProductImage();
            image.setImageUrl(imageUrl);
            image.setPrimaryImage(primaryIndex != null && i == primaryIndex);

            product.addImage(image);
        }
    }

    /*
     * =====================================================
     * SLUG GENERATOR
     * =====================================================
     */
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

    /*
     * =====================================================
     * FIND BY SLUG
     * =====================================================
     */
    public Product findBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + slug));
    }

    /*
     * =====================================================
     * QUERY METHODS
     * =====================================================
     */
    public Page<Product> findTopByCategorySlug(String slug, Pageable pageable) {

        return productRepository.findTopByCategorySlug(slug, pageable);
    }

    public Page<Product> findWithFilter(ProductFilterDTO filter, Pageable pageable) {

        return productRepository.findAll(
                ProductSpecification.filter(filter),
                pageable
        );
    }
}
