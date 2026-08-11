package com.murex.mxorbit.squadorchestrator.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Verifies the Task 6 disabled-mode foundation in isolation, without booting
 * the full application (no datasource / Temporal connection required).
 */
class SecurityConfigDisabledTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MxorbitSecurityPropertiesConfiguration.class, SecurityConfig.class);

	@Test
	void contextStartsWithSecurityDisabledByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(SecurityFilterChain.class);
			assertThat(context).getBean("disabledSecurityFilterChain").isNotNull();
		});
	}

	@Test
	void noJwtDecoderBeanExistsWhileDisabled() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(JwtDecoder.class));
	}

	@Test
	void exactlyOneSecurityFilterChainIsActiveWhenExplicitlyDisabled() {
		contextRunner.withPropertyValues("mxorbit.security.enabled=false").run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getBeansOfType(SecurityFilterChain.class)).hasSize(1)
					.containsOnlyKeys("disabledSecurityFilterChain");
		});
	}

	@Test
	void enabledModeFailsFastWhenRequiredPropertiesAreMissing() {
		contextRunner.withPropertyValues("mxorbit.security.enabled=true").run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure())
					.hasRootCauseMessage("mxorbit.security.enabled=true requires the following properties to be set: "
							+ "[mxorbit.security.issuer-uri, mxorbit.security.audience, "
							+ "mxorbit.security.required-scope, mxorbit.security.allowed-origins]");
		});
	}
}
