package ru.hrportal.repository.memory;

import ru.hrportal.model.TimeEntry;
import ru.hrportal.repository.TimeEntryRepository;

public class InMemoryTimeEntryRepository extends InMemoryRepository<TimeEntry>
    implements TimeEntryRepository {
  public InMemoryTimeEntryRepository() {
    super(TimeEntry::id);
  }
}
