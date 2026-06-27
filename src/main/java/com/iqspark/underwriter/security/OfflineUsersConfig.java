package com.iqspark.underwriter.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * In-memory demo users that back the offline/dev HTTP Basic mode. <b>Dev only</b> — in production
 * the OIDC resource-server mode is used instead (set {@code issuer-uri}), and these users are not
 * exercised. Passwords come from {@code underwriter.security.dev-password} (override via env).
 */
@Configuration
public class OfflineUsersConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsManager(
            PasswordEncoder encoder,
            @Value("${underwriter.security.dev-password:changeit}") String pw) {
        String hash = encoder.encode(pw);
        UserDetails broker = User.withUsername("broker").password(hash).roles(AppRoles.BROKER).build();
        UserDetails uw = User.withUsername("uw").password(hash).roles(AppRoles.UNDERWRITER).build();
        UserDetails senior = User.withUsername("senior").password(hash).roles(AppRoles.SENIOR_UNDERWRITER).build();
        UserDetails auditor = User.withUsername("auditor").password(hash).roles(AppRoles.AUDITOR).build();
        UserDetails admin = User.withUsername("admin").password(hash).roles(AppRoles.ADMIN).build();
        UserDetails service = User.withUsername("svc").password(hash).roles(AppRoles.SERVICE).build();
        return new InMemoryUserDetailsManager(broker, uw, senior, auditor, admin, service);
    }
}
