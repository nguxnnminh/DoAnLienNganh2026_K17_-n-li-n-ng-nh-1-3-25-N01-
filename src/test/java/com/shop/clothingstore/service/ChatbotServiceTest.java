package com.shop.clothingstore.service;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.entity.ProductImage;
import com.shop.clothingstore.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ChatbotServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ChatbotService chatbotService;

    // =============================================================
    // EDGE CASES: null & blank messages
    // =============================================================
    @Test
    @DisplayName("processMessage(null) returns welcome text")
    void processMessage_Null_ReturnsWelcomeText() {
        ChatbotService.ChatbotResponse response = chatbotService.processMessage(null);
        assertThat(response.getMessage()).contains("Xin chào");
        assertThat(response.getProducts()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("processMessage(blank) returns welcome text")
    void processMessage_Blank_ReturnsWelcomeText(String blank) {
        ChatbotService.ChatbotResponse response = chatbotService.processMessage(blank);
        assertThat(response.getMessage()).contains("Xin chào");
        assertThat(response.getProducts()).isEmpty();
    }

    // =============================================================
    // INTENT 1: Chào hỏi
    // =============================================================
    @ParameterizedTest
    @ValueSource(strings = {"xin chào", "hello", "hi", "chào bạn", "hey there"})
    @DisplayName("Greeting intent returns greeting text")
    void processMessage_Greeting_ReturnsGreeting(String greeting) {
        ChatbotService.ChatbotResponse response = chatbotService.processMessage(greeting);
        assertThat(response.getMessage()).contains("trợ lý mua sắm");
        assertThat(response.getProducts()).isEmpty();
    }

    // =============================================================
    // INTENT 2: Trợ giúp
    // =============================================================
    @ParameterizedTest
    @ValueSource(strings = {"giúp", "help", "hướng dẫn", "làm sao để mua"})
    @DisplayName("Help intent returns help text with bullet points")
    void processMessage_Help_ReturnsHelpText(String helpMsg) {
        ChatbotService.ChatbotResponse response = chatbotService.processMessage(helpMsg);
        assertThat(response.getMessage()).contains("Tôi có thể giúp bạn");
        assertThat(response.getMessage()).contains("Tìm sản phẩm");
        assertThat(response.getProducts()).isEmpty();
    }

    // =============================================================
    // INTENT 3: Best sellers
    // =============================================================
    @ParameterizedTest
    @ValueSource(strings = {"bán chạy", "best seller", "phổ biến", "hot", "trending"})
    @DisplayName("Best sellers intent returns products from repository")
    void processMessage_BestSellers_ReturnsProducts(String keyword) {
        Product product = createSampleProduct(1L, "Áo Hoodie", "ao-hoodie", 450000.0);
        when(productRepository.findBestSellers(PageRequest.of(0, 6)))
                .thenReturn(List.of(product));

        ChatbotService.ChatbotResponse response = chatbotService.processMessage(keyword);

        assertThat(response.getMessage()).contains("bán chạy");
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getName()).isEqualTo("Áo Hoodie");
    }

    @Test
    @DisplayName("Best sellers intent returns empty list when no bestsellers")
    void processMessage_BestSellersEmpty_ReturnsEmptyList() {
        when(productRepository.findBestSellers(PageRequest.of(0, 6)))
                .thenReturn(Collections.emptyList());

        ChatbotService.ChatbotResponse response = chatbotService.processMessage("hot");

        assertThat(response.getMessage()).contains("bán chạy");
        assertThat(response.getProducts()).isEmpty();
    }

    // =============================================================
    // INTENT 4: Tìm sản phẩm - có kết quả
    // =============================================================
    @Test
    @DisplayName("Search with keyword returns matching products")
    void processMessage_SearchWithKeyword_ReturnsProducts() {
        Product product = createSampleProduct(2L, "Quần Jeans Xanh", "quan-jeans-xanh", 620000.0);
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productService.findWithFilter(any(ProductFilterDTO.class), any(PageRequest.class)))
                .thenReturn(page);

        ChatbotService.ChatbotResponse response = chatbotService.processMessage("tìm quần jeans");

        assertThat(response.getMessage()).contains("Tìm thấy");
        assertThat(response.getProducts()).hasSize(1);
        assertThat(response.getProducts().get(0).getName()).isEqualTo("Quần Jeans Xanh");
    }

    @Test
    @DisplayName("Search with maxPrice under constraint passes correct filter")
    void processMessage_SearchWithMaxPrice_ParsesPriceUnder() {
        Page<Product> page = new PageImpl<>(Collections.emptyList());
        when(productService.findWithFilter(
                argThat(filter -> filter.getMaxPrice() != null
                && filter.getMaxPrice() == 500000.0
                && "áo".equals(filter.getKeyword())),
                any(PageRequest.class)))
                .thenReturn(page);

        chatbotService.processMessage("áo dưới 500k");

        verify(productService).findWithFilter(
                argThat(filter -> filter.getMaxPrice() != null
                && filter.getMaxPrice() == 500000.0),
                any(PageRequest.class));
    }

    @Test
    @DisplayName("Search with minPrice over constraint passes correct filter")
    void processMessage_SearchWithMinPrice_ParsesPriceOver() {
        Page<Product> page = new PageImpl<>(Collections.emptyList());
        when(productService.findWithFilter(
                argThat(filter -> filter.getMinPrice() != null
                && filter.getMinPrice() == 300000.0),
                any(PageRequest.class)))
                .thenReturn(page);

        chatbotService.processMessage("quần trên 300k");

        verify(productService).findWithFilter(
                argThat(filter -> filter.getMinPrice() != null
                && filter.getMinPrice() == 300000.0),
                any(PageRequest.class));
    }

    @Test
    @DisplayName("Search with color parses and appends color to keyword")
    void processMessage_SearchWithColor_ParsesColor() {
        Page<Product> page = new PageImpl<>(Collections.emptyList());
        when(productService.findWithFilter(
                argThat(filter -> filter.getKeyword() != null
                && filter.getKeyword().contains("đen")),
                any(PageRequest.class)))
                .thenReturn(page);

        chatbotService.processMessage("áo màu đen");

        verify(productService).findWithFilter(
                argThat(filter -> filter.getKeyword() != null
                && filter.getKeyword().contains("đen")),
                any(PageRequest.class));
    }

    @Test
    @DisplayName("Search with both color and price parses all constraints")
    void processMessage_SearchComplex_ParsesAllConstraints() {
        Page<Product> page = new PageImpl<>(Collections.emptyList());
        when(productService.findWithFilter(
                argThat(filter
                        -> filter.getKeyword() != null && filter.getKeyword().contains("xanh")
                && filter.getMaxPrice() != null && filter.getMaxPrice() == 400000.0
                && filter.getMinPrice() != null && filter.getMinPrice() == 100000.0),
                any(PageRequest.class)))
                .thenReturn(page);

        chatbotService.processMessage("tôi muốn áo xanh dưới 400k trên 100k");

        verify(productService).findWithFilter(
                argThat(filter
                        -> filter.getKeyword() != null && filter.getKeyword().contains("xanh")
                && filter.getMaxPrice() != null && filter.getMaxPrice() == 400000.0
                && filter.getMinPrice() != null && filter.getMinPrice() == 100000.0),
                any(PageRequest.class));
    }

    // =============================================================
    // INTENT 4: Tìm sản phẩm - không có kết quả
    // =============================================================
    @Test
    @DisplayName("Search with no results returns apology text")
    void processMessage_SearchNoResults_ReturnsApology() {
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());
        when(productService.findWithFilter(any(ProductFilterDTO.class), any(PageRequest.class)))
                .thenReturn(emptyPage);

        ChatbotService.ChatbotResponse response = chatbotService.processMessage("xyz không tồn tại");

        assertThat(response.getMessage()).contains("Xin lỗi");
        assertThat(response.getMessage()).contains("không tìm thấy");
        assertThat(response.getProducts()).isEmpty();
    }

    // =============================================================
    // BUILD INTRO verifications
    // =============================================================
    @Test
    @DisplayName("Build intro includes keyword and price in message")
    void processMessage_IntroIncludesKeywordAndPrice() {
        Product product = createSampleProduct(3L, "Áo Sơ Mi", "ao-so-mi", 350000.0);
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productService.findWithFilter(any(ProductFilterDTO.class), any(PageRequest.class)))
                .thenReturn(page);

        ChatbotService.ChatbotResponse response = chatbotService.processMessage("áo sơ mi dưới 500k");

        assertThat(response.getMessage()).contains("Tìm thấy");
        assertThat(response.getMessage()).contains("500k");
    }

    // =============================================================
    // ProductSummary mapping
    // =============================================================
    @Test
    @DisplayName("ProductSummary includes correct fields from Product")
    void processMessage_ProductSummaryMapping() {
        Product product = new Product();
        product.setId(99L);
        product.setName("Test Product");
        product.setSlug("test-product");
        // minPrice typically calculated from variants
        // We'll stub the getter via reflection or mock. For simplicity,
        // we rely on Product having getMinPrice() which in real code computes from variants.
        // Since we can't easily mock entity getters without Mockito mock(Product.class),
        // let's use a mock Product.

        Product mockProduct = org.mockito.Mockito.mock(Product.class);
        when(mockProduct.getId()).thenReturn(99L);
        when(mockProduct.getName()).thenReturn("Test Product");
        when(mockProduct.getSlug()).thenReturn("test-product");
        when(mockProduct.getMinPrice()).thenReturn(123000.0);

        ProductImage img = new ProductImage();
        img.setImageUrl("/images/test.jpg");
        img.setPrimaryImage(true);
        when(mockProduct.getImages()).thenReturn(List.of(img));

        Page<Product> page = new PageImpl<>(List.of(mockProduct));
        when(productService.findWithFilter(any(ProductFilterDTO.class), any(PageRequest.class)))
                .thenReturn(page);

        ChatbotService.ChatbotResponse response = chatbotService.processMessage("test");

        ChatbotService.ProductSummary summary = response.getProducts().get(0);
        assertThat(summary.getId()).isEqualTo(99L);
        assertThat(summary.getName()).isEqualTo("Test Product");
        assertThat(summary.getSlug()).isEqualTo("test-product");
        assertThat(summary.getMinPrice()).isEqualTo(123000.0);
        assertThat(summary.getImageUrl()).isEqualTo("/images/test.jpg");
    }

    @Test
    @DisplayName("ProductSummary falls back to first non-primary image")
    void processMessage_ProductSummaryFallbackImage() {
        Product mockProduct = org.mockito.Mockito.mock(Product.class);
        when(mockProduct.getId()).thenReturn(1L);
        when(mockProduct.getName()).thenReturn("No Primary");
        when(mockProduct.getSlug()).thenReturn("no-primary");
        when(mockProduct.getMinPrice()).thenReturn(100000.0);

        ProductImage img = new ProductImage();
        img.setImageUrl("/images/first.jpg");
        img.setPrimaryImage(false);
        when(mockProduct.getImages()).thenReturn(List.of(img));

        Page<Product> page = new PageImpl<>(List.of(mockProduct));
        when(productService.findWithFilter(any(ProductFilterDTO.class), any(PageRequest.class)))
                .thenReturn(page);

        ChatbotService.ChatbotResponse response = chatbotService.processMessage("no primary");

        assertThat(response.getProducts().get(0).getImageUrl()).isEqualTo("/images/first.jpg");
    }

    @Test
    @DisplayName("ProductSummary handles null image list")
    void processMessage_ProductSummaryNullImages() {
        Product mockProduct = org.mockito.Mockito.mock(Product.class);
        when(mockProduct.getId()).thenReturn(1L);
        when(mockProduct.getName()).thenReturn("No Images");
        when(mockProduct.getSlug()).thenReturn("no-images");
        when(mockProduct.getMinPrice()).thenReturn(50000.0);
        when(mockProduct.getImages()).thenReturn(null);

        Page<Product> page = new PageImpl<>(List.of(mockProduct));
        when(productService.findWithFilter(any(ProductFilterDTO.class), any(PageRequest.class)))
                .thenReturn(page);

        ChatbotService.ChatbotResponse response = chatbotService.processMessage("no images");

        assertThat(response.getProducts().get(0).getImageUrl()).isNull();
    }

    // =============================================================
    // Helper
    // =============================================================
    private Product createSampleProduct(Long id, String name, String slug, Double minPrice) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setSlug(slug);
        // getMinPrice() is likely a computed field; for real tests we would need
        // to set up variants. We'll use mock for precise control.
        return p;
    }
}
