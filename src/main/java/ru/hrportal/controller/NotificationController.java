package ru.hrportal.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import ru.hrportal.service.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final SessionService sessions;
    private final NotificationService notifications;

    public NotificationController(SessionService sessions, NotificationService notifications) {
        this.sessions = sessions;
        this.notifications = notifications;
    }

    @PostMapping("/read")
    public void read(HttpServletRequest request) {
        notifications.markRead(sessions.login(request));
    }
}
