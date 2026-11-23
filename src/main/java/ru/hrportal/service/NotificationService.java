package ru.hrportal.service;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.model.*;
import ru.hrportal.repository.*;

@Service
public class NotificationService {
  private final NotificationRepository repository;
  private final UserRepository users;
  private final WorkflowLock lock;

  public NotificationService(
      NotificationRepository repository, UserRepository users, WorkflowLock lock) {
    this.repository = repository;
    this.users = users;
    this.lock = lock;
  }

  public void send(String user, String text) {
    synchronized (lock) {
      if (user != null)
        repository.save(new Notification(repository.nextId(), user, text, false, Instant.now()));
    }
  }

  public void broadcast(String text) {
    users.findAll().forEach(a -> send(a.profile().login(), text));
  }

  public void managers(String text) {
    users.findAll().stream()
        .filter(a -> a.profile().isManager())
        .forEach(a -> send(a.profile().login(), text));
  }

  public List<Notification> list(String user) {
    synchronized (lock) {
      return repository.findAll().stream()
          .filter(n -> n.user().equals(user))
          .sorted(Comparator.comparing(Notification::id).reversed())
          .toList();
    }
  }

  public void markRead(String user) {
    synchronized (lock) {
      list(user).forEach(n -> repository.save(n.markRead()));
    }
  }
}
