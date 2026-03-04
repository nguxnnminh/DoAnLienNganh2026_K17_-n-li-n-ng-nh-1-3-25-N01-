package com.shop.clothingstore.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
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
import com.shop.clothingstore.specification.ProductSpecification;

import jakarta.transaction.Transactional;

@Service
public class ProductService extends GenericServiceBase<Product, Long> {

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;

    @Value("${upload.dir:uploads/images/products}")
    private String uploadDir;

    public ProductService(ProductRepository productRepository,
            SubCategoryRepository subCategoryRepository) {

        super(productRepository);
        this.productRepository = productRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    /*
     * =====================================================
     * CREATE PRODUCT
     * =====================================================
     */
    @Transactional
    public Product createProduct(ProductCreateDTO dto) throws IOException {

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

        return save(product);
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

        product.getProductVariants().removeIf(v
                -> variantDTOs == null
                || variantDTOs.stream().noneMatch(dto -> v.getId().equals(dto.getId()))
        );

        if (variantDTOs == null) {
            return;
        }

        for (VariantDTO dto : variantDTOs) {

            if (dto.getSize() == null || dto.getSize().isBlank()) {
                continue;
            }

            ProductVariant variant;

            if (dto.getId() != null && existing.containsKey(dto.getId())) {

                variant = existing.get(dto.getId());

                variant.setSize(dto.getSize());
                variant.setColor(dto.getColor());
                variant.setPrice(dto.getPrice());
                variant.setStock(dto.getStock());

            } else {

                variant = new ProductVariant();

                variant.setSize(dto.getSize());
                variant.setColor(dto.getColor());
                variant.setPrice(dto.getPrice());
                variant.setStock(dto.getStock());
                variant.setSold(0);

                product.addVariant(variant);
            }
        }
    }

    /*
     * =====================================================
     * DELETE PRODUCT
     * =====================================================
     */
    @Transactional
    public void deleteProduct(Long id) {
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
     * SAVE IMAGES
     * =====================================================
     */
    private void saveImages(Product product,
            List<MultipartFile> files,
            Integer primaryIndex) throws IOException {

        if (files == null || files.isEmpty()) {
            return;
        }

        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (int i = 0; i < files.size(); i++) {

            MultipartFile file = files.get(i);

            if (file.isEmpty()) {
                continue;
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            ProductImage image = new ProductImage();
            image.setImageUrl("/uploads/images/products/" + fileName);
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
