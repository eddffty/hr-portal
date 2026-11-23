package ru.hrportal;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.exception.DomainException;
import ru.hrportal.model.*;
import ru.hrportal.repository.memory.*;
import ru.hrportal.service.*;

class FigmaWorkflowTest {
  private WorkflowLock lock;
  private InMemoryAvailabilityRepository availabilityRepo;
  private AvailabilityService availability;
  private InMemoryShiftRepository shifts;
  private InMemoryRequestRepository requests;
  private InMemoryUserRepository users;
  private ShiftService shiftService;
  private PlannerService planner;
  private StaffService staff;
  private AccountService accounts;

  @BeforeEach
  void setup() {
    lock = new WorkflowLock();
    availabilityRepo = new InMemoryAvailabilityRepository();
    availability = new AvailabilityService(availabilityRepo, lock);
    shifts = new InMemoryShiftRepository();
    requests = new InMemoryRequestRepository();
    users = new InMemoryUserRepository();
    var offers = new InMemoryOfferRepository();
    var passwords = new PasswordHasher();
    accounts = new AccountService(users, passwords);
    staff = new StaffService(users, passwords);
    for (String user : List.of("alice", "boris"))
      staff.save(
          new StaffService.Input(
              user, user, "Специалист", "ИТ", user + "@example.test", "Password2026!", true),
          true);
    shiftService = new ShiftService(shifts, requests, new InMemoryTimeEntryRepository(), lock);
    shiftService.setAvailability(availability);
    var notes = new NotificationService(new InMemoryNotificationRepository(), users, lock);
    planner =
        new PlannerService(
            shiftService,
            accounts,
            requests,
            lock,
            notes,
            new AuditService(new InMemoryAuditRepository()),
            offers);
  }

  private Shift future(int offset) {
    LocalDate date = LocalDate.now().plusDays(offset);
    while (date.getDayOfWeek() == DayOfWeek.SUNDAY) date = date.plusDays(1);
    return shifts.save(
        new Shift(
            shifts.nextId(),
            date,
            LocalTime.of(8, 30),
            LocalTime.of(9, 50),
            0,
            1,
            "1108",
            null,
            false));
  }

  @Test
  void recurringAvailabilitySeparatesWeekParityAndCanBeEdited() {
    Shift shift = future(1);
    int parity = AvailabilityService.parity(shift.date());
    String cell = shift.date().getDayOfWeek().getValue() + "-0";
    availability.save("alice", parity, List.of(cell), "Пара");
    assertThrows(DomainException.class, () -> shiftService.available("alice", shift, Set.of()));
    availability.save("alice", parity, List.of(cell), "Вторая работа");
    assertEquals(1, availability.list("alice").size());
    assertEquals("Вторая работа", availability.list("alice").get(0).description());
    Shift other =
        new Shift(
            999, shift.date().plusWeeks(1), shift.start(), shift.end(), 0, 1, "1108", null, false);
    assertDoesNotThrow(() -> shiftService.available("alice", other, Set.of()));
    assertThrows(
        DomainException.class,
        () -> availability.delete("boris", availability.list("alice").get(0).id()));
    availability.delete("alice", availability.list("alice").get(0).id());
    assertDoesNotThrow(() -> shiftService.available("alice", shift, Set.of()));
  }

  @Test
  void candidatesPrioritizeRequestedEmployeeAndExcludeBusy() {
    Shift shift = future(2);
    requests.save(
        new ExchangeRequest(
            requests.nextId(),
            RequestType.ASSIGNMENT,
            "boris",
            null,
            List.of(shift.id()),
            null,
            RequestStatus.WAITING_MANAGER,
            "",
            Instant.now()));
    availability.save(
        "alice",
        AvailabilityService.parity(shift.date()),
        List.of(shift.date().getDayOfWeek().getValue() + "-0"),
        "Пара");
    var list = planner.candidates(shift.id());
    assertEquals("boris", list.get(0).login());
    assertTrue(list.get(0).requested());
    assertFalse(list.get(1).available());
    planner.save("manager", List.of(new PlannerService.Change(shift.id(), "boris")));
    assertEquals("boris", shiftService.get(shift.id()).owner());
    assertEquals(RequestStatus.APPROVED, requests.findAll().get(0).status());
  }

  @Test
  void bulkPlanHasNoPartialSaveOnConflicts() {
    Shift one = future(3),
        two =
            shifts.save(
                new Shift(
                    shifts.nextId(),
                    one.date(),
                    one.start(),
                    one.end(),
                    0,
                    4,
                    "4421",
                    null,
                    false));
    assertThrows(
        DomainException.class,
        () ->
            planner.save(
                "manager",
                List.of(
                    new PlannerService.Change(one.id(), "alice"),
                    new PlannerService.Change(two.id(), "alice"))));
    assertNull(shiftService.get(one.id()).owner());
    assertNull(shiftService.get(two.id()).owner());
  }

  @Test
  void disablingEmployeePreservesProfileButBlocksLogin() {
    staff.save(
        new StaffService.Input(
            "alice", "Алиса", "Специалист", "ИТ", "alice@example.test", "", false),
        false);
    assertThrows(DomainException.class, () -> accounts.authenticate("alice", "Password2026!"));
    assertThrows(DomainException.class, () -> accounts.activeProfile("alice"));
    assertEquals("Алиса", accounts.profile("alice").name());
    assertEquals(1, accounts.all().size());
  }
}
