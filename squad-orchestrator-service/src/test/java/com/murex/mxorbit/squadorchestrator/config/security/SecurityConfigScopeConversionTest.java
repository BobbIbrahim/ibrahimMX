package com.murex.mxorbit.squadorchestrator.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Confirms Spring Security 7's default {@code scp} claim conversion (as pinned
 * by {@link SecurityConfig#jwtAuthenticationConverter()}): delegated scope
 * values become {@code SCOPE_*} granted authorities, so a configured
 * {@code requiredScope} such as {@code access_as_user} is enforceable through
 * {@code SCOPE_access_as_user}. Uses only locally constructed {@link Jwt}
 * instances.
 */
class SecurityConfigScopeConversionTest {

	private final JwtAuthenticationConverter converter = newConverter();

	private static JwtAuthenticationConverter newConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

	private static Jwt jwtWithScopeClaim(String scp) {
		Instant now = Instant.now();
		Jwt.Builder builder = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "test-subject")
				.issuedAt(now).expiresAt(now.plusSeconds(60));
		if (scp != null) {
			builder.claim("scp", scp);
		}
		return builder.build();
	}

	@Test
	void scpContainingRequiredScopeMapsToScopePrefixedAuthority() {
		AbstractAuthenticationToken authentication = converter.convert(jwtWithScopeClaim("access_as_user"));

		assertThat(authorities(authentication)).contains("SCOPE_access_as_user");
	}

	@Test
	void multipleScopesMapIndependently() {
		AbstractAuthenticationToken authentication = converter.convert(jwtWithScopeClaim("access_as_user other_scope"));

		assertThat(authorities(authentication)).contains("SCOPE_access_as_user", "SCOPE_other_scope");
	}

	@Test
	void missingScpClaimDoesNotCreateTheRequiredAuthority() {
		AbstractAuthenticationToken authentication = converter.convert(jwtWithScopeClaim(null));

		assertThat(authorities(authentication)).doesNotContain("SCOPE_access_as_user");
	}

	@Test
	void userReadDoesNotBecomeSufficientForTheConfiguredMxorbitScope() {
		AbstractAuthenticationToken authentication = converter.convert(jwtWithScopeClaim("User.Read"));

		assertThat(authorities(authentication)).contains("SCOPE_User.Read").doesNotContain("SCOPE_access_as_user");
	}

	private static List<String> authorities(AbstractAuthenticationToken authentication) {
		return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
	}
}
