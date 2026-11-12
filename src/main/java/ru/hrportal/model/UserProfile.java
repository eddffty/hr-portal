package ru.hrportal.model;

import java.util.List;

public record UserProfile(
        String login,
        String name,
        String initials,
        String position,
        String department,
        String email,
        String since,
        int shifts,
        int hours,
        int resolved,
        int punctuality,
        double score,
        int rank,
        List<String> achievements,
        Role role) {
    public boolean isManager() {
        return role == Role.MANAGER;
    }

    public boolean isScheduler() {
        return role == Role.SCHEDULER;
    }
}
