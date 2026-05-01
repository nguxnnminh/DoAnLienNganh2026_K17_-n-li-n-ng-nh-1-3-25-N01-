package com.shop.clothingstore.controller.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shop.clothingstore.service.TryOnService;

/**
 * REST API for the user-facing Virtual Try-On feature.
 *
 * All try-on files (person uploads, results) are EPHEMERAL:
 * - Person images are deleted immediately after the try-on is generated
 * - Result images are auto-deleted after 5 minutes (enough time to view)
 */
@RestController
@RequestMapping("/api/tryon")
public class TryOnApiController {

    private static final Logger log = LoggerFactory.getLogger(TryOnApiController.class);

    /** How long to keep result images before auto-deleting (minutes) */
    private static final int RESULT_TTL_MINUTES = 5;

    private final TryOnService tryOnService;
    private final Path personUploadDir;
    private final Path resultDir;

    /** Daemon scheduler for deferred file cleanup */
    private final ScheduledExecutorService cleaner =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "tryon-cleaner");
                t.setDaemon(true);
                return t;
            });

    public TryOnApiController(
            TryOnService tryOnService,
            @Value("${upload.dir:src/main/resources/static/images}") String baseUploadDir) {
        this.tryOnService = tryOnService;
        this.personUploadDir = Paths.get(baseUploadDir, "tryon-persons");
        this.resultDir = Paths.get(baseUploadDir, "tryon-results");
    }

    /**
     * Upload a person (full-body) image for try-on.
     * Returns a personId that can be reused across multiple try-on requests.
     */
    @PostMapping("/upload-person")
    public ResponseEntity<?> uploadPersonImage(@RequestParam("personImage") MultipartFile personImage) {

        if (personImage.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Vui lòng chọn ảnh chân dung."));
        }

        try {
            Files.createDirectories(personUploadDir);

            String ext = "jpg";
            String original = personImage.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
            }
            String personId = UUID.randomUUID().toString();
            String filename = personId + "." + ext;
            Path filePath = personUploadDir.resolve(filename);

            Files.copy(personImage.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Person image uploaded: {} ({}KB)", filename, personImage.getSize() / 1024);

            return ResponseEntity.ok(Map.of(
                    "personId", personId,
                    "filename", filename,
                    "url", "/images/tryon-persons/" + filename
            ));

        } catch (IOException e) {
            log.error("Failed to save person image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi lưu ảnh: " + e.getMessage()));
        }
    }

    /**
     * Generate a virtual try-on image.
     * After generation, the person image is deleted immediately.
     * The result image is auto-deleted after RESULT_TTL_MINUTES.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateTryOn(
            @RequestParam("personId") String personId,
            @RequestParam("productId") Long productId) {

        // Find the person image
        Path personImage = null;

        try {
            if (Files.exists(personUploadDir)) {
                var match = Files.list(personUploadDir)
                        .filter(p -> p.getFileName().toString().startsWith(personId))
                        .findFirst();
                if (match.isPresent()) {
                    personImage = match.get();
                }
            }
        } catch (IOException e) {
            log.error("Error looking for person image", e);
        }

        if (personImage == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ảnh chân dung không tìm thấy. Vui lòng upload lại."));
        }

        final Path personImageToClean = personImage;

        try {
            byte[] resultBytes = tryOnService.generateTryOn(personImage, productId);

            // Save result to static files for display
            Files.createDirectories(resultDir);
            String resultFilename = "result_" + productId + "_" + System.currentTimeMillis() + ".png";
            Path resultPath = resultDir.resolve(resultFilename);
            Files.write(resultPath, resultBytes);

            String resultUrl = "/images/tryon-results/" + resultFilename;
            log.info("Try-on result saved: {}", resultUrl);

            // ── CLEANUP: delete person image immediately ──
            deleteQuietly(personImageToClean);

            // ── CLEANUP: schedule result deletion after TTL ──
            scheduleDelete(resultPath, RESULT_TTL_MINUTES);

            return ResponseEntity.ok(Map.of(
                    "resultUrl", resultUrl,
                    "productId", productId
            ));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Try-on generation failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Không thể tạo ảnh thử đồ. " + e.getMessage()));
        }
    }

    /**
     * Check Python bridge health status.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(tryOnService.checkHealth());
    }

    /**
     * Generate a full outfit try-on with multiple garments (chained sequentially).
     * Applies garments in order: top → bottom → accessories.
     */
    @PostMapping("/generate-outfit")
    public ResponseEntity<?> generateOutfitTryOn(
            @RequestParam("personId") String personId,
            @RequestParam("productIds") java.util.List<Long> productIds) {

        if (productIds == null || productIds.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Vui lòng chọn ít nhất 1 sản phẩm."));
        }

        // Find the person image
        Path personImage = null;
        try {
            if (Files.exists(personUploadDir)) {
                var match = Files.list(personUploadDir)
                        .filter(p -> p.getFileName().toString().startsWith(personId))
                        .findFirst();
                if (match.isPresent()) personImage = match.get();
            }
        } catch (IOException e) {
            log.error("Error looking for person image", e);
        }

        if (personImage == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ảnh chân dung không tìm thấy. Vui lòng upload lại."));
        }

        final Path personImageToClean = personImage;

        try {
            byte[] resultBytes = tryOnService.generateOutfitTryOn(personImage, productIds);

            Files.createDirectories(resultDir);
            String resultFilename = "outfit_" + System.currentTimeMillis() + ".png";
            Path resultPath = resultDir.resolve(resultFilename);
            Files.write(resultPath, resultBytes);

            String resultUrl = "/images/tryon-results/" + resultFilename;
            log.info("Outfit try-on result saved: {}", resultUrl);

            // Cleanup
            deleteQuietly(personImageToClean);
            scheduleDelete(resultPath, RESULT_TTL_MINUTES);

            return ResponseEntity.ok(Map.of(
                    "resultUrl", resultUrl,
                    "productIds", productIds
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Outfit try-on generation failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Không thể tạo ảnh thử đồ. " + e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
            log.debug("Cleaned up temp file: {}", path.getFileName());
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", path, e);
        }
    }

    private void scheduleDelete(Path path, int minutes) {
        cleaner.schedule(() -> deleteQuietly(path), minutes, TimeUnit.MINUTES);
        log.debug("Scheduled deletion of {} in {} min", path.getFileName(), minutes);
    }
}

