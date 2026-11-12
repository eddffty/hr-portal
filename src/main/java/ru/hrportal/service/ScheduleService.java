package ru.hrportal.service;

import static ru.hrportal.exception.DomainException.check;

import org.springframework.stereotype.Service;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.model.*;
import ru.hrportal.repository.*;

@Service
public class ScheduleService {
  private final ShiftService shifts;
  private final OfferRepository offers;
  private final UserRepository users;
  private final NotificationService notifications;
  private final AuditService audit;
  private final WorkflowLock lock;
  private final ShiftRepository repository;

  public ScheduleService(
      ShiftService shifts,
      OfferRepository offers,
      UserRepository users,
      NotificationService notifications,
      AuditService audit,
      WorkflowLock lock,
      ShiftRepository repository) {
    this.shifts = shifts;
    this.offers = offers;
    this.users = users;
    this.notifications = notifications;
    this.audit = audit;
    this.lock = lock;
    this.repository = repository;
  }

  public void create(String actor, ru.hrportal.dto.PortalDtos.NewShift input) {
    synchronized (lock) {
      check(input.building() == 1 || input.building() == 4, "Выберите корпус 1 или 4");
      check(
          input.date().getDayOfWeek() != java.time.DayOfWeek.SUNDAY,
          "Дежурства создаются с понедельника по субботу");
      check(!input.date().isBefore(java.time.LocalDate.now()), "Нельзя создать прошедшую смену");
      check(
          shifts.all().stream()
              .noneMatch(
                  s ->
                      s.date().equals(input.date())
                          && s.slot() == input.slot()
                          && s.building() == input.building()),
          "Смена на это время и корпус уже существует");
      java.time.LocalTime start =
          java.time.LocalTime.parse(
              new String[] {"08:30", "10:05", "11:40", "13:45", "15:20", "16:55", "18:30", "20:00"}
                  [input.slot()]);
      Shift shift =
          new Shift(
              0,
              input.date(),
              start,
              start.plusMinutes(80),
              input.slot(),
              input.building(),
              input.room(),
              input.owner(),
              false);
      if (input.owner() != null) {
        check(
            users.find(input.owner()).map(a -> a.profile().role() == Role.EMPLOYEE).orElse(false),
            "Сотрудник не найден");
        shifts.available(input.owner(), shift, java.util.Set.of());
      }
      Shift created =
          new Shift(
              repository.nextId(),
              shift.date(),
              shift.start(),
              shift.end(),
              shift.slot(),
              shift.building(),
              shift.room(),
              shift.owner(),
              false);
      repository.save(created);
      notifications.send(input.owner(), "Вам назначено новое дежурство " + input.date());
      audit.record(actor, "CREATE_SHIFT", created.id());
    }
  }

  public void assign(String actor, long id, String owner) {
    synchronized (lock) {
      Shift shift = shifts.get(id);
      shifts.editable(shift);
      shifts.unlocked(id, null);
      check(
          offers.findAll().stream()
              .noneMatch(
                  o ->
                      o.shiftId() == id
                          && (o.status() == OfferStatus.OPEN
                              || o.status() == OfferStatus.RESERVED)),
          "Смена опубликована на бирже");
      if (owner != null) {
        check(
            users.find(owner).map(a -> a.profile().role() == Role.EMPLOYEE).orElse(false),
            "Сотрудник не найден");
        shifts.available(owner, shift, java.util.Set.of(id));
      }
      shifts.assign(id, owner);
      notifications.send(shift.owner(), "Ответственный изменил назначение вашей смены");
      notifications.send(owner, "Вам назначено дежурство " + shift.date());
      audit.record(actor, "ASSIGN_SHIFT", id);
    }
  }

  public void publish(String actor) {
    synchronized (lock) {
      notifications.broadcast("Опубликовано обновлённое расписание дежурств");
      audit.record(actor, "PUBLISH_SCHEDULE", "schedule");
    }
  }
}
