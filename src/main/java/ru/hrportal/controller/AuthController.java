package ru.hrportal.controller;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import ru.hrportal.dto.PortalDtos.Login;
import ru.hrportal.model.UserProfile;
import ru.hrportal.service.*;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final AccountService accounts;
  private final SessionService sessions;

  public AuthController(AccountService accounts, SessionService sessions) {
    this.accounts = accounts;
    this.sessions = sessions;
  }

  @PostMapping("/login")
  public UserProfile login(
      @Valid @RequestBody Login input, HttpServletRequest request, HttpServletResponse response) {
    UserProfile profile = accounts.authenticate(input.login(), input.password());
    sessions.revoke(request);
    HttpSession old = request.getSession(false);
    if (old != null) old.invalidate();
    request.getSession(true).setAttribute("user", profile.login());
    response.setHeader("X-Portal-Token", sessions.create(profile.login()));
    return profile;
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest request) {
    sessions.revoke(request);
    HttpSession session = request.getSession(false);
    if (session != null) session.invalidate();
  }

  @GetMapping("/profile")
  public UserProfile profile(HttpServletRequest request) {
    return sessions.current(request);
  }

  @GetMapping("/rating")
  public List<UserProfile> rating(HttpServletRequest request) {
    sessions.current(request);
    return accounts.rating();
  }
}
