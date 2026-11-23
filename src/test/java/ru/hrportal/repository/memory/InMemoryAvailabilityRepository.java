package ru.hrportal.repository.memory;

import java.util.*;
import ru.hrportal.model.Availability;
import ru.hrportal.repository.AvailabilityRepository;

public class InMemoryAvailabilityRepository implements AvailabilityRepository {
  private final Map<Long, Availability> rows = new LinkedHashMap<>();
  private long sequence;

  public synchronized Optional<Availability> find(long id) {
    return Optional.ofNullable(rows.get(id));
  }

  public synchronized List<Availability> findAll() {
    return List.copyOf(rows.values());
  }

  public synchronized Availability save(Availability value) {
    rows.put(value.id(), value);
    return value;
  }

  public synchronized long nextId() {
    return ++sequence;
  }

  public synchronized void delete(long id) {
    rows.remove(id);
  }
}
