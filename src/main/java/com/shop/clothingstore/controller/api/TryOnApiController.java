package com.shop.clothingstore.controller.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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

import jakarta.annotation.PreDestroy;

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

    private static final long MAX_PERSON_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MAGIC_BYTES = Set.of(
            "ffd8ff",
            "89504e47",
            "52494646"
    );

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
            String ext = validatePersonImage(personImage);
            Files.createDirectories(personUploadDir);

            String personId = UUID.randomUUID().toString();
            String filename = personId + "." + ext;
            Path filePath = personUploadDir.resolve(filename).toAbsolutePath().normalize();
            if (!filePath.startsWith(personUploadDir.toAbsolutePath().normalize())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid file path."));
            }

            Files.copy(personImage.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Person image uploaded: {} ({}KB)", filename, personImage.getSize() / 1024);

            return ResponseEntity.ok(Map.of(
                    "personId", personId,
                    "filename", filename,
                    "url", "/images/tryon-persons/" + filename
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
        if (personId == null || !personId.matches("^[0-9a-fA-F-]{36}$")) {
            return null;
        }
        try {
            if (Files.exists(personUploadDir)) {
                try (Stream<Path> files = Files.list(personUploadDir)) {
                    return files
                            .filter(p -> p.getFileName().toString().startsWith(personId + "."))
                            .findFirst()
                            .orElse(null);
                }
            }
        } catch (IOException e) {
            log.error("Error scanning person upload dir", e);
        }
        return null;
    }

    private ResponseEntity<?> saveAndReturnResult(byte[] resultBytes, String baseName)
            throws IOException {
        if (resultBytes == null || resultBytes.length == 0) {
            throw new IOException("Try-on service returned an empty image.");
        }

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

    @PreDestroy
    void shutdownCleaner() {
        cleaner.shutdown();
    }

    private String validatePersonImage(MultipartFile image) throws IOException {
        if (image.getSize() > MAX_PERSON_IMAGE_SIZE) {
            throw new IllegalArgumentException("File too large. Maximum 5MB.");
        }

        String original = image.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file format. Allowed: " + ALLOWED_EXTENSIONS);
        }

        String magic = bytesToHex(image.getInputStream().readNBytes(8)).toLowerCase(Locale.ROOT);
        boolean validMagic = ALLOWED_MAGIC_BYTES.stream().anyMatch(magic::startsWith);
        if (!validMagic) {
            throw new IllegalArgumentException("Invalid file signature. The file extension may have been spoofed.");
        }
        return ext;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
