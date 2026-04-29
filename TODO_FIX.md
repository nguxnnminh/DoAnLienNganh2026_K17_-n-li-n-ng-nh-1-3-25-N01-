# PRODUCTION HARDENING - Fix ALL 21 Audit Issues
System health: 42/100 → Target 100/100 (Production Ready)

## APPROVED PLAN EXECUTION (Critical → Medium → Minor)

### 🔥 PHASE 1: CRITICAL SECURITY ✅ (Tests fixed)

- [x] 1.1 application.properties: Remove JWT fallback, secure DB config (env-only)
- [x] 1.2 AuthController.java: Fix reset URL (app.public-base-url) - FIXED (import @Value)
- [x] 1.3 Order.java: Add @Version optimistic locking
- [x] 1.4 OrderService.java: JPA auto-handles version (no code change needed)
- [x] 1.5 CheckoutService.java: Reject qty<=0 items before persist
- [x] 1.6 CSRF: Added tokens to ALL admin POST forms + logout (create/edit products, orders status, users edit/delete, dashboard export, both layouts)

### ⚠️ PHASE 2: MEDIUM (Next)
- [ ] Checkout deadlock fix (sort variants in CheckoutService)
- [ ] CouponService null-safety (usageCount)
- [ ] Coupon discountType validation
- [ ] Review product/variant mismatch fix
- [ ] Rate limit /login endpoint + IP spoofing fix
- [ ] Generic auth error (prevent email enumeration)
- [ ] GlobalExceptionHandler admin redirect fix
- [ ] ProductService primary image invariant
- [ ] Mockito test stubbing fixes

### ℹ️ PHASE 3: MINOR (Later)
- [ ] StorageService cleanup orphans
- [ ] ReportService BigDecimal (no double)
- [ ] Disable spring.jpa.open-in-view
- [ ] UI/backend pw policy align + remember-me

### ⚠️ PHASE 2: MEDIUM (After Critical)
- [ ] Checkout deadlock fix (sort variants)
- [ ] CouponService null-safety
- [ ] Coupon discountType validation
- [ ] Review product/variant fix
- [ ] Rate limit /login endpoint
- [ ] Fix IP spoofing in rate limiter
- [ ] Generic auth error (no email enum)
- [ ] Admin redirect fix
- [ ] Product images single-primary
- [ ] Fix Mockito test stubbing

### ℹ️ PHASE 3: MINOR
- [ ] File cleanup on delete
- [ ] BigDecimal end-to-end
- [ ] Disable open-in-view
- [ ] Fix remember-me / pw policy UI

### ✅ VALIDATION
- [ ] mvnw test (ALL pass)
- [ ] mvnw compile (no warnings)
- [ ] Startup fails w/o JWT_SECRET/DB env
- [ ] CSRF forms work (no 403)
- [ ] Concurrency safe (manual test)
- [ ] Full user/admin/guest flows

## Progress Log
*(Auto-updated after each step)*

**ENV Notes:** app.public-base-url=http://localhost:8080 (dev), override for prod.
