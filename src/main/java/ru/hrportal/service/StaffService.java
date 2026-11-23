package ru.hrportal.service;

import static ru.hrportal.exception.DomainException.check;

import java.util.*;
import org.springframework.stereotype.Service;
import ru.hrportal.model.*;
import ru.hrportal.repository.UserRepository;

@Service
public class StaffService {
  private ru.hrportal.repository.SessionRepository sessions;

  @org.springframework.beans.factory.annotation.Autowired
  public void setSessions(ru.hrportal.repository.SessionRepository sessions) {
    this.sessions = sessions;
  }

  private final UserRepository users;
  private final PasswordHasher passwords;

  public StaffService(UserRepository users, PasswordHasher passwords) {
    this.users = users;
    this.passwords = passwords;
  }

  public record Staff(UserProfile profile, boolean active) {}

  public record Input(
      String login,
      String name,
      String position,
      String department,
      String email,
      String password,
      boolean active) {}

  public synchronized List<Staff> list() {
    return users.findAll().stream()
        .filter(a -> a.profile().role() == Role.EMPLOYEE)
        .map(a -> new Staff(a.profile(), a.active()))
        .toList();
  }

  public synchronized void save(Input input, boolean create) {
    check(
        input.login() != null && input.login().matches("[a-z][a-z0-9_.-]{2,39}"),
        "Логин: от 3 до 40 латинских символов");
    check(
        input.name() != null && !input.name().isBlank() && input.name().length() <= 100,
        "Введите имя до 100 символов");
    check(
        input.position() != null
            && input.position().length() <= 100
            && input.department() != null
            && input.department().length() <= 150
            && input.email() != null
            && input.email().length() <= 150,
        "Проверьте данные сотрудника");
    UserAccount old = users.find(input.login()).orElse(null);
    check(
        create ? old == null : old != null && old.profile().role() == Role.EMPLOYEE,
        create ? "Логин уже занят" : "Сотрудник не найден");
    check(
        !create || (input.password() != null && input.password().length() >= 8),
        "Пароль должен содержать не менее 8 символов");
    check(
        input.password() == null
            || input.password().isEmpty()
            || input.password().length() >= 8 && input.password().length() <= 100,
        "Пароль должен содержать от 8 до 100 символов");
    String initials =
        Arrays.stream(input.name().trim().split("\\s+"))
            .limit(2)
            .map(p -> p.substring(0, 1))
            .reduce("", String::concat)
            .toUpperCase();
    UserProfile p = old == null ? null : old.profile();
    UserProfile updated =
        new UserProfile(
            input.login(),
            input.name().trim(),
            initials,
            input.position(),
            input.department(),
            input.email(),
            p == null ? java.time.LocalDate.now().toString() : p.since(),
            p == null ? 0 : p.shifts(),
            p == null ? 0 : p.hours(),
            p == null ? 0 : p.resolved(),
            p == null ? 100 : p.punctuality(),
            p == null ? 5 : p.score(),
            p == null ? 0 : p.rank(),
            p == null ? List.of() : p.achievements(),
            Role.EMPLOYEE);
    byte[] salt = old == null ? passwords.salt() : old.salt(),
        hash = old == null ? null : old.passwordHash();
    if (input.password() != null && !input.password().isEmpty()) {
      salt = passwords.salt();
      hash = passwords.hash(input.password(), salt);
    }
    users.save(new UserAccount(updated, salt, hash, input.active()));
    if (sessions != null
        && (!input.active() || input.password() != null && !input.password().isEmpty()))
      sessions.deleteForLogin(input.login());
  }
}
