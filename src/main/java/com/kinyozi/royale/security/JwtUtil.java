package com.kinyozi.royale.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
  private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

  @Value("${royale.app.jwtSecret}") private String secret;
  @Value("${royale.app.jwtExpirationMs}") private long expirationMs;

  @PostConstruct
  void validate() {
    if (secret == null || secret.isBlank()
        || secret.startsWith("CHANGE_ME_INSECURE_DEFAULT")
        || secret.startsWith("change-me")
        || secret.length() < 32) {
      throw new IllegalStateException(
          "JWT_SECRET is missing, too short (< 32 chars), or set to the insecure default. "
          + "Set the JWT_SECRET environment variable to a strong random value before starting.");
    }
    if (expirationMs <= 0) {
      throw new IllegalStateException("JWT_EXPIRATION_MS must be > 0");
    }
    if (expirationMs > 60L * 60L * 1000L) {
      // Access tokens should be short-lived — sessions are kept alive via
      // the refresh-token rotation flow. Refuse to boot with a value that
      // undermines that design.
      log.warn("JWT_EXPIRATION_MS is {} ms (> 60 min). Access tokens should be short-lived; "
               + "long-lived sessions must use the refresh-token endpoint.", expirationMs);
    }
    log.info("JWT signing secret loaded ({} chars), access-token TTL = {} ms.",
             secret.length(), expirationMs);
  }

  public long expirationSeconds() { return expirationMs / 1000L; }

  private SecretKey key(){ return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }

  public String generate(String username, String tenantId, String role){
    Date now = new Date();
    return Jwts.builder()
        .subject(username)
        .claims(Map.of("tenantId", tenantId, "role", role))
        .issuedAt(now)
        .expiration(new Date(now.getTime()+expirationMs))
        .signWith(key())
        .compact();
  }
  public Jws<Claims> parse(String token){
    return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
  }
}
