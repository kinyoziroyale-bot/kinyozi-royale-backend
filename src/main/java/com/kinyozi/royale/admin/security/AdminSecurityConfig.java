package com.kinyozi.royale.admin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Dedicated Spring Security filter chain for the Developer Portal.
 *
 * V5:
 *  - Applies the same hardened response headers as the tenant chain
 *    (previously admin responses had NO security headers because this
 *    chain evaluates first for /admin/** and Spring only applies headers
 *    from the matched chain).
 *  - Still uses {@code securityMatcher("/admin/**")} + {@code @Order(1)}.
 *  - Preflight (OPTIONS) is explicitly permitted.
 */
@Configuration
public class AdminSecurityConfig {

    private final AdminJwtFilter adminJwtFilter;

    public AdminSecurityConfig(AdminJwtFilter f) { this.adminJwtFilter = f; }

    @Bean
    @Order(1)
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")
                .csrf(c -> c.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .addHeaderWriter((request, response) -> {
                            response.setHeader("Permissions-Policy",
                                    "accelerometer=(), autoplay=(), camera=(), display-capture=(), "
                                  + "encrypted-media=(), fullscreen=(), geolocation=(), gyroscope=(), "
                                  + "magnetometer=(), microphone=(), midi=(), payment=(), picture-in-picture=(), "
                                  + "publickey-credentials-get=(), screen-wake-lock=(), sync-xhr=(), "
                                  + "usb=(), web-share=(), xr-spatial-tracking=()");
                            response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                            response.setHeader("Cross-Origin-Resource-Policy", "same-site");
                            response.setHeader("X-XSS-Protection", "0");
                        }))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/admin/**").permitAll()
                        .requestMatchers("/admin/auth/login").permitAll()
                        .requestMatchers("/admin/**")
                        .hasRole(AdminJwtUtil.ROLE_PLATFORM_ADMIN)
                        .anyRequest().denyAll())
                .addFilterBefore(adminJwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
