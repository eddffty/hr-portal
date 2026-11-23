package ru.hrportal.repository.jpa;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.Notification;
import ru.hrportal.persistence.entity.NotificationEntity;
import ru.hrportal.repository.NotificationRepository;

@Repository
public class JpaNotificationRepository extends JpaEntityRepository<Notification, NotificationEntity>
        implements NotificationRepository {
    public JpaNotificationRepository() {
        super(NotificationEntity.class, NotificationEntity::from, NotificationEntity::toDomain);
    }
}
