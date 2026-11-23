package ru.hrportal.repository.memory;

import ru.hrportal.model.Notification;
import ru.hrportal.repository.NotificationRepository;

public class InMemoryNotificationRepository extends InMemoryRepository<Notification>
    implements NotificationRepository {
  public InMemoryNotificationRepository() {
    super(Notification::id);
  }
}
