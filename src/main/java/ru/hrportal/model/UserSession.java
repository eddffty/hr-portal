package ru.hrportal.model;

import java.time.Instant;

public record UserSession(String id, String login, Instant expiresAt) {
}
