# Bug Fixes & Feature Update TODO

## Approved Plan (with user revisions)
1. Fix JSON serialization infinite recursion in entities
2. Fix repository inheritance inconsistency
3. Update product-detail.html: REMOVE review form, ADD star rating filter, FIX wishlist button, FIX Vietnamese font
4. Add WishlistApiControllerTest
5. Add ProductApiControllerTest
6. Run full test suite

## Progress
- [x] 1. WishlistItem.java - Add @JsonIgnore to user & product
- [x] 2. ProductImage.java - Add @JsonIgnore to product
- [x] 3. WishlistItemRepository.java - Extend BaseRepository
- [x] 4. base.html - Add Vietnamese font support (Be Vietnam Pro fallback)
- [x] 5. product-detail.html - Remove review form, add star filter JS, fix wishlist button
- [x] 6. WishlistApiControllerTest.java - New test file (6 tests, all pass)
- [x] 7. ProductApiControllerTest.java - New test file (5 tests, all pass)
- [x] 8. ApiExceptionHandler.java - Fix 401 response for Unauthorized errors
- [x] 9. Run mvnw test & verify all pass

## Test Results
- ClothingstoreApplicationTests: 1 passed
- ChatbotApiControllerTest: 5 passed
- ChatbotServiceTest: 29 passed
- ProductApiControllerTest: 5 passed
- WishlistApiControllerTest: 6 passed
- TOTAL: 46 tests, 0 failures, 0 errors
