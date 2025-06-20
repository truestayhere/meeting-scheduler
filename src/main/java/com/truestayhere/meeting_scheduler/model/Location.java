package com.truestayhere.meeting_scheduler.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "working_start_time")
    private LocalTime workingStartTime;

    @Column(name = "working_end_time")
    private LocalTime workingEndTime;

    @Version
    private Integer version;

    public Location(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }
}
