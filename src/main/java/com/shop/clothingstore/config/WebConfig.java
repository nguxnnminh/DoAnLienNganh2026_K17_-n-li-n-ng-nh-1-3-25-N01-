package com.shop.clothingstore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded images (products, garments, try-on results) directly from
 * the filesystem upload directory instead of the classpath.
 *
 * This is required because files created at runtime (e.g. try-on results)
 * are NOT visible through Spring Boot's default classpath-based static
 * resource serving.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.dir:src/main/resources/static/images}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /images/** to the filesystem upload directory
        // so newly created files are immediately accessible
        String fileRoot = "file:" + uploadDir.replace("\\", "/") + "/";

        // Check filesystem upload dir first (user-uploaded files),
        // then fall back to classpath static images (design assets like banners/category images).
        registry.addResourceHandler("/images/**")
                .addResourceLocations(fileRoot, "classpath:/static/images/")
                .setCachePeriod(0); // no caching for dev
    }
}
