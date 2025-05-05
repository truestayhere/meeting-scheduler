package com.truestayhere.meeting_scheduler.config;


import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
@Slf4j
public class JwtConfig {

    private final JwtProperties jwtProperties;

    // Decodes the users' token
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("Creating JwtDecoder bean in JwtConfig...");

        if (jwtProperties.getSecretKey() == null || jwtProperties.getSecretKey().isBlank()) {
            log.error("!!! JwtDecoder (JwtConfig): JWT Secret Key is null or empty !!!");
            throw new IllegalStateException("JWT Secret Key is not configured properly for JwtDecoder.");
        }
        log.debug("JwtDecoder (JwtConfig): Using secret key of length: {}", jwtProperties.getSecretKey().length());

        // Create SecretKey instance from the configured secret string
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);

        // Use HS256 algorithm identifier
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        // Build the decoder using the secret key
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }


    // Produces the secure token
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        log.debug("Creating JwtEncoder bean in JwtConfig using provided JWKSource...");

        // Build the encoder using JWKSource
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(jwkSource);
        log.debug("NimbusJwtEncoder created successfully from JWKSource bean.");

        return encoder;
    }


    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        log.info("Creating JWKSource bean in JwtConfig...");
        if (jwtProperties.getSecretKey() == null ||
                jwtProperties.getSecretKey().isBlank()) {
            log.error("!!! JWKSource: JWT Secret Key is null or empty !!!");
            throw new IllegalStateException("JWT Secret Key is not configured properly for JWKSource.");
        }

        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, JWSAlgorithm.HS256.getName());

        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKey)
                .keyID("jwt-signing-key-1")
                .algorithm(JWSAlgorithm.HS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();

        log.debug("Created OctetSequenceKey (JWK) with Key ID: {} and Algorithm: {}", jwk.getKeyID(), jwk.getAlgorithm());

        JWKSet jwkSet = new JWKSet(jwk);

        return new ImmutableJWKSet<>(jwkSet);
    }
}
