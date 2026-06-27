package com.iqspark.underwriter.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security (Phase 1). RBAC over the REST surface, dual-mode authentication:
 *
 * <ul>
 *   <li><b>Offline / dev (default):</b> HTTP Basic against in-memory role users
 *       ({@link OfflineUsersConfig}) — keeps the app fully runnable and testable with no IdP.</li>
 *   <li><b>Production:</b> when {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} is set,
 *       it runs as an OAuth2 Resource Server validating OIDC JWTs, with role claims mapped by
 *       {@link JwtRolesConverter}.</li>
 * </ul>
 *
 * Stateless sessions; CSRF disabled (token/Basic API, no browser session). Method security is on
 * for the authority-limit / four-eyes checks on binding.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final String issuerUri;

    public SecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri) {
        this.issuerUri = issuerUri;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: liveness + basic actuator probes + docs error.
                        .requestMatchers("/api/underwriting/health",
                                "/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/error").permitAll()
                        // Operational endpoints are admin-only.
                        .requestMatchers("/actuator/**").hasRole(AppRoles.ADMIN)
                        // API docs require authentication (any role).
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .authenticated()
                        // Underwrite a submission/document: underwriters or a service identity.
                        .requestMatchers(HttpMethod.POST,
                                "/api/underwriting/submissions", "/api/underwriting/documents")
                        .hasAnyRole(AppRoles.UNDERWRITER, AppRoles.SENIOR_UNDERWRITER, AppRoles.SERVICE)
                        // Binding/approval actions: underwriters only (admins are segregated).
                        .requestMatchers(HttpMethod.POST, "/api/underwriting/decisions/*/approvals")
                        .hasAnyRole(AppRoles.UNDERWRITER, AppRoles.SENIOR_UNDERWRITER)
                        // Read decisions + history: underwriters and auditors (read-only).
                        .requestMatchers(HttpMethod.GET,
                                "/api/underwriting/decisions/**", "/api/underwriting/history/**")
                        .hasAnyRole(AppRoles.UNDERWRITER, AppRoles.SENIOR_UNDERWRITER, AppRoles.AUDITOR)
                        // Comparables preview: underwriters.
                        .requestMatchers(HttpMethod.POST, "/api/underwriting/history/comparables")
                        .hasAnyRole(AppRoles.UNDERWRITER, AppRoles.SENIOR_UNDERWRITER)
                        .anyRequest().authenticated());

        if (issuerUri == null || issuerUri.isBlank()) {
            http.httpBasic(Customizer.withDefaults());
        } else {
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(JwtRolesConverter.create())));
        }
        return http.build();
    }
}
