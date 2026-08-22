package com.quince.lawyeraiassistant.security.authentication.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

import com.quince.lawyeraiassistant.security.authentication.ApplicationUserDetailsService;
import com.quince.lawyeraiassistant.security.identity.ApplicationUserRepository;
import com.quince.lawyeraiassistant.security.identity.AuthenticationConstants;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class SecurityConfiguration {

        @Bean
        PasswordEncoder passwordEncoder() {

                return PasswordEncoderFactories
                                .createDelegatingPasswordEncoder();
        }

        @Bean
        UserDetailsService userDetailsService(
                        ApplicationUserRepository userRepository) {

                return new ApplicationUserDetailsService(
                                userRepository);
        }

        @Bean
        DaoAuthenticationProvider daoAuthenticationProvider(
                        UserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder) {

                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
                                userDetailsService);

                provider.setPasswordEncoder(
                                passwordEncoder);

                return provider;
        }

        @Bean
        AuthenticationManager authenticationManager(
                        AuthenticationConfiguration configuration)
                        throws Exception {

                return configuration
                                .getAuthenticationManager();
        }

        @Bean
        SecretKey jwtSecretKey(
                        JwtSecurityProperties properties) {

                byte[] bytes = properties.getSecret()
                                .getBytes(
                                                StandardCharsets.UTF_8);

                if (bytes.length < 32) {

                        throw new IllegalStateException(
                                        "JWT secret must be at least 32 bytes");
                }

                return new SecretKeySpec(
                                bytes,
                                "HmacSHA256");
        }

        @Bean
        JwtEncoder jwtEncoder(
                        SecretKey secretKey) {

                return NimbusJwtEncoder
                                .withSecretKey(
                                                secretKey)
                                .build();
        }

        @Bean
        JwtDecoder jwtDecoder(
                        SecretKey secretKey,
                        JwtSecurityProperties properties) {

                NimbusJwtDecoder decoder = NimbusJwtDecoder
                                .withSecretKey(
                                                secretKey)
                                .build();

                decoder.setJwtValidator(
                                org.springframework.security.oauth2.jwt.JwtValidators
                                                .createDefaultWithIssuer(
                                                                properties.getIssuer()));

                return decoder;
        }

        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {

                JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

                authoritiesConverter.setAuthoritiesClaimName(
                                AuthenticationConstants.CLAIM_ROLES);

                authoritiesConverter.setAuthorityPrefix(
                                "ROLE_");

                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

                converter.setJwtGrantedAuthoritiesConverter(
                                authoritiesConverter);

                return converter;
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter)
                        throws Exception {

                http
                                .csrf(
                                                csrf -> csrf.disable())
                                .sessionManagement(
                                                session -> session.sessionCreationPolicy(
                                                                SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(
                                                authorization -> authorization

                                                                .requestMatchers(
                                                                                "/actuator/health",
                                                                                "/actuator/health/**",
                                                                                "/actuator/prometheus")
                                                                .permitAll()

                                                                /*
                                                                 * Login endpoint.
                                                                 */
                                                                .requestMatchers(
                                                                                "/api/auth/login")
                                                                .permitAll()

                                                                /*
                                                                 * MCP transport endpoint.
                                                                 *
                                                                 * The transport itself is public so that the internal
                                                                 * Spring AI MCP Client can establish the MCP session.
                                                                 *
                                                                 * Tenant-private access is NOT authorized here.
                                                                 * Tenant identity is propagated and verified through
                                                                 * the signed short-lived MCP execution token.
                                                                 */
                                                                .requestMatchers(
                                                                                "/mcp",
                                                                                "/mcp/**")
                                                                .permitAll()

                                                                /*
                                                                 * Health endpoint remains public.
                                                                 */
                                                                .requestMatchers(
                                                                                "/actuator/health")
                                                                .permitAll()

                                                                /*
                                                                 * Other actuator endpoints still require
                                                                 * authentication.
                                                                 */
                                                                .requestMatchers(
                                                                                "/actuator/**")
                                                                .authenticated()

                                                                /*
                                                                 * All normal application APIs require JWT
                                                                 * authentication.
                                                                 */
                                                                .anyRequest()
                                                                .authenticated())
                                .oauth2ResourceServer(
                                                resourceServer -> resourceServer.jwt(
                                                                jwt -> jwt.jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter)));

                return http.build();
        }
}