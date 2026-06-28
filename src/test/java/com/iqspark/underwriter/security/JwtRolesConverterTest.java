package com.iqspark.underwriter.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRolesConverterTest {

    @Test
    void mapsRolesClaimToRoleAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("roles", List.of("UNDERWRITER", "ADMIN"))
                .build();

        JwtAuthenticationConverter converter = JwtRolesConverter.create();
        List<String> authorities = converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorities).contains("ROLE_UNDERWRITER", "ROLE_ADMIN");
    }

    @Test
    void noRolesClaimYieldsNoRoleAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "u1").build();
        List<String> authorities = JwtRolesConverter.create().convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        assertThat(authorities).noneMatch(a -> a.startsWith("ROLE_"));
    }
}
