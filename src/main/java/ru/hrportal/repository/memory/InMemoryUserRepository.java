package ru.hrportal.repository.memory;

import java.util.*;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.UserAccount;
import ru.hrportal.repository.UserRepository;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<String, UserAccount> users = new LinkedHashMap<>();

    public synchronized Optional<UserAccount> find(String login) {
        return Optional.ofNullable(users.get(login));
    }

    public synchronized List<UserAccount> findAll() {
        return List.copyOf(users.values());
    }

    public synchronized void save(UserAccount user) {
        users.put(user.profile().login(), user);
    }
}
