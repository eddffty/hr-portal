package ru.hrportal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import ru.hrportal.dto.PortalDtos.*;
import ru.hrportal.model.Role;
import ru.hrportal.service.*;

@RestController
@RequestMapping("/api/manager")
public class ApprovalController {
    private final SessionService sessions;
    private final ExchangeService exchange;
    private final PortalQueryService query;

    public ApprovalController(
            SessionService sessions, ExchangeService exchange, PortalQueryService query) {
        this.sessions = sessions;
        this.exchange = exchange;
        this.query = query;
    }

    @GetMapping("/requests")
    public List<RequestView> requests(HttpServletRequest request) {
        return query.manager(sessions.require(request, Role.MANAGER));
    }

    @PostMapping("/requests/{id}")
    public void review(
            @PathVariable long id, @Valid @RequestBody Decision input, HttpServletRequest request) {
        exchange.review(sessions.require(request, Role.MANAGER), id, input.accept(), input.reason());
    }
}
