package ru.hrportal.repository.memory;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.Notification;
import ru.hrportal.repository.NotificationRepository;

@Repository
public class InMemoryNotificationRepository extends InMemoryRepository<Notification>
        implements NotificationRepository {
    public InMemoryNotificationRepository() {
        super(Notification::id);
    }
}
