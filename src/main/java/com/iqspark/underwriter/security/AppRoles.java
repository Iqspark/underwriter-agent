package com.iqspark.underwriter.security;

/**
 * Application roles (RBAC). Authorities are stored with the Spring {@code ROLE_} prefix; these
 * constants are the bare role names used with {@code hasRole(...)} / {@code hasAnyRole(...)}.
 */
public final class AppRoles {

    private AppRoles() {
    }

    public static final String BROKER = "BROKER";
    public static final String UNDERWRITER = "UNDERWRITER";
    public static final String SENIOR_UNDERWRITER = "SENIOR_UNDERWRITER";
    public static final String AUDITOR = "AUDITOR";
    public static final String ADMIN = "ADMIN";
    public static final String SERVICE = "SERVICE";
}
