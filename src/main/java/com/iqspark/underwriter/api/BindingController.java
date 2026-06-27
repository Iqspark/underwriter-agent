package com.iqspark.underwriter.api;

import com.iqspark.underwriter.security.AppRoles;
import com.iqspark.underwriter.security.authority.BindingService;
import com.iqspark.underwriter.security.authority.BindingStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Binding/approval actions on a decision, enforcing underwriting authority limits and four-eyes
 * (doc 11 §3.1). {@code action=APPROVE} binds the recommendation; {@code action=OVERRIDE} binds
 * despite a {@code DECLINE} (senior + four-eyes). Filter-level RBAC restricts this to underwriters;
 * the service applies the per-risk authority check.
 */
@RestController
@RequestMapping("/api/underwriting/decisions")
public class BindingController {

    private final BindingService bindingService;

    public BindingController(BindingService bindingService) {
        this.bindingService = bindingService;
    }

    @PostMapping("/{reference}/approvals")
    @PreAuthorize("hasAnyRole('" + AppRoles.UNDERWRITER + "','" + AppRoles.SENIOR_UNDERWRITER + "')")
    public BindingStatus approve(@PathVariable String reference,
                                 @RequestParam(defaultValue = "APPROVE") String action,
                                 Authentication authentication) {
        String approver = authentication.getName();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .collect(Collectors.toSet());
        return bindingService.submitApproval(reference, approver, roles, action);
    }
}
