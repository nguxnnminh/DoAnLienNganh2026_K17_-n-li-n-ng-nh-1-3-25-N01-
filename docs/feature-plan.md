# Kế hoạch triển khai 5 tính năng mới

> Ngày lập: 2026-05-29. Trạng thái cập nhật liên tục ở cột "Status".
> Nguyên tắc: clone/pull về là chạy được (schema tự apply, có runner backfill), mỗi tính năng có test.

| # | Tính năng | Status |
|---|-----------|--------|
| 1 | UI trang chủ (hero + slider) | ✅ Done (E2E verified) |
| 2 | Full-text search | ✅ Done (E2E verified) |
| 3 | Review có ảnh | ✅ Done |
| 4 | SSE notification | ✅ Done (E2E verified) |
| 5 | Referral system | ✅ Done (E2E + 8 unit test) |

> **Hoàn thành 2026-05-29.** Tất cả compile sạch, 81 test PASS (gồm 8 test referral mới),
> code-review đã quét + fix 6 lỗi HIGH (xem [work-log.md](work-log.md) mục #25b–#25g).

---

## F1 — UI trang chủ: hero banner + slider

**Mục tiêu:** Trang chủ có hero banner ảnh slider tự chạy, ấn tượng hơn.

**Phạm vi:**
- `templates/shop/home.html`: thêm hero section + slider (3-4 slide auto-rotate, nút prev/next, dots).
- CSS + JS thuần (không thư viện ngoài để clone về chạy ngay).
- Dùng ảnh sẵn có trong `static/images/`.

**Test:** load `/` → HTTP 200, hero render, slider chuyển slide.

---

## F2 — Full-text search (MySQL/MariaDB FULLTEXT)

**Mục tiêu:** Tìm kiếm nhanh + autocomplete gợi ý từ khóa.

**Phạm vi:**
- Runner `FullTextIndexInitializer`: tạo `FULLTEXT INDEX` trên `products(name, description)` nếu chưa có (idempotent, chạy mọi máy).
- `ProductRepository`: query `MATCH...AGAINST` (BOOLEAN MODE) + fallback LIKE.
- `ProductService.searchFullText()` + endpoint autocomplete `/api/products/suggest?q=`.
- Wire vào ô tìm kiếm shop (autocomplete dropdown).

**Test:** gọi `/api/products/suggest?q=ao` → trả gợi ý; tìm kiếm ra kết quả đúng.

---

## F3 — Review có ảnh đính kèm

**Mục tiêu:** Khách đính kèm ảnh khi đánh giá sản phẩm.

**Phạm vi:**
- `Review` entity: thêm `@ElementCollection List<String> imageUrls` (bảng phụ `review_images`).
- `ReviewController.createReview`: nhận `MultipartFile[] images`, lưu qua `FileStorageService`, gắn URL.
- `ReviewService`: lưu imageUrls.
- `product-detail.html`: form review thêm input file; hiển thị ảnh trong mỗi review.

**Test:** POST review kèm ảnh → ảnh lưu + hiển thị.

---

## F4 — SSE real-time notification

**Mục tiêu:** Admin/user nhận thông báo real-time (đơn mới, đổi trạng thái).

**Phạm vi:**
- `SseNotificationController` (WEB chain, session-auth — KHÔNG để dưới /api/ vì /api/ stateless): `GET /notifications/stream` trả `SseEmitter`.
- `NotificationService`: registry emitter theo userId; method `pushToUser()` + `pushToAdmins()`.
- Wire: khi đặt đơn / đổi trạng thái → push SSE.
- Frontend: `EventSource` trong `layout/base.html` + `layout/admin.html` → toast + badge.

**Test:** mở stream, tạo đơn → nhận event.

---

## F5 — Hệ thống mã giới thiệu (Referral)

**Mục tiêu:** Mỗi user có mã giới thiệu; người được giới thiệu + người giới thiệu nhận coupon khi đơn đầu hoàn tất.

**Phạm vi:**
- `User` entity: `referralCode` (unique), `referredById` (ai giới thiệu), `referralRewarded` (đã thưởng chưa).
- Runner backfill `referralCode` cho user cũ.
- Đăng ký với `?ref=CODE` → lưu `referredById` (web + API).
- `CheckoutService`: khi đơn đầu của người được giới thiệu hoàn tất → tặng coupon cho cả 2 (qua `CouponService`).
- `profile.html`: hiển thị mã + link giới thiệu.

**Test:** đăng ký có ref → liên kết đúng; đơn đầu → cả 2 nhận coupon.

---

## Tổng kết test
- Test tích hợp (MockMvc / @SpringBootTest + H2) cho từng tính năng.
- Test HTTP thực tế trên app đang chạy (MariaDB).
- Code-review agent quét toàn bộ trước khi chốt.
