# Danh sách tính năng đã hoàn thành — ClothingStore

> Đây là tài liệu người dùng / thành viên nhóm. Liệt kê tất cả những gì đã xây dựng được tính đến ngày **2026-05-29**.

---

## Stack kỹ thuật

| Lớp | Công nghệ |
|-----|-----------|
| Backend | Spring Boot 3.5, Spring Security, Spring Data JPA, Spring Session JDBC |
| Frontend | Thymeleaf, Tailwind CSS, vanilla JS |
| DB | MySQL 8 |
| Cache | Caffeine (in-memory) |
| Email | Gmail SMTP (async) |
| Auth | BCrypt + Session (web) · JWT HS256 (API) |
| AI Chatbot | Ollama + llama3.2:3b (local) |
| Virtual Try-On | Python bridge → OOTDiffusion / IDM-VTON (GPU) |
| File upload | Local disk `uploads/` · max 20 MB |
| Export | Apache POI (Excel .xlsx) |
| API docs | Springdoc OpenAPI / Swagger UI |
| Monitoring | Spring Boot Actuator (health · info · metrics) |

---

## 1. KHÁCH HÀNG (Customer-facing)

### 1.1 Trang chủ
- Best sellers: 1 sản phẩm bán chạy nhất mỗi category (top / bottom / accessories)
- Điều hướng đến trang sản phẩm, danh mục, liên hệ, đổi trả, bảng size

### 1.2 Danh sách sản phẩm (`/products`)
- Hiển thị tất cả sản phẩm, phân trang 12 SP/trang
- Lọc theo: khoảng giá, từ khóa tìm kiếm
- Sắp xếp: mới nhất · giá tăng · giá giảm · bán chạy
- Phân trang với "window" ±2 trang (tối đa 7 nút)
- Duyệt theo Category (`/products/{categorySlug}`)
- Duyệt theo SubCategory (`/products/{categorySlug}/{subSlug}`)

### 1.3 Chi tiết sản phẩm (`/product/{slug}`)
- Ảnh sản phẩm (nhiều ảnh)
- Chọn size + màu sắc (variant selector, JS-driven)
- Giá theo variant được chọn
- Stock real-time theo variant
- Đánh giá trung bình + số lượt review (tính in-memory, 1 query)
- Danh sách reviews + rating của từng người
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
- Chi tiết đơn hàng: sản phẩm, số lượng, giá, trạng thái
- Yêu cầu hủy đơn (CANCEL_REQUESTED)

### 1.7 Wishlist (`/wishlist`)
- Thêm / xóa sản phẩm yêu thích
- Xem toàn bộ danh sách yêu thích
- Chuyển từ wishlist sang giỏ hàng

### 1.8 Mã giảm giá (`/my-coupons`)
- Xem danh sách coupon hiện có của tài khoản
- Coupon có thể: % giảm · giảm tiền cố định · ngưỡng đơn tối thiểu · số lần dùng tối đa

### 1.9 Đánh giá sản phẩm (`/reviews`)
- Viết review sau khi mua hàng (rating 1–5 sao + nội dung)
- Chỉ 1 review/sản phẩm/người dùng

### 1.10 Hồ sơ cá nhân (`/profile`)
- Xem và cập nhật thông tin cá nhân (tên, SĐT, địa chỉ)
- Đổi mật khẩu

### 1.11 Thử đồ ảo — Try-On Studio (`/tryon-studio`)
- Upload ảnh người (full-body, max 5MB, jpg/png/webp)
- **Single garment**: thử 1 sản phẩm, chọn từ danh sách
- **Full outfit**: thử đồng thời áo (UPPER_BODY) + quần (LOWER_BODY)
- Ảnh kết quả tự xóa sau 5 phút (cleanup scheduler)
- Health check `/api/tryon/health` kiểm tra Python server

### 1.12 AI Chatbot
- Hộp chat nổi trên toàn site
- Kết nối Ollama local (llama3.2:3b)
- Tư vấn tìm sản phẩm theo yêu cầu tự nhiên
- Có cooldown 5 phút · timeout 10 giây
- Tự vô hiệu nếu Ollama chưa chạy

### 1.13 Các trang tĩnh
- `/contact` — Liên hệ
- `/returns` — Chính sách đổi trả
- `/sizing` — Bảng kích cỡ

---

## 2. XÁC THỰC (Auth)

| Tính năng | Trạng thái |
|-----------|-----------|
| Đăng ký tài khoản | ✅ |
| Đăng nhập form (Spring Security) | ✅ |
| Quên mật khẩu (gửi email token) | ✅ |
| Đặt lại mật khẩu qua link | ✅ |
| BCrypt password hashing | ✅ |
| Login rate limiting (chống brute-force) | ✅ |
| JWT cho API (HS256, 24h) | ✅ |
| Role: USER / ADMIN | ✅ |
| Redirect sau đăng nhập theo role | ✅ |

---

## 3. ADMIN

### 3.1 Dashboard (`/admin`)
- **KPIs**: Tổng đơn · Đơn hôm nay · Đang xử lý · Đang giao · Hoàn thành · Đã hủy
- **Doanh thu**: Hôm nay · Tuần này · Tháng này · Năm nay · Trung bình/đơn
- **Biểu đồ doanh thu**: theo ngày · tuần · tháng · năm (Chart.js)
- **Cảnh báo tồn kho thấp** (low stock alert)
- **Top sản phẩm bán chạy**
- **Đơn hàng gần đây**
- **Xuất báo cáo Excel** (Apache POI)

### 3.2 Quản lý sản phẩm (`/admin/products`)
- CRUD sản phẩm đầy đủ
- Upload nhiều ảnh (max 20 MB/file)
- Quản lý variants (size, màu, giá, tồn kho)
- Bật/tắt tính năng Virtual Try-On cho sản phẩm
- Tìm kiếm, lọc, phân trang

### 3.3 Quản lý danh mục (`/admin/categories`, `/admin/subcategories`)
- CRUD Category + SubCategory
- Slug URL auto-generate

### 3.4 Quản lý đơn hàng (`/admin/orders`)
- Danh sách tất cả đơn
- Chi tiết đơn hàng
- Cập nhật trạng thái: PENDING → PROCESSING → SHIPPING → COMPLETED / CANCELLED
- Duyệt/từ chối yêu cầu hủy

### 3.5 Quản lý người dùng (`/admin/users`)
- Danh sách người dùng
- Tạo · sửa · khóa tài khoản
- Gán / thu hồi role ADMIN

### 3.6 Quản lý mã giảm giá (`/admin/coupons`)
- CRUD coupon (% giảm · tiền cố định)
- Thiết lập: thời hạn · ngưỡng đơn tối thiểu · số lần dùng tối đa
- Phát coupon cho người dùng

---

## 4. REST API

| Endpoint | Mô tả | Auth |
|----------|-------|------|
| `POST /api/auth/login` | Đăng nhập → JWT | Public |
| `POST /api/auth/register` | Đăng ký | Public |
| `GET /api/products` | Danh sách SP | Public |
| `GET /api/products/{id}` | Chi tiết SP | Public |
| `GET /api/categories` | Danh sách category | Public |
| `GET /api/subcategories` | Danh sách subcategory | Public |
| `GET /api/cart` | Xem giỏ hàng | Public |
| `POST /api/cart/add` | Thêm vào giỏ | Public |
| `POST /api/orders/checkout` | Đặt hàng qua API | Public |
| `POST /api/chatbot` | Gửi tin nhắn chatbot | Public |
| `GET /api/coupons/validate` | Kiểm tra coupon | Public |
| `POST /api/tryon/upload-person` | Upload ảnh người | Public |
| `POST /api/tryon/generate` | Thử 1 sản phẩm | Public |
| `POST /api/tryon/generate-outfit` | Thử full outfit | Public |
| `GET /api/tryon/health` | Kiểm tra Python server | Public |
| `GET /api/recommendations` | SP gợi ý | Public |
| `GET /api/notifications` | Thông báo | USER |
| `GET /api/analytics/top-products` | Top SP | ADMIN |
| `GET /api/analytics/trending` | Trending | ADMIN |
| `GET /api/analytics/overview` | KPIs tổng quan | ADMIN |
| `GET /api/admin/**` | Quản trị qua API | ADMIN |

---

## 5. KỸ THUẬT / NON-FUNCTIONAL

| Hạng mục | Chi tiết |
|----------|---------|
| **Security headers** | X-Frame-Options DENY · HSTS 1 năm · Content-Type-Options · Referrer-Policy |
| **CORS** | Cấu hình riêng cho API chain |
| **Session** | JDBC-backed (MySQL), timeout 24h |
| **Cache** | Caffeine cho categories, products phổ biến |
| **Async** | Thread pool 4–8 cho email, notification |
| **Graceful shutdown** | 30s timeout per phase |
| **Login rate limit** | Chống brute-force |
| **File security** | Magic byte validation (jpg/png/webp), path traversal check |
| **CSRF** | Bật web chain · tắt API chain (stateless JWT) |
| **Input validation** | Jakarta Bean Validation + custom phone regex |
| **Exception handling** | GlobalExceptionHandler + ApiExceptionHandler |
| **Audit log** | Ghi log các thay đổi quan trọng |
| **Stock log** | Lịch sử thay đổi tồn kho |
| **Notification** | Thông báo real-time trong app |
| **Swagger UI** | `/swagger-ui.html` |
| **Actuator** | `/actuator/health` public · `/actuator/**` chỉ ADMIN |

---

## 6. ENTITIES (Database)

| Entity | Ghi chú |
|--------|---------|
| User | email · password · role · active |
| Role | ROLE_USER · ROLE_ADMIN |
| Address | giao hàng |
| Product | slug · totalSold · tryOnEnabled |
| ProductImage | nhiều ảnh/SP |
| ProductVariant | size · color · price · stock · totalSold |
| Category | slug |
| SubCategory | slug |
| GarmentType | loại quần áo cho try-on |
| SizeType | bảng size |
| Order | customerName · phone · address · status · coupon |
| OrderItem | SP + variant + qty + giá tại thời điểm đặt |
| OrderStatus | PENDING → PROCESSING → SHIPPING → COMPLETED / CANCELLED |
| Payment | liên kết với Order |
| Shipment | thông tin vận chuyển |
| Coupon | code · type · value · minOrder · maxUses · expiry |
| UserCoupon | mapping User–Coupon + usedAt |
| Review | rating · content · product · user |
| WishlistItem | user · product |
| Notification | user · message · read |
| AuditLog | bảng audit |
| StockLog | lịch sử tồn kho |
| PasswordResetToken | token · expiry |

---

## 7. CẤU TRÚC THƯ MỤC

```
src/main/java/com/shop/clothingstore/
├── config/          (Security, Cache, CORS, Async, DataInitializer…)
├── controller/      (web: Auth, Cart, Checkout, Order, Profile, Shop, TryOn, Wishlist)
│   ├── admin/       (Dashboard, Product, Category, SubCategory, Order, User, Coupon, TryOn)
│   └── api/         (Auth, Cart, Chatbot, Coupon, Notification, Order, Product, Analytics, TryOn, Wishlist)
├── dto/             (Form DTOs + API request/response)
├── entity/          (JPA entities + base classes)
├── event/           (UserRegisteredEvent)
├── exception/       (AppException, OutOfStock, ResourceNotFound…)
├── repository/      (Spring Data JPA repos + specifications)
├── security/        (JWT, LoginRateLimit, RequestIdFilter)
└── service/         (Business logic, AI chatbot, Try-On, Storage, Email)

src/main/resources/
├── application.properties
├── logback-spring.xml
├── templates/
│   ├── layout/      (base.html, admin.html)
│   ├── auth/        (login, register, forgot, reset)
│   ├── admin/       (dashboard, products, categories, orders, users, coupons)
│   └── shop/        (home, products, product-detail, cart, checkout, orders, wishlist, profile, tryon-studio, contact, returns, sizing)
└── static/
    ├── css/         (tailwind.css)
    ├── js/          (admin-spa.js)
    └── images/      (accessories, bottom, top, winter-collection)
```
