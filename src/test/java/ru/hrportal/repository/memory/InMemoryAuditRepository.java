package ru.hrportal.repository.memory;

import ru.hrportal.model.AuditEvent;
import ru.hrportal.repository.AuditRepository;

public class InMemoryAuditRepository extends InMemoryRepository<AuditEvent>
    implements AuditRepository {
  public InMemoryAuditRepository() {
    super(AuditEvent::id);
  }
}
