package com.murex.mxorbit.squadorchestrator.config.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

/**
 * Validates that a {@link Jwt} was issued for the MXORBIT API, i.e. carries the
 * exact configured {@code aud} value (Task 7).
 *
 * <p>
 * Matching is deliberately strict: the configured audience must appear verbatim
 * in {@link Jwt#getAudience()}. There is no substring, prefix,
 * case-insensitive, or URI-normalization matching, so a token audienced for
 * something else (in particular the Microsoft Graph {@code User.Read} scope,
 * which is not even an audience value) can never satisfy this validator.
 *
 * <p>
 * Immutable and side-effect free: it never logs claims or token contents, and
 * holds only the single expected audience value supplied at construction time.
 */
public final class MxorbitAudienceValidator implements OAuth2TokenValidator<Jwt> {

	private final String requiredAudience;

	public MxorbitAudienceValidator(String requiredAudience) {
		Assert.hasText(requiredAudience, "requiredAudience must not be blank");
		this.requiredAudience = requiredAudience;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		if (jwt.getAudience() != null && jwt.getAudience().contains(requiredAudience)) {
			return OAuth2TokenValidatorResult.success();
		}
		OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
				"The required audience '" + requiredAudience + "' is missing from the token", null);
		return OAuth2TokenValidatorResult.failure(error);
	}
}
