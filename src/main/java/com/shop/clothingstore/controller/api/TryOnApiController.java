package com.shop.clothingstore.controller.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shop.clothingstore.service.TryOnService;

@RestController
@RequestMapping("/api/tryon")
public class TryOnApiController {

    private static final Logger log = LoggerFactory.getLogger(TryOnApiController.class);

    private static final long MAX_PERSON_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MAGIC_BYTES = Set.of("ffd8ff", "89504e47", "52494646");

    private final TryOnService tryOnService;
    private final Path personUploadDir;

    public TryOnApiController(
            TryOnService tryOnService,
            @Value("${upload.dir:src/main/resources/static/images}") String baseUploadDir) {
        this.tryOnService = tryOnService;
        this.personUploadDir = Paths.get(baseUploadDir, "tryon-persons");
    }

    @PostMapping("/upload-person")
    public ResponseEntity<?> uploadPersonImage(@RequestParam("personImage") MultipartFile personImage) {
        if (personImage.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select a person photo to upload."));
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

            Files.copy(personImage.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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

    @PostMapping("/generate")
    public ResponseEntity<?> generateTryOn(
            @RequestParam("personId") String personId,
            @RequestParam("productId") Long productId) {

        Path personImage = findPersonImage(personId);
        if (personImage == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Person photo not found. Please upload again."));
        }

        try {
            byte[] result = tryOnService.generateTryOn(personImage, productId);
            log.info("Try-on done | product={} | {}KB", productId, result.length / 1024);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(result);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Try-on generation failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to generate try-on image. " + e.getMessage()));
        } finally {
            deleteQuietly(personImage);
        }
    }

    @PostMapping("/generate-outfit")
    public ResponseEntity<?> generateOutfitTryOn(
            @RequestParam("personId") String personId,
            @RequestParam("topProductId") Long topProductId,
            @RequestParam("bottomProductId") Long bottomProductId) {

        Path personImage = findPersonImage(personId);
        if (personImage == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Person photo not found. Please upload again."));
        }

        try {
            byte[] result = tryOnService.generateOutfitTryOn(personImage, topProductId, bottomProductId);
            log.info("Outfit try-on done | top={} bottom={} | {}KB", topProductId, bottomProductId, result.length / 1024);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(result);

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Outfit try-on generation failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Failed to generate try-on image. " + e.getMessage()));
        } finally {
            deleteQuietly(personImage);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(tryOnService.checkHealth());
    }

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

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", path, e);
        }
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
            throw new IllegalArgumentException("Invalid file signature.");
        }
        return ext;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
