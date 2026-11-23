package ru.hrportal.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import ru.hrportal.model.*;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    protected NotificationEntity() {
    }

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "user_login")
    private String user;

    @Column(name = "text", columnDefinition = "text")
    private String text;

    @Column(name = "is_read")
    private boolean read;

    @Column(name = "created_at")
    private Instant createdAt;

    public static NotificationEntity from(Notification value) {
        NotificationEntity entity = new NotificationEntity();
        entity.id = value.id();
        entity.user = value.user();
        entity.text = value.text();
        entity.read = value.read();
        entity.createdAt = value.createdAt();
        return entity;
    }

    public Notification toDomain() {
        return new Notification(id, user, text, read, createdAt);
    }
}
