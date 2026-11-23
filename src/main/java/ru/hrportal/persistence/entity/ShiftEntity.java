package ru.hrportal.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import ru.hrportal.model.*;

@Entity
@Table(name = "shifts")
public class ShiftEntity {
    protected ShiftEntity() {
    }

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "shift_date")
    private LocalDate date;

    @Column(name = "starts_at")
    private LocalTime start;

    @Column(name = "ends_at")
    private LocalTime end;

    @Column(name = "slot")
    private int slot;

    @Column(name = "building")
    private int building;

    @Column(name = "room")
    private String room;

    @Column(name = "owner")
    private String owner;

    @Column(name = "completed")
    private boolean completed;

    public static ShiftEntity from(Shift value) {
        ShiftEntity entity = new ShiftEntity();
        entity.id = value.id();
        entity.date = value.date();
        entity.start = value.start();
        entity.end = value.end();
        entity.slot = value.slot();
        entity.building = value.building();
        entity.room = value.room();
        entity.owner = value.owner();
        entity.completed = value.completed();
        return entity;
    }

    public Shift toDomain() {
        return new Shift(id, date, start, end, slot, building, room, owner, completed);
    }
}
