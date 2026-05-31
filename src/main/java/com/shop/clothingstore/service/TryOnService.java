package com.shop.clothingstore.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.shop.clothingstore.entity.GarmentType;
import com.shop.clothingstore.entity.Product;
import com.shop.clothingstore.repository.ProductRepository;
import com.shop.clothingstore.service.storage.FileStorageService;

/**
 * Orchestrates the Virtual Try-On workflow via the Python bridge (port 8081):
 * Replicate IDM-VTON (cloud) with automatic fallback to local CatVTON on GPU.
 * Full-outfit requests run two passes on the original person and composite them.
 */
@Service
public class TryOnService {

    private static final Logger log = LoggerFactory.getLogger(TryOnService.class);

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;   // timeout-aware (AsyncConfig)
    private final String baseUploadDir;

    @Value("${tryon.python.url:http://localhost:8081}")
    private String pythonServerUrl;

    public TryOnService(
            ProductRepository productRepository,
            FileStorageService fileStorageService,
            @Qualifier("tryOnRestTemplate") RestTemplate restTemplate,
            @Value("${upload.dir:src/main/resources/static/images}") String baseUploadDir) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
        this.restTemplate = restTemplate;
        this.baseUploadDir = baseUploadDir;
    }

    // ───────────────────────────────────────────────────────────────────
    // ADMIN: Preprocess garment image & enable try-on for a product
    // ───────────────────────────────────────────────────────────────────
    /**
     * Preprocess the garment image through the Python bridge (rembg +
     * normalize), save the result, and enable try-on for the product.
     *
     * If the Python bridge is unreachable, falls back to saving the raw garment
     * as-is so the admin flow never fully breaks.
     */
    @CacheEvict(value = "tryOnProducts", allEntries = true)
    public Product preprocessAndEnable(Long productId,
            MultipartFile garmentImage,
            GarmentType garmentType) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        String garmentUrl;
        try {
            garmentUrl = _callPreprocessGarment(garmentImage);
            log.info("Garment preprocessed via Python bridge → {}", garmentUrl);
        } catch (Exception e) {
            log.warn("Python bridge preprocessing unavailable ({}), saving raw garment", e.getMessage());
            garmentUrl = fileStorageService.upload(garmentImage, "tryon-garments");
        }

        product.setGarmentProcessedUrl(garmentUrl);
        product.setGarmentType(garmentType != null ? garmentType : GarmentType.UPPER_BODY);
        product.setTryOnEnabled(true);

        Product saved = productRepository.save(product);
        log.info("Try-on enabled | product={} | garment={} | type={}",
                productId, garmentUrl, product.getGarmentType());
        return saved;
    }

    /**
     * Disable try-on for a product.
     */
    @CacheEvict(value = "tryOnProducts", allEntries = true)
    public void disableTryOn(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.setTryOnEnabled(false);
        productRepository.save(product);
        log.info("Try-on disabled | product={}", productId);
    }

    // ───────────────────────────────────────────────────────────────────
    // USER: Single-garment try-on (async)
    // ───────────────────────────────────────────────────────────────────
    /** Async single-garment try-on. Runs on the tryOnExecutor thread pool. */
    @Async("tryOnExecutor")
    public CompletableFuture<byte[]> generateTryOnAsync(Path personImagePath, Long productId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateTryOn(personImagePath, productId);
            } catch (IOException e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        });
    }

    /**
     * Synchronous single-garment try-on. Used internally and from the async
     * wrapper.
     */
    public byte[] generateTryOn(Path personImagePath, Long productId) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (!product.isTryOnEnabled() || product.getGarmentProcessedUrl() == null) {
            throw new IllegalStateException("Try-on is not enabled for this product");
        }

        Path garmentPath = resolveGarmentPath(product.getGarmentProcessedUrl());

        byte[] personBytes = Files.readAllBytes(personImagePath);
        byte[] garmentBytes = Files.readAllBytes(garmentPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("person_image", namedResource(personBytes, "person.jpg"));
        body.add("garment_image", namedResource(garmentBytes, "garment.png"));
        body.add("garment_description", product.getName());
        body.add("category", mapGarmentType(product.getGarmentType()));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Calling Python bridge | product={} | category={}", productId,
                    mapGarmentType(product.getGarmentType()));
            ResponseEntity<byte[]> response
                    = restTemplate.postForEntity(pythonServerUrl + "/tryon", request, byte[].class);

            byte[] result = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && result != null) {
                log.info("Try-on result received | {} bytes", result.length);
                return result;
            }
            throw new IOException("Python bridge returned: " + response.getStatusCode());

        } catch (RestClientException e) {
            log.error("Failed to reach Python bridge", e);
            throw new IOException("Cannot connect to Python Try-On server ("
                    + pythonServerUrl + "). Make sure the server is running.", e);
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // USER: Full outfit try-on — top + bottom via compositing (NOT chaining)
    // ───────────────────────────────────────────────────────────────────
    /**
     * Generate a full outfit try-on image (top + bottom combined).
     *
     * <p>
     * The Python bridge runs CatVTON twice using the ORIGINAL person image,
     * then composites the two results using clothing segmentation. This is the
     * correct approach — not sequential chaining.</p>
     *
     * @param personImagePath path to the uploaded person photo
     * @param topProductId ID of the top garment product (UPPER_BODY)
     * @param bottomProductId ID of the bottom garment product (LOWER_BODY)
     * @return bytes of the final composite PNG
     */
    /** Async full-outfit try-on (top + bottom). */
    @Async("tryOnExecutor")
    public CompletableFuture<byte[]> generateOutfitTryOnAsync(
            Path personImagePath, Long topProductId, Long bottomProductId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateOutfitTryOn(personImagePath, topProductId, bottomProductId);
            } catch (IOException e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        });
    }

    public byte[] generateOutfitTryOn(Path personImagePath,
            Long topProductId,
            Long bottomProductId) throws IOException {

        Product top = resolveEnabledProduct(topProductId);
        Product bottom = resolveEnabledProduct(bottomProductId);

        Path topPath = resolveGarmentPath(top.getGarmentProcessedUrl());
        Path bottomPath = resolveGarmentPath(bottom.getGarmentProcessedUrl());

        byte[] personBytes = Files.readAllBytes(personImagePath);
        byte[] topBytes = Files.readAllBytes(topPath);
        byte[] bottomBytes = Files.readAllBytes(bottomPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // New /tryon/outfit endpoint: separate top + bottom fields,
        // NOT a list (avoids the old comma-separated workaround).
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("person_image", namedResource(personBytes, "person.jpg"));
        body.add("top_garment_image", namedResource(topBytes, "top.png"));
        body.add("bottom_garment_image", namedResource(bottomBytes, "bottom.png"));
        body.add("top_description", top.getName());
        body.add("bottom_description", bottom.getName());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Calling Python bridge for outfit try-on | top={} | bottom={}",
                    topProductId, bottomProductId);
            ResponseEntity<byte[]> response
                    = restTemplate.postForEntity(pythonServerUrl + "/tryon/outfit", request, byte[].class);

            byte[] result = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && result != null) {
                log.info("Outfit try-on result received | {} bytes", result.length);
                return result;
            }
            throw new IOException("Python bridge returned: " + response.getStatusCode());

        } catch (RestClientException e) {
            log.error("Failed to reach Python bridge for outfit", e);
            throw new IOException("Cannot connect to Python Try-On server. " + e.getMessage(), e);
        }
    }

    /**
     * Legacy method: accepts a list of product IDs (order-dependent). Routes to
     * the new outfit method for 2 items (top + bottom), or falls back to
     * single-garment for 1 item. Preserved for backward compatibility with old
     * API controller calls.
     *
     * @deprecated Use generateOutfitTryOn(Path, Long, Long) directly.
     */
    @Deprecated
    public byte[] generateOutfitTryOn(Path personImagePath, List<Long> productIds) throws IOException {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("At least one product ID required");
        }
        if (productIds.size() == 1) {
            return generateTryOn(personImagePath, productIds.get(0));
        }
        // For list-based calls: treat first as top, second as bottom
        return generateOutfitTryOn(personImagePath, productIds.get(0), productIds.get(1));
    }

    // ───────────────────────────────────────────────────────────────────
    // PRODUCT LIST (cached)
    // ───────────────────────────────────────────────────────────────────
    @Cacheable("tryOnProducts")
    public java.util.List<Product> findAllTryOnEnabled() {
        return productRepository.findAllTryOnEnabled();
    }

    // ───────────────────────────────────────────────────────────────────
    // HEALTH CHECK
    // ───────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkHealth() {
        try {
            ResponseEntity<Map> response
                    = restTemplate.getForEntity(pythonServerUrl + "/health", Map.class);
            return response.getBody();
        } catch (Exception e) {
            return Map.of("status", "offline", "error", e.getMessage());
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // Private helpers
    // ───────────────────────────────────────────────────────────────────
    private Product resolveEnabledProduct(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (!p.isTryOnEnabled() || p.getGarmentProcessedUrl() == null) {
            throw new IllegalStateException("Try-on not enabled for product: " + p.getName());
        }
        return p;
    }

    private Path resolveGarmentPath(String garmentUrl) throws IOException {
        // URL format: /images/tryon-garments/xxx.png
        // File lives at: {upload.dir}/tryon-garments/xxx.png
        String relative = garmentUrl.replaceFirst("^/images/", "");
        Path path = Path.of(baseUploadDir, relative);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Garment image not found on disk: " + path);
        }
        return path;
    }

    /**
     * Call the Python bridge to preprocess a garment image (rembg + normalize).
     * Returns the public URL where the result was saved.
     */
    private String _callPreprocessGarment(MultipartFile garmentImage) throws IOException {
        byte[] bytes = garmentImage.getBytes();
        String originalName = garmentImage.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".png";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("garment_image", namedResource(bytes, "garment" + ext));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                pythonServerUrl + "/preprocess/garment", request, byte[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Preprocess endpoint returned: " + response.getStatusCode());
        }

        // Delegate to uploadBytes — no MockMultipartFile, no temp file, no test dependency.
        return fileStorageService.uploadBytes(response.getBody(), "garment_processed.png", "tryon-garments");
    }

    private static ByteArrayResource namedResource(byte[] bytes, String filename) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private static String mapGarmentType(GarmentType type) {
        if (type == null) {
            return "upper_body";
        }
        return switch (type) {
            case UPPER_BODY ->
                "upper_body";
            case LOWER_BODY ->
                "lower_body";
        };
    }

}
