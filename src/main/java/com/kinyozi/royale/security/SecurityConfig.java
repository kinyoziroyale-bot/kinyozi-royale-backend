package com.kinyozi.royale.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 * Tenant SecurityFilterChain. CORS is delegated to the shared
 * {@code corsConfigurationSource} bean. Method-level authorization
 * ({@code @PreAuthorize}) is enabled.
 *
 * V5:
 * - Password change endpoint (/auth/password/change) explicitly requires
 * authentication.
 * - /auth/refresh and /auth/logout are public (they authenticate via the
 * HttpOnly refresh cookie, not the Authorization header).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter f) { this.jwtFilter = f; }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean public AuthenticationManager authManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> {
                    h.frameOptions(f -> f.deny());
                    h.contentTypeOptions(Customizer.withDefaults());
                    h.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                    h.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"));
                    h.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000));
                    h.permissionsPolicy(pp -> pp.policy("camera=(), microphone=(), geolocation=(), payment=()"));

                    // These will now properly reference 'h' instead of the permissions configurer
                    h.addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin"));
                    h.addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Resource-Policy", "same-site"));
                }).authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public auth endpoints — refresh/logout authenticate via the HttpOnly cookie.
                        .requestMatchers(
                                "/auth/register", "/auth/login",
                                "/auth/refresh",  "/auth/logout",
                                "/health").permitAll()
                        // Password change MUST be authenticated.
                        .requestMatchers("/auth/password/change").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}