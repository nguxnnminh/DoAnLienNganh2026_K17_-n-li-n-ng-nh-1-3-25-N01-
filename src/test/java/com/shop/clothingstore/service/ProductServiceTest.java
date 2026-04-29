package com.shop.clothingstore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.clothingstore.dto.ProductUpdateDTO;
import com.shop.clothingstore.dto.VariantDTO;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductVariant;
import com.shop.clothingstore.entity.SubCategory;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.service.storage.FileStorageService;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SubCategoryRepository subCategoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductService productService;

    private Product existingProduct;
    private ProductVariant existingVariant;
    private SubCategory subCategory;

    @BeforeEach
    void setUp() {
        subCategory = new SubCategory();
        subCategory.setName("T-Shirts");

        existingVariant = new ProductVariant();
        existingVariant.setSize("M");
        existingVariant.setColor("Black");
        existingVariant.setPrice(new BigDecimal("250000"));
        existingVariant.setStock(10);
        existingVariant.setSold(5);

        existingProduct = new Product();
        existingProduct.setName("Test Shirt");
        existingProduct.setSlug("test-shirt");
        existingProduct.setActive(true);
        existingProduct.setSubCategory(subCategory);
        existingProduct.getProductVariants().add(existingVariant);
        existingVariant.setProduct(existingProduct);
    }

    // =====================================================
    // BUG-02: updateVariants must not duplicate existing variants
    // =====================================================

    @Test
    void updateProduct_existingVariantUpdated_notDuplicated() throws Exception {
        // Simulate variant with a known ID
        injectId(existingVariant, 42L);

        when(productRepository.findProductForEdit(1L))
                .thenReturn(Optional.of(existingProduct));
        when(subCategoryRepository.findById(any()))
                .thenReturn(Optional.of(subCategory));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(productRepository.findBySlug(any())).thenReturn(Optional.empty());

        // DTO references the same variant ID with updated price
        VariantDTO updatedDto = new VariantDTO(42L, "M", "Black", new BigDecimal("300000"), 8);
        ProductUpdateDTO dto = buildUpdateDto(List.of(updatedDto));

        Product result = productService.updateProduct(1L, dto);

        // Exactly ONE variant — no duplicates
        assertThat(result.getProductVariants()).hasSize(1);

        ProductVariant v = result.getProductVariants().get(0);
        assertThat(v.getPrice()).isEqualByComparingTo("300000");
        assertThat(v.getStock()).isEqualTo(8);
        // sold must be preserved (not reset)
        assertThat(v.getSold()).isEqualTo(5);
    }

    @Test
    void updateProduct_newVariantAdded_existingKept() throws Exception {
        injectId(existingVariant, 42L);

        when(productRepository.findProductForEdit(1L))
                .thenReturn(Optional.of(existingProduct));
        when(subCategoryRepository.findById(any()))
                .thenReturn(Optional.of(subCategory));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(productRepository.findBySlug(any())).thenReturn(Optional.empty());

        VariantDTO existingDto = new VariantDTO(42L, "M", "Black", new BigDecimal("250000"), 10);
        VariantDTO newDto      = new VariantDTO(null, "L", "White", new BigDecimal("270000"), 5);

        Product result = productService.updateProduct(1L, buildUpdateDto(List.of(existingDto, newDto)));

        assertThat(result.getProductVariants()).hasSize(2);
    }

    @Test
    void updateProduct_variantRemovedFromDto_deletedFromProduct() throws Exception {
        injectId(existingVariant, 42L);

        // Add a second variant that will be removed
        ProductVariant secondVariant = new ProductVariant();
        secondVariant.setSize("L");
        secondVariant.setColor("White");
        secondVariant.setPrice(new BigDecimal("270000"));
        secondVariant.setStock(3);
        secondVariant.setSold(0);
        secondVariant.setProduct(existingProduct);
        injectId(secondVariant, 99L);
        existingProduct.getProductVariants().add(secondVariant);

        when(productRepository.findProductForEdit(1L))
                .thenReturn(Optional.of(existingProduct));
        when(subCategoryRepository.findById(any()))
                .thenReturn(Optional.of(subCategory));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(productRepository.findBySlug(any())).thenReturn(Optional.empty());

        // DTO only contains variant 42 — variant 99 should be removed
        VariantDTO keepDto = new VariantDTO(42L, "M", "Black", new BigDecimal("250000"), 10);

        Product result = productService.updateProduct(1L, buildUpdateDto(List.of(keepDto)));

        assertThat(result.getProductVariants()).hasSize(1);
        assertThat(result.getProductVariants().get(0).getSize()).isEqualTo("M");
    }

    // =====================================================
    // BUG-01: image deletion must not crash on unmodifiable list
    // =====================================================

    @Test
    void updateProduct_imageDeletion_doesNotThrow() throws Exception {
        injectId(existingVariant, 42L);

        when(productRepository.findProductForEdit(1L))
                .thenReturn(Optional.of(existingProduct));
        when(subCategoryRepository.findById(any()))
                .thenReturn(Optional.of(subCategory));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(productRepository.findBySlug(any())).thenReturn(Optional.empty());

        VariantDTO dto = new VariantDTO(42L, "M", "Black", new BigDecimal("250000"), 10);
        ProductUpdateDTO updateDto = buildUpdateDto(List.of(dto));
        // Request deletion of image IDs even though the product has no images —
        // must not throw UnsupportedOperationException
        updateDto.setImagesToDelete(List.of(1L, 2L, 3L));

        // This call must NOT throw UnsupportedOperationException (BUG-01)
        Product result = productService.updateProduct(1L, updateDto);
        assertThat(result).isNotNull();
    }

    // =====================================================
    // minPrice refresh after variant changes
    // =====================================================

    @Test
    void addVariant_refreshesMinPrice() {
        ProductVariant cheap = new ProductVariant();
        cheap.setPrice(new BigDecimal("100000"));
        cheap.setStock(5);
        cheap.setSold(0);
        cheap.setSize("S");
        cheap.setColor("Red");

        ProductVariant expensive = new ProductVariant();
        expensive.setPrice(new BigDecimal("500000"));
        expensive.setStock(3);
        expensive.setSold(0);
        expensive.setSize("XL");
        expensive.setColor("Blue");

        Product product = new Product();
        product.addVariant(cheap);
        product.addVariant(expensive);

        assertThat(product.getMinPrice()).isEqualByComparingTo("100000");
    }

    // =====================================================
    // SubCategoryId required validation
    // =====================================================

    @Test
    void updateProduct_nullSubCategoryId_throwsIllegalArgument() throws Exception {
        when(productRepository.findProductForEdit(1L))
                .thenReturn(Optional.of(existingProduct));

        ProductUpdateDTO dto = buildUpdateDto(List.of());
        dto.setSubCategoryId(null);

        assertThatThrownBy(() -> productService.updateProduct(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SubCategoryId");
    }

    // =====================================================
    // Helpers
    // =====================================================

    private ProductUpdateDTO buildUpdateDto(List<VariantDTO> variants) {
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setName("Test Shirt");
        dto.setDescription("desc");
        dto.setSubCategoryId(1L);
        dto.setActive(true);
        dto.setVariants(new ArrayList<>(variants));
        dto.setImagesToDelete(new ArrayList<>());
        dto.setNewImages(new ArrayList<>());
        dto.setPrimaryImageIndex(null);
        return dto;
    }

    /** Inject an ID into a BaseEntity via reflection (no public setId). */
    private void injectId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field =
                    com.shop.clothingstore.entity.base.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject ID for test", e);
        }
    }

}
