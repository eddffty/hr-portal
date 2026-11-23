package ru.hrportal.model;

public record Availability(
        long id, String user, int parity, int weekday, int slot, String description) {
}
