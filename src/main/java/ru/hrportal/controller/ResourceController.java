package ru.hrportal.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.*;
import ru.hrportal.dto.PortalDtos.*;
import ru.hrportal.model.*;
import ru.hrportal.service.*;

@RestController
@RequestMapping("/api")
public class ResourceController {
    private final SessionService sessions;
    private final ShiftService shifts;
    private final PortalQueryService query;
    private final ExchangeService exchange;
    private final NotificationService notifications;

    public ResourceController(
            SessionService sessions,
            ShiftService shifts,
            PortalQueryService query,
            ExchangeService exchange,
            NotificationService notifications) {
        this.sessions = sessions;
        this.shifts = shifts;
        this.query = query;
        this.exchange = exchange;
        this.notifications = notifications;
    }

    @GetMapping("/shifts")
    public List<ShiftView> shifts(
            HttpServletRequest req,
            @RequestParam(required = false) LocalDate week,
            @RequestParam(required = false) Integer building,
            @RequestParam(defaultValue = "false") boolean mine) {
        String login = sessions.login(req);
        LocalDate start = (week == null ? LocalDate.now() : week).with(java.time.DayOfWeek.MONDAY);
        return shifts.all().stream()
                .filter(s -> !s.date().isBefore(start) && s.date().isBefore(start.plusDays(7)))
                .filter(s -> building == null || s.building() == building)
                .filter(s -> !mine || login.equals(s.owner()))
                .map(query::shift)
                .toList();
    }

    @GetMapping("/shifts/{id}")
    public ShiftView shift(@PathVariable long id, HttpServletRequest req) {
        sessions.current(req);
        return query.shift(shifts.get(id));
    }

    @GetMapping("/offers")
    public List<OfferView> offers(HttpServletRequest req) {
        return query.state(sessions.login(req), LocalDate.now()).offers();
    }

    @GetMapping("/requests")
    public List<RequestView> requests(HttpServletRequest req) {
        UserProfile viewer = sessions.current(req);
        return viewer.isManager()
                ? query.manager(viewer.login())
                : exchange.visibleRequests(viewer.login()).stream()
                .map(r -> query.request(r, viewer.login()))
                .toList();
    }

    @GetMapping("/notifications")
    public List<Notification> notifications(HttpServletRequest req) {
        return notifications.list(sessions.login(req));
    }

    @GetMapping("/time")
    public List<TimeView> time(HttpServletRequest req) {
        return query.state(sessions.login(req), LocalDate.now()).timeEntries();
    }
}
