package com.kinyozi.royale.admin.bootstrap;

import com.kinyozi.royale.admin.model.PlatformAdmin;
import com.kinyozi.royale.admin.repository.PlatformAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Bootstraps the initial Platform Administrator ONLY when a non-empty,
 * securely provided password is present via {@code ROYALE_ADMIN_PASSWORD}.
 * When the password is blank (default), no account is auto-created —
 * administrators must be seeded through a secure out-of-band channel.
 * The runner is also a no-op if an account with the username already
 * exists.
 */
@Component
public class PlatformAdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

    private final PlatformAdminRepository admins;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;
    private final String email;
    private final String fullName;

    public PlatformAdminBootstrap(PlatformAdminRepository admins,
                                  PasswordEncoder encoder,
                                  @Value("${royale.admin.bootstrap.username:superadmin}") String username,
                                  @Value("${royale.admin.bootstrap.password:}") String password,
                                  @Value("${royale.admin.bootstrap.email:admin@kinyozi.app}") String email,
                                  @Value("${royale.admin.bootstrap.fullName:Platform Administrator}") String fullName) {
        this.admins = admins;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
    }

    @Override
    public void run(String... args) {
        if (password == null || password.isBlank()) {
            log.warn("ROYALE_ADMIN_PASSWORD not set — skipping platform admin bootstrap. "
                    + "Seed an administrator manually via a secure channel.");
            return;
        }
        if (password.length() < 12) {
            log.error("ROYALE_ADMIN_PASSWORD is too weak (< 12 chars); refusing to bootstrap admin.");
            return;
        }
        if (admins.existsByUsername(username)) return;
        admins.save(PlatformAdmin.builder()
                .username(username)
                .email(email)
                .fullName(fullName)
                .passwordHash(encoder.encode(password))
                .active(true)
                .createdAt(Instant.now())
                .build());
        log.info("Bootstrapped platform administrator '{}'.", username);
    }
}
