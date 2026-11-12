package ru.hrportal.service;

import static ru.hrportal.exception.DomainException.check;

import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.exception.DomainException;
import ru.hrportal.model.*;
import ru.hrportal.repository.*;

@Service
public class ShiftService {
  private final ShiftRepository shifts;
  private final RequestRepository requests;
  private final TimeEntryRepository entries;
  private final WorkflowLock lock;

  public ShiftService(
      ShiftRepository shifts,
      RequestRepository requests,
      TimeEntryRepository entries,
      WorkflowLock lock) {
    this.shifts = shifts;
    this.requests = requests;
    this.entries = entries;
    this.lock = lock;
  }

  public Shift get(long id) {
    return shifts
        .find(id)
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Смена не найдена"));
  }

  public List<Shift> all() {
    synchronized (lock) {
      return shifts.findAll().stream()
          .sorted(
              Comparator.comparing(Shift::date)
                  .thenComparing(Shift::start)
                  .thenComparing(Shift::building))
          .toList();
    }
  }

  public List<Shift> mine(String user) {
    return all().stream().filter(s -> user.equals(s.owner())).toList();
  }

  public void editable(Shift shift) {
    check(!shift.completed(), "Дежурство уже завершено");
    check(!shift.date().isBefore(LocalDate.now()), "Прошедшую смену нельзя изменить");
    check(
        entries.findAll().stream().noneMatch(e -> e.shiftId() == shift.id()),
        "Учёт времени этой смены уже начат");
  }

  public boolean overlaps(Shift a, Shift b) {
    return a.date().equals(b.date()) && a.start().isBefore(b.end()) && b.start().isBefore(a.end());
  }

  public void available(String user, Shift target, Set<Long> excluding) {
    check(
        all().stream()
            .noneMatch(
                s -> user.equals(s.owner()) && !excluding.contains(s.id()) && overlaps(s, target)),
        "У сотрудника есть другая смена в это время");
  }

  public void unlocked(long id, Long exceptRequest) {
    check(
        requests.findAll().stream()
            .noneMatch(
                r ->
                    r.status().pending()
                        && r.shiftIds().contains(id)
                        && !Objects.equals(r.id(), exceptRequest)),
        "По этой смене уже рассматривается заявка");
  }

  public Shift assign(long id, String user) {
    Shift updated = get(id).assign(user);
    shifts.save(updated);
    return updated;
  }

  public void complete(long id) {
    shifts.save(get(id).finish());
  }
}
