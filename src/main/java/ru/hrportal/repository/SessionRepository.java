package ru.hrportal.repository;

import java.time.Instant;
import java.util.Optional;
import ru.hrportal.model.UserSession;

public interface SessionRepository {
  Optional<UserSession> find(String id);

  void save(UserSession session);

  void delete(String id);

  void deleteExpired(Instant now);

  void deleteForLogin(String login);
}
