package ru.hrportal.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.*;
import java.util.function.Function;

import org.springframework.transaction.annotation.Transactional;
import ru.hrportal.repository.EntityRepository;


@Transactional
public abstract class JpaEntityRepository<T, E> implements EntityRepository<T> {
    @PersistenceContext
    protected EntityManager em;
    private final Class<E> entityType;
    private final Function<T, E> toEntity;
    private final Function<E, T> toDomain;

    protected JpaEntityRepository(
            Class<E> entityType, Function<T, E> toEntity, Function<E, T> toDomain) {
        this.entityType = entityType;
        this.toEntity = toEntity;
        this.toDomain = toDomain;
    }

    public Optional<T> find(long id) {
        return Optional.ofNullable(em.find(entityType, id)).map(toDomain);
    }

    public List<T> findAll() {
        return em
                .createQuery("select e from " + entityType.getSimpleName() + " e order by e.id", entityType)
                .getResultList()
                .stream()
                .map(toDomain)
                .toList();
    }

    public T save(T value) {
        em.merge(toEntity.apply(value));
        return value;
    }

    public long nextId() {
        return ((Number) em.createNativeQuery("select nextval('portal_ids')").getSingleResult())
                .longValue();
    }

    protected void deleteEntity(long id) {
        E entity = em.find(entityType, id);
        if (entity != null) em.remove(entity);
    }
}
