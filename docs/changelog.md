# Changelog — Lịch sử thay đổi & Kế hoạch phát triển

> **Quy tắc ghi file này:**
> - Mỗi khi sửa hoặc thêm tính năng, **luôn ghi vào đây** theo format BEFORE/AFTER.
> - Phần **PLANNED** là những gì sẽ làm tiếp theo.
> - Phần **DONE** là những gì đã thay đổi kể từ khi bắt đầu dùng file này.
>
> 📋 **Xem bảng chi tiết vị trí file + giải thích từng thay đổi:** [work-log.md](work-log.md)

---

## FORMAT MẪU

```
### [YYYY-MM-DD] Tiêu đề thay đổi

**BEFORE:**
Mô tả trạng thái cũ — code cũ làm gì, vấn đề gì.

**AFTER:**
Mô tả trạng thái mới — code mới làm gì, cải thiện thế nào.

**Files thay đổi:**
- `path/to/file.java`

**Lý do:**
Tại sao cần thay đổi.
```

---

## PLANNED — Việc sẽ làm tiếp

### [TODO-01] Tích hợp thanh toán online (VNPay / MoMo / SePay)

**BEFORE:**
Hiện tại chỉ hỗ trợ COD (thanh toán khi nhận hàng). Không có cổng thanh toán trực tuyến.

**AFTER (kế hoạch):**
- Tích hợp SePay.vn (VietQR) hoặc VNPay
- Thêm trạng thái thanh toán: PENDING_PAYMENT → PAID → ...
- Webhook xử lý kết quả thanh toán
- Admin xem lịch sử giao dịch

**Files sẽ thay đổi:**
- `CheckoutController.java`
- `CheckoutService.java`
- `Payment.java`
- `PaymentService.java`
- `templates/shop/checkout.html`

---

### [TODO-02] Cải thiện UI/UX trang chủ

**BEFORE:**
Trang chủ chỉ hiển thị best sellers đơn giản (3 sản phẩm). Không có banner, slider hay section nổi bật.

**AFTER (kế hoạch):**
- Hero banner với slider ảnh
- Section "Mới nhất" (latest arrivals)
- Section "Bán chạy" (best sellers) cho từng category
- Section khuyến mãi / flash sale
- Cải thiện responsive mobile

**Files sẽ thay đổi:**
- `templates/shop/home.html`
- `static/css/tailwind.css`
- `ShopController.java` (thêm data mới vào model)

---

### [TODO-03] Hệ thống đánh giá nâng cao

**BEFORE:**
Review chỉ có rating + text. Không có ảnh review, không có filter, không có helpful vote.

**AFTER (kế hoạch):**
- Upload ảnh vào review
- Filter review theo số sao
- Nút "Hữu ích" (helpful vote)
- Hiển thị biểu đồ phân bố rating (1–5 sao)

**Files sẽ thay đổi:**
- `Review.java`
- `ReviewService.java`
- `ReviewController.java`
- `templates/shop/product-detail.html`

---

### [TODO-04] Real-time notification (WebSocket / SSE)

**BEFORE:**
Notification đang là pull (client gọi API định kỳ). Không có real-time push.

**AFTER (kế hoạch):**
- Server-Sent Events (SSE) cho notification real-time
- Admin nhận thông báo ngay khi có đơn mới
- User nhận thông báo cập nhật trạng thái đơn hàng

**Files sẽ thay đổi:**
- `NotificationService.java`
- `NotificationApiController.java`
- `templates/layout/base.html`

---

### [TODO-05] Trang quản trị: thống kê nâng cao

**BEFORE:**
Dashboard có biểu đồ doanh thu theo ngày/tuần/tháng/năm. Chưa có: thống kê theo sản phẩm chi tiết, theo khu vực, conversion rate.

**AFTER (kế hoạch):**
- Biểu đồ top 10 sản phẩm bán chạy (bar chart)
- Biểu đồ phân bố đơn theo tỉnh/thành
- Tỷ lệ chuyển đổi giỏ hàng → đơn hàng
- Báo cáo xuất theo khoảng thời gian tùy chọn

**Files sẽ thay đổi:**
- `DashboardService.java`
- `DashboardDTO.java`
- `templates/admin/dashboard.html`
- `ReportService.java`

---

### [TODO-06] Search nâng cao (Elasticsearch / Full-text)

**BEFORE:**
Tìm kiếm dùng JPA LIKE query (`%keyword%`). Không có full-text search, không có gợi ý từ khóa.

**AFTER (kế hoạch):**
- Full-text search với MySQL FULLTEXT index
- Gợi ý từ khóa (autocomplete)
- Tìm kiếm theo tag / thuộc tính
- Kết quả được rank theo relevance

**Files sẽ thay đổi:**
- `ProductSpecification.java`
- `ProductRepository.java`
- `ProductService.java`
- `templates/shop/products.html`

---

### [TODO-07] Mobile optimization & PWA

**BEFORE:**
Site responsive cơ bản. Không có PWA, không có offline mode.

**AFTER (kế hoạch):**
- Service Worker cho offline mode
- Tối ưu hình ảnh (WebP, lazy loading)
- Touch gestures cho slider ảnh sản phẩm
- Bottom navigation bar trên mobile

---

### [TODO-08] Hệ thống mã giới thiệu (Referral)

**BEFORE:**
Không có hệ thống referral. User không có lý do để giới thiệu bạn bè.

**AFTER (kế hoạch):**
- Mỗi user có referral code riêng
- Khi người được giới thiệu đặt đơn đầu tiên: cả 2 nhận coupon
- Thống kê referral trong profile

---

## DONE — Đã thay đổi

### [2026-05-29] Nâng cấp AI Chatbot (Tier 1) + cài đặt Ollama

**BEFORE:**
- Chatbot chạy fallback rule-based vì chưa cài Ollama (AI path không hoạt động).
- Mỗi tin nhắn xử lý độc lập — không nhớ ngữ cảnh, hỏi nối tiếp ("còn màu khác không?") không hiểu.
- Response hiện toàn bộ một lúc (`stream=false`), cảm giác chậm.
- Không có gợi ý câu hỏi cho người dùng.

**AFTER:**
- Đã cài **Ollama + model llama3.2:3b** (chạy cổng 11434) → AI path hoạt động đầy đủ, trả lời tiếng Việt.
- **Ngữ cảnh hội thoại đa lượt**: lưu 6 lượt gần nhất trong `HttpSession` (spring-session-jdbc), truyền vào LLM ở cả `planAction` và `answerGeneral`. Hỏi nối tiếp đã hiểu ngữ cảnh.
- **Typewriter effect**: chữ bot hiện dần từng ký tự (cảm giác như ChatGPT), kèm caret nhấp nháy.
- **Quick-reply chips**: 5 nút gợi ý sẵn (Bán chạy / Tư vấn size / Đổi trả / Vận chuyển / Hoodie dưới 500k), tự ẩn sau tin đầu.
- Đã test: AI search ✅, follow-up giữ ngữ cảnh ✅, FAQ rule-based ✅, tư vấn size ✅.

**Files thay đổi:**
- `service/AiChatbotService.java` — overload `processMessage(msg, history)`, `appendHistory()`, truyền history vào `planAction`/`answerGeneral`
- `controller/api/ChatbotApiController.java` — quản lý lịch sử hội thoại trong session (cap 12 entries)
- `templates/layout/base.html` — typewriter effect, quick-reply chips, CSS `.chat-chip`/`.chat-caret`

**Lý do:**
Nâng trải nghiệm chatbot lên mức "demo ấn tượng" cho hội đồng mà vẫn giữ KISS — typewriter là client-side (không thêm rủi ro backend SSE), memory dùng session sẵn có.

---

### [2026-05-29] Khởi tạo file changelog

**BEFORE:**
Chưa có hệ thống theo dõi thay đổi.

**AFTER:**
Tạo `docs/changelog.md` để track before/after mọi thay đổi.

**Files thay đổi:**
- `docs/changelog.md` (file này)

---

### [2026-05-29] Thêm .claudee vào .gitignore

**BEFORE:**
`.claudee/` (thư mục chứa Claude Code templates, agents, hooks) chưa có trong `.gitignore`, có thể bị commit lên git.

**AFTER:**
Đã thêm vào `.gitignore`:
```
### Claude Kit (local AI assistant config) ###
.claudee/
```

**Files thay đổi:**
- `.gitignore`

---

### [2026-05-29] Cài đặt Claude Code slash commands từ .claudee

**BEFORE:**
Các commands `/brainstorm`, `/plan`, `/debug`, `/fix`, `/code-review`... chưa khả dụng trong project.

**AFTER:**
Copy toàn bộ từ `.claudee/commands/` → `.claude/commands/` (54 commands) và `.claudee/agents/` → `.claude/agents/` (16 subagents).

Slash commands khả dụng ngay:
- `/brainstorm` — Brainstorm tính năng / kiến trúc
- `/plan` — Lập kế hoạch implement
- `/cook` — Implement từng bước
- `/fix` — Fix bug tự động
- `/debug` — Debug có hệ thống
- `/code-review` — Review code
- `/test` — Chạy test
- `/backend-development` — Hỗ trợ Java/Spring
- `/databases` — Hỗ trợ SQL/JPA
- `/research` — Tìm tài liệu
- `/scout` — Khám phá codebase

**Files thay đổi:**
- `.claude/commands/` (54 files mới)
- `.claude/agents/` (16 files mới)
