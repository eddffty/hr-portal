package ru.hrportal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.hrportal.dto.PortalDtos.*;
import ru.hrportal.model.Role;
import ru.hrportal.service.*;

@RestController
@RequestMapping("/api")
public class ExchangeController {
    private final SessionService sessions;
    private final ExchangeService exchange;

    public ExchangeController(SessionService sessions, ExchangeService exchange) {
        this.sessions = sessions;
        this.exchange = exchange;
    }

    private String employee(HttpServletRequest request) {
        return sessions.require(request, Role.EMPLOYEE);
    }

    @PostMapping("/offers/{shiftId}")
    public void publish(@PathVariable long shiftId, HttpServletRequest request) {
        exchange.publish(employee(request), shiftId);
    }

    @DeleteMapping("/offers/{id}")
    public void withdraw(@PathVariable long id, HttpServletRequest request) {
        exchange.withdraw(employee(request), id);
    }

    @PostMapping("/exchange/{id}/apply")
    public void apply(@PathVariable long id, HttpServletRequest request) {
        exchange.apply(employee(request), id);
    }

    @PostMapping("/exchange/{id}/decline")
    public void decline(@PathVariable long id, HttpServletRequest request) {
        exchange.decline(employee(request), id);
    }

    @PostMapping("/proposals")
    public void propose(@Valid @RequestBody Proposal input, HttpServletRequest request) {
        exchange.propose(employee(request), input.shiftIds(), input.comment());
    }

    @PostMapping("/swaps")
    public void swap(@Valid @RequestBody Swap input, HttpServletRequest request) {
        exchange.swap(employee(request), input.ownShiftId(), input.targetShiftId(), input.comment());
    }

    @PostMapping("/requests/{id}/decision")
    public void respond(
            @PathVariable long id, @Valid @RequestBody Decision input, HttpServletRequest request) {
        exchange.respond(employee(request), id, input.accept());
    }

    @PostMapping("/requests/{id}/cancel")
    public void cancel(@PathVariable long id, HttpServletRequest request) {
        exchange.cancel(employee(request), id);
    }
}
