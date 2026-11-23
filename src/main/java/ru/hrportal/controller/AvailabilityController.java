package ru.hrportal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import ru.hrportal.model.*;
import ru.hrportal.service.*;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {
    private final SessionService sessions;
    private final AvailabilityService availability;
    private final AccountService accounts;

    public AvailabilityController(
            SessionService sessions, AvailabilityService availability, AccountService accounts) {
        this.sessions = sessions;
        this.availability = availability;
        this.accounts = accounts;
    }

    public record Input(
            @Min(0) @Max(1) int parity,
            @NotEmpty @Size(max = 48) List<String> cells,
            @NotBlank @Size(max = 200) String description) {
    }

    @GetMapping
    public List<Availability> list(
            @RequestParam(required = false) String user, HttpServletRequest req) {
        UserProfile viewer = sessions.current(req);
        if (user == null) user = viewer.login();
        if (viewer.role() == Role.EMPLOYEE && !viewer.login().equals(user))
            throw new ru.hrportal.exception.DomainException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Недостаточно прав");
        accounts.profile(user);
        return availability.list(user);
    }

    @PutMapping
    public void save(@Valid @RequestBody Input input, HttpServletRequest req) {
        availability.save(
                sessions.require(req, Role.EMPLOYEE), input.parity(), input.cells(), input.description());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id, HttpServletRequest req) {
        availability.delete(sessions.require(req, Role.EMPLOYEE), id);
    }
}
