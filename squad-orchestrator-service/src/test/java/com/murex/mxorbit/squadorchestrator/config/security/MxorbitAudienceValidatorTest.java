package com.murex.mxorbit.squadorchestrator.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Unit tests for {@link MxorbitAudienceValidator}. Uses only locally
 * constructed {@link Jwt} instances (no issuer, no network, no real access
 * token) so the audience-matching rule can be verified in isolation.
 */
class MxorbitAudienceValidatorTest {

	private static final String EXPECTED_AUDIENCE = "api://mxorbit-api";

	private final MxorbitAudienceValidator validator = new MxorbitAudienceValidator(EXPECTED_AUDIENCE);

	private static Jwt jwtWithAudience(List<String> audience) {
		Jwt.Builder builder = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "test-subject")
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
		if (audience != null) {
			builder.audience(audience);
		}
		return builder.build();
	}

	@Test
	void succeedsWhenExpectedAudienceIsTheOnlyAudience() {
		OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of(EXPECTED_AUDIENCE)));

		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	void succeedsWhenExpectedAudienceIsOneOfMultipleAudiences() {
		OAuth2TokenValidatorResult result = validator
				.validate(jwtWithAudience(List.of("api://some-other-api", EXPECTED_AUDIENCE)));

		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	void failsWhenAudienceIsAbsent() {
		OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(null));

		assertThat(result.hasErrors()).isTrue();
		assertThat(result.getErrors()).singleElement()
				.satisfies(error -> assertThat(error.getErrorCode()).isEqualTo("invalid_token"));
	}

	@Test
	void failsWhenAudienceDiffers() {
		OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of("api://some-other-api")));

		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	void failsOnCaseOnlyDifference() {
		OAuth2TokenValidatorResult result = validator
				.validate(jwtWithAudience(List.of(EXPECTED_AUDIENCE.toUpperCase())));

		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	void doesNotTreatUserReadAsAnAudience() {
		OAuth2TokenValidatorResult result = validator.validate(jwtWithAudience(List.of("User.Read")));

		assertThat(result.hasErrors()).isTrue();
	}
}
