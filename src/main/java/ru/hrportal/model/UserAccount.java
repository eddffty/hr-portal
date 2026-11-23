package ru.hrportal.model;

public record UserAccount(UserProfile profile, byte[] salt, byte[] passwordHash, boolean active) {
    public UserAccount(UserProfile profile, byte[] salt, byte[] passwordHash) {
        this(profile, salt, passwordHash, true);
    }
}
