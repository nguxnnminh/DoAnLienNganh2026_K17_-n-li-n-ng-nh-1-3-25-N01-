# Danh sách tính năng đã hoàn thành — ClothingStore

> Tài liệu nội bộ nhóm. Liệt kê tất cả những gì đã xây dựng tính đến ngày **2026-05-31**.

---

## Stack kỹ thuật

| Lớp | Công nghệ |
|-----|-----------|
| Backend | Spring Boot 3.5, Spring Security, Spring Data JPA, Spring Session JDBC |
| Frontend (Web) | Thymeleaf, Tailwind CSS 3, Vanilla JS |
| Mobile | Flutter, Dart 3.11+, Riverpod 2.6, GoRouter 14, Dio 5 |
| DB | MySQL 8 (FULLTEXT INDEX cho tìm kiếm) |
| Cache | Caffeine (in-memory) |
| Email | Gmail SMTP (async) |
| Auth | BCrypt + Session JDBC (web) · JWT HS256 (API / mobile) |
| AI Chatbot | Rule-based FAQ + Ollama + llama3.2:3b (tùy chọn) |
| Virtual Try-On | Python FastAPI bridge → Replicate IDM-VTON (cloud) → CatVTON local (GPU) · SegFormer mask |
| Real-time | Server-Sent Events (SSE) |
| File upload | Local disk `uploads/` · max 20 MB |
| Export | Apache POI (Excel .xlsx) |
| API docs | Springdoc OpenAPI / Swagger UI |
| Monitoring | Spring Boot Actuator (health · info · metrics) |

---

## 1. KHÁCH HÀNG (Customer-facing)

### 1.1 Trang chủ (`/`)
- Hero banner slider 3 slide, tự xoay, điều hướng tay
- Best sellers: sản phẩm bán chạy nhất mỗi category (top / bottom / accessories)
- Điều hướng đến trang sản phẩm, danh mục, liên hệ, đổi trả, bảng size

### 1.2 Danh sách sản phẩm (`/products`)
- Hiển thị tất cả sản phẩm, phân trang 12 SP/trang
- Lọc theo: khoảng giá, từ khóa tìm kiếm, danh mục, màu sắc
- Sắp xếp: mới nhất · giá tăng · giá giảm · bán chạy
- Phân trang với "window" ±2 trang (tối đa 7 nút)
- Duyệt theo Category (`/products/{categorySlug}`)
- Duyệt theo SubCategory (`/products/{categorySlug}/{subSlug}`)
- **Full-text Search**: MySQL `MATCH AGAINST` với FULLTEXT INDEX (tên, mô tả, danh mục)
- **Autocomplete**: `/api/products/suggest?q=` gợi ý kết quả realtime khi gõ

### 1.3 Chi tiết sản phẩm (`/product/{slug}`)
- Ảnh sản phẩm (nhiều ảnh, gallery)
- Chọn size + màu sắc (variant selector, JS-driven)
- Giá hiển thị theo variant được chọn
- Stock real-time theo variant
- Đánh giá trung bình + số lượt review (1 query, tính in-memory)
- Danh sách reviews + rating + ảnh đính kèm của từng người
- Sản phẩm liên quan (4 SP, EntityGraph pre-fetch ảnh)
- Nút Yêu thích (wishlist toggle, chỉ khi đã đăng nhập)
- Nút Thêm vào giỏ hàng
- Nút Thử đồ ảo (nếu sản phẩm hỗ trợ try-on)

### 1.4 Giỏ hàng (`/cart`)
- Thêm / bớt số lượng
- Xóa sản phẩm
- Hiển thị subtotal
- Hoạt động cả khi chưa đăng nhập (session-based)

### 1.5 Thanh toán (`/checkout`)
- Nhập họ tên, số điện thoại VN (validation regex), địa chỉ giao hàng
- Phí ship: miễn phí nếu đơn ≥ 500.000 VNĐ · 30.000 VNĐ nếu dưới ngưỡng
- Chọn mã giảm giá (coupon) từ danh sách khả dụng
- Nhập ghi chú đơn hàng
- Hỗ trợ cả khách (guest) và người dùng đã đăng nhập
- Trang xác nhận (`/checkout/success`) sau đặt hàng thành công

### 1.6 Quản lý đơn hàng của tôi (`/my-orders`)
- Danh sách đơn theo thời gian
- Chi tiết đơn hàng: sản phẩm, số lượng, giá, trạng thái, thông tin giao hàng
- Yêu cầu hủy đơn (CANCEL_REQUESTED)

### 1.7 Wishlist (`/wishlist`)
- Thêm / xóa sản phẩm yêu thích
- Xem toàn bộ danh sách yêu thích
- Chuyển từ wishlist sang giỏ hàng

### 1.8 Mã giảm giá (`/my-coupons`)
- Xem danh sách coupon hiện có của tài khoản
- Coupon có thể: % giảm · giảm tiền cố định · ngưỡng đơn tối thiểu · số lần dùng tối đa

### 1.9 Đánh giá sản phẩm
- Viết review sau khi mua hàng (rating 1–5 sao + nội dung)
- **Upload tối đa 5 ảnh/review** (jpg/png/webp, validate magic bytes)
- Chỉ 1 review/sản phẩm/người dùng
- Hiển thị ảnh review trên trang chi tiết sản phẩm

### 1.10 Hồ sơ cá nhân (`/profile`)
- Xem và cập nhật thông tin cá nhân (tên, SĐT)
- Đổi mật khẩu
- **Quản lý nhiều địa chỉ**: thêm / sửa / xóa địa chỉ giao hàng

### 1.11 Thông báo real-time (`/notifications/stream`)
- **SSE (Server-Sent Events)**: không cần WebSocket, hoạt động mọi browser
- Nhận thông báo tức thì: xác nhận đặt hàng, cập nhật trạng thái, đơn đã giao
- Xem lịch sử thông báo (`/api/notifications`)
- Tự động reconnect khi mất kết nối

### 1.12 Hệ thống mã giới thiệu (Referral)
- Mỗi tài khoản có mã giới thiệu riêng
- Người mới đăng ký với mã giới thiệu → được gắn người giới thiệu
- Khi đơn hàng đầu tiên của người được giới thiệu hoàn tất (DELIVERED):
  - Người giới thiệu nhận coupon cảm ơn
  - Người được giới thiệu nhận coupon chào mừng
- Sự kiện xử lý bất đồng bộ qua `UserRegisteredEvent`

### 1.13 Thử đồ ảo — Try-On Studio (`/tryon-studio`)
- Upload ảnh người (full-body, max 5MB, jpg/png/webp)
- **Single garment**: thử 1 sản phẩm, chọn từ danh sách
- **Full outfit**: thử đồng thời áo (UPPER_BODY) + quần (LOWER_BODY)
- Garment types: `UPPER_BODY` · `LOWER_BODY`
- **Full outfit** chạy CatVTON 2 lượt trên ảnh gốc rồi composite (không chain tuần tự)
- Ảnh kết quả tự xóa sau 5 phút (cleanup scheduler)
- Health check `/api/tryon/health` kiểm tra Python server
- Backend: Replicate IDM-VTON (cloud) → fallback CatVTON local trên GPU (mask bằng SegFormer human-parsing)

### 1.14 AI Chatbot
- Hộp chat nổi trên toàn site (web)
- **Tầng 1 — Rule-based FAQ**: vận chuyển, đổi trả, size, thanh toán, coupon, chất liệu → trả lời ngay, luôn hoạt động
- **Tầng 2 — Rule-based Search**: parse giá (regex VN: triệu/k/nghìn/ngàn), category, màu → query ProductService
- **Tầng 3 — AI (Ollama LLaMA 3.2)**: câu hỏi tự do, gửi context + lịch sử → LLaMA 3.2:3b (tùy chọn)
- Lịch sử hội thoại: tối đa 12 lượt (session-based)
- Cooldown 5 phút · timeout 10 giây khi Ollama lỗi
- Luôn trả lời — không bao giờ lỗi dù Ollama không chạy

### 1.15 Các trang tĩnh
- `/contact` — Liên hệ
- `/returns` — Chính sách đổi trả
- `/sizing` — Bảng kích cỡ

---

## 2. XÁC THỰC (Auth)

| Tính năng | Trạng thái |
|-----------|-----------|
| Đăng ký tài khoản (có hỗ trợ referral code) | ✅ |
| Đăng nhập form (Spring Security) | ✅ |
| Quên mật khẩu (gửi email token) | ✅ |
| Đặt lại mật khẩu qua link (token 1-time, 24h) | ✅ |
| BCrypt password hashing (10 rounds) | ✅ |
| Login rate limiting — tối đa 5 lần/IP/15 phút | ✅ |
| JWT cho API (HS256, 24h) | ✅ |
| Role: USER / ADMIN | ✅ |
| Redirect sau đăng nhập theo role | ✅ |

---

## 3. ADMIN

### 3.1 Dashboard (`/admin`)
- **KPIs**: Tổng đơn · Đơn hôm nay · Đang xử lý · Đang giao · Hoàn thành · Đã hủy
- **Doanh thu**: Hôm nay · Tuần này · Tháng này · Năm nay · Trung bình/đơn
- **Biểu đồ doanh thu**: theo ngày · tuần · tháng · năm (Chart.js)
- **Cảnh báo tồn kho thấp** (low stock alert theo ngưỡng)
- **Top sản phẩm bán chạy**
- **Đơn hàng gần đây**
- **Xuất báo cáo Excel** (Apache POI — tổng hợp đơn hàng, doanh thu)
- **Thông báo real-time**: nhận SSE khi có đơn mới, yêu cầu hủy

### 3.2 Quản lý sản phẩm (`/admin/products`)
- CRUD sản phẩm đầy đủ
- Upload nhiều ảnh (max 20 MB/file, validate magic bytes)
- Quản lý variants (size, màu, giá, tồn kho)
- Bật/tắt tính năng Virtual Try-On per sản phẩm
- Gán GarmentType (UPPER_BODY / LOWER_BODY)
- Tìm kiếm, lọc, phân trang

### 3.3 Quản lý danh mục (`/admin/categories`, `/admin/subcategories`)
- CRUD Category + SubCategory
- Slug URL auto-generate

### 3.4 Quản lý đơn hàng (`/admin/orders`)
- Danh sách tất cả đơn, lọc theo trạng thái
- Chi tiết đơn hàng (sản phẩm, thông tin khách, giao hàng)
- Cập nhật trạng thái: PENDING → CONFIRMED → SHIPPED → DELIVERED / CANCELLED
- Duyệt / từ chối yêu cầu hủy của khách

### 3.5 Quản lý người dùng (`/admin/users`)
- Danh sách người dùng
- Tạo · sửa · khóa / mở khóa tài khoản
- Gán / thu hồi role ADMIN

### 3.6 Quản lý mã giảm giá (`/admin/coupons`)
- CRUD coupon (% giảm · tiền cố định)
- Thiết lập: thời hạn · ngưỡng đơn tối thiểu · số lần dùng tối đa
- Phát coupon thủ công cho người dùng
- Coupon tự động phát qua hệ thống referral

---

## 4. FLUTTER MOBILE APP

> iOS / Android · Riverpod 2.6 · GoRouter · Dio + JWT · Flutter 3.11+

### 4.1 Màn hình chính
- **Home**: hero slider, danh sách sản phẩm bán chạy, điều hướng danh mục
- **Shop**: danh sách sản phẩm, lọc, tìm kiếm
- **Product Detail**: ảnh, variant selector, reviews, nút thêm giỏ / wishlist
- **Cart**: xem, cập nhật số lượng, xóa sản phẩm
- **Checkout**: nhập thông tin giao hàng, chọn coupon, đặt hàng
- **Checkout Success**: xác nhận đặt hàng thành công

### 4.2 Tài khoản
- **Login / Register / Forgot Password / Reset Password**
- **My Orders**: danh sách + chi tiết đơn hàng
- **Wishlist**: danh sách yêu thích
- **My Coupons**: coupon của tôi
- **Profile**: cập nhật thông tin cá nhân
- **Notifications**: lịch sử thông báo

### 4.3 Kỹ thuật mobile
- **State management**: Riverpod (Provider pattern, code-gen)
- **Navigation**: GoRouter (deep link, named routes)
- **HTTP**: Dio + cookie_jar + dio_cookie_manager (session persistence)
- **Storage**: `flutter_secure_storage` (JWT) · `shared_preferences` (settings)
- **Image**: `cached_network_image` + shimmer loading
- **UI**: Google Fonts, smooth_page_indicator, gap
- **Format**: `intl` (tiền tệ VNĐ, ngày giờ)

---

## 5. REST API

| Endpoint | Mô tả | Auth |
|----------|-------|------|
| `POST /api/auth/register` | Đăng ký (có referral code) | Public |
| `POST /api/auth/login` | Đăng nhập → JWT | Public |
| `POST /api/auth/forgot-password` | Gửi link reset | Public |
| `POST /api/auth/reset-password` | Đặt lại mật khẩu | Public |
| `GET /api/products` | Danh sách SP (phân trang, lọc) | Public |
| `GET /api/products/{id}` | Chi tiết SP | Public |
| `GET /api/products/suggest?q=` | Autocomplete full-text | Public |
| `GET /api/products/{id}/similar` | SP tương tự | Public |
| `GET /api/categories` | Danh mục (có subcategory) | Public |
| `GET /api/subcategories` | Subcategory | Public |
| `GET /api/cart` | Xem giỏ hàng | Public |
| `POST /api/cart/add` | Thêm vào giỏ | Public |
| `POST /api/orders/checkout` | Đặt hàng | Public |
| `GET /api/orders/my` | Đơn hàng của tôi | USER |
| `POST /api/orders/{id}/cancel` | Hủy đơn | USER |
| `POST /api/orders/{id}/cancel-request` | Yêu cầu hủy đơn | USER |
| `GET /api/wishlist` | Danh sách wishlist | USER |
| `POST /api/wishlist/{id}` | Thêm vào wishlist | USER |
| `DELETE /api/wishlist/{id}` | Xóa khỏi wishlist | USER |
| `GET /api/coupons` | Tất cả coupon | Public |
| `GET /api/coupons/my` | Coupon của tôi | USER |
| `POST /api/coupons/validate` | Kiểm tra coupon | Public |
| `GET /api/profile` | Thông tin cá nhân | USER |
| `PUT /api/profile` | Cập nhật hồ sơ | USER |
| `POST /api/profile/change-password` | Đổi mật khẩu | USER |
| `POST /api/chatbot` | Chat AI | Public |
| `GET /notifications/stream` | SSE real-time | USER |
| `GET /api/notifications` | Lịch sử thông báo | USER |
| `POST /api/tryon/upload-person` | Upload ảnh người | Public |
| `POST /api/tryon/generate` | Thử 1 sản phẩm | Public |
| `POST /api/tryon/generate-outfit` | Thử full outfit | Public |
| `GET /api/tryon/health` | Kiểm tra Python server | Public |
| `GET /api/analytics/**` | Thống kê doanh thu | ADMIN |
| `GET /api/admin/orders` | Tất cả đơn hàng | ADMIN |
| `POST /api/admin/orders/{id}/status` | Cập nhật trạng thái | ADMIN |
| `GET /api/admin/users` | Quản lý người dùng | ADMIN |
| `POST /api/admin/dashboard/export-excel` | Xuất Excel | ADMIN |

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 6. KỸ THUẬT / NON-FUNCTIONAL

| Hạng mục | Chi tiết |
|----------|---------|
| **Security headers** | X-Frame-Options DENY · HSTS 1 năm · Content-Type-Options · Referrer-Policy |
| **CORS** | Cấu hình riêng cho API chain (cho phép mobile app) |
| **Session** | JDBC-backed (MySQL), timeout 24h |
| **Cache** | Caffeine cho categories, products phổ biến |
| **Async** | Thread pool 4–8 cho email, notification, SSE |
| **Graceful shutdown** | 30s timeout per phase |
| **Login rate limit** | Tối đa 5 lần/IP/15 phút |
| **File security** | Magic byte validation (jpg/png/webp), path traversal check |
| **CSRF** | Bật web chain · tắt API chain (stateless JWT) |
| **Input validation** | Jakarta Bean Validation + custom phone regex |
| **Exception handling** | GlobalExceptionHandler + ApiExceptionHandler |
| **Audit log** | Ghi log các thay đổi quan trọng |
| **Stock log** | Lịch sử thay đổi tồn kho |
| **SSE** | Server-Sent Events real-time (không cần WebSocket) |
| **Referral events** | Async `UserRegisteredEvent` + `OrderDeliveredEvent` |
| **Full-text search** | MySQL FULLTEXT INDEX trên name/description/category |
| **Request tracing** | X-Request-ID header trên mọi request |
| **Swagger UI** | `/swagger-ui.html` |
| **Actuator** | `/actuator/health` public · `/actuator/**` chỉ ADMIN |

---

## 7. ENTITIES (Database — 19 bảng @Entity + enums)

| Entity | Ghi chú |
|--------|---------|
| User | email · password · role · active · referralCode · referredBy |
| Role | ROLE_USER · ROLE_ADMIN |
| Address | entity địa chỉ (User dùng 1 field `address` mặc định) |
| Product | slug · totalSold · tryOnEnabled · garmentType |
| ProductImage | nhiều ảnh/SP |
| ProductVariant | size · color · price · stock · totalSold |
| Category | slug |
| SubCategory | slug · parent Category |
| GarmentType *(enum)* | UPPER_BODY · LOWER_BODY |
| SizeType | bảng size |
| Order | customerName · phone · address · status · coupon · notes |
| OrderItem | SP + variant + qty + giá tại thời điểm đặt |
| OrderStatus | PENDING → CONFIRMED → SHIPPED → DELIVERED / CANCELLED |
| Payment | liên kết với Order (COD) |
| Shipment | thông tin vận chuyển, tracking |
| Coupon | code · type · value · minOrder · maxUses · expiry |
| UserCoupon | mapping User–Coupon + usedCount + usedAt |
| Review | rating · content · product · user · images |
| WishlistItem | user · product |
| Notification | user · message · read · type |
| AuditLog | bảng audit thay đổi hệ thống |
| StockLog | lịch sử thay đổi tồn kho |
| PasswordResetToken | token · expiry · used |

---

## 8. VIRTUAL TRY-ON — Kiến trúc Python Server

```
Python FastAPI (port 8081)
         │
    ┌────┴─────┐
    ▼          ▼ (hết quota / không token)
Replicate    CatVTON local (GPU, fp16, 768×1024)
IDM-VTON     ├─ SegFormer parse → agnostic mask
(cloud)      └─ UniPC @ 20 steps · outfit = 2 lượt + composite
```

| Backend | Điều kiện | Ghi chú |
|---------|-----------|---------|
| Replicate IDM-VTON | Có `REPLICATE_API_TOKEN` + còn quota | `cuuupid/idm-vton`, cloud, nhanh |
| CatVTON local | Không token / cloud hết quota (402/429) | `zhengchong/CatVTON`, GPU ≥ 4GB, weights ~4GB |
| Mock mode | `MOCK_INFERENCE=true` | Ảnh giả, dùng cho phát triển |

**Masking**: SegFormer (`mattmdjaga/segformer_b2_clothes`, ATR 18-class) tạo cloth-agnostic mask,
bảo vệ mặt/tóc/tay/trang-phục-còn-lại (thay AutoMasker DensePose+SCHP vì detectron2 không build trên Windows).
**Tinh chỉnh (env)**: `TRYON_STEPS` · `TRYON_SCHEDULER` · `TRYON_PARSER_DEVICE` · `TRYON_CFG`.
**Preprocessing**: `rembg` xóa nền ảnh garment · normalize về 768×1024
**Auto-cleanup**: kết quả tự xóa sau 5 phút

---

## 9. CẤU TRÚC THƯ MỤC

```
src/main/java/com/shop/clothingstore/
├── config/          SecurityConfig · CacheConfig · CORS · AsyncConfig · DataInitializer
├── controller/
│   ├── (web)        Auth · Cart · Checkout · Order · Profile · Shop · TryOn · Wishlist · Coupon
│   ├── admin/       Dashboard · Product · Category · SubCategory · Order · User · Coupon
│   └── api/         Auth · Cart · Chatbot · Coupon · Notification · Order · Product
│                    Analytics · TryOn · Wishlist · Profile · Address
├── dto/             Form DTOs · API request/response
├── entity/          JPA entities + base classes
├── event/           UserRegisteredEvent · OrderDeliveredEvent
├── exception/       AppException · OutOfStock · ResourceNotFound · GlobalExceptionHandler
├── repository/      Spring Data JPA repos + JPA Specifications (filtering)
├── security/        JwtAuthenticationFilter · LoginRateLimitFilter · RequestIdFilter
└── service/         Business logic · AiChatbotService · TryOnService · SseService
                     EmailService · FileStorageService · ReferralService · ReportService

src/main/resources/
├── application.properties
├── logback-spring.xml
├── templates/
│   ├── layout/      base.html · admin.html
│   ├── auth/        login · register · forgot-password · reset-password
│   ├── admin/       dashboard · products · categories · subcategories · orders · users · coupons
│   └── shop/        home · products · product-detail · cart · checkout · checkout-success
│                    my-orders · order-detail · wishlist · my-coupons · profile
│                    tryon-studio · contact · returns · sizing
└── static/
    ├── css/         tailwind.css
    ├── js/          admin-spa.js · various page scripts
    └── images/      accessories · bottom · top · winter-collection

mobile-app/lib/                "nova_mobile"
├── main.dart        App entry point
├── core/            network/api_client.dart (Dio + JWT + cookie) · theme · config
├── features/        auth · home · shop · product · cart · checkout · orders · wishlist · search · profile · notifications
│                    (mỗi feature: screen + Riverpod provider)
├── models/          user · product · cart · order · wishlist · notification · category
├── router/          GoRouter setup
└── shared/          Widgets dùng chung
```
