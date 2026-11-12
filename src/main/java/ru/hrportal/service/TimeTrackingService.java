package ru.hrportal.service;

import static ru.hrportal.exception.DomainException.check;

import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.model.*;
import ru.hrportal.repository.*;

@Service
public class TimeTrackingService {
  private final TimeEntryRepository entries;
  private final OfferRepository offers;
  private final ShiftService shifts;
  private final WorkflowLock lock;
  private final AuditService audit;

  public TimeTrackingService(
      TimeEntryRepository entries,
      OfferRepository offers,
      ShiftService shifts,
      WorkflowLock lock,
      AuditService audit) {
    this.entries = entries;
    this.offers = offers;
    this.shifts = shifts;
    this.lock = lock;
    this.audit = audit;
  }

  public List<TimeEntry> list(String user, boolean all) {
    synchronized (lock) {
      return entries.findAll().stream().filter(e -> all || e.user().equals(user)).toList();
    }
  }

  public void start(String user, long shiftId) {
    synchronized (lock) {
      Shift shift = shifts.get(shiftId);
      check(user.equals(shift.owner()), "Это не ваша смена");
      check(shift.date().equals(LocalDate.now()), "Начать можно только сегодняшнее дежурство");
      shifts.editable(shift);
      shifts.unlocked(shiftId, null);
      check(
          offers.findAll().stream()
              .noneMatch(
                  o ->
                      o.shiftId() == shiftId
                          && (o.status() == OfferStatus.OPEN
                              || o.status() == OfferStatus.RESERVED)),
          "Сначала снимите смену с биржи");
      check(
          entries.findAll().stream().noneMatch(e -> e.user().equals(user) && e.endedAt() == null),
          "У вас уже есть начатое дежурство");
      entries.save(new TimeEntry(entries.nextId(), user, shiftId, Instant.now(), null));
      audit.record(user, "START_SHIFT", shiftId);
    }
  }

  public void stop(String user, long entryId) {
    synchronized (lock) {
      TimeEntry e =
          entries
              .find(entryId)
              .orElseThrow(
                  () ->
                      new ru.hrportal.exception.DomainException(
                          org.springframework.http.HttpStatus.NOT_FOUND,
                          "Запись времени не найдена"));
      check(e.user().equals(user), "Это чужая запись времени");
      check(e.endedAt() == null, "Дежурство уже завершено");
      entries.save(e.finish(Instant.now()));
      shifts.complete(e.shiftId());
      audit.record(user, "END_SHIFT", e.shiftId());
    }
  }
}
