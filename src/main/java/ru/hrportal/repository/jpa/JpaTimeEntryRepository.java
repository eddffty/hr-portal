package ru.hrportal.repository.jpa;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.TimeEntry;
import ru.hrportal.persistence.entity.TimeEntryEntity;
import ru.hrportal.repository.TimeEntryRepository;

@Repository
public class JpaTimeEntryRepository extends JpaEntityRepository<TimeEntry, TimeEntryEntity>
        implements TimeEntryRepository {
    public JpaTimeEntryRepository() {
        super(TimeEntryEntity.class, TimeEntryEntity::from, TimeEntryEntity::toDomain);
    }
}
