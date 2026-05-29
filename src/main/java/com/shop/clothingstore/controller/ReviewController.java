package com.shop.clothingstore.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.service.ReviewService;
import com.shop.clothingstore.service.UserService;
import com.shop.clothingstore.service.storage.FileStorageService;

@Controller
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB / ảnh
    private static final int MAX_IMAGES = 5;
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final String REVIEW_IMAGE_FOLDER = "review-images";

    private final ReviewService reviewService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public ReviewController(ReviewService reviewService, UserService userService,
            FileStorageService fileStorageService) {
        this.reviewService = reviewService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/reviews/{orderItemId}")
    public String createReview(
            @PathVariable Long orderItemId,
            @RequestParam double rating,
            @RequestParam String comment,
            @RequestParam(name = "images", required = false) MultipartFile[] images,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        try {
            // Upload ảnh (nếu có) trước khi tạo review
            List<String> imageUrls = uploadImages(images);

            Long orderId = reviewService.createReview(user.getId(), orderItemId, rating, comment, imageUrls);

            redirectAttributes.addFlashAttribute("success", "Review submitted successfully!");
            return "redirect:/orders/" + orderId;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/my-orders";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/my-orders";
        }
    }

    /**
     * Validate + lưu ảnh review qua FileStorageService. Trả về danh sách URL công khai.
     * Bỏ qua phần tử rỗng; chặn file quá lớn / sai định dạng.
     */
    private List<String> uploadImages(MultipartFile[] images) {
        List<String> urls = new ArrayList<>();
        if (images == null) {
            return urls;
        }
        int count = 0;
        for (MultipartFile img : images) {
            if (img == null || img.isEmpty()) {
                continue;
            }
            if (count >= MAX_IMAGES) {
                break;
            }
            if (img.getSize() > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException("Mỗi ảnh tối đa 5MB.");
            }
            String ext = extension(img.getOriginalFilename());
            if (!ALLOWED_EXT.contains(ext)) {
                throw new IllegalArgumentException("Chỉ chấp nhận ảnh jpg, png, webp.");
            }
            try {
                urls.add(fileStorageService.upload(img, REVIEW_IMAGE_FOLDER));
                count++;
            } catch (IOException e) {
                log.error("Failed to upload review image", e);
                throw new IllegalStateException("Không thể lưu ảnh đánh giá. Vui lòng thử lại.");
            }
        }
        return urls;
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
