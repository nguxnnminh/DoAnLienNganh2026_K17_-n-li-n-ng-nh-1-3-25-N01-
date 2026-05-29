package com.shop.clothingstore.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.clothingstore.dto.api.ChatbotResponse;
import com.shop.clothingstore.dto.ProductFilterDTO;
import com.shop.clothingstore.entity.Category;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.config.ChatbotAiProperties;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.repository.CategoryRepository;
import com.shop.clothingstore.repository.SubCategoryRepository;
import com.shop.clothingstore.service.ai.AiRequestException;
import com.shop.clothingstore.service.ai.OllamaChatClient;

@Service
public class AiChatbotService {

    private static final Logger log = LoggerFactory.getLogger(AiChatbotService.class);

    // Price patterns work on normalize()d text (diacritics already stripped)
    private static final Pattern PRICE_RANGE_PATTERN = Pattern.compile(
            "(?:tu\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|m|k|nghin|ngan|d|vnd)?\\s*(?:den|toi|-)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|m|k|nghin|ngan|d|vnd)?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PRICE_UNDER_PATTERN = Pattern.compile(
            "(?:duoi|<|toi\\s*da|max|under)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|m|k|nghin|ngan|d|vnd)?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PRICE_OVER_PATTERN = Pattern.compile(
            "(?:tren|>=|toi\\s*thieu|min|over|hon)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|m|k|nghin|ngan|d|vnd)?",
            Pattern.CASE_INSENSITIVE
    );

    private final OllamaChatClient ollama;
    private final ChatbotAiProperties aiProps;
    private final ObjectMapper objectMapper;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    private volatile Instant aiDisabledUntil = null;

    public AiChatbotService(
            OllamaChatClient ollama,
            ChatbotAiProperties aiProps,
            ObjectMapper objectMapper,
            ProductService productService,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository) {
        this.ollama = ollama;
        this.aiProps = aiProps;
        this.objectMapper = objectMapper;
        this.productService = productService;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    public boolean isEnabledAndConfigured() {
        return ollama.isConfigured();
    }

    public ChatbotResponse processMessage(String userMessage) {
        return processMessage(userMessage, List.of());
    }

    /**
     * Process a message with prior conversation history for multi-turn context.
     *
     * @param history previous turns as a list of {role, content} maps (oldest first);
     *                only consulted on the AI path so follow-ups like "còn màu khác không?"
     *                are understood. Rule-based answers stay stateless and instant.
     */
    public ChatbotResponse processMessage(String userMessage, List<Map<String, Object>> history) {
        if (userMessage == null || userMessage.isBlank()) {
            return ChatbotResponse.text("Xin chào! Mình có thể giúp bạn tìm sản phẩm, tư vấn size hoặc giải đáp về đơn hàng, vận chuyển, đổi trả. Bạn cần gì?");
        }

        if (userMessage.length() > 2000) {
            userMessage = userMessage.substring(0, 2000);
        }

        List<Map<String, Object>> convo = history != null ? history : List.of();

        // 1. Rule-based FAQs
        ChatbotResponse ruleResult = handleRuleBased(userMessage);
        if (ruleResult != null) return ruleResult;

        // 2. Rule-based product search — works even without Ollama
        ChatbotResponse ruleProduct = tryRuleBasedProductSearch(userMessage);
        if (ruleProduct != null) return ruleProduct;

        // 3. AI path
        if (!isEnabledAndConfigured()) {
            return ChatbotResponse.text("AI chatbot chưa được cấu hình. Bạn có thể mô tả loại sản phẩm (áo/quần/giày...), màu sắc và khoảng giá để mình tìm giúp nhé!");
        }

        Instant disabledUntil = aiDisabledUntil;
        if (disabledUntil != null && Instant.now().isBefore(disabledUntil)) {
            return ChatbotResponse.text("AI đang tạm quá tải. Hãy mô tả loại sản phẩm, màu sắc và khoảng giá — mình sẽ gợi ý ngay!");
        }

        String planner = planAction(userMessage, convo);
        ActionPlan plan = parseActionPlan(planner).orElseGet(ActionPlan::general);

        if ("best_sellers".equals(plan.intent())) {
            List<Product> products = productRepository.findBestSellers(PageRequest.of(0, clamp(plan.limit(), 1, 8)));
            String msg = products.isEmpty()
                    ? "Chưa có dữ liệu bán chạy. Bạn muốn mình gợi ý theo loại/màu không?"
                    : "Đây là những sản phẩm đang bán chạy nhất tại NOVA:";
            return ChatbotResponse.withProducts(msg, products);
        }

        if ("search_products".equals(plan.intent())) {
            ProductFilterDTO filter = new ProductFilterDTO();
            applyCategoryHints(filter, plan.keyword(), userMessage);

            // Build keyword: combine AI keyword + detected color
            String norm = normalize(userMessage);
            String color = detectColor(norm);
            String aiKeyword = blankToNull(plan.keyword());

            if (filter.getCategoryId() == null && filter.getSubCategoryId() == null) {
                // No category found — use AI keyword for name search
                filter.setKeyword(aiKeyword);
            } else if (color != null) {
                // Category found + color detected — search by color in product name
                filter.setKeyword(color);
            }

            filter.setMinPrice(plan.minPrice());
            filter.setMaxPrice(plan.maxPrice());
            applyPriceHints(filter, userMessage);

            int limit = clamp(plan.limit(), 1, 8);
            List<Product> products = searchWithFallback(filter, limit);

            if (products.isEmpty()) {
                return ChatbotResponse.text("Không tìm thấy sản phẩm phù hợp 😔 Thử mô tả: loại (áo thun / jeans / giày...), màu sắc, và khoảng giá nhé!");
            }
            return ChatbotResponse.withProducts(buildSearchIntro(plan, products.size()), products);
        }

        String answer = answerGeneral(userMessage, convo);
        if (answer == null || answer.isBlank()) {
            return ChatbotResponse.text("Mình có thể giúp bạn tìm sản phẩm theo loại, màu sắc hoặc giá. Hãy mô tả thứ bạn đang tìm nhé!");
        }
        return ChatbotResponse.text(answer);
    }

    // ─── Rule-based product search (no AI needed) ────────────────────────────

    private ChatbotResponse tryRuleBasedProductSearch(String raw) {
        String norm = normalize(raw);

        // Must look like a product search intent
        boolean isSearchIntent = containsAny(norm,
                "tim", "can", "mua", "xem", "goi y", "de nghi", "co khong", "ban co",
                "show", "find", "buy", "search", "want", "need",
                "muon", "thich", "ung y", "phu hop", "mac gi", "mac voi",
                "ao", "quan", "giay", "tui", "mu", "phu kien", "do mac"
        );
        if (!isSearchIntent) return null;

        ProductFilterDTO filter = new ProductFilterDTO();
        applyCategoryHints(filter, null, raw);

        // Only proceed if a specific subcategory or category was detected
        if (filter.getCategoryId() == null && filter.getSubCategoryId() == null) return null;

        String color = detectColor(norm);
        if (color != null) filter.setKeyword(color);
        applyPriceHints(filter, raw);

        List<Product> products = searchWithFallback(filter, 6);
        if (products.isEmpty()) return null; // let AI try

        String intro = buildRuleIntro(filter, color, products.size());
        return ChatbotResponse.withProducts(intro, products);
    }

    // ─── Multi-step fallback search ───────────────────────────────────────────

    private List<Product> searchWithFallback(ProductFilterDTO base, int limit) {
        PageRequest page = PageRequest.of(0, limit);

        // Step 1: full criteria
        List<Product> r = productService.findWithFilter(base, page).getContent();
        if (!r.isEmpty()) return r;

        // Step 2: drop keyword, keep category + price
        if (base.getKeyword() != null) {
            ProductFilterDTO f2 = copyFilter(base);
            f2.setKeyword(null);
            r = productService.findWithFilter(f2, page).getContent();
            if (!r.isEmpty()) return r;
        }

        // Step 3: drop price constraints, keep category + keyword
        if (base.getMinPrice() != null || base.getMaxPrice() != null) {
            ProductFilterDTO f3 = copyFilter(base);
            f3.setMinPrice(null);
            f3.setMaxPrice(null);
            r = productService.findWithFilter(f3, page).getContent();
            if (!r.isEmpty()) return r;
        }

        // Step 4: subcategory only (drop keyword + price)
        if (base.getSubCategoryId() != null) {
            ProductFilterDTO f4 = new ProductFilterDTO();
            f4.setSubCategoryId(base.getSubCategoryId());
            r = productService.findWithFilter(f4, page).getContent();
            if (!r.isEmpty()) return r;
        }

        // Step 5: category only
        if (base.getCategoryId() != null) {
            ProductFilterDTO f5 = new ProductFilterDTO();
            f5.setCategoryId(base.getCategoryId());
            r = productService.findWithFilter(f5, page).getContent();
            if (!r.isEmpty()) return r;
        }

        return List.of();
    }

    private static ProductFilterDTO copyFilter(ProductFilterDTO src) {
        ProductFilterDTO f = new ProductFilterDTO();
        f.setCategoryId(src.getCategoryId());
        f.setSubCategoryId(src.getSubCategoryId());
        f.setKeyword(src.getKeyword());
        f.setMinPrice(src.getMinPrice());
        f.setMaxPrice(src.getMaxPrice());
        return f;
    }

    // ─── Rule-based FAQ answers ───────────────────────────────────────────────

    private ChatbotResponse handleRuleBased(String raw) {
        String msg = normalize(raw); // diacritics-free, lowercase

        if (containsAny(msg, "xin chao", "chao", "hello", "hi", "hey", "alo", "helo")) {
            return ChatbotResponse.text("Xin chào! Mình là trợ lý của NOVA 👋 Bạn muốn tìm sản phẩm, hỏi về size, vận chuyển hay đổi trả?");
        }

        if (containsAny(msg, "ship", "van chuyen", "giao hang", "phi ship", "free ship",
                "bao lau", "khi nao", "thoi gian giao", "nhan hang", "delivery")) {
            return ChatbotResponse.text("""
                    📦 Thông tin vận chuyển NOVA:
                    • Miễn phí ship cho đơn từ 500.000₫ trở lên.
                    • Đơn dưới 500.000₫: phí ship cố định 30.000₫.
                    • Thời gian giao: 2–4 ngày (nội thành), 3–6 ngày (tỉnh khác).
                    • Hiện chỉ giao trong Việt Nam.
                    """.strip());
        }

        if (containsAny(msg, "doi tra", "hoan tien", "tra hang", "chinh sach", "refund",
                "return", "doi hang", "khieu nai", "bao hanh")) {
            return ChatbotResponse.text("""
                    🔄 Chính sách đổi trả NOVA:
                    • Đổi trả miễn phí trong 14 ngày kể từ khi nhận hàng.
                    • Sản phẩm phải còn nguyên tem, chưa qua sử dụng.
                    • Lỗi sản xuất: đổi mới hoặc hoàn tiền 100%.
                    • Vào "Đơn hàng của tôi" trên website để tạo yêu cầu đổi trả.
                    """.strip());
        }

        if (containsAny(msg, "size", "kich co", "chon size", "bang size", "fit",
                "rong", "chat", "m la", "l la", "xl la", "xxl", "size nao")) {
            return ChatbotResponse.text("""
                    📏 Bảng size NOVA:
                    • S   → 48–55 kg, cao 155–165 cm
                    • M   → 55–65 kg, cao 163–170 cm
                    • L   → 65–75 kg, cao 168–175 cm
                    • XL  → 75–85 kg, cao 173–180 cm
                    • XXL → 85–95 kg, cao 178–185 cm
                    Nếu đang ở giữa hai size, nên chọn size lớn hơn để mặc thoải mái hơn!
                    """.strip());
        }

        if (containsAny(msg, "thanh toan", "payment", "pay", "cod", "tien mat",
                "chuyen khoan", "atm", "the tin dung", "momo", "zalopay", "vnpay", "banking")) {
            return ChatbotResponse.text("""
                    💳 Hình thức thanh toán NOVA:
                    • Thanh toán khi nhận hàng (COD).
                    • Chuyển khoản ngân hàng.
                    Sắp tới sẽ có thêm các hình thức thanh toán trực tuyến!
                    """.strip());
        }

        if (containsAny(msg, "ma giam gia", "coupon", "discount", "khuyen mai",
                "giam gia", "sale", "uu dai", "voucher", "promo")) {
            return ChatbotResponse.text("""
                    🎁 Mã giảm giá & Khuyến mãi NOVA:
                    • Nhập mã giảm giá tại bước Thanh toán.
                    • Theo dõi NOVA trên mạng xã hội để nhận mã mới nhất.
                    • Đơn hàng đầu tiên thường có ưu đãi đặc biệt — tạo tài khoản để nhận ngay!
                    """.strip());
        }

        if (containsAny(msg, "chat lieu", "material", "cotton", "chat luong",
                "vai", "hang that", "hang chinh hang", "hang fake", "co tot")) {
            return ChatbotResponse.text("""
                    ✨ Chất lượng sản phẩm NOVA:
                    • Vải cotton cao cấp, garment-dyed, không phai màu, bền đẹp.
                    • Thiết kế minimal, form oversized — phù hợp phong cách hiện đại.
                    • 100% hàng chính hãng, kiểm tra chất lượng trước khi gửi đi.
                    • Cam kết đổi/hoàn tiền nếu có lỗi sản xuất.
                    """.strip());
        }

        if (containsAny(msg, "cua hang", "dia chi", "showroom", "store", "offline",
                "den truc tiep", "lien he", "contact", "ho tro", "cskh", "hotline")) {
            return ChatbotResponse.text("""
                    📍 Về NOVA:
                    • NOVA hiện kinh doanh online qua website này.
                    • Hỗ trợ trực tiếp qua kênh mạng xã hội của NOVA.
                    • Mình (chatbot) có thể giúp bạn tìm sản phẩm, tư vấn size và giải đáp thắc mắc ngay tại đây!
                    """.strip());
        }

        if (containsAny(msg, "don hang", "order", "trang thai", "theo doi",
                "tracking", "da dat", "khi nao den")) {
            return ChatbotResponse.text("Để kiểm tra đơn hàng, vào mục \"Đơn hàng của tôi\" sau khi đăng nhập. Nếu có vấn đề, hãy mô tả để mình hỗ trợ thêm nhé.");
        }

        if (containsAny(msg, "ban chay", "hot", "trending", "pho bien",
                "duoc ua chuong", "nhieu nguoi mua", "top san pham", "bestseller", "best seller")) {
            List<Product> products = productRepository.findBestSellers(PageRequest.of(0, 6));
            String txt = products.isEmpty()
                    ? "Hiện chưa có dữ liệu bán chạy. Bạn muốn mình gợi ý theo loại/màu không?"
                    : "Đây là những sản phẩm đang bán chạy nhất tại NOVA:";
            return ChatbotResponse.withProducts(txt, products);
        }

        if (containsAny(msg, "xem tat ca", "show all", "tat ca san pham",
                "co gi", "co nhung gi", "hang moi", "new arrival", "moi nhat")) {
            List<Product> products = productRepository.findBestSellers(PageRequest.of(0, 8));
            return ChatbotResponse.withProducts("Một số sản phẩm nổi bật tại NOVA — vào trang Shop để xem toàn bộ nhé:", products);
        }

        if (containsAny(msg, "outfit", "phoi do", "mac gi", "set do", "mix do", "mac voi gi")) {
            List<Product> products = productRepository.findBestSellers(PageRequest.of(0, 6));
            return ChatbotResponse.withProducts("Gợi ý một số sản phẩm bạn có thể phối đồ cùng nhau:", products);
        }

        return null;
    }

    // ─── Price extraction ─────────────────────────────────────────────────────

    private void applyPriceHints(ProductFilterDTO filter, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return;
        String msg = normalize(userMessage); // diacritics-free

        BigDecimal min = filter.getMinPrice();
        BigDecimal max = filter.getMaxPrice();

        Matcher range = PRICE_RANGE_PATTERN.matcher(msg);
        if (range.find()) {
            if (min == null) min = parseMoney(range.group(1), range.group(2));
            if (max == null) max = parseMoney(range.group(3), range.group(4));
        } else {
            Matcher under = PRICE_UNDER_PATTERN.matcher(msg);
            if (under.find() && max == null) max = parseMoney(under.group(1), under.group(2));
            Matcher over = PRICE_OVER_PATTERN.matcher(msg);
            if (over.find() && min == null) min = parseMoney(over.group(1), over.group(2));
        }

        if (min != null) filter.setMinPrice(min);
        if (max != null) filter.setMaxPrice(max);
    }

    private static BigDecimal parseMoney(String numberPart, String unitPart) {
        if (numberPart == null || numberPart.isBlank()) return null;
        String n = numberPart.trim().replace(",", ".");
        BigDecimal value;
        try {
            value = new BigDecimal(n);
        } catch (NumberFormatException ex) {
            String digits = numberPart.replaceAll("[^0-9]", "");
            if (digits.isBlank()) return null;
            value = new BigDecimal(digits);
        }
        String unit = unitPart == null ? "" : unitPart.trim().toLowerCase();
        if (unit.contains("trieu") || unit.equals("tr") || unit.equals("m")) {
            return value.multiply(BigDecimal.valueOf(1_000_000));
        }
        if (unit.contains("nghin") || unit.equals("k") || unit.equals("ngan")) {
            return value.multiply(BigDecimal.valueOf(1_000));
        }
        return value;
    }

    // ─── Category / subcategory detection ────────────────────────────────────

    private void applyCategoryHints(ProductFilterDTO filter, String keyword, String userMessage) {
        String k = (keyword == null ? "" : keyword).trim();
        String msg = (userMessage == null ? "" : userMessage).trim();
        // Normalize (diacritics-free, lowercase, collapsed spaces)
        String combined = normalize(k + " " + msg);

        // ── SHOES ─────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "shoes", "shoe", "sneaker", "sneakers", "kicks", "boot", "boots",
                "sandal", "sandals", "loafer", "loafers", "oxford", "slip on", "slip-on",
                "giay", "dep", "giay the thao", "giay sneaker", "giay boot", "giay da",
                "giay dep", "giay chay bo", "running shoes")) {
            setSubCategoryBySlug(filter, "shoes");
            return;
        }

        // ── BAG ───────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "bag", "bags", "backpack", "tote", "clutch", "handbag", "purse", "satchel",
                "crossbody", "shoulder bag", "fanny pack", "waist bag",
                "tui", "balo", "ba lo",
                "tui xach", "tui tote", "tui deo cheo", "tui deo vai",
                "tui khoac", "tui the thao",
                "vi", "wallet")) {
            setSubCategoryBySlug(filter, "bag");
            return;
        }

        // ── CAP ───────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "cap", "caps", "hat", "hats", "beanie", "bucket hat", "snapback",
                "baseball cap", "dad hat", "trucker hat", "visor",
                "mu", "non",
                "mu luoi trai", "mu bucket", "non bucket", "mu luoi",
                "mu beret", "mu len", "mu snapback")) {
            setSubCategoryBySlug(filter, "cap");
            return;
        }

        // ── JEANS ─────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "jeans", "jean", "denim", "skinny jeans", "straight jeans", "wide leg jeans",
                "quan bo", "bo", "quan jeans", "quan jean",
                "quan denim", "jeans rach", "quan rach")) {
            setSubCategoryBySlug(filter, "jeans");
            return;
        }

        // ── SHORTS ────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "shorts", "short", "boxer shorts", "cargo shorts",
                "quan short", "quan ngan", "quan the thao ngan",
                "quan boxer", "short quan")) {
            setSubCategoryBySlug(filter, "shorts");
            return;
        }

        // ── PANTS ─────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "pants", "trousers", "chinos", "slacks", "jogger", "joggers",
                "sweatpants", "cargo", "cargo pants", "wide leg", "straight leg",
                "khaki", "kaki",
                "quan dai", "quan kaki", "quan tay",
                "quan jogger", "quan au", "quan ong rong", "quan tay dai",
                "quan nit", "quan the thao dai")) {
            setSubCategoryBySlug(filter, "pants");
            return;
        }

        // ── HOODIE ────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "hoodie", "hoodies", "sweater", "sweatshirt", "pullover", "crewneck",
                "zip up", "zip-up", "bomber", "windbreaker", "fleece", "knit",
                "ao ni", "ao khoac", "ao hoodie", "ao sweater",
                "ni bong", "ao chui dau", "ao len", "ao nit",
                "ao gio", "ao bomber", "ao zip",
                "mua dong", "dong phuc", "ao am")) {
            setSubCategoryBySlug(filter, "hoodie");
            return;
        }

        // ── TEE ───────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "tee", "t-shirt", "tshirt", "graphic tee", "oversized tee",
                "basic tee", "crop top", "polo",
                "ao thun", "thun", "phong",
                "ao phong", "ao cotton", "ao tee", "ao tshirt",
                "ao co tron", "ao trơn", "ao co tron tay ngan",
                "mua he", "he ve")) {
            setSubCategoryBySlug(filter, "tee");
            return;
        }

        // ── SHIRT ─────────────────────────────────────────────────────────────
        if (containsAny(combined,
                "shirt", "shirts", "flannel", "oxford shirt", "button", "overshirt",
                "chambray", "linen shirt", "dress shirt",
                "so mi", "ao so mi",
                "ao caro", "ao ke", "ao soc", "ao flannel",
                "ao kieu", "ao tay dai")) {
            setSubCategoryBySlug(filter, "shirt");
            return;
        }

        // ── CATEGORY FALLBACK ─────────────────────────────────────────────────
        if (containsAny(combined, "bottom", "quan", "pants", "shorts", "jeans")) {
            setCategoryBySlug(filter, "bottom");
        } else if (containsAny(combined, "top", "ao", "tee", "hoodie", "shirt", "do mac")) {
            setCategoryBySlug(filter, "top");
        } else if (containsAny(combined,
                "accessories", "acc", "phu kien", "bag", "shoes", "cap", "giay", "tui", "mu")) {
            setCategoryBySlug(filter, "accessories");
        }
    }

    // ─── Color detection ─────────────────────────────────────────────────────

    static String detectColor(String normalizedMsg) {
        if (normalizedMsg == null) return null;
        // Order matters — check more specific terms first
        if (containsAny(normalizedMsg, "den", "black", "mau den")) return "đen";
        if (containsAny(normalizedMsg, "trang", "white", "mau trang", "off white")) return "trắng";
        if (containsAny(normalizedMsg, "xam", "gray", "grey", "mau xam")) return "xám";
        if (containsAny(normalizedMsg, "xanh la", "xanh la cay", "green")) return "xanh lá";
        if (containsAny(normalizedMsg, "xanh duong", "xanh lam", "navy", "blue", "xanh navy")) return "xanh";
        if (containsAny(normalizedMsg, "xanh", "mau xanh")) return "xanh";
        if (containsAny(normalizedMsg, "do", "red", "mau do")) return "đỏ";
        if (containsAny(normalizedMsg, "hong", "pink", "mau hong")) return "hồng";
        if (containsAny(normalizedMsg, "tim", "purple", "violet", "mau tim")) return "tím";
        if (containsAny(normalizedMsg, "vang", "yellow", "mau vang")) return "vàng";
        if (containsAny(normalizedMsg, "nau", "brown", "chocolate", "mau nau")) return "nâu";
        if (containsAny(normalizedMsg, "cam", "orange", "mau cam")) return "cam";
        if (containsAny(normalizedMsg, "be", "kem", "beige", "cream", "nude")) return "kem";
        if (containsAny(normalizedMsg, "bac", "silver", "mau bac")) return "bạc";
        if (containsAny(normalizedMsg, "tron", "basic", "plain", "mau tron")) return null; // plain/solid — no color to filter
        return null;
    }

    // ─── AI planner ───────────────────────────────────────────────────────────

    private String planAction(String userMessage, List<Map<String, Object>> history) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", """
Bạn là trợ lý bán hàng của website thời trang NOVA. Phân tích câu hỏi (có thể tham chiếu các tin nhắn trước) và trả về JSON hành động.

Danh mục sản phẩm NOVA:
- Top (áo): tee (áo thun, phông, t-shirt, polo), hoodie (áo nỉ, sweater, bomber, áo len, áo gió), shirt (sơ mi, áo caro, flannel)
- Bottom (quần): pants (quần dài, kaki, cargo, jogger, âu), shorts (quần short, ngắn), jeans (quần bò, denim)
- Accessories (phụ kiện): shoes (giày, dép, sneaker, boot), bag (túi, balo), cap (mũ, nón)

Chỉ trả về MỘT JSON (không markdown, không giải thích):
{
  "intent": "search_products" | "best_sellers" | "general",
  "keyword": string | null,
  "color": string | null,
  "minPrice": number | null,
  "maxPrice": number | null,
  "limit": number
}

Quy tắc:
- "bán chạy", "hot", "trending", "phổ biến", "bestseller" -> best_sellers
- Muốn tìm/mua/xem/gợi ý sản phẩm -> search_products, keyword là tên loại SP bằng tiếng Anh (shoes/bag/cap/tee/hoodie/shirt/pants/shorts/jeans)
- Màu sắc: trích xuất ra trường "color" (vd: "black", "white", "blue", "red", "gray", "green", "pink", "brown", "yellow", "purple")
- Giá: trích xuất VND — "500k"=500000, "1 triệu"=1000000, "1tr5"=1500000
- Câu hỏi chung (ship, đổi trả, size, thanh toán...) -> general
- limit mặc định 6, tối đa 8
""".trim()
            ));
            appendHistory(messages, history);
            messages.add(Map.of("role", "user", "content", userMessage));
            return ollama.chat(messages);
        } catch (AiRequestException e) {
            handleAiHttpError(e.getStatusCode(), e.getMessage());
            return "";
        } catch (IOException | InterruptedException | RuntimeException e) {
            log.warn("AI planAction failed, fallback to general. err={}", e.getMessage());
            return "";
        }
    }

    private String answerGeneral(String userMessage, List<Map<String, Object>> history) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", """
Bạn là trợ lý CSKH thân thiện của website thời trang NOVA.

Thông tin cửa hàng:
- Sản phẩm: áo thun (tee), hoodie, sơ mi (shirt), quần dài (pants), quần short, jeans, giày (shoes), túi/balo (bag), mũ (cap)
- Miễn phí ship đơn từ 500.000đ, giao 2-6 ngày làm việc trong nước
- Đổi trả miễn phí trong 14 ngày, còn nguyên tem
- Thanh toán: COD hoặc chuyển khoản
- Size: S/M/L/XL/XXL
- Chất liệu: cotton cao cấp, garment-dyed, form oversized

Quy tắc trả lời:
- Ngắn gọn, thân thiện, dùng tiếng Việt
- Nếu không có thông tin chính xác, hướng dẫn khách vào mục tương ứng trên web
- Không bịa đặt thông tin
- Có thể tham chiếu ngữ cảnh các tin nhắn trước để trả lời câu hỏi nối tiếp
""".trim()
            ));
            appendHistory(messages, history);
            messages.add(Map.of("role", "user", "content", userMessage));
            return ollama.chat(messages);
        } catch (AiRequestException e) {
            handleAiHttpError(e.getStatusCode(), e.getMessage());
            return "";
        } catch (Exception e) {
            log.warn("AI answerGeneral failed. err={}", e.getMessage());
            return "";
        }
    }

    /**
     * Append up to the last 6 prior turns (role=user|assistant) to the LLM message list.
     * Defends against malformed history entries and caps size to bound the prompt.
     */
    private void appendHistory(List<Map<String, Object>> messages, List<Map<String, Object>> history) {
        if (history == null || history.isEmpty()) {
            return;
        }
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            Map<String, Object> turn = history.get(i);
            if (turn == null) continue;
            Object role = turn.get("role");
            Object content = turn.get("content");
            if (role == null || content == null) continue;
            String r = String.valueOf(role);
            if (!"user".equals(r) && !"assistant".equals(r)) continue;
            messages.add(Map.of("role", r, "content", String.valueOf(content)));
        }
    }

    private void handleAiHttpError(int status, String message) {
        if (status == 429) {
            int cooldown = Math.max(30, aiProps.getCooldownSeconds());
            aiDisabledUntil = Instant.now().plusSeconds(cooldown);
            log.warn("AI 429 — disabled for {}s. err={}", cooldown, message);
            return;
        }
        if (status == 401 || status == 403) {
            int cooldown = Math.max(60, aiProps.getCooldownSeconds());
            aiDisabledUntil = Instant.now().plusSeconds(cooldown);
            log.warn("AI auth error {} — disabled for {}s. err={}", status, cooldown, message);
            return;
        }
        log.warn("AI request failed: status={}. err={}", status, message);
    }

    // ─── JSON parsing ─────────────────────────────────────────────────────────

    private Optional<ActionPlan> parseActionPlan(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String jsonCandidate = extractFirstJsonObject(raw).orElse(raw);
        try {
            JsonNode node = objectMapper.readTree(jsonCandidate);
            String intent = text(node.get("intent")).orElse("general");
            String keyword = text(node.get("keyword")).orElse(null);
            // Merge AI-extracted color into keyword if keyword is generic
            String color = text(node.get("color")).orElse(null);
            if (color != null && keyword != null) {
                keyword = keyword + " " + color;
            } else if (color != null) {
                keyword = color;
            }
            BigDecimal min = decimal(node.get("minPrice")).orElse(null);
            BigDecimal max = decimal(node.get("maxPrice")).orElse(null);
            int limit = integer(node.get("limit")).orElse(6);
            return Optional.of(new ActionPlan(intent, keyword, min, max, limit));
        } catch (JsonProcessingException e) {
            log.debug("Failed to parse AI JSON plan: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractFirstJsonObject(String raw) {
        int start = raw.indexOf('{');
        if (start < 0) return Optional.empty();
        int depth = 0;
        for (int i = start; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return Optional.of(raw.substring(start, i + 1));
            }
        }
        return Optional.empty();
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    static String normalize(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase();
        String nfd = Normalizer.normalize(lower, Normalizer.Form.NFD);
        String stripped = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Handle đ/Đ which doesn't decompose via NFD
        stripped = stripped.replace('đ', 'd').replace('Đ', 'D');
        return stripped.replaceAll("\\s+", " ").trim();
    }

    private static boolean containsAny(String input, String... needles) {
        if (input == null || input.isBlank() || needles == null) return false;
        for (String n : needles) {
            if (n != null && !n.isBlank() && input.contains(n)) return true;
        }
        return false;
    }

    private void setCategoryBySlug(ProductFilterDTO filter, String slug) {
        if (slug == null || slug.isBlank()) return;
        categoryRepository.findBySlug(slug.trim().toLowerCase())
                .map(Category::getId)
                .ifPresent(filter::setCategoryId);
    }

    private void setSubCategoryBySlug(ProductFilterDTO filter, String slug) {
        if (slug == null || slug.isBlank()) return;
        subCategoryRepository.findBySlug(slug.trim().toLowerCase())
                .map(sc -> sc.getId())
                .ifPresent(filter::setSubCategoryId);
    }

    private static Optional<String> text(JsonNode node) {
        if (node == null || node.isNull()) return Optional.empty();
        String s = node.asText();
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s)) return Optional.empty();
        return Optional.of(s.trim());
    }

    private static Optional<BigDecimal> decimal(JsonNode node) {
        if (node == null || node.isNull()) return Optional.empty();
        try {
            if (node.isNumber()) return Optional.of(BigDecimal.valueOf(node.asLong()));
            String s = node.asText();
            if (s == null || s.isBlank()) return Optional.empty();
            String cleaned = s.replaceAll("[^0-9]", "");
            if (cleaned.isBlank()) return Optional.empty();
            return Optional.of(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> integer(JsonNode node) {
        if (node == null || node.isNull()) return Optional.empty();
        try {
            if (node.isInt() || node.isLong()) return Optional.of(node.asInt());
            String s = node.asText();
            if (s == null || s.isBlank()) return Optional.empty();
            String cleaned = s.replaceAll("[^0-9-]", "");
            return Optional.of(Integer.parseInt(cleaned));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static int clamp(int n, int min, int max) {
        return Math.max(min, Math.min(max, n));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String buildSearchIntro(ActionPlan plan, int count) {
        StringBuilder sb = new StringBuilder("Mình tìm thấy ").append(count).append(" sản phẩm phù hợp");
        if (plan.keyword() != null && !plan.keyword().isBlank()) {
            sb.append(" với \"").append(plan.keyword().trim()).append("\"");
        }
        if (plan.maxPrice() != null) {
            sb.append(", dưới ").append(formatPrice(plan.maxPrice()));
        }
        if (plan.minPrice() != null) {
            sb.append(", từ ").append(formatPrice(plan.minPrice()));
        }
        sb.append(":");
        return sb.toString();
    }

    private String buildRuleIntro(ProductFilterDTO filter, String color, int count) {
        StringBuilder sb = new StringBuilder("Mình tìm thấy ").append(count).append(" sản phẩm");
        if (color != null) sb.append(" màu ").append(color);
        if (filter.getMaxPrice() != null) sb.append(", dưới ").append(formatPrice(filter.getMaxPrice()));
        if (filter.getMinPrice() != null) sb.append(", từ ").append(formatPrice(filter.getMinPrice()));
        sb.append(":");
        return sb.toString();
    }

    private static String formatPrice(BigDecimal price) {
        long v = price.longValue();
        if (v >= 1_000_000) return (v / 1_000_000) + " triệu₫";
        if (v >= 1_000) return (v / 1_000) + "k₫";
        return v + "₫";
    }

    private record ActionPlan(String intent, String keyword, BigDecimal minPrice, BigDecimal maxPrice, int limit) {
        static ActionPlan general() {
            return new ActionPlan("general", null, null, null, 6);
        }
    }
}
