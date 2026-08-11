package com.murex.mxorbit.squadorchestrator.config.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;


@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "mxorbit.security.enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain disabledSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll()).csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable()).formLogin(formLogin -> formLogin.disable());
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "mxorbit.security.enabled", havingValue = "true")
    public SecurityFilterChain enabledSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                                          JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "mxorbit.security.enabled", havingValue = "true")
    public JwtDecoder jwtDecoder(MxorbitSecurityProperties properties) {
        properties.validateForEnabledMode();
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(properties.getIssuerUri());
        decoder.setJwtValidator(buildJwtValidator(properties));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> buildJwtValidator(MxorbitSecurityProperties properties) {
        OAuth2TokenValidator<Jwt> standardValidators = JwtValidators.createDefaultWithIssuer(properties.getIssuerUri());
        OAuth2TokenValidator<Jwt> audienceValidator = new MxorbitAudienceValidator(properties.getAudience());
        return new DelegatingOAuth2TokenValidator<>(standardValidators, audienceValidator);
    }

    @Bean
    @ConditionalOnProperty(name = "mxorbit.security.enabled", havingValue = "true")
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
