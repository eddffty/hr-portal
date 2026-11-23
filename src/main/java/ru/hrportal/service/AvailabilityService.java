package ru.hrportal.service;

import static ru.hrportal.exception.DomainException.check;

import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;
import org.springframework.stereotype.Service;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.model.*;
import ru.hrportal.repository.AvailabilityRepository;

@Service
public class AvailabilityService {
  private final AvailabilityRepository repository;
  private final WorkflowLock lock;

  public AvailabilityService(AvailabilityRepository repository, WorkflowLock lock) {
    this.repository = repository;
    this.lock = lock;
  }

  public static int parity(LocalDate date) {
    return date.get(WeekFields.ISO.weekOfWeekBasedYear()) % 2;
  }

  public List<Availability> list(String user) {
    synchronized (lock) {
      return repository.findAll().stream().filter(r -> r.user().equals(user)).toList();
    }
  }

  public void save(String user, int parity, List<String> cells, String description) {
    synchronized (lock) {
      check(parity == 0 || parity == 1, "Выберите чётную или нечётную неделю");
      check(cells != null && !cells.isEmpty() && cells.size() <= 48, "Выберите ячейки занятости");
      check(
          description != null && !description.isBlank() && description.length() <= 200,
          "Введите описание до 200 символов");
      for (String cell : cells)
        check(cell != null && cell.matches("[1-6]-[0-7]"), "Неверная ячейка занятости");
      for (String cell : new HashSet<>(cells)) {
        int day = cell.charAt(0) - '0', slot = cell.charAt(2) - '0';
        long id =
            list(user).stream()
                .filter(r -> r.parity() == parity && r.weekday() == day && r.slot() == slot)
                .map(Availability::id)
                .findFirst()
                .orElseGet(repository::nextId);
        repository.save(new Availability(id, user, parity, day, slot, description.trim()));
      }
    }
  }

  public void delete(String user, long id) {
    synchronized (lock) {
      Availability row =
          repository
              .find(id)
              .orElseThrow(
                  () ->
                      new ru.hrportal.exception.DomainException(
                          org.springframework.http.HttpStatus.NOT_FOUND, "Занятость не найдена"));
      check(row.user().equals(user), "Можно удалять только свою занятость");
      repository.delete(id);
    }
  }
}
