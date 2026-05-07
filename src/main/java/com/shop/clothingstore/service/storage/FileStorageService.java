package com.shop.clothingstore.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_MAGIC_BYTES = Set.of(
            "ffd8ff", // JPEG
            "89504e47", // PNG
            "47494638", // GIF
            "52494646" // WEBP (RIFF)
    );

    @Value("${upload.dir:src/main/resources/static/images}")
    private String baseUploadDir;

    public String upload(MultipartFile file, String folder) throws IOException {

        // =====================================================
        // VALIDATE
        // =====================================================
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File too large. Maximum 5MB, current file: "
                    + (file.getSize() / 1024 / 1024) + "MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported file format. Allowed: " + ALLOWED_EXTENSIONS);
        }

        // Validate magic bytes (file signature)
        byte[] header = file.getInputStream().readNBytes(8);
        String magic = bytesToHex(header).toLowerCase();
        boolean validMagic = ALLOWED_MAGIC_BYTES.stream().anyMatch(magic::startsWith);
        if (!validMagic) {
            throw new IllegalArgumentException(
                    "Invalid file signature. The file extension may have been spoofed.");
        }

        // =====================================================
        // UPLOAD
        // =====================================================
        Path uploadPath = Paths.get(baseUploadDir, folder);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID() + "." + extension;

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String publicUrl = "/images/" + folder + "/" + fileName;

        log.info("File uploaded | path={} | size={}KB | original={}",
                publicUrl, file.getSize() / 1024, originalName);

        return publicUrl;
    }

    /**
     * Upload raw bytes with a known filename.
     * Used when the caller already has bytes (e.g. from Python bridge preprocessing)
     * and wrapping in a MultipartFile is unnecessary.
     *
     * @param bytes    raw file content
     * @param filename original filename (used only for extension detection)
     * @param folder   target subfolder under baseUploadDir
     * @return public URL of the saved file (e.g. /images/tryon-garments/uuid.png)
     */
    public String uploadBytes(byte[] bytes, String filename, String folder) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Bytes must not be empty");
        }

        String extension = "png"; // safe default for garment preprocessing
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            extension = "png";
        }

        Path uploadPath = Paths.get(baseUploadDir, folder);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID() + "." + extension;
        Path   filePath = uploadPath.resolve(fileName);
        Files.write(filePath, bytes);

        String publicUrl = "/images/" + folder + "/" + fileName;
        log.info("Bytes uploaded | path={} | size={}KB", publicUrl, bytes.length / 1024);
        return publicUrl;
    }

    public void delete(String fileUrl) throws IOException {

        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        String relativePath = fileUrl.replaceFirst("^/images/", "");
        Path filePath = Paths.get(baseUploadDir, relativePath);

        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("File deleted | path={}", fileUrl);
        } else {
            log.warn("File not found for deletion | path={}", fileUrl);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
