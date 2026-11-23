package ru.hrportal.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import ru.hrportal.service.*;

@RestController
@RequestMapping("/api/staff")
public class StaffController {
    private final SessionService sessions;
    private final StaffService staff;

    public StaffController(SessionService sessions, StaffService staff) {
        this.sessions = sessions;
        this.staff = staff;
    }

    @GetMapping
    public List<StaffService.Staff> list(HttpServletRequest req) {
        sessions.requirePlanner(req);
        return staff.list();
    }

    @PostMapping
    public void create(@RequestBody StaffService.Input input, HttpServletRequest req) {
        sessions.requirePlanner(req);
        staff.save(input, true);
    }

    @PutMapping
    public void update(@RequestBody StaffService.Input input, HttpServletRequest req) {
        sessions.requirePlanner(req);
        staff.save(input, false);
    }
}
