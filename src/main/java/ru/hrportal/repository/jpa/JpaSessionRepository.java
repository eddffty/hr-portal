package ru.hrportal.repository.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.hrportal.model.UserSession;
import ru.hrportal.persistence.entity.UserSessionEntity;
import ru.hrportal.repository.SessionRepository;

@Repository
@Transactional
public class JpaSessionRepository implements SessionRepository {
    @PersistenceContext
    private EntityManager em;

    public Optional<UserSession> find(String id) {
        return Optional.ofNullable(em.find(UserSessionEntity.class, id))
                .map(UserSessionEntity::toDomain);
    }

    public void save(UserSession value) {
        em.merge(UserSessionEntity.from(value));
    }

    public void delete(String id) {
        UserSessionEntity value = em.find(UserSessionEntity.class, id);
        if (value != null) em.remove(value);
    }

    public void deleteExpired(Instant now) {
        em.createQuery("delete from UserSessionEntity s where s.expiresAt <= :now")
                .setParameter("now", now)
                .executeUpdate();
    }

    public void deleteForLogin(String login) {
        em.createQuery("delete from UserSessionEntity s where s.login = :login")
                .setParameter("login", login)
                .executeUpdate();
    }
}
