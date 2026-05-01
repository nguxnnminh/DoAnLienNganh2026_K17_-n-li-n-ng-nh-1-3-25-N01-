package com.shop.clothingstore.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * Orchestrates the Virtual Try-On workflow:
 * <ul>
 *   <li>Admin: preprocess garment → enable try-on on product</li>
 *   <li>User:  generate try-on image (person + garment → result)</li>
 * </ul>
 *
 * Calls the Python Bridge server which forwards to HF Space (IDM-VTON).
 */
@Service
public class TryOnService {

    private static final Logger log = LoggerFactory.getLogger(TryOnService.class);

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;
    private final String baseUploadDir;

    @Value("${tryon.python.url:http://localhost:8081}")
    private String pythonServerUrl;

    public TryOnService(ProductRepository productRepository,
                        FileStorageService fileStorageService,
                        @Value("${upload.dir:src/main/resources/static/images}") String baseUploadDir) {
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
        this.baseUploadDir = baseUploadDir;
        this.restTemplate = new RestTemplate();
    }

    // ───────────────────────────────────────────────
    // ADMIN: Preprocess garment & enable try-on
    // ───────────────────────────────────────────────

    /**
     * Upload and save a garment image, then enable try-on for a product.
     */
    public Product preprocessAndEnable(Long productId,
                                       MultipartFile garmentImage,
                                       GarmentType garmentType) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // Save the garment image via the existing file storage service
        String garmentUrl = fileStorageService.upload(garmentImage, "tryon-garments");

        product.setGarmentProcessedUrl(garmentUrl);
        product.setGarmentType(garmentType != null ? garmentType : GarmentType.UPPER_BODY);
        product.setTryOnEnabled(true);

        Product saved = productRepository.save(product);
        log.info("Try-on enabled for product #{} | garment={} type={}",
                productId, garmentUrl, product.getGarmentType());

        return saved;
    }

    /**
     * Disable try-on for a product.
     */
    public void disableTryOn(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.setTryOnEnabled(false);
        productRepository.save(product);
        log.info("Try-on disabled for product #{}", productId);
    }

    // ───────────────────────────────────────────────
    // USER: Generate try-on image
    // ───────────────────────────────────────────────

    /**
     * Call the Python bridge to generate a try-on image.
     *
     * @param personImagePath  path to the user's uploaded person image
     * @param productId        the product to try on
     * @return byte array of the result PNG image
     */
    public byte[] generateTryOn(Path personImagePath, Long productId) throws IOException {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (!product.isTryOnEnabled() || product.getGarmentProcessedUrl() == null) {
            throw new IllegalStateException("Try-on is not enabled for this product");
        }

        // Resolve garment image path from its URL
        // URL format is /images/tryon-garments/xxx.png
        // File is at {upload.dir}/tryon-garments/xxx.png
        String garmentRelative = product.getGarmentProcessedUrl().replaceFirst("^/images/", "");
        Path garmentPath = Path.of(baseUploadDir, garmentRelative);

        if (!Files.exists(garmentPath)) {
            throw new IllegalStateException("Garment image not found: " + garmentPath);
        }

        // Build multipart request to Python bridge
        byte[] personBytes = Files.readAllBytes(personImagePath);
        byte[] garmentBytes = Files.readAllBytes(garmentPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("person_image", new ByteArrayResource(personBytes) {
            @Override public String getFilename() { return "person.jpg"; }
        });
        body.add("garment_image", new ByteArrayResource(garmentBytes) {
            @Override public String getFilename() { return "garment.png"; }
        });
        body.add("garment_description", product.getName());
        body.add("category", mapGarmentType(product.getGarmentType()));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Calling Python bridge for try-on: product #{}", productId);
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    pythonServerUrl + "/tryon", request, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Try-on result received: {} bytes", response.getBody().length);
                return response.getBody();
            } else {
                throw new IOException("Python bridge returned: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Failed to call Python bridge", e);
            throw new IOException("Không thể kết nối tới Python Try-On server. "
                    + "Hãy đảm bảo server đang chạy trên " + pythonServerUrl, e);
        }
    }

    /**
     * Check if the Python bridge is reachable.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> checkHealth() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    pythonServerUrl + "/health", Map.class);
            return response.getBody();
        } catch (Exception e) {
            return Map.of("status", "offline", "error", e.getMessage());
        }
    }

    /**
     * Generate a full outfit try-on with multiple garments (chained sequentially).
     *
     * @param personImagePath  path to the user's person image
     * @param productIds       list of product IDs to try on (applied in order)
     * @return byte array of the final result PNG image
     */
    public byte[] generateOutfitTryOn(Path personImagePath, java.util.List<Long> productIds) throws IOException {

        java.util.List<Product> products = new java.util.ArrayList<>();
        java.util.List<String> descriptions = new java.util.ArrayList<>();

        for (Long pid : productIds) {
            Product p = productRepository.findById(pid)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + pid));
            if (!p.isTryOnEnabled() || p.getGarmentProcessedUrl() == null) {
                throw new IllegalStateException("Try-on chưa bật cho sản phẩm: " + p.getName());
            }
            products.add(p);
            descriptions.add(p.getName());
        }

        byte[] personBytes = Files.readAllBytes(personImagePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("person_image", new ByteArrayResource(personBytes) {
            @Override public String getFilename() { return "person.jpg"; }
        });

        java.util.List<String> categories = new java.util.ArrayList<>();

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            String garmentRelative = p.getGarmentProcessedUrl().replaceFirst("^/images/", "");
            Path garmentPath = Path.of(baseUploadDir, garmentRelative);
            if (!Files.exists(garmentPath)) {
                throw new IllegalStateException("Garment image not found: " + garmentPath);
            }
            byte[] garmentBytes = Files.readAllBytes(garmentPath);
            final int idx = i;
            body.add("garment_images", new ByteArrayResource(garmentBytes) {
                @Override public String getFilename() { return "garment_" + idx + ".png"; }
            });
            categories.add(mapGarmentType(p.getGarmentType()));
        }

        body.add("garment_descriptions", String.join(",", descriptions));
        body.add("garment_categories", String.join(",", categories));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Calling Python bridge for outfit try-on: {} garments", products.size());
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                    pythonServerUrl + "/tryon/outfit", request, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Outfit try-on result received: {} bytes", response.getBody().length);
                return response.getBody();
            } else {
                throw new IOException("Python bridge returned: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Failed to call Python bridge for outfit", e);
            throw new IOException("Không thể kết nối tới Python Try-On server. " + e.getMessage(), e);
        }
    }

    private String mapGarmentType(GarmentType type) {
        if (type == null) return "upper_body";
        return switch (type) {
            case UPPER_BODY -> "upper_body";
            case LOWER_BODY -> "lower_body";
            case FULL_BODY  -> "dresses";
        };
    }
}
