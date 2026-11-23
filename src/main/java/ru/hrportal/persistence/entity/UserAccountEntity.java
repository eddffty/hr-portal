package ru.hrportal.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import ru.hrportal.model.*;

@Entity
@Table(name = "users")
public class UserAccountEntity {
    protected UserAccountEntity() {
    }

    @Id
    @Column(name = "login")
    private String login;

    @Column(name = "name")
    private String name;

    @Column(name = "initials")
    private String initials;

    @Column(name = "position")
    private String position;

    @Column(name = "department")
    private String department;

    @Column(name = "email")
    private String email;

    @Column(name = "employed_since")
    private String since;

    @Column(name = "shifts")
    private int shifts;

    @Column(name = "hours")
    private int hours;

    @Column(name = "resolved")
    private int resolved;

    @Column(name = "punctuality")
    private int punctuality;

    @Column(name = "score")
    private double score;

    @Column(name = "rating_rank")
    private int rank;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "users_achievements", joinColumns = @JoinColumn(name = "entity_id"))
    @Column(name = "item_value")
    @OrderColumn(name = "position_index")
    private List<String> achievements = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Column(name = "salt")
    private byte[] salt;

    @Column(name = "password_hash")
    private byte[] passwordHash;

    @Column(name = "active")
    private boolean active;

    public static UserAccountEntity from(UserAccount value) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.login = value.profile().login();
        entity.name = value.profile().name();
        entity.initials = value.profile().initials();
        entity.position = value.profile().position();
        entity.department = value.profile().department();
        entity.email = value.profile().email();
        entity.since = value.profile().since();
        entity.shifts = value.profile().shifts();
        entity.hours = value.profile().hours();
        entity.resolved = value.profile().resolved();
        entity.punctuality = value.profile().punctuality();
        entity.score = value.profile().score();
        entity.rank = value.profile().rank();
        entity.achievements = new ArrayList<>(value.profile().achievements());
        entity.role = value.profile().role();
        entity.salt = value.salt();
        entity.passwordHash = value.passwordHash();
        entity.active = value.active();
        return entity;
    }

    public UserAccount toDomain() {
        return new UserAccount(
                new UserProfile(
                        login,
                        name,
                        initials,
                        position,
                        department,
                        email,
                        since,
                        shifts,
                        hours,
                        resolved,
                        punctuality,
                        score,
                        rank,
                        achievements,
                        role),
                salt,
                passwordHash,
                active);
    }
}
