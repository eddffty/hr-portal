package ru.hrportal.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import ru.hrportal.model.*;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {
    protected AuditEventEntity() {
    }

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "actor")
    private String actor;

    @Column(name = "action")
    private String action;

    @Column(name = "target")
    private String target;

    @Column(name = "created_at")
    private Instant createdAt;

    public static AuditEventEntity from(AuditEvent value) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.id = value.id();
        entity.actor = value.actor();
        entity.action = value.action();
        entity.target = value.target();
        entity.createdAt = value.createdAt();
        return entity;
    }

    public AuditEvent toDomain() {
        return new AuditEvent(id, actor, action, target, createdAt);
    }
}
