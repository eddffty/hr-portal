package ru.hrportal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import ru.hrportal.service.*;
import ru.hrportal.service.PlannerService.Change;

@RestController
@RequestMapping("/api/planner")
public class PlannerController {
    private final SessionService sessions;
    private final PlannerService planner;

    public PlannerController(SessionService sessions, PlannerService planner) {
        this.sessions = sessions;
        this.planner = planner;
    }

    public record Plan(@NotEmpty @Size(max = 96) List<@NotNull @Valid Change> changes) {
    }

    @GetMapping("/candidates/{id}")
    public List<PlannerService.Candidate> candidates(@PathVariable long id, HttpServletRequest req) {
        sessions.requirePlanner(req);
        return planner.candidates(id);
    }

    @PostMapping("/save")
    public void save(@Valid @RequestBody Plan input, HttpServletRequest req) {
        planner.save(sessions.requirePlanner(req), input.changes());
    }
}
