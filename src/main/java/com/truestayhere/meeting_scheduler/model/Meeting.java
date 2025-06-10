package com.truestayhere.meeting_scheduler.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "meeting")
@Getter
@Setter
@NoArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @ManyToOne // Many meetings can be planned in one location
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToMany // A meeting has several attendees, an attendee can have several meetings planned
    @JoinTable( // Creates table for bidirectional many-to-many relationship
            name = "meeting_attendee",
            joinColumns = @JoinColumn(name = "meeting_id"),
            inverseJoinColumns = @JoinColumn(name = "attendee_id")
    )
    private Set<Attendee> attendees = new HashSet<>(); // Avoiding duplicate attendees

    @Version
    private Integer version;

    public Meeting(String title, LocalDateTime startTime, LocalDateTime endTime, Location location) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
    }

    // Adds attendee to the meeting
    public void addAttendee(Attendee attendee) {
        this.attendees.add(attendee);
    }

    // Removes attendee from the meeting
    public void removeAttendee(Attendee attendee) {
        this.attendees.remove(attendee);
    }
}
