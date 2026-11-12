package ru.hrportal.model;

import java.time.Instant;

public record AuditEvent(long id, String actor, String action, String target, Instant createdAt) {
}
