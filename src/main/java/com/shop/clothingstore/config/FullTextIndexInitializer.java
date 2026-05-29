package com.shop.clothingstore.config;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Tạo FULLTEXT index cho bảng `products` nếu chưa có.
 *
 * <p>Mục tiêu: máy nào clone/pull repo về, chạy app là full-text search hoạt động ngay —
 * không cần chạy SQL thủ công. Idempotent: nếu index đã tồn tại thì bỏ qua.</p>
 *
 * <p>Chạy SAU khi Hibernate đã tạo/cập nhật schema (ApplicationReadyEvent). Nếu DB không
 * hỗ trợ FULLTEXT (hiếm), chỉ log cảnh báo — search vẫn chạy nhờ fallback LIKE trong service.</p>
 */
@Component
public class FullTextIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(FullTextIndexInitializer.class);

    private static final String INDEX_NAME = "ft_products_name_desc";
    private static final String TABLE_NAME = "products";

    // Phòng thủ: chỉ cho phép định danh an toàn (chữ/số/gạch dưới). Nếu sau này ai đó
    // đổi các hằng số trên thành giá trị cấu hình động → chặn ngay nguy cơ SQL injection.
    static {
        if (!INDEX_NAME.matches("[A-Za-z0-9_]+") || !TABLE_NAME.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException("Unsafe SQL identifier: " + TABLE_NAME + " / " + INDEX_NAME);
        }
    }

    private final DataSource dataSource;

    public FullTextIndexInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void ensureFullTextIndex() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            if (indexExists(stmt)) {
                log.info("FULLTEXT index '{}' already exists — skip", INDEX_NAME);
                return;
            }

            stmt.executeUpdate(
                    "ALTER TABLE " + TABLE_NAME + " ADD FULLTEXT INDEX " + INDEX_NAME + " (name, description)");
            log.info("Created FULLTEXT index '{}' on {}(name, description)", INDEX_NAME, TABLE_NAME);

        } catch (Exception e) {
            // Không chặn khởi động: search sẽ fallback sang LIKE nếu MATCH không dùng được.
            log.warn("Could not create FULLTEXT index '{}' — full-text search will fall back to LIKE. Reason: {}",
                    INDEX_NAME, e.getMessage());
        }
    }

    private boolean indexExists(Statement stmt) {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(1) FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + TABLE_NAME + "' " +
                "AND INDEX_NAME = '" + INDEX_NAME + "'")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            log.warn("Could not check FULLTEXT index existence: {}", e.getMessage());
            return false;
        }
    }
}
