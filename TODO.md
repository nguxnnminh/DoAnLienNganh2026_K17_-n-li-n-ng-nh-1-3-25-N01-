# TODO - Fix Chatbot Product Link

## Steps
- [x] 1. Understand the bug: chatbot links to `/products/{productSlug}` which conflicts with `/products/{categorySlug}`
- [x] 2. Create plan and get user approval
- [x] 3. Edit `src/main/resources/templates/layout/base.html` — change chatbot product href from `/products/${p.slug}` to `/product/${p.slug}`
- [ ] 4. Restart app (`mvn spring-boot:run`) and verify clicking best-seller products in chatbot works


