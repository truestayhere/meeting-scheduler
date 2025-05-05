package com.truestayhere.meeting_scheduler.config;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "jwt") // Load properties' starting with "jwt" values
@Getter
@Setter
@Validated // Enable validation
public class JwtProperties {

    @NotBlank(message = "JWT secret key must be configured.")
    private String secretKey;

    @NotBlank(message = "JWT issuer must be configured.")
    private String issuer;

    @NotNull(message = "JWT expiration minutes must be configured.")
    @Positive(message = "JWT expiration minutes must be positive.")
    private Integer expirationMinutes;
}
