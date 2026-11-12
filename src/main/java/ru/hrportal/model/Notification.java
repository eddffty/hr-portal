package ru.hrportal.model;

import java.time.Instant;

public record Notification(long id, String user, String text, boolean read, Instant createdAt) {
    public Notification markRead() {
        return new Notification(id, user, text, true, createdAt);
    }
}
