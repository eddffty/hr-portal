package ru.hrportal.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import ru.hrportal.model.Role;
import ru.hrportal.service.*;

@RestController
@RequestMapping("/api/time")
public class TimeTrackingController {
    private final SessionService sessions;
    private final TimeTrackingService time;

    public TimeTrackingController(SessionService sessions, TimeTrackingService time) {
        this.sessions = sessions;
        this.time = time;
    }

    @PostMapping("/start/{shiftId}")
    public void start(@PathVariable long shiftId, HttpServletRequest request) {
        time.start(sessions.require(request, Role.EMPLOYEE), shiftId);
    }

    @PostMapping("/stop/{entryId}")
    public void stop(@PathVariable long entryId, HttpServletRequest request) {
        time.stop(sessions.require(request, Role.EMPLOYEE), entryId);
    }
}
