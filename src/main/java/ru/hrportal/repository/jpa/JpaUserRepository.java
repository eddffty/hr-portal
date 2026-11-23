package ru.hrportal.repository.jpa;

import jakarta.persistence.*;

import java.util.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.hrportal.model.UserAccount;
import ru.hrportal.persistence.entity.UserAccountEntity;
import ru.hrportal.repository.UserRepository;

@Repository
@Transactional
public class JpaUserRepository implements UserRepository {
    @PersistenceContext
    private EntityManager em;

    public Optional<UserAccount> find(String login) {
        return Optional.ofNullable(em.find(UserAccountEntity.class, login))
                .map(UserAccountEntity::toDomain);
    }

    public List<UserAccount> findAll() {
        return em
                .createQuery("select u from UserAccountEntity u order by u.login", UserAccountEntity.class)
                .getResultList()
                .stream()
                .map(UserAccountEntity::toDomain)
                .toList();
    }

    public void save(UserAccount value) {
        em.merge(UserAccountEntity.from(value));
    }
}
