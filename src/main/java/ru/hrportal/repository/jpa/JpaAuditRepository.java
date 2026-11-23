package ru.hrportal.repository.jpa;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.AuditEvent;
import ru.hrportal.persistence.entity.AuditEventEntity;
import ru.hrportal.repository.AuditRepository;

@Repository
public class JpaAuditRepository extends JpaEntityRepository<AuditEvent, AuditEventEntity>
        implements AuditRepository {
    public JpaAuditRepository() {
        super(AuditEventEntity.class, AuditEventEntity::from, AuditEventEntity::toDomain);
    }
}
