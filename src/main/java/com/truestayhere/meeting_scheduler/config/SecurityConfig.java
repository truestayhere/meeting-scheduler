package com.truestayhere.meeting_scheduler.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // Marks as configuration class
@EnableWebSecurity // Enables Spring Security's web security
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF protection
                .csrf(AbstractHttpConfigurer::disable)
                // Require authentication for ALL requests for now
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().authenticated())
                // Configure session management to be stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Enable HTTP Basic authentication
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    // (add later) Bcrypt password encoder Bean
}
