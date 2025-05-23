package com.truestayhere.meeting_scheduler.service;


import com.nimbusds.jose.JWSAlgorithm;
import com.truestayhere.meeting_scheduler.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getExpirationMinutes(), ChronoUnit.MINUTES);

        // Collect roles into a string
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        // Build JWT claims
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer()) // Who issued the token
                .issuedAt(now) // When the token was issued
                .expiresAt(expiresAt) // When the token expires
                .subject(authentication.getName()) // The user's email
                .claim("scope", scope) // Claim for roles
                .build();

        JwsHeader header = JwsHeader.with(JWSAlgorithm.HS256::getName)
                .keyId("jwt-signing-key-1")
                .build();
        log.debug("Explicit JWS Header created: {}", header);

        JwtEncoderParameters parameters = JwtEncoderParameters.from(header, claims);
        log.debug("JwtEncoderParameters created with explicit header.");

        log.debug("Attempting jwtEncoder.encode()...");
        // Encode the claims into a JWT string
        String tokenValue = jwtEncoder.encode(parameters).getTokenValue();

        log.info("Generated JWT for user: {}", authentication.getName());
        log.debug("Token scope: {}", scope);
        return tokenValue;
    }
}
