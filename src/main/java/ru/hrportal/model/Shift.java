package ru.hrportal.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Shift(
        long id,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        int slot,
        int building,
        String room,
        String owner,
        boolean completed) {
    public Shift assign(String login) {
        return new Shift(id, date, start, end, slot, building, room, login, completed);
    }

    public Shift finish() {
        return new Shift(id, date, start, end, slot, building, room, owner, true);
    }
}
