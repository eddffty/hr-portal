package ru.hrportal.service;

import java.time.*;
import java.util.*;

import org.springframework.stereotype.Service;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.dto.PortalDtos.*;
import ru.hrportal.model.*;
import ru.hrportal.repository.*;

@Service
public class PortalQueryService {
    private final ShiftService shifts;
    private final ExchangeService exchange;
    private final AccountService accounts;
    private final NotificationService notifications;
    private final TimeTrackingService time;
    private final RequestRepository requests;
    private final OfferRepository offers;
    private final WorkflowLock lock;

    public PortalQueryService(
            ShiftService shifts,
            ExchangeService exchange,
            AccountService accounts,
            NotificationService notifications,
            TimeTrackingService time,
            RequestRepository requests,
            OfferRepository offers,
            WorkflowLock lock) {
        this.shifts = shifts;
        this.exchange = exchange;
        this.accounts = accounts;
        this.notifications = notifications;
        this.time = time;
        this.requests = requests;
        this.offers = offers;
        this.lock = lock;
    }

    private String name(String login) {
        return login == null ? "Свободно" : accounts.profile(login).name();
    }

    public ShiftView shift(Shift s) {
        boolean locked =
                requests.findAll().stream()
                        .anyMatch(r -> r.status().pending() && r.shiftIds().contains(s.id()));
        Long offerId =
                offers.findAll().stream()
                        .filter(
                                o ->
                                        o.shiftId() == s.id()
                                                && (o.status() == OfferStatus.OPEN || o.status() == OfferStatus.RESERVED))
                        .map(ExchangeOffer::id)
                        .findFirst()
                        .orElse(null);
        return new ShiftView(
                s.id(),
                s.date(),
                s.start(),
                s.end(),
                s.slot(),
                s.building(),
                s.room(),
                s.owner(),
                name(s.owner()),
                s.completed(),
                locked,
                offerId);
    }

    private OfferView offer(ExchangeOffer o, String user) {
        Shift s = shifts.get(o.shiftId());
        boolean available = shifts.mine(user).stream().noneMatch(other -> shifts.overlaps(other, s));
        return new OfferView(
                o.id(),
                shift(s),
                o.owner(),
                name(o.owner()),
                o.status(),
                o.owner().equals(user),
                available);
    }

    public RequestView request(ExchangeRequest r, String viewer) {
        return new RequestView(
                r.id(),
                r.type(),
                r.status(),
                r.initiator(),
                name(r.initiator()),
                r.recipient(),
                r.recipient() == null ? "Руководитель" : name(r.recipient()),
                r.shiftIds().stream().map(shifts::get).map(this::shift).toList(),
                r.comment(),
                r.createdAt(),
                r.status() == RequestStatus.WAITING_COLLEAGUE && viewer.equals(r.recipient()),
                r.status().pending() && viewer.equals(r.initiator()));
    }

    private TimeView entry(TimeEntry e) {
        Shift s = shifts.get(e.shiftId());
        long minutes =
                Duration.between(e.startedAt(), e.endedAt() == null ? Instant.now() : e.endedAt())
                        .toMinutes();
        long planned = Duration.between(s.start(), s.end()).toMinutes();
        return new TimeView(
                e.id(),
                e.user(),
                name(e.user()),
                shift(s),
                e.startedAt(),
                e.endedAt(),
                minutes,
                planned,
                e.endedAt() == null ? 0 : minutes - planned);
    }

    public State state(String user, LocalDate week) {
        synchronized (lock) {
            LocalDate start = week.with(DayOfWeek.MONDAY);
            return new State(
                    LocalDate.now(),
                    start,
                    shifts.all().stream()
                            .filter(s -> !s.date().isBefore(start) && s.date().isBefore(start.plusDays(6)))
                            .map(this::shift)
                            .toList(),
                    shifts.mine(user).stream().map(this::shift).toList(),
                    exchange.visibleOffers(user).stream().map(o -> offer(o, user)).toList(),
                    exchange.visibleRequests(user).stream().map(r -> request(r, user)).toList(),
                    notifications.list(user),
                    time.list(user, accounts.profile(user).role() != Role.EMPLOYEE).stream()
                            .map(this::entry)
                            .toList(),
                    accounts.all().stream().filter(p -> p.role() == Role.EMPLOYEE).toList());
        }
    }

    public List<RequestView> manager(String viewer) {
        synchronized (lock) {
            return exchange.managerRequests().stream().map(r -> request(r, viewer)).toList();
        }
    }
}
