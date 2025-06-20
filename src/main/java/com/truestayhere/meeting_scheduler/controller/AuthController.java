package com.truestayhere.meeting_scheduler.controller;


import com.truestayhere.meeting_scheduler.dto.request.LoginRequestDTO;
import com.truestayhere.meeting_scheduler.dto.response.LoginResponseDTO;
import com.truestayhere.meeting_scheduler.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j // Automatically adds logger
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    // POST api/auth/token - Check user credentials and return the JWT token
    @PostMapping("/token")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginRequestDTO loginRequest) {
        log.info("Authentication attempt for user: {}", loginRequest.email());

        // Create a user credentials object
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());

        // Trigger UserDetailsService.loadByUsername and PasswordEncoder check
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        log.info("Authentication successfull for user: {}", loginRequest.email());

        // Generate JWT token for the user
        String jwt = tokenService.generateToken(authentication);

        // Return the token in the response
        return ResponseEntity.ok(new LoginResponseDTO(jwt)); // 200 OK
    }

}
