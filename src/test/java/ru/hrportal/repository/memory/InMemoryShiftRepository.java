package ru.hrportal.repository.memory;

import ru.hrportal.model.Shift;
import ru.hrportal.repository.ShiftRepository;

public class InMemoryShiftRepository extends InMemoryRepository<Shift> implements ShiftRepository {
  public InMemoryShiftRepository() {
    super(Shift::id);
  }
}
