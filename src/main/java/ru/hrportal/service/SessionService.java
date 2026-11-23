package ru.hrportal.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.hrportal.exception.DomainException;
import ru.hrportal.model.*;
import ru.hrportal.repository.SessionRepository;

@Service
public class SessionService {
  private final AccountService accounts;
  private final SessionRepository repository;
  private static final Duration TIMEOUT = Duration.ofHours(2);

  public SessionService(AccountService accounts, SessionRepository repository) {
    this.accounts = accounts;
    this.repository = repository;
  }

  public String create(String login) {
    byte[] random = new byte[32];
    new SecureRandom().nextBytes(random);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    Instant now = Instant.now();
    repository.deleteExpired(now);
    repository.save(new UserSession(key(token), login, now.plus(TIMEOUT)));
    return token;
  }

  private String key(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  public void revoke(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer "))
      repository.delete(key(authorization.substring(7)));
  }

  public UserProfile current(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization != null) {
      if (!authorization.startsWith("Bearer "))
        throw new DomainException(HttpStatus.UNAUTHORIZED, "Войдите в систему");
      UserSession tokenSession =
          repository
              .find(key(authorization.substring(7)))
              .orElseThrow(
                  () ->
                      new DomainException(
                          HttpStatus.UNAUTHORIZED, "Сессия завершена. Войдите снова"));
      if (!tokenSession.expiresAt().isAfter(Instant.now())) {
        repository.delete(tokenSession.id());
        throw new DomainException(HttpStatus.UNAUTHORIZED, "Сессия завершена. Войдите снова");
      }
      return accounts.activeProfile(tokenSession.login());
    }
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("user") == null)
      throw new DomainException(HttpStatus.UNAUTHORIZED, "Войдите в систему");
    return accounts.activeProfile((String) session.getAttribute("user"));
  }

  public String login(HttpServletRequest request) {
    return current(request).login();
  }

  public String require(HttpServletRequest request, Role role) {
    UserProfile p = current(request);
    if (p.role() != role)
      throw new DomainException(HttpStatus.FORBIDDEN, "Недостаточно прав для этого действия");
    return p.login();
  }

  public String requirePlanner(HttpServletRequest request) {
    UserProfile profile = current(request);
    if (profile.role() == Role.EMPLOYEE)
      throw new DomainException(HttpStatus.FORBIDDEN, "Недостаточно прав");
    return profile.login();
  }
}
