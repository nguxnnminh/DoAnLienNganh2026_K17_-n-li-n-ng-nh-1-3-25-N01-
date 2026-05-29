# Nhật ký công việc — Work Log

> File này ghi lại **chi tiết từng thay đổi** do AI assistant (Claude) thực hiện trên dự án.
> Mục đích: bất kỳ ai đọc vào cũng hiểu **đã sửa file nào, ở vị trí nào, sửa gì và tại sao**.
>
> **Quy ước:** Mỗi lần sửa/thêm code, một dòng mới được thêm vào bảng tương ứng. Cập nhật liên tục.

---

## 📌 Tổng quan nhanh

| Hạng mục | Số lượng |
|----------|----------|
| Tổng số commit của AI | 3 (`7beb15b`, `cf94230`, `6183271`) |
| File code Java đã sửa | 6 |
| File template (HTML) đã sửa | 1 |
| File cấu hình đã sửa | 2 (`.gitignore`, `application.properties`) |
| File tài liệu đã tạo | 4 (`README.md`, `docs/features.md`, `docs/changelog.md`, `docs/work-log.md`) |
| Phần mềm đã cài cho môi trường chạy | JDK 17, MariaDB 11.4, Ollama + llama3.2:3b |

---

## 🗂️ BẢNG 1 — Các file CODE đã sửa (chi tiết vị trí + giải thích)

| # | File (vị trí) | Vị trí cụ thể trong file | Đã sửa gì | Tại sao / Giải thích cho người đọc |
|---|---------------|--------------------------|-----------|-----------------------------------|
| 1 | `src/main/resources/application.properties` | Dòng 23 — mục `EMAIL CONFIG` | Đổi `spring.mail.password=${MAIL_PASSWORD:abxk aruk haxy ftrn}` → `${MAIL_PASSWORD:}` | **Bảo mật:** mật khẩu Gmail thật đang bị lộ trong source code (commit lên git ai cũng thấy). Xóa đi, để rỗng — app vẫn chạy bình thường, mật khẩu thật chỉ nạp qua biến môi trường `MAIL_PASSWORD`. |
| 2 | `src/main/resources/application.properties` | Dòng 45 — mục `JWT CONFIG` | Đổi giá trị default của `jwt.secret` thành chuỗi ghi rõ "chỉ dùng local dev" | **Bảo mật:** secret cũ là chuỗi bí mật trông như thật bị commit lên git → token JWT có thể bị giả mạo. Đổi thành chuỗi rõ ràng chỉ dùng cho máy local. |
| 3 | `src/main/java/com/shop/clothingstore/repository/CouponRepository.java` | Sau dòng `findByActiveTrueAndUserSpecificFalse()` | Thêm method `boolean existsByCodeIgnoreCase(String code)` | **Hiệu năng:** để thay thế cách kiểm tra mã coupon cũ (xem mục #4). Spring Data JPA tự sinh câu query `EXISTS` rất nhanh. |
| 4 | `src/main/java/com/shop/clothingstore/service/CouponService.java` | Method `existsByCode()` | Bỏ `couponRepository.findAll().stream()...` thay bằng `couponRepository.existsByCodeIgnoreCase(code.trim())` | **Hiệu năng:** code cũ tải TOÀN BỘ coupon từ DB vào RAM rồi lọc — chậm và tốn bộ nhớ khi có nhiều coupon. Code mới chỉ chạy 1 câu query đếm. |
| 5 | `src/main/java/com/shop/clothingstore/service/CouponService.java` | Method `save()` và `delete()` | Xóa annotation `@SuppressWarnings("null")` thừa | **Dọn code:** annotation này che cảnh báo không cần thiết, gây khó đọc. |
| 6 | `src/main/java/com/shop/clothingstore/controller/ProfileController.java` | Đầu class | Thêm hằng số `MIN_PASSWORD_LENGTH = 8` và `PHONE_PATTERN` (regex SĐT Việt Nam) | **Chuẩn hóa:** để dùng lại cho việc kiểm tra dữ liệu (xem #7, #8). |
| 7 | `src/main/java/com/shop/clothingstore/controller/ProfileController.java` | Method `updateProfile()` | Thêm kiểm tra: họ tên không rỗng/≤100 ký tự, SĐT đúng định dạng VN, địa chỉ ≤500 ký tự; `orElseThrow` có thông báo rõ | **Bảo mật + đúng đắn:** trước đây người dùng có thể lưu họ tên 10.000 ký tự hay SĐT sai định dạng. Giờ kiểm tra trước khi lưu vào DB. |
| 8 | `src/main/java/com/shop/clothingstore/controller/ProfileController.java` | Method `changePassword()` | Đổi yêu cầu độ dài mật khẩu tối thiểu từ 6 → 8 ký tự | **Bảo mật + nhất quán:** lúc đăng ký yêu cầu ≥8 ký tự, nhưng đổi mật khẩu chỉ cần ≥6 → người dùng có thể hạ cấp độ mạnh mật khẩu. Giờ đồng bộ 8 ký tự ở cả 2 nơi. |
| 9 | `src/main/java/com/shop/clothingstore/controller/ReviewController.java` | Dòng gọi `findByEmail(...).orElseThrow()` | Thêm thông báo lỗi rõ ràng vào `orElseThrow` | **Đúng đắn:** trước đây nếu lỗi sẽ ném exception trống → trả về lỗi 500 khó hiểu. Giờ có thông báo rõ. |
| 10 | `src/main/java/com/shop/clothingstore/controller/api/AuthApiController.java` | Method `register()` | Đổi thông báo lỗi "Email already in use: <email>" → "Registration failed..." (chung chung) + ghi log nội bộ | **Bảo mật:** thông báo cũ tiết lộ email nào đã tồn tại trong hệ thống → kẻ xấu dò được danh sách email. Giờ trả thông báo chung. |
| 11 | `src/main/java/com/shop/clothingstore/service/AiChatbotService.java` | Đầu method `processMessage()` | Thêm giới hạn cắt tin nhắn người dùng tối đa 2000 ký tự | **Bảo mật:** chặn người dùng gửi tin nhắn cực dài (vài trăm KB) gây tốn tài nguyên / tràn bộ nhớ khi gọi AI. |
| 12 | `src/main/java/com/shop/clothingstore/service/AiChatbotService.java` | `record ActionPlan` + chỗ gọi `ActionPlan.general()` | Xóa tham số thừa `messageHint` không dùng | **Dọn code:** tham số khai báo nhưng không bao giờ dùng. |

---

## 🤖 BẢNG 2 — Nâng cấp AI Chatbot (Tier 1)

| # | File (vị trí) | Vị trí cụ thể trong file | Đã thêm/sửa gì | Giải thích chi tiết |
|---|---------------|--------------------------|----------------|---------------------|
| 13 | `src/main/java/com/shop/clothingstore/service/AiChatbotService.java` | Method `processMessage` | Thêm overload `processMessage(String userMessage, List<Map<String,Object>> history)`; bản cũ `processMessage(String)` gọi lại bản mới với history rỗng | **Ngữ cảnh hội thoại:** cho phép truyền lịch sử các tin nhắn trước vào để bot "nhớ" cuộc trò chuyện. |
| 14 | `src/main/java/com/shop/clothingstore/service/AiChatbotService.java` | Method mới `appendHistory(...)` | Thêm method ghép tối đa 6 lượt hội thoại gần nhất (role user/assistant) vào danh sách gửi cho LLM, có kiểm tra dữ liệu rác | **Ngữ cảnh hội thoại:** lấy đúng lịch sử hợp lệ, giới hạn 6 lượt để không làm prompt quá dài. |
| 15 | `src/main/java/com/shop/clothingstore/service/AiChatbotService.java` | Method `planAction(...)` và `answerGeneral(...)` | Thêm tham số `history`, gọi `appendHistory()` trước tin nhắn hiện tại; cập nhật system prompt nhắc LLM tham chiếu ngữ cảnh | **Ngữ cảnh hội thoại:** giờ bot hiểu câu hỏi nối tiếp như "còn màu khác không?", "rẻ hơn được không?". |
| 16 | `src/main/java/com/shop/clothingstore/controller/api/ChatbotApiController.java` | Toàn bộ controller | Thêm quản lý lịch sử hội thoại trong `HttpSession` (key `chatbot_history`, tối đa 12 entries); đọc history trước khi xử lý, ghi lại sau khi trả lời; parse phòng thủ dữ liệu session | **Ngữ cảnh hội thoại:** lưu cuộc trò chuyện theo từng phiên người dùng, dùng session sẵn có (spring-session-jdbc) nên không cần thêm bảng DB. |
| 16b | `src/main/java/com/shop/clothingstore/controller/api/ChatbotApiController.java` | Method `chat()` — đầu method | **Bỏ "cổng chặn"** `if (!isEnabledAndConfigured()) return "not enabled"`; luôn gọi `processMessage()` | **Đảm bảo chat chạy mọi máy:** trước đây nếu AI tắt (hoặc máy clone về không cài Ollama + ai đó set `CHATBOT_AI_ENABLED=false`), controller chặn ngay → chat chỉ trả "not enabled", cả FAQ lẫn tìm sản phẩm đều không chạy. Giờ luôn vào `processMessage()` → rule-based FAQ + tìm sản phẩm **luôn hoạt động**, chỉ câu hỏi tự do mới cần AI và tự fallback lịch sự. Đã test với AI tắt: FAQ ✅, tìm SP ra 2 kết quả ✅, bán chạy 6 SP ✅. |
| 17 | `src/main/resources/templates/layout/base.html` | Khối CSS `/* ===== CHATBOT ===== */` | Thêm CSS class `.chat-chip` (nút gợi ý) và `.chat-caret` (con trỏ nhấp nháy) | **Giao diện:** tạo kiểu cho nút gợi ý câu hỏi và hiệu ứng con trỏ gõ chữ. |
| 18 | `src/main/resources/templates/layout/base.html` | Trong `#chat-messages`, sau bong bóng chào | Thêm 5 nút quick-reply: Bán chạy / Tư vấn size / Đổi trả / Vận chuyển / Hoodie dưới 500k | **Trải nghiệm:** gợi ý sẵn câu hỏi để người dùng không bí, bấm 1 nút là gửi luôn. |
| 19 | `src/main/resources/templates/layout/base.html` | Khối `<script>` | Thêm hàm `quickAsk(text)` (bấm chip để gửi) và `typeWriter(el, text, done)` (hiệu ứng gõ chữ); sửa `sendChat()` để dùng typewriter và ẩn chip sau tin đầu | **Trải nghiệm:** chữ bot hiện dần từng ký tự như ChatGPT, cảm giác mượt và "thông minh" hơn. Đây là hiệu ứng phía client (không cần sửa backend) nên an toàn. |

---

## 📄 BẢNG 3 — File tài liệu đã tạo

| # | File | Nội dung | Mục đích |
|---|------|----------|----------|
| 20 | `README.md` | Giới thiệu, tech stack, hướng dẫn cài đặt/chạy, biến môi trường, kiến trúc, roadmap | Tài liệu chính của dự án — người mới đọc vào hiểu ngay cách chạy |
| 21 | `docs/features.md` | Liệt kê đầy đủ mọi tính năng đã làm (khách hàng, admin, API, kỹ thuật, entities) | Tổng kết toàn bộ chức năng dự án |
| 22 | `docs/changelog.md` | Lịch sử thay đổi theo format BEFORE/AFTER + danh sách 8 việc dự định làm tiếp (TODO) | Theo dõi mọi thay đổi và kế hoạch tương lai |
| 23 | `docs/work-log.md` | Chính file này — bảng chi tiết mọi thay đổi với vị trí file + giải thích | Để bất kỳ ai cũng check được AI đã làm gì, ở đâu, tại sao |

---

## ⚙️ BẢNG 4 — Cài đặt môi trường chạy (không thuộc code dự án)

| # | Việc đã làm | Vị trí cài | Mục đích |
|---|-------------|-----------|----------|
| 24 | Cài JDK 17 (Temurin) portable | `C:\devtools\jdk\jdk-17.0.13+11` | Để biên dịch & chạy Spring Boot (máy chưa có Java) |
| 25 | Cài MariaDB 11.4 portable (thay MySQL) | `C:\devtools\mariadb\mariadb-11.4.4-winx64`, data tại `C:\devtools\mariadb-data` | Database cho app (tương thích 100% với mysql-connector-j). Đã tạo DB `clothingstore` |
| 26 | Cài Ollama + model llama3.2:3b | Ollama tại `%LOCALAPPDATA%\Programs\Ollama`, chạy cổng 11434 | Để AI Chatbot hoạt động (LLM chạy local, miễn phí) |
| 27 | Sửa cấu hình git: xóa credential `Viet1117`, đăng nhập lại `halam03` | Windows Credential Manager | Để push code lên đúng tài khoản có quyền |

---

## 🚀 BẢNG 5 — 5 tính năng lớn mới (2026-05-29)

> Mỗi tính năng: code → compile → test → E2E → review → fix. Xem kế hoạch: [feature-plan.md](feature-plan.md)

### F1 — UI trang chủ: hero banner + slider
| File | Đã làm | Giải thích |
|------|--------|-----------|
| `templates/shop/home.html` | Thay hero tĩnh bằng slider 3 slide tự xoay (5.5s), nút prev/next, dots, hiệu ứng ken-burns; CSS `.hero-slider/.hero-slide/.hero-dot`; JS điều khiển (pause khi hover/ẩn tab, tôn trọng prefers-reduced-motion) | Trang chủ ấn tượng hơn. Vanilla JS, không thư viện → clone về chạy ngay. **E2E:** homepage HTTP 200, có 3 slide ✅ |

### F2 — Full-text search (MySQL/MariaDB FULLTEXT) + autocomplete
| File | Đã làm | Giải thích |
|------|--------|-----------|
| `config/FullTextIndexInitializer.java` (MỚI) | Tạo FULLTEXT index `ft_products_name_desc` trên products(name,description) lúc khởi động nếu chưa có (idempotent), guard chống SQL injection | Máy clone về tự có index, không cần chạy SQL tay |
| `repository/ProductRepository.java` | Thêm `fullTextSearchIds` (MATCH AGAINST BOOLEAN MODE, xếp relevance) + `findByIdInAndActiveTrue` (EntityGraph tránh lazy) | Truy vấn full-text + tải an toàn |
| `service/ProductService.java` | `fullTextSearch(query, limit)` — thử MATCH, fallback LIKE; `buildBooleanQuery` | Luôn ra kết quả kể cả khi index lỗi/tiếng Việt |
| `controller/api/ProductApiController.java` | Endpoint `GET /api/products/suggest?q=` | Autocomplete API |
| `templates/shop/products.html` | Dropdown autocomplete (debounce 220ms, phím ↑↓ Enter Esc), CSS `.ac-*` | Gợi ý SP khi gõ. **E2E:** suggest('hoodie')→2 ✅ |

### F3 — Review có ảnh đính kèm
| File | Đã làm | Giải thích |
|------|--------|-----------|
| `entity/Review.java` | Thêm `@ElementCollection List<String> imageUrls` (bảng phụ `review_images`) | Lưu nhiều ảnh/review (ddl-auto tự tạo bảng) |
| `service/ReviewService.java` | Overload `createReview(...imageUrls)`, giới hạn 5 ảnh | Lưu URL ảnh |
| `controller/ReviewController.java` | Nhận `MultipartFile[] images`, validate (≤5MB, jpg/png/webp), upload qua FileStorageService | Upload an toàn |
| `templates/shop/order-detail.html` | Form review thêm `enctype=multipart` + input file | Khách chọn ảnh |
| `templates/shop/product-detail.html` | Hiển thị gallery ảnh trong mỗi review | Xem ảnh review |

### F4 — SSE real-time notification
| File | Đã làm | Giải thích |
|------|--------|-----------|
| `service/SseService.java` (MỚI) | Registry SseEmitter theo userId + kênh admin; push tới user/admin (best-effort, thread-safe ConcurrentHashMap/CopyOnWriteArrayList) | Đẩy thông báo real-time |
| `controller/SseNotificationController.java` (MỚI) | `GET /notifications/stream` (web-chain, session-auth) | Trình duyệt EventSource kết nối |
| `service/NotificationService.java` | Inject SseService; push tới user khi tạo notification + push "đơn mới" tới admin | Wire real-time vào nghiệp vụ |
| `templates/layout/base.html` + `admin.html` | EventSource + toast (gate theo đăng nhập) | Hiện toast. **E2E:** /notifications/stream anonymous→302 (bảo vệ auth) ✅ |

### F5 — Hệ thống mã giới thiệu (referral)
| File | Đã làm | Giải thích |
|------|--------|-----------|
| `entity/User.java` | Thêm `referralCode` (unique), `referredById`, `referralRewarded` | Dữ liệu referral |
| `service/ReferralService.java` (MỚI) | Sinh mã, gắn người giới thiệu (chống tự ref), trao thưởng đơn đầu | Logic referral |
| `config/ReferralBackfillInitializer.java` (MỚI) | Gán mã cho user cũ lúc khởi động (idempotent) | Clone về user cũ vẫn có mã. **Verified:** backfill 2 user ✅ |
| `service/UserService.java` | Overload `registerUser(...refCode, fullName)` — sinh mã + gắn ref + set tên trong 1 transaction | Đăng ký nguyên tử |
| `service/OrderService.java` | Khi đơn COMPLETED → `rewardOnFirstCompletedOrder` (best-effort) | Trao coupon cho cả 2 |
| `controller/AuthController.java` + `api/AuthApiController.java` | Nhận `?ref=CODE` / field ref | Liên kết người giới thiệu |
| `templates/shop/profile.html` | Card hiển thị mã + link giới thiệu + nút copy | Khách chia sẻ. **E2E:** đăng ký ref→referred_by_id liên kết đúng ✅ |
| `templates/auth/register.html` | Hidden input `ref` | Gửi mã khi đăng ký |

---

## 🛡️ BẢNG 6 — Fix theo code-review (6 lỗi HIGH)

| # | File | Lỗi | Cách fix |
|---|------|-----|----------|
| 25b | `ReferralService.java` + `UserRepository.markReferralRewarded` | **Trao coupon TRÙNG** khi 2 đơn của 1 khách hoàn tất đồng thời (race) | Atomic check-and-set bằng `UPDATE ... WHERE referral_rewarded=false` (chỉ 1 luồng thắng) + `@Transactional(REQUIRES_NEW)` |
| 25c | `NotificationService.java` (5 method notify*) | Lỗi lưu notification làm **rollback nghiệp vụ chính** (đặt đơn/đổi trạng thái) | `@Transactional(REQUIRES_NEW)` trên các public method → notification chạy TX riêng |
| 25d | `SseService.java` `remove()` | Race TOCTOU giữa `isEmpty()` và `remove()` | Dùng `userEmitters.compute()` nguyên tử |
| 25e | `SseService.java` `subscribe()` | Admin nhận **toast trùng** (đăng ký cả kênh user lẫn admin) | Admin chỉ đăng ký kênh admin |
| 25f | `UserService` + `AuthApiController` | Đăng ký API lưu user 2 lần (fullName set sau) — mất nguyên tử | Overload `registerUser(...fullName)` set trước khi lưu, 1 transaction |
| 25g | `FullTextIndexInitializer.java` | Pattern Statement nối chuỗi (nguy cơ injection nếu sau này dùng giá trị động) | Static guard validate định danh `[A-Za-z0-9_]+` |

**Kết quả:** 81 test PASS (gồm 8 test mới `ReferralServiceTest`), compile sạch, app chạy + smoke test 5 tính năng OK.

---

## 🔗 Liên kết
- Chi tiết tính năng: [features.md](features.md)
- Lịch sử BEFORE/AFTER: [changelog.md](changelog.md)
- Hướng dẫn chạy: [../README.md](../README.md)

---

*Cập nhật lần cuối: 2026-05-29*
