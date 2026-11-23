package ru.hrportal.repository.memory;

import java.util.*;
import java.util.function.ToLongFunction;
import ru.hrportal.repository.EntityRepository;

public abstract class InMemoryRepository<T> implements EntityRepository<T> {
  private final Map<Long, T> values = new LinkedHashMap<>();
  private final ToLongFunction<T> id;
  private long sequence;

  protected InMemoryRepository(ToLongFunction<T> id) {
    this.id = id;
  }

  public synchronized Optional<T> find(long key) {
    return Optional.ofNullable(values.get(key));
  }

  public synchronized List<T> findAll() {
    return List.copyOf(values.values());
  }

  public synchronized T save(T value) {
    values.put(id.applyAsLong(value), value);
    return value;
  }

  public synchronized long nextId() {
    return ++sequence;
  }
}
