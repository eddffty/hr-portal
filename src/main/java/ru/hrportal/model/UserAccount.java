package ru.hrportal.model;

public record UserAccount(UserProfile profile, byte[] salt, byte[] passwordHash) {
}
