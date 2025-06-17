package com.truestayhere.meeting_scheduler.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity // tells JPA this is a database table
@Table(name = "attendee")
@Getter
@Setter
@NoArgsConstructor
public class Attendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrementing id
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "working_start_time")
    private LocalTime workingStartTime;

    @Column(name = "working_end_time")
    private LocalTime workingEndTime;

    // Connection to meeting_attendee join table
    @ManyToMany(mappedBy = "attendees")
    private Set<Meeting> meetings = new HashSet<>();

    @Version
    private Integer version;

    // Lombok does empty constructor, getters and setters

    public Attendee(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = Role.USER; // Default role
    }

    // Constructor that also takes the role
    public Attendee(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

}
