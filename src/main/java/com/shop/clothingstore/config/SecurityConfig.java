package com.shop.clothingstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.shop.clothingstore.security.JwtAuthenticationFilter;
import com.shop.clothingstore.security.LoginRateLimitFilter;
import com.shop.clothingstore.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final JwtAuthenticationFilter jwtFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            CustomAuthenticationSuccessHandler successHandler,
            JwtAuthenticationFilter jwtFilter,
            LoginRateLimitFilter loginRateLimitFilter,
            CorsConfigurationSource corsConfigurationSource) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
        this.jwtFilter = jwtFilter;
        this.loginRateLimitFilter = loginRateLimitFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session
                        -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/auth/**",
                        "/api/products/**",
                        "/api/categories/**",
                        "/api/subcategories/**",
                        "/api/cart/**",
                        "/api/recommendations/**",
                        "/api/chatbot/**",
                        "/api/coupons/validate",
                        "/api/tryon/**"
                ).permitAll()
                .requestMatchers("/api/notifications/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/orders/checkout").permitAll()
                .requestMatchers("/api/analytics/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated())
                // Rate limit filter runs before JWT filter so throttled requests
                // are rejected before any token parsing overhead
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // Unauthenticated requests → 401 (not Spring's default 403)
                .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(
                            "{\"status\":401,\"error\":\"UNAUTHORIZED\","
                            + "\"message\":\"Authentication required\"}");
                }))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(ct -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/login", "/register",
                        "/products/**", "/product/**", "/cart/**", "/checkout/**",
                        "/tryon-studio",
                        "/contact", "/returns", "/sizing",
                        "/forgot-password", "/reset-password",
                        "/css/**", "/js/**", "/images/**"
                ).permitAll()
                .requestMatchers("/my-orders", "/my-coupons", "/profile", "/orders/**", "/reviews/**", "/wishlist/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
                .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .permitAll())
                .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(ct -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
