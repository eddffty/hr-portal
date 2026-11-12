package ru.hrportal.repository.memory;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.TimeEntry;
import ru.hrportal.repository.TimeEntryRepository;

@Repository
public class InMemoryTimeEntryRepository extends InMemoryRepository<TimeEntry>
        implements TimeEntryRepository {
    public InMemoryTimeEntryRepository() {
        super(TimeEntry::id);
    }
}
