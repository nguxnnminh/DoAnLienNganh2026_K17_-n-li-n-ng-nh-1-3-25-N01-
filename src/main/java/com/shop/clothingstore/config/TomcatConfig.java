package com.shop.clothingstore.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tăng giới hạn số lượng parts trong multipart request của Tomcat. Mặc định
 * Tomcat 10.1+ giới hạn maxPartCount thấp, gây ra
 * FileCountLimitExceededException khi form upload có nhiều fields + files.
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            // Cho phép tối đa 100 parts (fields + files) trong 1 multipart request
            connector.setMaxParameterCount(1000);
            connector.setMaxPartCount(100);
        });
    }
}
