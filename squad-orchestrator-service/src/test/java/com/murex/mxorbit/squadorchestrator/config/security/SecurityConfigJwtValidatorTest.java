package com.murex.mxorbit.squadorchestrator.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for {@link SecurityConfig#buildJwtValidator}, proving that Spring
 * Security's standard issuer/timestamp validators are combined with (not
 * replaced by) exact audience validation. Exercises the validator directly
 * against locally constructed {@link Jwt} instances: constructing these
 * validators never performs network access (no metadata/JWK request, no call to
 * a real issuer), which is what makes this test possible without mocks.
 */
class SecurityConfigJwtValidatorTest {

	private static final String ISSUER = "https://login.microsoftonline.com/test-tenant-id/v2.0";
	private static final String AUDIENCE = "api://mxorbit-api";

	private final MxorbitSecurityProperties properties = new MxorbitSecurityProperties();

	SecurityConfigJwtValidatorTest() {
		properties.setIssuerUri(ISSUER);
		properties.setAudience(AUDIENCE);
		properties.setRequiredScope("access_as_user");
		properties.setAllowedOrigins(List.of("https://example.test"));
	}

	private static Jwt validJwt(String issuer, List<String> audience) {
		Instant now = Instant.now();
		return Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "test-subject").issuer(issuer)
				.audience(audience).issuedAt(now.minusSeconds(5)).expiresAt(now.plusSeconds(60)).build();
	}

	@Test
	void succeedsWhenIssuerAndAudienceMatch() {
		OAuth2TokenValidator<Jwt> validator = SecurityConfig.buildJwtValidator(properties);

		OAuth2TokenValidatorResult result = validator.validate(validJwt(ISSUER, List.of(AUDIENCE)));

		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	void failsWhenIssuerDiffersEvenIfAudienceMatches() {
		OAuth2TokenValidator<Jwt> validator = SecurityConfig.buildJwtValidator(properties);

		OAuth2TokenValidatorResult result = validator
				.validate(validJwt("https://login.microsoftonline.com/other-tenant/v2.0", List.of(AUDIENCE)));

		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	void failsWhenAudienceDiffersEvenIfIssuerMatches() {
		OAuth2TokenValidator<Jwt> validator = SecurityConfig.buildJwtValidator(properties);

		OAuth2TokenValidatorResult result = validator.validate(validJwt(ISSUER, List.of("User.Read")));

		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	void failsWhenTokenIsExpiredEvenIfIssuerAndAudienceMatch() {
		OAuth2TokenValidator<Jwt> validator = SecurityConfig.buildJwtValidator(properties);

		Instant now = Instant.now();
		Jwt expired = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "test-subject").issuer(ISSUER)
				.audience(List.of(AUDIENCE)).issuedAt(now.minusSeconds(600)).expiresAt(now.minusSeconds(300)).build();

		OAuth2TokenValidatorResult result = validator.validate(expired);

		assertThat(result.hasErrors()).isTrue();
	}
}
