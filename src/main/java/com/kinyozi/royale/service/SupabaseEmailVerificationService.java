package com.kinyozi.royale.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinyozi.royale.config.SupabaseProperties;
import com.kinyozi.royale.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifies that a caller has proven ownership of an email address via
 * <b>your</b> Supabase project's Auth service.
 *
 * <p>Flow:
 * <ol>
 *   <li>The frontend uses supabase-js (with your anon key) to send a
 *       verification email/OTP to the user.</li>
 *   <li>The user clicks the link / enters the code — supabase-js hands the
 *       frontend a short-lived {@code access_token}.</li>
 *   <li>The frontend sends that token to Spring Boot as the
 *       {@code X-Supabase-Access-Token} header on {@code POST /auth/register}.</li>
 *   <li>This service calls {@code GET {SUPABASE_URL}/auth/v1/user} using the
 *       token. If Supabase returns the user with a non-null
 *       {@code email_confirmed_at} and the email matches, verification
 *       passes.</li>
 * </ol>
 *
 * <p>No Supabase user is persisted on our side and no Supabase session is
 * kept. Once verification passes Spring Boot issues its own JWT + refresh
 * cookie exactly as before.
 */
@Service
public class SupabaseEmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseEmailVerificationService.class);

    private final SupabaseProperties props;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public SupabaseEmailVerificationService(SupabaseProperties props) {
        this.props = props;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean isEnabled() {
        return props.getEmailVerification().isEnabled()
                && props.getUrl() != null && !props.getUrl().isBlank()
                && props.getAnonKey() != null && !props.getAnonKey().isBlank();
    }

    /**
     * Verify that {@code accessToken} was issued by our configured Supabase
     * project AND belongs to {@code expectedEmail} AND the email has been
     * confirmed. Throws {@link BadRequestException} otherwise.
     */
    public void requireVerifiedEmail(String accessToken, String expectedEmail) {
        if (!isEnabled()) return; // feature off — skip silently

        if (accessToken == null || accessToken.isBlank())
            throw new BadRequestException("Email verification token is missing");

        try {
            String base = props.getUrl().replaceAll("/+$", "");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/auth/v1/user"))
                    .timeout(Duration.ofSeconds(8))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("apikey", props.getAnonKey())
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("Supabase email verification failed: HTTP {}", resp.statusCode());
                throw new BadRequestException("Email verification failed. Please verify your email and try again.");
            }
            JsonNode body = json.readTree(resp.body());
            String email = body.path("email").asText(null);
            String confirmedAt = body.path("email_confirmed_at").asText(null);
            if (email == null || confirmedAt == null || confirmedAt.isBlank() || "null".equals(confirmedAt))
                throw new BadRequestException("Email has not been verified");
            if (expectedEmail == null || !email.equalsIgnoreCase(expectedEmail.trim()))
                throw new BadRequestException("Verified email does not match the address on the form");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Supabase verification call failed: {}", e.toString());
            throw new BadRequestException("Could not verify email at this time. Please try again.");
        }
    }
}
