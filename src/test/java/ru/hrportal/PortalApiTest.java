package ru.hrportal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PortalApiTest {
  @Autowired MockMvc mvc;

  @Test
  void scheduleIsIdenticalForEmployeeManagerAndScheduler() throws Exception {
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var employee = login("glukhov", "Pavel2026!");
    var manager = login("manager", "Manager2026!");
    var scheduler = login("scheduler", "Schedule2026!");
    String week = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();
    String path = "/api/state?week=" + week;
    var employeeSchedule =
        json.readTree(
                mvc.perform(get(path).session(employee))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("schedule");
    var managerSchedule =
        json.readTree(
                mvc.perform(get(path).session(manager))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("schedule");
    var schedulerSchedule =
        json.readTree(
                mvc.perform(get(path).session(scheduler))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("schedule");
    org.junit.jupiter.api.Assertions.assertEquals(employeeSchedule, managerSchedule);
    org.junit.jupiter.api.Assertions.assertEquals(employeeSchedule, schedulerSchedule);
    org.junit.jupiter.api.Assertions.assertTrue(
        java.util.stream.StreamSupport.stream(managerSchedule.spliterator(), false)
            .anyMatch(s -> s.get("owner").asText().equals("glukhov")));
  }

  private MockHttpSession login(String user, String password) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/login")
                    .contentType("application/json")
                    .content("{\"login\":\"" + user + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(user))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn()
            .getRequest()
            .getSession();
  }

  @Test
  void closedAccessAndRolePermissions() throws Exception {
    mvc.perform(get("/api/state")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/manager/requests")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/login")
                .contentType("application/json")
                .content("{\"login\":\"demo\",\"password\":\"demo\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/login")
                .contentType("application/json")
                .content("{\"login\":\"ivanov\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
    MockHttpSession employee = login("ivanov", "Ivanov2026!");
    mvc.perform(get("/api/manager/requests").session(employee)).andExpect(status().isForbidden());
    mvc.perform(post("/api/schedule/publish").session(employee)).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/manager/requests/1")
                .session(employee)
                .contentType("application/json")
                .content("{\"accept\":true}"))
        .andExpect(status().isForbidden());
    MockHttpSession scheduler = login("scheduler", "Schedule2026!");
    mvc.perform(post("/api/schedule/publish").session(scheduler)).andExpect(status().isOk());
    mvc.perform(get("/api/manager/requests").session(scheduler)).andExpect(status().isForbidden());
    MockHttpSession manager = login("manager", "Manager2026!");
    mvc.perform(get("/api/manager/requests").session(manager)).andExpect(status().isOk());
    mvc.perform(
            post("/api/proposals")
                .session(manager)
                .contentType("application/json")
                .content("{\"shiftIds\":[1]}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void personalProfileFivePointRatingAndLogout() throws Exception {
    MockHttpSession session = login("smirnova", "Anna2026!");
    mvc.perform(get("/api/profile").session(session))
        .andExpect(jsonPath("$.score").value(4.9))
        .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    mvc.perform(get("/api/rating").session(session)).andExpect(jsonPath("$.length()").value(3));
    mvc.perform(get("/api/state").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule.length()").value(96));
    mvc.perform(post("/api/logout").session(session)).andExpect(status().isOk());
    mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
  }

  @Test
  void sharedBoardDeclineReservationAndApprovalAcrossFourSessions() throws Exception {
    MockHttpSession owner = login("ivanov", "Ivanov2026!"),
        declines = login("smirnova", "Anna2026!"),
        applies = login("glukhov", "Pavel2026!"),
        manager = login("manager", "Manager2026!");
    com.fasterxml.jackson.databind.ObjectMapper json =
        new com.fasterxml.jackson.databind.ObjectMapper();
    var initial =
        json.readTree(
            mvc.perform(get("/api/state").session(declines))
                .andReturn()
                .getResponse()
                .getContentAsString());
    long offerId = 0, shiftId = 0;
    for (var o : initial.get("offers"))
      if (o.get("owner").asText().equals("ivanov")) {
        offerId = o.get("id").asLong();
        shiftId = o.get("shift").get("id").asLong();
      }
    org.junit.jupiter.api.Assertions.assertTrue(offerId > 0);
    mvc.perform(post("/api/exchange/" + offerId + "/decline").session(declines))
        .andExpect(status().isOk());
    var other =
        json.readTree(
            mvc.perform(get("/api/state").session(applies))
                .andReturn()
                .getResponse()
                .getContentAsString());
    final long id = offerId;
    org.junit.jupiter.api.Assertions.assertTrue(
        java.util.stream.StreamSupport.stream(other.get("offers").spliterator(), false)
            .anyMatch(o -> o.get("id").asLong() == id));
    mvc.perform(post("/api/exchange/" + offerId + "/apply").session(applies))
        .andExpect(status().isOk());
    var ownerState =
        json.readTree(
            mvc.perform(get("/api/state").session(owner))
                .andReturn()
                .getResponse()
                .getContentAsString());
    long request = ownerState.get("requests").get(0).get("id").asLong();
    mvc.perform(
            post("/api/manager/requests/" + request)
                .session(manager)
                .contentType("application/json")
                .content("{\"accept\":true}"))
        .andExpect(status().isOk());
    var result =
        json.readTree(
            mvc.perform(get("/api/state").session(applies))
                .andReturn()
                .getResponse()
                .getContentAsString());
    final long assigned = shiftId;
    org.junit.jupiter.api.Assertions.assertTrue(
        java.util.stream.StreamSupport.stream(result.get("myShifts").spliterator(), false)
            .anyMatch(s -> s.get("id").asLong() == assigned));
    mvc.perform(get("/api/state").session(declines)).andExpect(jsonPath("$.requests").isEmpty());
  }
}
