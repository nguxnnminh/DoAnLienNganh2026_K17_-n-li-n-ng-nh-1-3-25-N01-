# ClothingStore — Web Bán Quần Áo Thông Minh

> Đồ án liên ngành 2026 · K17 · Nhóm N01  
> Spring Boot · Thymeleaf · MySQL · Tailwind CSS · AI Chatbot · Virtual Try-On

---

## Giới thiệu

**ClothingStore** là ứng dụng web thương mại điện tử bán quần áo tích hợp trí tuệ nhân tạo. Điểm nổi bật của dự án:

- **Virtual Try-On** — Thử đồ ảo bằng AI (OOTDiffusion), cho phép khách hàng xem trước trang phục trên ảnh của mình trước khi mua
- **AI Chatbot** — Tư vấn mua sắm bằng ngôn ngữ tự nhiên (Ollama + LLaMA 3.2)
- Hệ thống quản trị đầy đủ với dashboard phân tích doanh thu theo thời gian thực

---

## Tech Stack

| Lớp | Công nghệ |
|-----|-----------|
| Backend | Java 17 · Spring Boot 3.5 · Spring Security · Spring Data JPA |
| Frontend | Thymeleaf · Tailwind CSS · Vanilla JS |
| Database | MySQL 8 · Spring Session JDBC · Caffeine Cache |
| Auth | BCrypt · Session (Web) · JWT HS256 (API) |
| AI Chatbot | Ollama · LLaMA 3.2:3b (local inference) |
| Virtual Try-On | Python FastAPI · OOTDiffusion · IDM-VTON · PyTorch CUDA |
| Email | Gmail SMTP (async) |
| Export | Apache POI (Excel .xlsx) |
| API Docs | Springdoc OpenAPI / Swagger UI |
| Monitoring | Spring Boot Actuator |

---

## Tính năng

### Khách hàng

| Tính năng | Mô tả |
|-----------|-------|
| Trang chủ | Best sellers theo từng category |
| Danh sách SP | Lọc giá · tìm kiếm · sắp xếp · phân trang |
| Duyệt theo danh mục | Category + SubCategory |
| Chi tiết sản phẩm | Chọn size/màu · giá theo variant · tồn kho · reviews · SP liên quan |
| Giỏ hàng | Session-based · hoạt động khi chưa đăng nhập |
| Thanh toán | COD · free ship ≥ 500k · mã giảm giá · ghi chú · khách vãng lai |
| Đơn hàng | Xem lịch sử · chi tiết · yêu cầu hủy |
| Wishlist | Thêm/xóa · xem danh sách yêu thích |
| Coupon | Xem mã giảm giá của tài khoản |
| Đánh giá | Rating 1–5 sao + nội dung |
| Hồ sơ | Cập nhật thông tin · đổi mật khẩu |
| **Virtual Try-On** | Upload ảnh · thử 1 SP · thử full outfit (áo + quần) |
| **AI Chatbot** | Tư vấn ngôn ngữ tự nhiên · filter giá tự động |
| Trang tĩnh | Liên hệ · Đổi trả · Bảng size |

### Xác thực

- Đăng ký · Đăng nhập · Đăng xuất
- Quên mật khẩu (email reset link)
- Login rate limiting (chống brute-force)
- Phân quyền: `USER` / `ADMIN`

### Admin

| Module | Tính năng |
|--------|-----------|
| Dashboard | KPIs · biểu đồ doanh thu (ngày/tuần/tháng/năm) · cảnh báo tồn kho · xuất Excel |
| Sản phẩm | CRUD · nhiều ảnh · variants (size/màu/giá/stock) · bật/tắt Try-On |
| Danh mục | CRUD Category + SubCategory |
| Đơn hàng | Xem tất cả · cập nhật trạng thái · duyệt yêu cầu hủy |
| Người dùng | CRUD · khóa tài khoản · gán role ADMIN |
| Coupon | Tạo % giảm/tiền cố định · thời hạn · ngưỡng đơn · số lần dùng |

### REST API

```
POST   /api/auth/login              Đăng nhập → JWT
POST   /api/auth/register           Đăng ký
GET    /api/products                Danh sách sản phẩm
GET    /api/categories              Danh mục
GET    /api/cart                    Giỏ hàng
POST   /api/orders/checkout         Đặt hàng
POST   /api/chatbot                 AI Chatbot
GET    /api/coupons/validate        Kiểm tra coupon
POST   /api/tryon/upload-person     Upload ảnh người
POST   /api/tryon/generate          Thử 1 sản phẩm
POST   /api/tryon/generate-outfit   Thử full outfit
GET    /api/tryon/health            Kiểm tra Python server
GET    /api/analytics/**            Thống kê (ADMIN)
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Cài đặt & Chạy

### Yêu cầu

- **Java 17+** ([Temurin JDK 17](https://adoptium.net/temurin/releases/?version=17))
- **MySQL 8** đang chạy
- **Python 3.10+** (chỉ cần nếu dùng Virtual Try-On)
- **Ollama** (chỉ cần nếu dùng AI Chatbot)
- **GPU NVIDIA** với CUDA 11.8+ (chỉ cần cho Try-On inference)

### 1. Clone & cấu hình database

```bash
git clone <repo-url>
cd clothingstore
```

Tạo database MySQL:
```sql
CREATE DATABASE clothingstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Cấu hình `application.properties`

File `src/main/resources/application.properties` — mặc định dùng:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/clothingstore
spring.datasource.username=root
spring.datasource.password=
```

Thay đổi `username`/`password` cho phù hợp với MySQL của bạn.

### 3. Chạy Spring Boot

```bash
./mvnw spring-boot:run
```

Server khởi động tại: **http://localhost:8080**

> Lần đầu chạy, `DataInitializer` tự tạo dữ liệu mẫu (categories, subcategories, sản phẩm, admin account).

**Tài khoản mặc định:**

| Role | Email | Mật khẩu |
|------|-------|----------|
| Admin | admin@clothingstore.com | admin123 |
| User | user@clothingstore.com | user123 |

### 4. (Tùy chọn) Chạy AI Chatbot

```bash
# Cài Ollama: https://ollama.com
ollama pull llama3.2:3b
ollama serve
```

Chatbot tự bật khi Ollama đang chạy tại `http://localhost:11434`.

### 5. (Tùy chọn) Chạy Virtual Try-On Server

```bash
cd python-tryon-server

# Cài dependencies (cần GPU NVIDIA + CUDA 11.8)
pip install torch==2.0.1 torchvision==0.15.2 --index-url https://download.pytorch.org/whl/cu118
pip install -r requirements.txt

# Tải model weights
python download_models.py

# Khởi động server tại cổng 8081
python main.py
```

> **Mock mode** (không cần GPU): thêm `tryon.mock=true` vào `application.properties` để dùng ảnh giả cho mục đích phát triển.

---

## Cấu trúc thư mục

```
clothingstore/
├── src/main/java/com/shop/clothingstore/
│   ├── config/          SecurityConfig · CacheConfig · AsyncConfig · DataInitializer
│   ├── controller/
│   │   ├── (web)        Auth · Cart · Checkout · Order · Profile · Shop · TryOn · Wishlist
│   │   ├── admin/       Dashboard · Product · Category · Order · User · Coupon
│   │   └── api/         REST endpoints
│   ├── dto/             Form DTOs · API request/response
│   ├── entity/          JPA entities (26 tables)
│   ├── repository/      Spring Data JPA (20 repos)
│   ├── security/        JWT · LoginRateLimit · RequestIdFilter
│   └── service/         Business logic · AI Chatbot · Try-On · Email · Storage
│
├── src/main/resources/
│   ├── application.properties
│   ├── templates/
│   │   ├── layout/      base.html · admin.html
│   │   ├── auth/        login · register · forgot/reset password
│   │   ├── admin/       dashboard · products · categories · orders · users · coupons
│   │   └── shop/        home · products · detail · cart · checkout · orders · profile · tryon
│   └── static/          tailwind.css · admin-spa.js · images
│
├── python-tryon-server/ FastAPI bridge · OOTDiffusion inference
├── docs/
│   ├── features.md      Danh sách tính năng đầy đủ
│   └── changelog.md     Lịch sử thay đổi (BEFORE/AFTER)
└── pom.xml
```

---

## Database Schema (tóm tắt)

```
User ──── Role
 │
 ├── Order ──── OrderItem ──── ProductVariant ──── Product ──── ProductImage
 │      │                                              │
 │      └── Coupon ◄── UserCoupon                     └── Category/SubCategory
 │
 ├── WishlistItem ──── Product
 ├── Review ──── Product
 ├── Notification
 └── Address

Product ──── GarmentType (Try-On)
          └── SizeType
```

**26 entities** · `spring.jpa.hibernate.ddl-auto=update` (tự tạo/cập nhật bảng)

---

## Virtual Try-On — Kiến trúc

```
User Upload ──► Java API ──► Python FastAPI (port 8081)
                                    │
                        ┌───────────┼───────────┐
                        ▼           ▼           ▼
                  OOTDiffusion  IDM-VTON    Mock Mode
                  (primary)    (fallback)  (dev only)
                        │
                        └──► Result PNG ──► Auto-delete sau 5 phút
```

- **OOTDiffusion**: xử lý full outfit (áo + quần) trong single session — không cần compositing
- **IDM-VTON**: fallback khi OOTDiffusion không khả dụng
- **Mock mode**: trả ảnh giả ngay lập tức, không cần GPU

---

## AI Chatbot — Cách hoạt động

```
User: "Tìm áo hoodie dưới 300k màu đen"
         │
         ▼
  AiChatbotService
  ├── Parse giá (regex: "dưới 300k" → maxPrice=300000)
  ├── Parse category ("hoodie" → SubCategory lookup)
  ├── Parse màu ("màu đen" → color filter)
  ├── Query ProductService với filters
  └── Gửi context + câu hỏi → Ollama LLaMA 3.2
         │
         ▼
  Response: danh sách SP + tư vấn tự nhiên
```

---

## Security

- **Web chain**: Form login + Spring Session JDBC · CSRF bật
- **API chain**: JWT HS256 · Stateless · CSRF tắt
- **Headers**: `X-Frame-Options: DENY` · `HSTS 1 năm` · `Content-Type-Options`
- **Upload**: Validate magic bytes (không tin đuôi file) · chống path traversal
- **Rate limit**: Login rate limiting chống brute-force
- **Actuator**: `/actuator/health` public · `/actuator/**` chỉ ADMIN

---

## Các trang chính

| URL | Mô tả |
|-----|-------|
| `/` | Trang chủ |
| `/products` | Tất cả sản phẩm |
| `/products/{category}` | SP theo danh mục |
| `/product/{slug}` | Chi tiết sản phẩm |
| `/cart` | Giỏ hàng |
| `/checkout` | Thanh toán |
| `/my-orders` | Đơn hàng của tôi |
| `/wishlist` | Yêu thích |
| `/tryon-studio` | Thử đồ ảo |
| `/profile` | Hồ sơ cá nhân |
| `/admin` | Trang quản trị |
| `/swagger-ui.html` | API docs |

---

## Biến môi trường

| Biến | Mặc định | Mô tả |
|------|---------|-------|
| `MAIL_USERNAME` | nguyennhatminh1811@gmail.com | Gmail gửi email |
| `MAIL_PASSWORD` | *(app password)* | Gmail App Password |
| `JWT_SECRET` | *(default local key)* | JWT signing key (≥32 ký tự) |
| `APP_PUBLIC_BASE_URL` | http://localhost:8080 | Base URL public |
| `CHATBOT_AI_ENABLED` | true | Bật/tắt AI chatbot |
| `OLLAMA_BASE_URL` | http://localhost:11434 | Ollama server URL |
| `OLLAMA_MODEL` | llama3.2:3b | Model Ollama |
| `TRYON_PYTHON_URL` | http://localhost:8081 | Python Try-On server |
| `MOCK_INFERENCE` | false | Mock Try-On (không cần GPU) |
| `GPU_ID` | 0 | GPU index cho inference |

---

## Đóng góp & Phát triển

```bash
# Chạy tests
./mvnw test

# Build JAR
./mvnw clean package -DskipTests

# Chạy với profile production
./mvnw spring-boot:run -Dspring-boot.run.profiles=production
```

**Ghi lại mọi thay đổi** vào [docs/changelog.md](docs/changelog.md) theo format BEFORE/AFTER.

---

## Roadmap

- [ ] Thanh toán online (SePay / VietQR)
- [ ] Real-time notification (SSE)
- [ ] Full-text search (MySQL FULLTEXT)
- [ ] Cải thiện UI trang chủ (hero banner, slider)
- [ ] Review có ảnh đính kèm
- [ ] Hệ thống mã giới thiệu (referral)

---

## Nhóm phát triển

**Đồ án liên ngành 2026 — K17 — Nhóm N01**

---

*Xem chi tiết tính năng: [docs/features.md](docs/features.md)*  
*Lịch sử thay đổi: [docs/changelog.md](docs/changelog.md)*
