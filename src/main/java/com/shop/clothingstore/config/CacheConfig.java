package com.shop.clothingstore.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // ConcurrentMapCacheManager tự động được tạo khi
    // spring.cache.type=simple trong application.properties
}
