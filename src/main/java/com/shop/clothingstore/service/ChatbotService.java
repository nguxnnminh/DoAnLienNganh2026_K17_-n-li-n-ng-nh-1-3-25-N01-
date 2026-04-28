package com.shop.clothingstore.service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.repository.ProductRepository;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    // Static compiled patterns for performance
    private static final Pattern PRICE_RANGE_PATTERN = Pattern.compile(
            "(?:từ\\s*(\\d+(?:[.,]\\d{3})?)\\s*(k|nghìn|đồng)?\\s*(?:đến|tới|-)\\s*(\\d+(?:[.,]\\d{3})?)\\s*(k|nghìn|đồng)?)"
            + "|(?:between\\s*(\\d+)\\s*(?:and|to)\\s*(\\d+))"
    );
    private static final Pattern PRICE_UNDER_PATTERN = Pattern.compile(
            "(dưới|under|<|tối đa|max)\\s*(\\d+(?:[.,]\\d{3})?)\\s*(k|nghìn|đồng|đ)?"
    );
    private static final Pattern PRICE_OVER_PATTERN = Pattern.compile(
            "(?<!đến\\s|tới\\s|to\\s|and\\s)(trên|over|>|tối thiểu|min|từ)\\s*(\\d+(?:[.,]\\d{3})?)\\s*(k|nghìn|đồng|đ)?"
    );

    private static final String[] COLORS = {
        "xanh da trời", "xanh dương", "xanh lá",
        "đen", "trắng", "xanh", "đỏ", "vàng",
        "xám", "nâu", "hồng", "tím", "cam",
        "be", "navy", "cream"
    };

    private static final String[] STOP_WORDS = {
        "tôi", "muốn", "tìm", "mua", "kiếm", "cho", "xem",
        "cần", "hãy", "giúp", "sản phẩm", "có", "không",
        "cái", "chiếc", "đôi", "bộ", "một", "vài", "màu"
    };

    private final ProductService productService;
    private final ProductRepository productRepository;

    public ChatbotService(ProductService productService, ProductRepository productRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
    }

    public ChatbotResponse processMessage(String message) {

        if (message == null || message.isBlank()) {
            return ChatbotResponse.text("Xin chào! Tôi có thể giúp bạn tìm sản phẩm. "
                    + "Hãy mô tả sản phẩm bạn muốn, ví dụ: \"áo màu đen dưới 500k\"");
        }

        String lower = message.toLowerCase().trim();
        log.info("Chatbot received: {}", message);

        // =====================================================
        // INTENT 1: Chào hỏi
        // =====================================================
        if (matchesAny(lower, "xin chào", "hello", "hi", "chào", "hey")) {
            return ChatbotResponse.text(
                    "Xin chào! Tôi là trợ lý mua sắm. Bạn muốn tìm sản phẩm gì? "
                    + "Ví dụ: \"tìm áo thun dưới 300k\" hoặc \"quần jeans màu đen\"");
        }

        // =====================================================
        // INTENT 2: Hỏi trợ giúp
        // =====================================================
        if (matchesAny(lower, "giúp", "help", "hướng dẫn", "làm sao")) {
            return ChatbotResponse.text(
                    "Tôi có thể giúp bạn:\n"
                    + "• Tìm sản phẩm: \"tìm áo khoác\"\n"
                    + "• Lọc theo giá: \"áo dưới 500k\"\n"
                    + "• Lọc theo màu: \"quần màu đen\"\n"
                    + "• Xem best sellers: \"sản phẩm bán chạy\"\n"
                    + "• Xem giỏ hàng: \"giỏ hàng\"");
        }

        // =====================================================
        // INTENT 3: Best sellers
        // =====================================================
        if (matchesAny(lower, "bán chạy", "best seller", "phổ biến", "hot", "trending")) {
            List<Product> products = productRepository.findBestSellers(PageRequest.of(0, 6));

            return ChatbotResponse.withProducts(
                    "Đây là những sản phẩm bán chạy nhất:", products);
        }

        // =====================================================
        // INTENT 4: Tìm sản phẩm (parse keyword + price + color)
        // =====================================================
        ParsedQuery parsed = parseQuery(lower);

        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setKeyword(parsed.keyword);
        filter.setMaxPrice(parsed.maxPrice);
        filter.setMinPrice(parsed.minPrice);

        List<Product> products = productService.findWithFilter(
                filter, PageRequest.of(0, 8)
        ).getContent();

        if (products.isEmpty()) {
            return ChatbotResponse.text(
                    "Xin lỗi, tôi không tìm thấy sản phẩm nào phù hợp với \""
                    + message + "\". Bạn thử mô tả khác xem?");
        }

        String intro = buildIntro(parsed, products.size());
        return ChatbotResponse.withProducts(intro, products);
    }

    // =====================================================
    // PARSE QUERY: tách keyword, giá, màu sắc
    // =====================================================
    private ParsedQuery parseQuery(String message) {

        ParsedQuery result = new ParsedQuery();
        String original = message;

        // Parse giá range: "từ 200k đến 500k", "100k - 500k", "200k-500k"
        Matcher mRange = PRICE_RANGE_PATTERN.matcher(message);
        if (mRange.find()) {
            String minStr = mRange.group(1) != null ? mRange.group(1) : mRange.group(5);
            String maxStr = mRange.group(3) != null ? mRange.group(3) : mRange.group(6);
            if (minStr != null) {
                result.minPrice = parsePrice(minStr);
            }
            if (maxStr != null) {
                result.maxPrice = parsePrice(maxStr);
            }
            message = message.replace(mRange.group(), "");
        }

        // Parse giá: "dưới 500k", "< 300k", "tối đa 500.000đ", "under 500000"
        Matcher m1 = PRICE_UNDER_PATTERN.matcher(message);
        if (m1.find()) {
            result.maxPrice = parsePrice(m1.group(2));
            message = message.replace(m1.group(), "");
        }

        // Parse giá: "trên 300k", "> 200000", "từ 100k" (khi không có range)
        Matcher m2 = PRICE_OVER_PATTERN.matcher(message);
        if (m2.find()) {
            if ("từ".equals(m2.group(1)) && result.minPrice != null) {
                // already parsed in range, do nothing
            } else {
                result.minPrice = parsePrice(m2.group(2));
                message = message.replace(m2.group(), "");
            }
        }

        // Parse màu sắc: sắp xếp theo độ dài giảm dần để match cụm dài trước
        for (String color : COLORS) {
            if (message.contains(color)) {
                result.color = color;
                message = message.replace(color, "");
                break;
            }
        }

        // Xóa stop words
        for (String word : STOP_WORDS) {
            message = message.replace(word, "");
        }

        // Clean up
        result.keyword = message.replaceAll("\\s+", " ").trim();
        if (result.keyword.isEmpty()) {
            result.keyword = null;
        }
        // Append color vào keyword để search
        if (result.color != null && result.keyword != null) {
            result.keyword = result.keyword + " " + result.color;
        } else if (result.color != null) {
            result.keyword = result.color;
        }

        log.debug("Parsed query | original='{}' | keyword={} | color={} | minPrice={} | maxPrice={}",
                original, result.keyword, result.color, result.minPrice, result.maxPrice);
        return result;
    }

    private Double parsePrice(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replaceAll("[.,]", "");
        double value = Double.parseDouble(cleaned);
        if (value < 1000) {
            value *= 1000; // "500k" -> 500000
        }
        return value;
    }

    private String buildIntro(ParsedQuery parsed, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tìm thấy ").append(count).append(" sản phẩm");

        if (parsed.keyword != null) {
            sb.append(" cho \"").append(parsed.keyword).append("\"");
        }
        if (parsed.maxPrice != null) {
            sb.append(" dưới ").append(formatPrice(parsed.maxPrice));
        }
        if (parsed.minPrice != null) {
            sb.append(" từ ").append(formatPrice(parsed.minPrice));
        }
        sb.append(":");
        return sb.toString();
    }

    private String formatPrice(Double price) {
        if (price >= 1_000_000) {
            return String.format("%.0ftr", price / 1_000_000);
        }
        return String.format("%.0fk", price / 1000);
    }

    private boolean matchesAny(String input, String... keywords) {
        for (String kw : keywords) {
            if (input.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================
    // INNER CLASSES
    // =====================================================
    private static class ParsedQuery {

        String keyword;
        Double maxPrice;
        Double minPrice;
        String color;
    }

    public static class ChatbotResponse {

        private String message;
        private List<ProductSummary> products;

        public static ChatbotResponse text(String message) {
            ChatbotResponse r = new ChatbotResponse();
            r.message = message;
            r.products = List.of();
            return r;
        }

        public static ChatbotResponse withProducts(String message, List<Product> products) {
            ChatbotResponse r = new ChatbotResponse();
            r.message = message;
            r.products = products.stream().map(ProductSummary::from).toList();
            return r;
        }

        public String getMessage() {
            return message;
        }

        public List<ProductSummary> getProducts() {
            return products;
        }
    }

    public static class ProductSummary {

        private Long id;
        private String name;
        private String slug;
        private Double minPrice;
        private String imageUrl;

        public static ProductSummary from(Product p) {
            ProductSummary s = new ProductSummary();
            s.id = p.getId();
            s.name = p.getName();
            s.slug = p.getSlug();
            s.minPrice = p.getMinPrice();
            if (p.getImages() == null) {
                s.imageUrl = null;
            } else {
                s.imageUrl = p.getImages().stream()
                        .filter(img -> img.isPrimaryImage())
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse(p.getImages().isEmpty() ? null
                                : p.getImages().iterator().next().getImageUrl());
            }
            return s;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSlug() {
            return slug;
        }

        public Double getMinPrice() {
            return minPrice;
        }

        public String getImageUrl() {
            return imageUrl;
        }
    }
}
