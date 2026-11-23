package ru.hrportal.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import ru.hrportal.model.*;

@Entity
@Table(name = "time_entries")
public class TimeEntryEntity {
    protected TimeEntryEntity() {
    }

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "user_login")
    private String user;

    @Column(name = "shift_id")
    private long shiftId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public static TimeEntryEntity from(TimeEntry value) {
        TimeEntryEntity entity = new TimeEntryEntity();
        entity.id = value.id();
        entity.user = value.user();
        entity.shiftId = value.shiftId();
        entity.startedAt = value.startedAt();
        entity.endedAt = value.endedAt();
        return entity;
    }

    public TimeEntry toDomain() {
        return new TimeEntry(id, user, shiftId, startedAt, endedAt);
    }
}
