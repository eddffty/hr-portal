package ru.hrportal.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;

@Component
public class DatabaseWorkflow {
    @PersistenceContext
    private EntityManager em;
    private final TransactionTemplate transaction;

    public DatabaseWorkflow(PlatformTransactionManager manager) {
        transaction = new TransactionTemplate(manager);
    }

    public <T> T execute(Supplier<T> action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) return action.get();
        return transaction.execute(
                status -> {
                    em.createNativeQuery("select id from workflow_guard where id = 1 for update")
                            .getSingleResult();
                    return action.get();
                });
    }
}
