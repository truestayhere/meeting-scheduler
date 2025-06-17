package com.truestayhere.meeting_scheduler.service;


import com.truestayhere.meeting_scheduler.model.Attendee;
import com.truestayhere.meeting_scheduler.repository.AttendeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final AttendeeRepository attendeeRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Fetch an attendee by username (email)
        Attendee attendee = attendeeRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        // Convert an Attendee role to Spring Security GrantedAuthority
        GrantedAuthority authority = new SimpleGrantedAuthority(attendee.getRole().getValue());

        // Return a Spring Security User object
        return new User(
                attendee.getEmail(),
                attendee.getPassword(),
                Collections.singletonList(authority)
        );
    }
}
