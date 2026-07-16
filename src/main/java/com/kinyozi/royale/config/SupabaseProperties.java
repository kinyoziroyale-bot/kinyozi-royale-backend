package com.kinyozi.royale.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for your EXTERNAL Supabase project.
 *
 * <p>Supabase is used here for TWO independent things:
 * <ol>
 *   <li><b>Database</b> — Supabase PostgreSQL replaces the local Postgres.
 *       Configure via the standard {@code spring.datasource.*} properties
 *       (see {@code application.properties}).</li>
 *   <li><b>Email verification during registration</b> — Supabase Auth is used
 *       ONLY to send a verification code/link to a prospective owner and to
 *       prove they own the email address. Spring Boot then completes the
 *       registration and issues its own JWT + refresh cookie exactly as
 *       before.</li>
 * </ol>
 *
 * <p>Set these env vars (or override in application-*.properties):
 * <pre>
 *   SUPABASE_URL              = https://&lt;project-ref&gt;.supabase.co
 *   SUPABASE_ANON_KEY         = &lt;anon/publishable key&gt;
 *   SUPABASE_EMAIL_VERIFY     = true       (enable the check)
 * </pre>
 *
 * <p>The service-role key is NOT required for the email-verification flow
 * and should not be placed on this machine unless you need it for other
 * server-side Supabase Admin API calls.
 */
@Component
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    /** Base URL of your Supabase project, e.g. https://abc.supabase.co */
    private String url = "";

    /** Anon / publishable API key. Sent as the {@code apikey} header. */
    private String anonKey = "";

    /** Optional service-role key. Only required if you call the Admin API. */
    private String serviceKey = "";

    private EmailVerification emailVerification = new EmailVerification();

    public static class EmailVerification {
        /** When true, {@code POST /auth/register} requires a valid
         *  {@code X-Supabase-Access-Token} header proving the email was
         *  verified through your Supabase project. */
        private boolean enabled = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getAnonKey() { return anonKey; }
    public void setAnonKey(String anonKey) { this.anonKey = anonKey; }
    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
    public EmailVerification getEmailVerification() { return emailVerification; }
    public void setEmailVerification(EmailVerification e) { this.emailVerification = e; }
}
