package ru.hrportal.repository;

import java.util.List;
import java.util.Optional;

public interface EntityRepository<T> {
    Optional<T> find(long id);

    List<T> findAll();

    T save(T value);

    long nextId();
}
