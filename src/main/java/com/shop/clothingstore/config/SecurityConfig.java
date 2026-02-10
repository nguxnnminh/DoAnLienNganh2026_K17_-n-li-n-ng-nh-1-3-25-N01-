package com.shop.clothingstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.shop.clothingstore.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            CustomAuthenticationSuccessHandler successHandler
    ) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                // ================= PUBLIC =================
                .requestMatchers(
                        "/",
                        "/login",
                        "/register",
                        "/products/**",
                        "/cart/**",
                        "/checkout/**", // ⭐ cho phép guest checkout
                        "/forgot-password",
                        "/reset-password",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                ).permitAll()

                // ================= USER ONLY =================
                .requestMatchers(
                        "/my-orders",
                        "/profile"
                ).authenticated()

                // ================= ADMIN ONLY =================
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // ================= DEFAULT =================
                .anyRequest().permitAll()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .permitAll()
            )

            .logout(logout -> logout
                    .logoutUrl("/logout")                 // URL logout
                    .logoutSuccessUrl("/")                // Redirect sau logout
                    .invalidateHttpSession(true)          // Xóa session
                    .clearAuthentication(true)            // Xóa authentication
                    .deleteCookies("JSESSIONID")          // Xóa cookie login
                    .permitAll()
            );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
