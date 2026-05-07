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
 * <h3>Key changes vs v2</h3>
 * <ul>
 * <li>Delegates to OOTDiffusion single-session pipeline via Python bridge
 * v3</li>
 * <li>Person image is deleted AFTER the synchronous call completes — no race
 * condition with async path</li>
 * <li>Outfit endpoint sends one /tryon/outfit request (no chaining on Java
 * side)</li>
 * <li>CompletionException is unwrapped to surface the root cause to the
 * user</li>
 * </ul>
 *
 * <p>
 * All try-on files are EPHEMERAL: person images are deleted immediately after
 * generation; result images auto-delete after RESULT_TTL_MINUTES.</p>
 */
@RestController
@RequestMapping("/api/tryon")
public class TryOnApiController {

    private static final Logger log = LoggerFactory.getLogger(TryOnApiController.class);

    /**
     * How long to keep result images before auto-deleting (minutes)
     */
    private static final int RESULT_TTL_MINUTES = 5;

    private final TryOnService tryOnService;
    private final Path personUploadDir;
    private final Path resultDir;

    /**
     * Daemon scheduler for deferred result file cleanup
     */
    private final ScheduledExecutorService cleaner
            = Executors.newSingleThreadScheduledExecutor(r -> {
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

    // ── Upload person photo ───────────────────────────────
    /**
     * Upload a full-body person photo and receive a personId for subsequent
     * try-on calls. The person photo is retained only until the next generate
     * call then immediately deleted.
     */
    @PostMapping("/upload-person")
    public ResponseEntity<?> uploadPersonImage(@RequestParam("personImage") MultipartFile personImage) {

        if (personImage.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please select a person photo to upload."));
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

            Files.copy(personImage.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Person image uploaded: {} ({}KB)", filename, personImage.getSize() / 1024);

            return ResponseEntity.ok(Map.of(
                    "personId", personId,
                    "filename", filename,
                    "url", "/images/tryon-persons/" + filename
            ));

        } catch (IOException e) {
            log.error("Failed to save person image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save image: " + e.getMessage()));
        }
    }

    // ── Single-garment try-on ─────────────────────────────
    /**
     * Generate a single-garment virtual try-on image. Person image is deleted
     * immediately after generation (success OR failure).
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateTryOn(
            @RequestParam("personId") String personId,
            @RequestParam("productId") Long productId) {

        Path personImage = findPersonImage(personId);
        if (personImage == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Person photo not found. Please upload again."));
        }

        try {
            // generateTryOn is synchronous — person delete in finally is safe
            byte[] resultBytes = tryOnService.generateTryOn(personImage, productId);
            return saveAndReturnResult(resultBytes, "result_" + productId + "_" + System.currentTimeMillis());

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.concurrent.CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Try-on generation failed", cause);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to generate try-on image. " + cause.getMessage()));
        } catch (IOException e) {
            log.error("Try-on generation failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to generate try-on image. " + e.getMessage()));
        } finally {
            // Person image is deleted AFTER synchronous call completes — no race condition
            deleteQuietly(personImage);
        }
    }

    // ── Full outfit try-on (top + bottom) ─────────────────
    /**
     * Generate a full outfit try-on (top + bottom combined) using the new
     * dual-inference + compositing pipeline.
     *
     * <p>
     * Unlike the old approach, this does NOT chain AI outputs. The Python
     * bridge runs IDM-VTON twice on the original person photo and uses clothing
     * segmentation to merge the results.</p>
     *
     * @param personId the person photo UUID from /upload-person
     * @param topProductId ID of an UPPER_BODY try-on enabled product
     * @param bottomProductId ID of a LOWER_BODY try-on enabled product
     */
    @PostMapping("/generate-outfit")
    public ResponseEntity<?> generateOutfitTryOn(
            @RequestParam("personId") String personId,
            @RequestParam("topProductId") Long topProductId,
            @RequestParam("bottomProductId") Long bottomProductId) {

        Path personImage = findPersonImage(personId);
        if (personImage == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Person photo not found. Please upload again."));
        }

        try {
            // Single /tryon/outfit call → OOTDiffusion two-step session on Python bridge
            byte[] resultBytes = tryOnService.generateOutfitTryOn(
                    personImage, topProductId, bottomProductId);
            return saveAndReturnResult(resultBytes, "outfit_" + System.currentTimeMillis());

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (java.util.concurrent.CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Outfit try-on generation failed", cause);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to generate try-on image. " + cause.getMessage()));
        } catch (IOException e) {
            log.error("Outfit try-on generation failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to generate try-on image. " + e.getMessage()));
        } finally {
            // Always clean up person image after sync call completes
            deleteQuietly(personImage);
        }
    }

    // ── Health check ──────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(tryOnService.checkHealth());
    }

    // ── Private helpers ───────────────────────────────────
    private Path findPersonImage(String personId) {
        try {
            if (Files.exists(personUploadDir)) {
                return Files.list(personUploadDir)
                        .filter(p -> p.getFileName().toString().startsWith(personId + "."))
                        .findFirst()
                        .orElse(null);
            }
        } catch (IOException e) {
            log.error("Error scanning person upload dir", e);
        }
        return null;
    }

    private ResponseEntity<?> saveAndReturnResult(byte[] resultBytes, String baseName)
            throws IOException {

        Files.createDirectories(resultDir);
        String resultFilename = baseName + ".png";
        Path resultPath = resultDir.resolve(resultFilename);
        Files.write(resultPath, resultBytes);

        String resultUrl = "/images/tryon-results/" + resultFilename;
        log.info("Try-on result saved: {}", resultUrl);

        scheduleDelete(resultPath, RESULT_TTL_MINUTES);

        return ResponseEntity.ok(Map.of("resultUrl", resultUrl));
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
            log.debug("Cleaned up: {}", path.getFileName());
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", path, e);
        }
    }

    private void scheduleDelete(Path path, int minutes) {
        cleaner.schedule(() -> deleteQuietly(path), minutes, TimeUnit.MINUTES);
        log.debug("Scheduled deletion of {} in {} min", path.getFileName(), minutes);
    }
}
