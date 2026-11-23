package ru.hrportal.repository.memory;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ru.hrportal.model.UserSession;
import ru.hrportal.repository.SessionRepository;

public class InMemorySessionRepository implements SessionRepository {
  private final ConcurrentHashMap<String, UserSession> sessions = new ConcurrentHashMap<>();

  public Optional<UserSession> find(String id) {
    return Optional.ofNullable(sessions.get(id));
  }

  public void save(UserSession session) {
    sessions.put(session.id(), session);
  }

  public void delete(String id) {
    sessions.remove(id);
  }

  public void deleteExpired(Instant now) {
    sessions.values().removeIf(s -> !s.expiresAt().isAfter(now));
  }

  public void deleteForLogin(String login) {
    sessions.values().removeIf(s -> s.login().equals(login));
  }
}
