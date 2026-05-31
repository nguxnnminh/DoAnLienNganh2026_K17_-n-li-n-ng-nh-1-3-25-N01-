package com.shop.clothingstore.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

/**
 * Configures async execution for the try-on pipeline and a timeout-aware
 * RestTemplate for calls to the Python bridge.
 *
 * <p>The try-on thread pool is separate from Spring's default task executor
 * so that slow AI calls never block web request threads.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * RestTemplate with explicit timeouts for the Python bridge.
     * <ul>
     *   <li>Connect timeout: 10 seconds — fail fast if the bridge is down</li>
     *   <li>Read timeout: 300 seconds — CatVTON outfit = two HF Space calls. 300s covers slow queue.</li>
     * </ul>
     */
    @Bean(name = "tryOnRestTemplate")
    public RestTemplate tryOnRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 10 seconds
        factory.setReadTimeout(300_000);    // 5 minutes (CatVTON outfit = two HF Space calls)
        return new RestTemplate(factory);
    }

    /**
     * Dedicated thread pool for asynchronous try-on jobs.
     * Keeps AI processing completely isolated from the Tomcat servlet pool.
     */
    @Bean(name = "tryOnExecutor")
    public Executor tryOnExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // 2 concurrent AI calls max (HF rate limits)
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);    // Queue up to 20 pending requests
        executor.setThreadNamePrefix("tryon-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
