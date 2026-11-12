package ru.hrportal.model;

public enum RequestStatus {
    WAITING_COLLEAGUE,
    WAITING_MANAGER,
    APPROVED,
    REJECTED_COLLEAGUE,
    REJECTED_MANAGER,
    CANCELLED;

    public boolean pending() {
        return this == WAITING_COLLEAGUE || this == WAITING_MANAGER;
    }
}
