package com.murex.mxorbit.squadorchestrator.config.security;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mxorbit.security")
public class MxorbitSecurityProperties {

	private boolean enabled = false;
	private String issuerUri;
	private String audience;
	private String requiredScope;
	private List<String> allowedOrigins = List.of();

	public void validateForEnabledMode() {
		List<String> missing = new ArrayList<>();
		if (issuerUri == null || issuerUri.isBlank()) {
			missing.add("mxorbit.security.issuer-uri");
		}
		if (audience == null || audience.isBlank()) {
			missing.add("mxorbit.security.audience");
		}
		if (requiredScope == null || requiredScope.isBlank()) {
			missing.add("mxorbit.security.required-scope");
		}
		if (allowedOrigins == null || allowedOrigins.isEmpty()) {
			missing.add("mxorbit.security.allowed-origins");
		}
		if (!missing.isEmpty()) {
			throw new IllegalStateException(
					"mxorbit.security.enabled=true requires the following properties to be set: " + missing);
		}
	}
}
