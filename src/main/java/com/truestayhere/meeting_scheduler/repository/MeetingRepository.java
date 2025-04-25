package com.truestayhere.meeting_scheduler.repository;


import com.truestayhere.meeting_scheduler.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // Find meetings that start in a specific timeframe
    // Example SQL Query:
    // SELECT m.id, m.title, m.startTime, m.endTime, m.location_id FROM meeting m WHERE m.startTime BETWEEN ? AND ? ORDER BY m.startTime;
    List<Meeting> findByStartTimeBetween(LocalDateTime rangeStart, LocalDateTime rangeEnd);
    // Using List here instead of ArrayList allows the method to potentially return any type of list, not just ArrayList

    // Find meetings in a specific location in a specific timeframe
    // Example SQL Query:
    // SELECT m.id, m.title, m.startTime, m.endTime, m.location_id FROM meeting m WHERE location_id = ? AND m.startTime BETWEEN ? AND ? ORDER BY m.startTime;
    List<Meeting> findByLocation_idAndStartTimeBetween(Long locationId, LocalDateTime rangeStart, LocalDateTime rangeEnd);

    // Find meetings that overlap a specific timeframe (if meeting starts before the range end AND ends after the range start)
    // Example SQL Query:
    // SELECT m.id, m.title, m.startTime, m.endTime, m.location_id FROM meeting m WHERE m.startTime < ? AND m.endTime > ? ORDER BY m.startTime;
    List<Meeting> findByStartTimeBeforeAndEndTimeAfter(LocalDateTime rangeEnd, LocalDateTime rangeStart);

    // Find a meeting in a specific location that overlap a specific timeframe
    // Example SQL Query:
    // SELECT m.id, m.title, m.startTime, m.endTime, m.location_id FROM meeting m WHERE location_id = ? AND m.startTime < ? AND m.endTime > ? ORDER BY m.startTime;
    List<Meeting> findByLocation_idAndStartTimeBeforeAndEndTimeAfter(Long locationId, LocalDateTime rangeEnd, LocalDateTime rangeStart);

    // Find meetings attended by specific person
    // Example SQL Query:
    // SELECT m.id, m.title, m.startTime, m.endTime, m.location_id FROM meeting m INNER JOIN meeting_attendee ma ON m.id = ma.meeting_id WHERE ma.attendee_id = ? ORDER BY m.startTime;
    List<Meeting> findByAttendees_id(Long attendeeId);
    // JPA can automatically manage joins (like joining meeting_attendee table in this query)

    // Find meeting attended by specific person that start in a specific timeframe
    // Example SQL Query:
    // SELECT m.id, m.title, m.startTime, m.endTime, m.location_id FROM meeting m INNER JOIN meeting_attendee ma ON m.id = ma.meeting_id WHERE ma.attendee_id = ? AND m.startTime BETWEEN ? AND ? ORDER BY m.startTime;
    List<Meeting> findByAttendees_idAndStartTimeBetween(Long attendeeId, LocalDateTime rangeStart, LocalDateTime rangeEnd);

    // Find meeting attended by specific person that overlap a specific timeframe
    // Example SQL Query:
    // SELECT m.id, m.title, m.startTime, m.endTime, m.location_id FROM meeting m INNER JOIN meeting_attendee ma ON m.id = ma.meeting_id WHERE ma.attendee_id = ? AND m.startTime < ? AND m.endTime > ? ORDER BY m.startTime;
    List<Meeting> findByAttendees_idAndStartTimeBeforeAndEndTimeAfter(Long attendeeId, LocalDateTime rangeEnd, LocalDateTime rangeStart);

    // Find a meeting in specific location that starts and ends at specific time
    // Example SQL Query:
    // SELECT m.id, m.title, m.startTime, m.endTime, m.location_id FROM meeting m WHERE m.location_id = ? AND m.startTime = ? AND m.endTime = ? ORDER BY m.startTime;
    List<Meeting> findByLocation_idAndStartTimeAndEndTime(Long location_id, LocalDateTime startTime, LocalDateTime endTime);
    // Returns List<> in case duplicates exist

    // More queries to be added later...

}
