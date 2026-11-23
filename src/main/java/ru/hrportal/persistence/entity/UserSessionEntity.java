package ru.hrportal.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import ru.hrportal.model.*;

@Entity
@Table(name = "user_sessions")
public class UserSessionEntity {
    protected UserSessionEntity() {
    }

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "login")
    private String login;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public static UserSessionEntity from(UserSession value) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.id = value.id();
        entity.login = value.login();
        entity.expiresAt = value.expiresAt();
        return entity;
    }

    public UserSession toDomain() {
        return new UserSession(id, login, expiresAt);
    }
}
