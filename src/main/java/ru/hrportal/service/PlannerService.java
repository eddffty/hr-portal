package ru.hrportal.service;

import static ru.hrportal.exception.DomainException.check;

import java.util.*;
import org.springframework.stereotype.Service;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.model.*;
import ru.hrportal.repository.*;

@Service
public class PlannerService {
  private final ShiftService shifts;
  private final AccountService accounts;
  private final RequestRepository requests;
  private final WorkflowLock lock;
  private final NotificationService notes;
  private final AuditService audit;
  private final OfferRepository offers;

  public PlannerService(
      ShiftService shifts,
      AccountService accounts,
      RequestRepository requests,
      WorkflowLock lock,
      NotificationService notes,
      AuditService audit,
      OfferRepository offers) {
    this.shifts = shifts;
    this.accounts = accounts;
    this.requests = requests;
    this.lock = lock;
    this.notes = notes;
    this.audit = audit;
    this.offers = offers;
  }

  public record Candidate(
      String login, String name, boolean requested, boolean available, String reason) {}

  public record Change(
      @jakarta.validation.constraints.Positive long shiftId,
      @jakarta.validation.constraints.Size(max = 40) String owner) {}

  public List<Candidate> candidates(long id) {
    synchronized (lock) {
      Shift shift = shifts.get(id);
      return accounts.all().stream()
          .filter(p -> p.role() == Role.EMPLOYEE)
          .map(
              p -> {
                boolean requested =
                    requests.findAll().stream()
                        .anyMatch(
                            r ->
                                r.type() == RequestType.ASSIGNMENT
                                    && r.status() == RequestStatus.WAITING_MANAGER
                                    && r.initiator().equals(p.login())
                                    && r.shiftIds().contains(id));
                String reason = "";
                try {
                  shifts.available(p.login(), shift, Set.of(id));
                } catch (ru.hrportal.exception.DomainException e) {
                  reason = e.getReason();
                }
                return new Candidate(p.login(), p.name(), requested, reason.isEmpty(), reason);
              })
          .sorted(
              Comparator.comparing(Candidate::available)
                  .reversed()
                  .thenComparing(Candidate::requested, Comparator.reverseOrder())
                  .thenComparing(Candidate::name))
          .toList();
    }
  }

  public void save(String actor, List<Change> changes) {
    synchronized (lock) {
      check(changes != null && !changes.isEmpty() && changes.size() <= 96, "Выберите назначения");
      Set<Long> ids = new HashSet<>();
      for (Change c : changes) check(ids.add(c.shiftId()), "Смена указана дважды");
      for (Change c : changes) {
        Shift shift = shifts.get(c.shiftId());
        shifts.editable(shift);
        check(
            offers.findAll().stream()
                .noneMatch(
                    o ->
                        o.shiftId() == shift.id()
                            && (o.status() == OfferStatus.OPEN
                                || o.status() == OfferStatus.RESERVED)),
            "Сначала снимите смену с биржи");
        check(
            requests.findAll().stream()
                .noneMatch(
                    r ->
                        r.status().pending()
                            && r.type() != RequestType.ASSIGNMENT
                            && r.shiftIds().contains(shift.id())),
            "По смене рассматривается обмен");
        if (c.owner() != null) {
          check(accounts.profile(c.owner()).role() == Role.EMPLOYEE, "Выберите сотрудника");
          shifts.available(c.owner(), shift, ids);
        }
      }
      for (int i = 0; i < changes.size(); i++)
        for (int j = i + 1; j < changes.size(); j++) {
          Change a = changes.get(i), b = changes.get(j);
          if (a.owner() != null && a.owner().equals(b.owner()))
            check(
                !shifts.overlaps(shifts.get(a.shiftId()), shifts.get(b.shiftId())),
                "Выбранные назначения пересекаются");
        }
      for (Change c : changes) {
        Shift old = shifts.get(c.shiftId());
        shifts.assign(c.shiftId(), c.owner());
        notes.send(old.owner(), "Обновлено расписание смены " + old.date());
        notes.send(c.owner(), "Вам назначено дежурство " + old.date());
        audit.record(actor, "PLAN_ASSIGNMENT", c.shiftId());
        if (c.owner() != null)
          for (ExchangeRequest r : requests.findAll())
            if (r.type() == RequestType.ASSIGNMENT
                && r.status() == RequestStatus.WAITING_MANAGER
                && r.shiftIds().contains(c.shiftId())) {
              List<Long> remaining = r.shiftIds().stream().filter(id -> id != c.shiftId()).toList();
              boolean accepted = r.initiator().equals(c.owner());
              if (remaining.isEmpty())
                requests.save(
                    r.status(accepted ? RequestStatus.APPROVED : RequestStatus.REJECTED_MANAGER));
              else
                requests.save(
                    new ExchangeRequest(
                        r.id(),
                        r.type(),
                        r.initiator(),
                        r.recipient(),
                        remaining,
                        r.offerId(),
                        r.status(),
                        r.comment(),
                        r.createdAt()));
              notes.send(
                  r.initiator(),
                  accepted
                      ? "Заявка учтена при составлении графика"
                      : "На запрошенную смену назначен другой сотрудник");
            }
      }
      notes.broadcast("Сохранено обновлённое расписание дежурств");
    }
  }
}
