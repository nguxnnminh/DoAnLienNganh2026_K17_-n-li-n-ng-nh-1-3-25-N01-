package com.shop.clothingstore.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    @Value("${upload.dir:src/main/resources/static/images}")
    private String baseUploadDir;

    public String upload(MultipartFile file, String folder) throws IOException {

        // =====================================================
        // VALIDATE
        // =====================================================
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được rỗng");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(
                    "Chỉ chấp nhận file ảnh. Loại file nhận được: " + contentType);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File quá lớn. Tối đa 5MB, file hiện tại: "
                    + (file.getSize() / 1024 / 1024) + "MB");
        }

        // =====================================================
        // UPLOAD
        // =====================================================
        Path uploadPath = Paths.get(baseUploadDir, folder);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String publicUrl = "/images/" + folder + "/" + fileName;

        log.info("File uploaded | path={} | size={}KB | original={}",
                publicUrl, file.getSize() / 1024, originalName);

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
}
