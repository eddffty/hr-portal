package ru.hrportal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import ru.hrportal.model.Shift;
import ru.hrportal.repository.ShiftRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TabAuthenticationTest {
  @Autowired MockMvc mvc;
  @Autowired ShiftRepository shifts;

  private record Login(String token, MockHttpSession cookie) {}

  private Login login(String user, String password, MockHttpSession cookie) throws Exception {
    var request =
        post("/api/login")
            .contentType("application/json")
            .content("{\"login\":\"" + user + "\",\"password\":\"" + password + "\"}");
    if (cookie != null) request.session(cookie);
    var result =
        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Portal-Token"))
            .andReturn();
    return new Login(
        result.getResponse().getHeader("X-Portal-Token"),
        (MockHttpSession) result.getRequest().getSession());
  }

  @Test
  void glukhovCanAcceptOfferWhileAnotherTabIsLoggedInAsManager() throws Exception {
    Login employee = login("glukhov", "Pavel2026!", null);
    Login manager = login("manager", "Manager2026!", employee.cookie());
    assertTrue(employee.cookie().isInvalid());
    Login owner = login("ivanov", "Ivanov2026!", null);
    LocalDate future = LocalDate.now().plusDays(90);
    Shift shift =
        shifts.save(
            new Shift(
                shifts.nextId(),
                future,
                LocalTime.of(8, 30),
                LocalTime.of(9, 50),
                0,
                1,
                "1108",
                "ivanov",
                false));
    mvc.perform(
            post("/api/offers/" + shift.id()).header("Authorization", "Bearer " + owner.token()))
        .andExpect(status().isOk());
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var state =
        json.readTree(
            mvc.perform(
                    get("/api/state")
                        .header("Authorization", "Bearer " + employee.token())
                        .session(manager.cookie()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    long offerId = 0;
    for (var offer : state.get("offers"))
      if (offer.get("shift").get("id").asLong() == shift.id()) offerId = offer.get("id").asLong();
    assertTrue(offerId > 0);
    mvc.perform(
            post("/api/exchange/" + offerId + "/apply")
                .header("Authorization", "Bearer " + employee.token())
                .session(manager.cookie()))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/profile")
                .header("Authorization", "Bearer " + employee.token())
                .session(manager.cookie()))
        .andExpect(jsonPath("$.login").value("glukhov"));
    mvc.perform(
            get("/api/manager/requests")
                .header("Authorization", "Bearer " + employee.token())
                .session(manager.cookie()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/manager/requests").header("Authorization", "Bearer " + manager.token()))
        .andExpect(status().isOk());
  }

  @Test
  void logoutOnlyRevokesTheCurrentTabAndInvalidTokenNeverFallsBackToCookie() throws Exception {
    Login employee = login("glukhov", "Pavel2026!", null);
    Login manager = login("manager", "Manager2026!", employee.cookie());
    mvc.perform(
            get("/api/profile")
                .header("Authorization", "Bearer invalid-token")
                .session(manager.cookie()))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/logout")
                .header("Authorization", "Bearer " + employee.token())
                .session(manager.cookie()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/profile").header("Authorization", "Bearer " + employee.token()))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/profile").header("Authorization", "Bearer " + manager.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.login").value("manager"));
  }
}
