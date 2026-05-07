package com.shop.clothingstore.service;

import java.io.IOException;
import java.math.BigDecimal;
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

    private static final Pattern PRICE_RANGE_PATTERN = Pattern.compile(
            "(?:từ\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|k|nghìn|ngan|đ|vnd)?\\s*(?:đến|tới|-)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|k|nghìn|ngan|đ|vnd)?)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern PRICE_UNDER_PATTERN = Pattern.compile(
            "(?:dưới|<|tối\\s*đa|max|under)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|k|nghìn|ngan|đ|vnd)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern PRICE_OVER_PATTERN = Pattern.compile(
            "(?:trên|>=|tối\\s*thiểu|min|over|hơn)\\s*(\\d+(?:[.,]\\d+)?)\\s*(triệu|tr|m|k|nghìn|ngan|đ|vnd)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
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
        if (userMessage == null || userMessage.isBlank()) {
            return ChatbotResponse.text("Hello! I can help you find products, advise on sizing, or answer questions about orders, shipping, and returns. How can I help?");
        }

        // Rule-based: handle common questions without AI
        ChatbotResponse ruleResult = handleRuleBased(userMessage);
        if (ruleResult != null) return ruleResult;

        if (!isEnabledAndConfigured()) {
            return ChatbotResponse.text("AI chatbot is not enabled/configured. You can ask me about products (type, color, price range) and I'll suggest some options.");
        }

        Instant disabledUntil = aiDisabledUntil;
        if (disabledUntil != null && Instant.now().isBefore(disabledUntil)) {
            return ChatbotResponse.text("AI chatbot is temporarily overloaded. Describe what you need — type (top/bottom/accessory), color, and price range — and I'll suggest products.");
        }

        String planner = planAction(userMessage);
        ActionPlan plan = parseActionPlan(planner).orElseGet(() -> ActionPlan.general(""));

        if ("best_sellers".equals(plan.intent())) {
            List<Product> products = productRepository.findBestSellers(PageRequest.of(0, clamp(plan.limit(), 1, 8)));
            String msg = products.isEmpty()
                    ? "No best-seller data available yet. Want me to suggest by type/color/price?"
                    : "Here are some best-selling products at NOVA:";
            return ChatbotResponse.withProducts(msg, products);
        }

        if ("search_products".equals(plan.intent())) {
            ProductFilterDTO filter = new ProductFilterDTO();
            applyCategoryHints(filter, plan.keyword(), userMessage);
            if (filter.getCategoryId() == null && filter.getSubCategoryId() == null) {
                filter.setKeyword(blankToNull(plan.keyword()));
            }
            filter.setMinPrice(plan.minPrice());
            filter.setMaxPrice(plan.maxPrice());
            applyPriceHints(filter, userMessage);

            int limit = clamp(plan.limit(), 1, 8);
            List<Product> products = productService.findWithFilter(filter, PageRequest.of(0, limit)).getContent();
            if (products.isEmpty()) {
                // Retry with category-only (drop keyword)
                ProductFilterDTO fallback = new ProductFilterDTO();
                applyCategoryHints(fallback, plan.keyword(), userMessage);
                fallback.setMinPrice(filter.getMinPrice());
                fallback.setMaxPrice(filter.getMaxPrice());
                List<Product> fallbackProducts = productService.findWithFilter(fallback, PageRequest.of(0, limit)).getContent();
                if (!fallbackProducts.isEmpty()) {
                    return ChatbotResponse.withProducts("Couldn't find that exact type, but here are some similar products:", fallbackProducts);
                }
                return ChatbotResponse.text("No matching products found 😔 Try describing: type (tee / jeans / shoes / bag…), color, and price range!");
            }

            String intro = buildSearchIntro(plan, products.size());
            return ChatbotResponse.withProducts(intro, products);
        }

        String answer = answerGeneral(userMessage);
        if (answer == null || answer.isBlank()) {
            return ChatbotResponse.text("I can help you find products by type, color, or price. Describe what you're looking for and I'll suggest some options!");
        }
        return ChatbotResponse.text(answer);
    }

    /**
     * Rule-based answers for common store questions — no AI needed.
     * Returns null if no rule matches (let AI handle it).
     */
    private ChatbotResponse handleRuleBased(String raw) {
        String msg = normalize(raw.toLowerCase());

        // ── Greeting ──────────────────────────────────────────────────────────
        if (containsAny(msg, "xin chao", "chào", "hello", "hi", "hey", "alo", "helo")) {
            return ChatbotResponse.text("Hello! I'm NOVA's assistant 👋 Looking for products, size advice, shipping info, or return policy? Just ask!");
        }

        // ── Shipping ──────────────────────────────────────────────────────────
        if (containsAny(msg, "ship", "van chuyen", "vận chuyển", "giao hang", "giao hàng",
                "phi ship", "phí ship", "free ship", "bao lau", "bao lâu", "khi nao", "khi nào",
                "thoi gian giao", "thời gian giao", "nhan hang", "nhận hàng")) {
            return ChatbotResponse.text("""
                    📦 NOVA Shipping Info:
                    • Free shipping on orders from 500,000₫ and above.
                    • Orders under 500,000₫: flat 30,000₫ shipping fee.
                    • Delivery time: 2–4 business days (city), 3–6 days (other provinces).
                    • Currently ships within Vietnam only.
                    """.strip());
        }

        // ── Return / Refund ───────────────────────────────────────────────────
        if (containsAny(msg, "doi tra", "đổi trả", "hoan tien", "hoàn tiền", "tra hang", "trả hàng",
                "chinh sach", "chính sách", "refund", "return", "doi hang", "đổi hàng",
                "khieu nai", "khiếu nại", "bao hanh", "bảo hành")) {
            return ChatbotResponse.text("""
                    🔄 NOVA Return & Refund Policy:
                    • Free returns within 14 days of receiving your order.
                    • Items must be unused, with tags intact and original packaging.
                    • Manufacturing defects are eligible for 100% exchange/refund.
                    • To return, go to "My Orders" on the website and follow the instructions.
                    """.strip());
        }

        // ── Size / Sizing guide ───────────────────────────────────────────────
        if (containsAny(msg, "size", "kich co", "kích cỡ", "chon size", "chọn size",
                "size chart", "bang size", "bảng size", "fit", "rong", "rộng", "chat", "chật",
                "m la", "l la", "xl la", "xxl", "size nao", "size nào")) {
            return ChatbotResponse.text("""
                    📏 NOVA Size Guide:
                    • S   → 48–55 kg, height 155–165 cm
                    • M   → 55–65 kg, height 163–170 cm
                    • L   → 65–75 kg, height 168–175 cm
                    • XL  → 75–85 kg, height 173–180 cm
                    • XXL → 85–95 kg, height 178–185 cm
                    If you're between two sizes, we recommend sizing up for a more comfortable fit!
                    """.strip());
        }

        // ── Payment ───────────────────────────────────────────────────────────
        if (containsAny(msg, "thanh toan", "thanh toán", "payment", "pay", "cod", "tien mat", "tiền mặt",
                "chuyen khoan", "chuyển khoản", "atm", "the tin dung", "thẻ tín dụng",
                "momo", "zalopay", "vnpay", "banking")) {
            return ChatbotResponse.text("""
                    💳 NOVA Payment Methods:
                    • Cash on delivery (COD).
                    • Bank transfer.
                    More online payment options coming soon!
                    """.strip());
        }

        // ── Coupon / Discount ─────────────────────────────────────────────────
        if (containsAny(msg, "ma giam gia", "mã giảm giá", "coupon", "discount", "khuyen mai", "khuyến mãi",
                "giam gia", "giảm giá", "sale", "uu dai", "ưu đãi", "voucher", "promo")) {
            return ChatbotResponse.text("""
                    🎁 NOVA Coupons & Promotions:
                    • Enter your coupon code at Checkout.
                    • Follow NOVA on social media for the latest discount codes.
                    • First orders often include a special offer — create an account to claim it!
                    """.strip());
        }

        // ── Product quality / material ────────────────────────────────────────
        if (containsAny(msg, "chat lieu", "chất liệu", "material", "cotton", "chat luong", "chất lượng",
                "vai", "vải", "hang that", "hàng thật", "hang chinh hang", "hàng chính hãng",
                "hang fake", "nhu the nao", "như thế nào", "co tot", "có tốt")) {
            return ChatbotResponse.text("""
                    ✨ NOVA Product Quality:
                    • Premium garment-dyed cotton fabric, color-fast and durable.
                    • Minimal design, oversized fit — perfect for a modern aesthetic.
                    • 100% authentic products, quality-checked before dispatch.
                    • Full exchange/refund guaranteed for manufacturing defects.
                    """.strip());
        }

        // ── Store / Contact ───────────────────────────────────────────────────
        if (containsAny(msg, "cua hang", "cửa hàng", "dia chi", "địa chỉ", "showroom", "store",
                "offline", "den truc tiep", "đến trực tiếp", "lien he", "liên hệ",
                "contact", "ho tro", "hỗ trợ", "cskh", "hotline")) {
            return ChatbotResponse.text("""
                    📍 About NOVA:
                    • NOVA currently operates online through this website.
                    • For direct support, reach us via NOVA's social media channels.
                    • I (chatbot) can help you find products, advise on sizing, and answer questions right here!
                    """.strip());
        }

        // ── Order status ──────────────────────────────────────────────────────
        if (containsAny(msg, "don hang", "đơn hàng", "order", "trang thai", "trạng thái",
                "theo doi", "theo dõi", "tracking", "da dat", "đã đặt", "khi nao den", "khi nào đến")) {
            return ChatbotResponse.text("To check your order status, go to \"My Orders\" after logging in. If something looks wrong, describe the issue and I'll help you.");
        }

        // ── Best sellers / trending ───────────────────────────────────────────
        if (containsAny(msg, "ban chay", "bán chạy", "hot", "trending", "pho bien", "phổ biến",
                "duoc ua chuong", "được ưa chuộng", "nhieu nguoi mua", "nhiều người mua",
                "top san pham", "top sản phẩm", "bestseller", "best seller")) {
            List<Product> products = productRepository.findBestSellers(PageRequest.of(0, 6));
            String txt = products.isEmpty()
                    ? "Hiện chưa có dữ liệu bán chạy. Bạn muốn mình gợi ý theo loại/màu không?"
                    : "Đây là những sản phẩm đang bán chạy nhất tại NOVA:";
            return ChatbotResponse.withProducts(txt, products);
        }

        // ── Show all / browse ─────────────────────────────────────────────────
        if (containsAny(msg, "xem tat ca", "xem tất cả", "show all", "tat ca san pham", "tất cả sản phẩm",
                "co gi", "có gì", "co nhung gi", "có những gì", "hang moi", "hàng mới", "new arrival")) {
            List<Product> products = productRepository.findBestSellers(PageRequest.of(0, 8));
            return ChatbotResponse.withProducts("Một số sản phẩm nổi bật tại NOVA — bạn vào trang Shop để xem toàn bộ nhé:", products);
        }

        return null; // No rule matched → let AI handle
    }

    private void applyPriceHints(ProductFilterDTO filter, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return;
        }
        String msg = userMessage.toLowerCase();

        // If AI already provided constraints, keep them.
        BigDecimal min = filter.getMinPrice();
        BigDecimal max = filter.getMaxPrice();

        Matcher range = PRICE_RANGE_PATTERN.matcher(msg);
        if (range.find()) {
            BigDecimal minParsed = parseMoney(range.group(1), range.group(2));
            BigDecimal maxParsed = parseMoney(range.group(3), range.group(4));
            if (min == null) {
                min = minParsed;
            }
            if (max == null) {
                max = maxParsed;
            }
        } else {
            Matcher under = PRICE_UNDER_PATTERN.matcher(msg);
            if (under.find() && max == null) {
                max = parseMoney(under.group(1), under.group(2));
            }
            Matcher over = PRICE_OVER_PATTERN.matcher(msg);
            if (over.find() && min == null) {
                min = parseMoney(over.group(1), over.group(2));
            }
        }

        if (min != null) {
            filter.setMinPrice(min);
        }
        if (max != null) {
            filter.setMaxPrice(max);
        }
    }

    private static BigDecimal parseMoney(String numberPart, String unitPart) {
        if (numberPart == null || numberPart.isBlank()) {
            return null;
        }
        String n = numberPart.trim().replace(",", ".");
        BigDecimal value;
        try {
            value = new BigDecimal(n);
        } catch (NumberFormatException ex) {
            String digits = numberPart.replaceAll("[^0-9]", "");
            if (digits.isBlank()) {
                return null;
            }
            value = new BigDecimal(digits);
        }

        String unit = unitPart == null ? "" : unitPart.trim().toLowerCase();
        if (unit.contains("triệu") || unit.equals("tr") || unit.equals("m")) {
            return value.multiply(BigDecimal.valueOf(1_000_000));
        }
        if (unit.contains("nghìn") || unit.equals("k") || unit.equals("ngan")) {
            return value.multiply(BigDecimal.valueOf(1_000));
        }
        // If unit is empty and the number is small (e.g. "500" in "dưới 500k" without k),
        // do not guess. Return as-is (VND) and let UI/user refine.
        return value;
    }

    private void applyCategoryHints(ProductFilterDTO filter, String keyword, String userMessage) {
        String k = (keyword == null ? "" : keyword).trim().toLowerCase();
        String msg = (userMessage == null ? "" : userMessage).trim().toLowerCase();
        String combined = normalize(k + " " + msg);

        // ── SUBCATEGORY: shoes ────────────────────────────────────────────────
        if (containsAny(combined,
                "shoes", "shoe", "sneaker", "sneakers", "kicks", "boot", "boots",
                "sandal", "sandals", "loafer", "loafers", "oxford",
                "giay", "giày", "dep", "dép",
                "giày thể thao", "giày sneaker", "giày boot", "giày da",
                "giay the thao", "giay sneaker", "giay da",
                "giầy", "giày dép", "giay dep")) {
            setSubCategoryBySlug(filter, "shoes");
            return;
        }

        // ── SUBCATEGORY: bag ──────────────────────────────────────────────────
        if (containsAny(combined,
                "bag", "bags", "backpack", "tote", "clutch", "handbag", "purse", "satchel",
                "tui", "túi", "balo", "ba lo", "ba lô",
                "tui xach", "túi xách", "tui tote", "túi tote",
                "vi", "ví", "wallet")) {
            setSubCategoryBySlug(filter, "bag");
            return;
        }

        // ── SUBCATEGORY: cap ──────────────────────────────────────────────────
        if (containsAny(combined,
                "cap", "caps", "hat", "hats", "beanie", "bucket hat", "snapback", "baseball cap",
                "mu", "mũ", "non", "nón",
                "mu luoi trai", "mũ lưỡi trai",
                "mu bucket", "nón bucket", "non bucket")) {
            setSubCategoryBySlug(filter, "cap");
            return;
        }

        // ── SUBCATEGORY: jeans ────────────────────────────────────────────────
        if (containsAny(combined,
                "jeans", "jean", "denim",
                "quan bo", "quần bò", "quan bo", "bo",
                "quan jeans", "quần jeans", "quan jean", "quần jean")) {
            setSubCategoryBySlug(filter, "jeans");
            return;
        }

        // ── SUBCATEGORY: shorts ───────────────────────────────────────────────
        if (containsAny(combined,
                "shorts", "short",
                "quan short", "quần short", "quan ngan", "quần ngắn",
                "quan the thao", "quần thể thao")) {
            setSubCategoryBySlug(filter, "shorts");
            return;
        }

        // ── SUBCATEGORY: pants ────────────────────────────────────────────────
        if (containsAny(combined,
                "pants", "trousers", "chinos", "slacks", "jogger", "joggers", "sweatpants",
                "quan dai", "quần dài", "quan kaki", "quần kaki",
                "quan tay", "quan jogger", "quần jogger")) {
            setSubCategoryBySlug(filter, "pants");
            return;
        }

        // ── SUBCATEGORY: hoodie ───────────────────────────────────────────────
        if (containsAny(combined,
                "hoodie", "hoodies", "sweater", "sweatshirt", "pullover", "crewneck",
                "ao ni", "áo nỉ", "ao khoac", "áo khoác",
                "ao hoodie", "áo hoodie", "ao sweater",
                "ni bong", "nỉ bông", "ao chui dau", "áo chui đầu")) {
            setSubCategoryBySlug(filter, "hoodie");
            return;
        }

        // ── SUBCATEGORY: tee ──────────────────────────────────────────────────
        if (containsAny(combined,
                "tee", "t-shirt", "tshirt", "graphic tee", "oversized tee",
                "ao thun", "áo thun", "thun", "phong", "phông",
                "ao phong", "áo phông", "ao cotton", "áo cotton",
                "ao tee", "ao tshirt", "polo")) {
            setSubCategoryBySlug(filter, "tee");
            return;
        }

        // ── SUBCATEGORY: shirt ────────────────────────────────────────────────
        if (containsAny(combined,
                "shirt", "shirts", "flannel", "oxford shirt", "button", "overshirt",
                "so mi", "sơ mi", "ao so mi", "áo sơ mi",
                "ao caro", "áo caro", "ao kẻ", "ao ke")) {
            setSubCategoryBySlug(filter, "shirt");
            return;
        }

        // ── CATEGORY FALLBACK ─────────────────────────────────────────────────
        if (containsAny(combined,
                "bottom", "quan", "quần",
                "pants", "shorts", "jeans")) {
            setCategoryBySlug(filter, "bottom");
        } else if (containsAny(combined,
                "top", "ao", "áo",
                "tee", "hoodie", "shirt")) {
            setCategoryBySlug(filter, "top");
        } else if (containsAny(combined,
                "accessories", "acc", "phu kien", "phụ kiện",
                "bag", "shoes", "cap", "giay", "tui", "mu")) {
            setCategoryBySlug(filter, "accessories");
        }
    }

    private static String normalize(String input) {
        if (input == null) return "";
        // Lowercase already done by caller; just collapse spaces
        return input.replaceAll("\\s+", " ").trim();
    }

    private void setCategoryBySlug(ProductFilterDTO filter, String slug) {
        if (slug == null || slug.isBlank()) {
            return;
        }
        categoryRepository.findBySlug(slug.trim().toLowerCase())
                .map(Category::getId)
                .ifPresent(filter::setCategoryId);
    }

    private void setSubCategoryBySlug(ProductFilterDTO filter, String slug) {
        if (slug == null || slug.isBlank()) {
            return;
        }
        subCategoryRepository.findBySlug(slug.trim().toLowerCase())
                .map(sc -> sc.getId())
                .ifPresent(filter::setSubCategoryId);
    }

    private static boolean containsAny(String input, String... needles) {
        if (input == null || input.isBlank() || needles == null) {
            return false;
        }
        for (String n : needles) {
            if (n != null && !n.isBlank() && input.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private String planAction(String userMessage) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", """
Bạn là trợ lý bán hàng cho website thời trang NOVA. Nhiệm vụ: phân tích câu hỏi và trả về JSON hành động.

Danh mục sản phẩm NOVA:
- Top (áo): tee (áo thun, phông, t-shirt), hoodie (áo nỉ, sweater), shirt (sơ mi, áo caro)
- Bottom (quần): pants (quần dài, kaki, jogger), shorts (quần short, ngắn), jeans (quần bò, denim)
- Accessories (phụ kiện): shoes (giày, dép, sneaker, boot), bag (túi, balo, ba lô), cap (mũ, nón)

Bạn CHỈ trả về MỘT JSON (không markdown, không giải thích):
{
  "intent": "search_products" | "best_sellers" | "general",
  "keyword": string | null,
  "minPrice": number | null,
  "maxPrice": number | null,
  "limit": number
}

Quy tắc:
- "bán chạy", "hot", "trending", "phổ biến", "bestseller" -> best_sellers
- Muốn tìm/mua/xem/gợi ý sản phẩm cụ thể -> search_products, keyword là tên loại sản phẩm bằng tiếng Anh (shoes/bag/cap/tee/hoodie/shirt/pants/shorts/jeans)
- Giá: trích xuất VND, "500k"=500000, "1 triệu"=1000000, "1tr5"=1500000
- Câu hỏi chung (ship, đổi trả, size, thanh toán...) -> general
- limit mặc định 6, tối đa 8
""".trim()
            ));
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

    private String answerGeneral(String userMessage) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", """
Bạn là trợ lý CSKH thân thiện của website thời trang NOVA (nova-fashion.vn).

Thông tin cửa hàng:
- Sản phẩm: áo thun (tee), hoodie, sơ mi (shirt), quần dài (pants), quần short, jeans, giày (shoes), túi/balo (bag), mũ (cap)
- Miễn phí ship đơn từ 500.000đ, giao 2-6 ngày làm việc trong nước
- Đổi trả miễn phí trong 14 ngày, còn nguyên tem
- Thanh toán: COD hoặc chuyển khoản
- Size: S/M/L/XL/XXL
- Chất liệu: cotton cao cấp, garment-dyed, form oversized

Quy tắc trả lời:
- Ngắn gọn, thân thiện, dùng tiếng Việt
- Nếu không có thông tin chính xác (tồn kho, trạng thái đơn...) hãy hướng dẫn khách vào mục tương ứng trên web
- Không bịa đặt thông tin
""".trim()
            ));
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

    private void handleAiHttpError(int status, String message) {
        if (status == 429) {
            int cooldown = Math.max(30, aiProps.getCooldownSeconds());
            aiDisabledUntil = Instant.now().plusSeconds(cooldown);
            log.warn("AI provider returned 429; disable AI temporarily for {}s. err={}", cooldown, message);
            return;
        }
        if (status == 401 || status == 403) {
            int cooldown = Math.max(60, aiProps.getCooldownSeconds());
            aiDisabledUntil = Instant.now().plusSeconds(cooldown);
            log.warn("AI auth/permission error {}; disable AI temporarily for {}s. err={}", status, cooldown, message);
            return;
        }
        log.warn("AI request failed: status={}. err={}", status, message);
    }

    private Optional<ActionPlan> parseActionPlan(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String jsonCandidate = extractFirstJsonObject(raw).orElse(raw);
        try {
            JsonNode node = objectMapper.readTree(jsonCandidate);
            String intent = text(node.get("intent")).orElse("general");
            String keyword = text(node.get("keyword")).orElse(null);
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
        if (start < 0) {
            return Optional.empty();
        }
        int depth = 0;
        for (int i = start; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return Optional.of(raw.substring(start, i + 1));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> text(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        String s = node.asText();
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s)) {
            return Optional.empty();
        }
        return Optional.of(s.trim());
    }

    private static Optional<BigDecimal> decimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        try {
            if (node.isNumber()) {
                return Optional.of(BigDecimal.valueOf(node.asLong()));
            }
            String s = node.asText();
            if (s == null || s.isBlank()) {
                return Optional.empty();
            }
            String cleaned = s.replaceAll("[^0-9]", "");
            if (cleaned.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> integer(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        try {
            if (node.isInt() || node.isLong()) {
                return Optional.of(node.asInt());
            }
            String s = node.asText();
            if (s == null || s.isBlank()) {
                return Optional.empty();
            }
            String cleaned = s.replaceAll("[^0-9-]", "");
            int parsed = Integer.parseInt(cleaned);
            return Optional.of(parsed);
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
            sb.append(" dưới ").append(plan.maxPrice().toPlainString()).append("₫");
        }
        if (plan.minPrice() != null) {
            sb.append(" từ ").append(plan.minPrice().toPlainString()).append("₫");
        }
        sb.append(":");
        return sb.toString();
    }

    private record ActionPlan(String intent, String keyword, BigDecimal minPrice, BigDecimal maxPrice, int limit) {
        static ActionPlan general(String messageHint) {
            return new ActionPlan("general", null, null, null, 6);
        }
    }
}

