package com.finvault.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.finvault.common.domain.Role;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCurrentPrincipalFields() {
        UserPrincipal principal = new UserPrincipal(42L, "user@finvault.local", "hash", Role.ADMIN, true);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThat(SecurityUtils.currentUserId()).isEqualTo(42L);
        assertThat(SecurityUtils.currentEmail()).isEqualTo("user@finvault.local");
        assertThat(principal.getUsername()).isEqualTo("user@finvault.local");
        assertThat(principal.getPassword()).isEqualTo("hash");
        assertThat(principal.role()).isEqualTo(Role.ADMIN);
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(List.copyOf(principal.getAuthorities()).get(0).getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void requiresPrincipalForUserIdAndUsesSystemEmailFallback() {
        assertThatThrownBy(SecurityUtils::currentUserId).isInstanceOf(IllegalStateException.class);
        assertThat(SecurityUtils.currentEmail()).isEqualTo("system");
    }
}
