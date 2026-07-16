package com.kinyozi.royale.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
            OwnerDto owner,
            BusinessDto business,
            java.util.List<AuxUserDto> auxiliaryUsers
    ) {
        public record OwnerDto(
                @NotBlank @Size(min = 2, max = 64) String username,
                @NotBlank @Email String email,
                @NotBlank @Pattern(regexp = "^[+0-9\\s()\\-]{7,20}$",
                        message = "Enter a valid phone number") String phone,
                @NotBlank
                @Size(min = 8, max = 128, message = "Password must be at least 8 characters")
                @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,128}$",
                    message = "Password must contain at least one letter and one number")
                String password
        ) {}

        public record BusinessDto(
                @NotBlank @Size(min = 2, max = 120) String name,
                @NotBlank @Pattern(
                        regexp = "STARTER|GROWTH|PRO|ENTERPRISE",
                        message = "Unsupported subscription plan") String plan
        ) {}

        /**
         * Auxiliary users can ONLY be created with staff-level roles.
         * Registration MUST NOT be a privilege-escalation path.
         */
        public record AuxUserDto(
                @NotBlank @Size(min = 2, max = 64) String username,
                @NotBlank @Pattern(
                        regexp = "MANAGER|CASHIER",
                        message = "Auxiliary users can only be MANAGER or CASHIER") String role
        ) {}
    }

    /** Login uses email + password. */
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    /**
     * Response of /auth/login, /auth/register and /auth/refresh.
     *
     * <p>The refresh token itself is delivered as an HttpOnly cookie
     * ({@code kr_refresh}) and is NOT included in this body. Backward
     * compatibility: existing clients that only read {@code token} continue
     * to work; they simply won't benefit from silent refresh until upgraded.
     */
    public record AuthResponse(
            String token,
            long expiresInSeconds,
            String tenantId,
            String username,
            String businessName,
            String role,
            String email,
            String phone,
            String businessCode
    ) {}

    /** Password change requires the current password (defense against XSS-driven takeover). */
    public record PasswordChangeRequest(
            @NotBlank String currentPassword,
            @NotBlank
            @Size(min = 8, max = 128, message = "Password must be at least 8 characters")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,128}$",
                    message = "Password must contain at least one letter and one number")
            String newPassword
    ) {}
}
