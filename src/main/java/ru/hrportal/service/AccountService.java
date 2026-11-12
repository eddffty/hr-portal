package ru.hrportal.service;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.hrportal.exception.DomainException;
import ru.hrportal.model.*;
import ru.hrportal.repository.UserRepository;

@Service
public class AccountService {
    private final UserRepository users;
    private final PasswordHasher passwords;

    public AccountService(UserRepository users, PasswordHasher passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    public UserProfile authenticate(String login, String password) {
        UserAccount user = users.find(login.trim().toLowerCase(Locale.ROOT)).orElse(null);
        UserAccount candidate = user == null ? users.findAll().get(0) : user;
        boolean valid = passwords.matches(password, candidate.salt(), candidate.passwordHash());
        if (user == null || !valid)
            throw new DomainException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        return user.profile();
    }

    public UserProfile profile(String login) {
        return users
                .find(login)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "Войдите в систему"))
                .profile();
    }

    public List<UserProfile> all() {
        return users.findAll().stream().map(UserAccount::profile).toList();
    }

    public List<UserProfile> rating() {
        return all().stream()
                .filter(p -> p.role() == Role.EMPLOYEE)
                .sorted(Comparator.comparingDouble(UserProfile::score).reversed())
                .toList();
    }
}
