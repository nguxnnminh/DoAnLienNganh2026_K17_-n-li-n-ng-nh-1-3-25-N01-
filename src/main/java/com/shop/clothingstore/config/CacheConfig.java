package com.shop.clothingstore.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache definitions and TTLs:
     *
     *  categories     — 1 h   (admin-driven changes, rare)
     *  subCategories  — 1 h   (same)
     *  bestSellers    — 15 min (updated by checkouts, should refresh reasonably often)
     *  tryOnProducts  — 30 min (admin enables/disables try-on)
     *  dashboardData  — 5 min  (admin dashboard stats)
     */
    @Bean
    @SuppressWarnings("null")
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Register named caches with individual TTLs.
        // Spring will use the default spec for any cache name not listed here.
        manager.registerCustomCache("categories",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(100)
                        .build());

        manager.registerCustomCache("subCategories",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(500)
                        .build());

        manager.registerCustomCache("bestSellers",
                Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .build());

        manager.registerCustomCache("tryOnProducts",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build());

        manager.registerCustomCache("dashboardData",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(10)
                        .build());

        return manager;
    }
}
