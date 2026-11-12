package ru.hrportal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.*;
import ru.hrportal.dto.PortalDtos.*;
import ru.hrportal.model.Role;
import ru.hrportal.service.*;

@RestController
@RequestMapping("/api")
public class ScheduleController {
    private final SessionService sessions;
    private final PortalQueryService query;
    private final ScheduleService schedules;

    public ScheduleController(
            SessionService sessions, PortalQueryService query, ScheduleService schedules) {
        this.sessions = sessions;
        this.query = query;
        this.schedules = schedules;
    }

    @GetMapping("/state")
    public State state(HttpServletRequest request, @RequestParam(required = false) LocalDate week) {
        return query.state(sessions.login(request), week == null ? LocalDate.now() : week);
    }

    @PutMapping("/schedule/shifts/{id}")
    public void assign(
            @PathVariable long id, @RequestBody Assignment input, HttpServletRequest request) {
        schedules.assign(sessions.require(request, Role.SCHEDULER), id, input.owner());
    }

    @PostMapping("/schedule/publish")
    public void publish(HttpServletRequest request) {
        schedules.publish(sessions.require(request, Role.SCHEDULER));
    }

    @PostMapping("/schedule/shifts")
    public void create(@Valid @RequestBody NewShift input, HttpServletRequest request) {
        schedules.create(sessions.require(request, Role.SCHEDULER), input);
    }
}
