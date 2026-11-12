package ru.hrportal.repository.memory;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.AuditEvent;
import ru.hrportal.repository.AuditRepository;

@Repository
public class InMemoryAuditRepository extends InMemoryRepository<AuditEvent>
        implements AuditRepository {
    public InMemoryAuditRepository() {
        super(AuditEvent::id);
    }
}
