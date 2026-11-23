package ru.hrportal;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.hrportal.config.*;
import ru.hrportal.model.*;
import ru.hrportal.repository.*;

@SpringBootTest
class PersistenceTest {
  @Autowired ShiftRepository shifts;
  @Autowired OfferRepository offers;
  @Autowired DatabaseWorkflow workflow;
  @Autowired DemoData demo;
  @Autowired JdbcTemplate jdbc;
  @Autowired org.flywaydb.core.Flyway flyway;

  @Test
  void failedWorkflowRollsBackBothAssignments() {
    List<Shift> selected =
        shifts.findAll().stream().filter(s -> s.owner() != null).limit(2).toList();
    assertThrows(
        IllegalStateException.class,
        () ->
            workflow.execute(
                () -> {
                  shifts.save(selected.get(0).assign("manager"));
                  shifts.save(selected.get(1).assign("manager"));
                  throw new IllegalStateException("Simulated failure after both writes");
                }));
    for (Shift original : selected)
      assertEquals(original, shifts.find(original.id()).orElseThrow());
  }

  @Test
  void demoInitializationDoesNotOverwriteExistingDatabase() {
    int count = shifts.findAll().size();
    Shift original = shifts.findAll().get(0);
    try {
      workflow.execute(() -> shifts.save(original.assign("manager")));
      demo.run(null);
      assertEquals(count, shifts.findAll().size());
      assertEquals("manager", shifts.find(original.id()).orElseThrow().owner());
    } finally {
      workflow.execute(() -> shifts.save(original));
    }
  }

  @Test
  void flywayAndRelationalCollectionsArePersisted() {
    assertEquals(2, flyway.info().applied().length);
    ExchangeOffer original = offers.findAll().get(0);
    try {
      workflow.execute(() -> offers.save(original.decline("manager")));
      assertTrue(offers.find(original.id()).orElseThrow().declinedBy().contains("manager"));
      assertEquals(
          1,
          jdbc.queryForObject(
              "select count(*) from exchange_offers_declined_by where entity_id = ? and item_value"
                  + " = ?",
              Integer.class,
              original.id(),
              "manager"));
    } finally {
      workflow.execute(() -> offers.save(original));
    }
  }
}
