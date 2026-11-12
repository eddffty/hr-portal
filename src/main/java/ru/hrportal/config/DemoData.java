package ru.hrportal.config;

import jakarta.annotation.PostConstruct;

import java.time.*;
import java.util.*;

import org.springframework.stereotype.Component;
import ru.hrportal.model.*;
import ru.hrportal.repository.*;
import ru.hrportal.service.*;

@Component
public class DemoData {
    private final UserRepository users;
    private final ShiftRepository shifts;
    private final OfferRepository offers;
    private final PasswordHasher passwords;
    private final NotificationService notifications;

    public DemoData(
            UserRepository users,
            ShiftRepository shifts,
            OfferRepository offers,
            PasswordHasher passwords,
            NotificationService notifications) {
        this.users = users;
        this.shifts = shifts;
        this.offers = offers;
        this.passwords = passwords;
        this.notifications = notifications;
    }

    private void add(UserProfile p, String password) {
        byte[] salt = passwords.salt();
        users.save(new UserAccount(p, salt, passwords.hash(password, salt)));
    }

    @PostConstruct
    public void initialize() {
        add(
                new UserProfile(
                        "ivanov",
                        "Иван Иванов",
                        "ИИ",
                        "Специалист поддержки",
                        "Отдел информационных технологий",
                        "ivanov@university.example",
                        "Сентябрь 2025",
                        24,
                        32,
                        86,
                        98,
                        4.7,
                        2,
                        List.of("Надёжный коллега", "20 смен без опозданий", "Помощник команды"),
                        Role.EMPLOYEE),
                "Ivanov2026!");
        add(
                new UserProfile(
                        "smirnova",
                        "Анна Смирнова",
                        "АС",
                        "Старший специалист",
                        "Отдел информационных технологий",
                        "smirnova@university.example",
                        "Март 2024",
                        30,
                        40,
                        112,
                        100,
                        4.9,
                        1,
                        List.of("Лидер месяца", "100 закрытых заявок", "Всегда вовремя"),
                        Role.EMPLOYEE),
                "Anna2026!");
        add(
                new UserProfile(
                        "glukhov",
                        "Павел Глухов",
                        "ПГ",
                        "Специалист поддержки",
                        "Отдел информационных технологий",
                        "glukhov@university.example",
                        "Февраль 2026",
                        18,
                        24,
                        65,
                        95,
                        4.3,
                        3,
                        List.of("Отличный старт", "Помощник команды"),
                        Role.EMPLOYEE),
                "Pavel2026!");
        add(
                new UserProfile(
                        "manager",
                        "Александр Петров",
                        "АП",
                        "Руководитель отдела",
                        "Отдел информационных технологий",
                        "petrov@university.example",
                        "Январь 2023",
                        0,
                        0,
                        0,
                        100,
                        0.0,
                        0,
                        List.of("Руководитель команды"),
                        Role.MANAGER),
                "Manager2026!");
        add(
                new UserProfile(
                        "scheduler",
                        "Мария Соколова",
                        "МС",
                        "Ответственный за расписание",
                        "Отдел информационных технологий",
                        "schedule@university.example",
                        "Май 2024",
                        0,
                        0,
                        0,
                        100,
                        0,
                        0,
                        List.of("Организатор команды"),
                        Role.SCHEDULER),
                "Schedule2026!");
        String[] logins = {"ivanov", "smirnova", "glukhov"};
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        String[] starts = {"08:30", "10:05", "11:40", "13:45", "15:20", "16:55", "18:30", "20:00"};
        for (int w = -2; w <= 5; w++)
            for (int d = 0; d < 6; d++)
                for (int r = 0; r < 8; r++)
                    for (int b : new int[]{1, 4}) {
                        LocalDate date = monday.plusWeeks(w).plusDays(d);
                        int code = Math.floorMod(r + d + w, 4);
                        String owner =
                                b == 1
                                        ? (code == 3 ? null : logins[code])
                                        : (code == 3 ? logins[Math.floorMod(r + d + 1, 3)] : null);
                        Shift shift =
                                new Shift(
                                        shifts.nextId(),
                                        date,
                                        LocalTime.parse(starts[r]),
                                        LocalTime.parse(starts[r]).plusMinutes(80),
                                        r,
                                        b,
                                        b == 1 ? "1108" : "4421",
                                        owner,
                                        date.isBefore(LocalDate.now()));
                        shifts.save(shift);
                    }
        for (String login : logins)
            shifts.findAll().stream()
                    .filter(
                            s -> login.equals(s.owner()) && !s.completed() && s.date().isAfter(LocalDate.now()))
                    .findFirst()
                    .ifPresent(
                            s ->
                                    offers.save(
                                            new ExchangeOffer(
                                                    offers.nextId(), s.id(), login, OfferStatus.OPEN, Set.of())));
        notifications.broadcast(
                "Демонстрационное расписание опубликовано. Данные общие для всех сотрудников.");
    }
}
