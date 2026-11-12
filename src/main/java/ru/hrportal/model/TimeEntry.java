package ru.hrportal.model;

import java.time.Instant;

public record TimeEntry(long id, String user, long shiftId, Instant startedAt, Instant endedAt) {
    public TimeEntry finish(Instant end) {
        return new TimeEntry(id, user, shiftId, startedAt, end);
    }
}
