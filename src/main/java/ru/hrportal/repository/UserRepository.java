package ru.hrportal.repository;

import java.util.List;
import java.util.Optional;

import ru.hrportal.model.UserAccount;

public interface UserRepository {
    Optional<UserAccount> find(String login);

    List<UserAccount> findAll();

    void save(UserAccount account);
}
