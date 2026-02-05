package com.shop.clothingstore.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.shop.clothingstore.dto.ProductCreateDTO;
import com.shop.clothingstore.dto.ProductUpdateDTO;
import com.shop.clothingstore.dto.VariantDTO;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductImage;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.SubCategoryRepository;

import jakarta.transaction.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SubCategoryRepository subCategoryRepository;

    @Value("${upload.dir:uploads/images/products}")
    private String uploadDir;

    public ProductService(ProductRepository productRepository,
                          SubCategoryRepository subCategoryRepository) {
        this.productRepository = productRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // ================= CREATE =================
    @Transactional
    public Product createProduct(ProductCreateDTO dto) throws IOException {

        SubCategory subCategory = subCategoryRepository.findById(dto.getSubCategoryId())
                .orElseThrow(() -> new RuntimeException("SubCategory không tồn tại"));

        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSubCategory(subCategory);
        product.setActive(dto.getActive() != null ? dto.getActive() : true);

        if (dto.getVariants() != null) {
            for (VariantDTO v : dto.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setSize(v.getSize());
                variant.setColor(v.getColor());
                variant.setPrice(v.getPrice());
                variant.setStock(v.getStock());
                product.addVariant(variant);
            }
        }

        Product saved = productRepository.save(product);

        saveImages(saved, dto.getImages(), dto.getPrimaryImageIndex());

        return saved;
    }

    // ================= UPDATE =================
    @Transactional
    public Product updateProduct(Long id, ProductUpdateDTO dto) throws IOException {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product không tồn tại"));

        SubCategory subCategory = subCategoryRepository.findById(dto.getSubCategoryId())
                .orElseThrow(() -> new RuntimeException("SubCategory không tồn tại"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setSubCategory(subCategory);
        product.setActive(dto.getActive());

        // Clear variants cũ
        product.getProductVariants().clear();

        // Add variants mới
        if (dto.getVariants() != null) {
            for (VariantDTO v : dto.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setSize(v.getSize());
                variant.setColor(v.getColor());
                variant.setPrice(v.getPrice());
                variant.setStock(v.getStock());
                product.addVariant(variant);
            }
        }

        // Xóa ảnh được chọn
        if (dto.getImagesToDelete() != null) {
            product.getImages().removeIf(img ->
                    dto.getImagesToDelete().contains(img.getId()));
        }

        // Thêm ảnh mới
        if (dto.getNewImages() != null && !dto.getNewImages().isEmpty()) {
            saveImages(product, dto.getNewImages(), dto.getPrimaryImageIndex());
        }

        return productRepository.save(product);
    }

    // ================= DELETE =================
    @Transactional
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product không tồn tại"));

        productRepository.delete(product);
    }

    public Product getProductForEdit(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    // ================= SAVE IMAGE =================
    private void saveImages(Product product,
                            List<MultipartFile> files,
                            Integer primaryIndex) throws IOException {

        if (files == null || files.isEmpty()) return;

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (int i = 0; i < files.size(); i++) {

            MultipartFile file = files.get(i);
            if (file.isEmpty()) continue;

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath);

            ProductImage image = new ProductImage();
            image.setImageUrl("/images/products/" + fileName);
            image.setPrimaryImage(primaryIndex != null && i == primaryIndex);

            product.addImage(image);
        }
    }
}
