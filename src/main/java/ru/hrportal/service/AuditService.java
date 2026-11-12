package ru.hrportal.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import ru.hrportal.model.AuditEvent;
import ru.hrportal.repository.AuditRepository;

@Service
public class AuditService {
    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    public void record(String actor, String action, Object target) {
        repository.save(
                new AuditEvent(repository.nextId(), actor, action, String.valueOf(target), Instant.now()));
    }
}
