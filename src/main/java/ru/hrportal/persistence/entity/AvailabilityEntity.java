package ru.hrportal.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import ru.hrportal.model.*;

@Entity
@Table(name = "availability")
public class AvailabilityEntity {
    protected AvailabilityEntity() {
    }

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "user_login")
    private String user;

    @Column(name = "parity")
    private int parity;

    @Column(name = "weekday")
    private int weekday;

    @Column(name = "slot")
    private int slot;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    public static AvailabilityEntity from(Availability value) {
        AvailabilityEntity entity = new AvailabilityEntity();
        entity.id = value.id();
        entity.user = value.user();
        entity.parity = value.parity();
        entity.weekday = value.weekday();
        entity.slot = value.slot();
        entity.description = value.description();
        return entity;
    }

    public Availability toDomain() {
        return new Availability(id, user, parity, weekday, slot, description);
    }
}
