package ru.hrportal;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.exception.DomainException;
import ru.hrportal.model.*;
import ru.hrportal.repository.memory.*;
import ru.hrportal.service.*;

class ExchangeWorkflowTest {
  InMemoryShiftRepository shifts;
  InMemoryOfferRepository offers;
  InMemoryRequestRepository requests;
  InMemoryTimeEntryRepository entries;
  InMemoryUserRepository users;
  NotificationService notifications;
  ExchangeService exchange;
  ShiftService shiftService;
  TimeTrackingService time;
  ScheduleService schedule;

  @BeforeEach
  void setup() {
    WorkflowLock lock = new WorkflowLock();
    shifts = new InMemoryShiftRepository();
    offers = new InMemoryOfferRepository();
    requests = new InMemoryRequestRepository();
    entries = new InMemoryTimeEntryRepository();
    users = new InMemoryUserRepository();
    notifications = new NotificationService(new InMemoryNotificationRepository(), users, lock);
    AuditService audit = new AuditService(new InMemoryAuditRepository());
    shiftService = new ShiftService(shifts, requests, entries, lock);
    exchange = new ExchangeService(offers, requests, shiftService, notifications, audit, lock);
    time = new TimeTrackingService(entries, offers, shiftService, lock, audit);
    schedule = new ScheduleService(shiftService, offers, users, notifications, audit, lock, shifts);
    for (String login : List.of("a", "b", "c", "manager")) {
      UserProfile p =
          new UserProfile(
              login,
              login,
              login,
              "ИТ",
              "ИТ",
              login + "@example.test",
              "2026",
              0,
              0,
              0,
              100,
              4.7,
              1,
              List.of(),
              login.equals("manager") ? Role.MANAGER : Role.EMPLOYEE);
      users.save(new UserAccount(p, new byte[0], new byte[0]));
    }
  }

  Shift shift(String owner, int day, int hour) {
    return shifts.save(
        new Shift(
            shifts.nextId(),
            LocalDate.now().plusDays(day),
            LocalTime.of(hour, 0),
            LocalTime.of(hour + 1, 0),
            0,
            1,
            "1108",
            owner,
            false));
  }

  long publish(Shift s) {
    exchange.publish(s.owner(), s.id());
    return offers.findAll().get(offers.findAll().size() - 1).id();
  }

  long latest() {
    return requests.findAll().get(requests.findAll().size() - 1).id();
  }

  @Test
  void decliningHidesOnlyForThatEmployee() {
    long id = publish(shift("a", 1, 8));
    exchange.decline("b", id);
    assertTrue(exchange.visibleOffers("b").isEmpty());
    assertEquals(1, exchange.visibleOffers("c").size());
    assertEquals(OfferStatus.OPEN, exchange.offer(id).status());
    assertEquals(1, exchange.visibleOffers("a").size());
  }

  @Test
  void applyingReservesGloballyButKeepsOwnerUntilApproval() {
    Shift s = shift("a", 1, 8);
    long id = publish(s);
    exchange.apply("b", id);
    assertTrue(exchange.visibleOffers("c").isEmpty());
    assertTrue(exchange.visibleOffers("b").isEmpty());
    assertEquals("a", shiftService.get(s.id()).owner());
    assertEquals(1, exchange.visibleRequests("b").size());
    assertTrue(exchange.visibleRequests("c").isEmpty());
    assertThrows(DomainException.class, () -> exchange.apply("c", id));
  }

  @Test
  void approvedTransferChangesSharedAssignmentExactlyOnce() {
    Shift s = shift("a", 1, 8);
    long id = publish(s);
    exchange.apply("b", id);
    long request = latest();
    exchange.review("manager", request, true);
    assertEquals("b", shiftService.get(s.id()).owner());
    assertTrue(exchange.visibleOffers("a").isEmpty());
    assertTrue(shiftService.mine("a").isEmpty());
    assertEquals(1, shiftService.mine("b").size());
    assertThrows(DomainException.class, () -> exchange.review("manager", request, true));
    assertFalse(notifications.list("a").isEmpty());
    assertFalse(notifications.list("b").isEmpty());
  }

  @Test
  void managerRejectionReopensForOthersButNotRejectedApplicant() {
    Shift s = shift("a", 1, 8);
    long id = publish(s);
    exchange.apply("b", id);
    exchange.review("manager", latest(), false);
    assertEquals("a", shiftService.get(s.id()).owner());
    assertTrue(exchange.visibleOffers("b").isEmpty());
    assertEquals(1, exchange.visibleOffers("c").size());
    exchange.apply("c", id);
    assertEquals(RequestStatus.WAITING_MANAGER, exchange.request(latest()).status());
  }

  @Test
  void cancellingAnApplicationReopensOffer() {
    long id = publish(shift("a", 1, 8));
    exchange.apply("b", id);
    exchange.cancel("b", latest());
    assertEquals(1, exchange.visibleOffers("b").size());
    assertEquals(1, exchange.visibleOffers("c").size());
  }

  @Test
  void twoConcurrentApplicantsCannotBothReserve() {
    long id = publish(shift("a", 1, 8));
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch go = new CountDownLatch(1);
    try {
      List<Future<Boolean>> result = new ArrayList<>();
      for (String user : List.of("b", "c"))
        result.add(
            pool.submit(
                () -> {
                  go.await();
                  try {
                    exchange.apply(user, id);
                    return true;
                  } catch (DomainException e) {
                    return false;
                  }
                }));
      go.countDown();
      long wins =
          result.stream()
              .filter(
                  f -> {
                    try {
                      return f.get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  })
              .count();
      assertEquals(1, wins);
      assertEquals(1, requests.findAll().size());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void directSwapRequiresColleagueThenManagerAndChangesBothOwners() {
    Shift a = shift("a", 1, 8), b = shift("b", 2, 10);
    exchange.swap("a", a.id(), b.id(), "Обмен");
    long id = latest();
    assertThrows(DomainException.class, () -> exchange.review("manager", id, true));
    assertThrows(DomainException.class, () -> exchange.respond("c", id, true));
    exchange.respond("b", id, true);
    assertEquals("a", shiftService.get(a.id()).owner());
    exchange.review("manager", id, true);
    assertEquals("b", shiftService.get(a.id()).owner());
    assertEquals("a", shiftService.get(b.id()).owner());
  }

  @Test
  void decliningDirectSwapDisappearsOnlyFromRecipientInbox() {
    Shift a = shift("a", 1, 8),
        b = shift("b", 2, 10),
        a2 = shift("a", 3, 12),
        c = shift("c", 4, 14);
    exchange.swap("a", a.id(), b.id(), "");
    long first = latest();
    exchange.swap("a", a2.id(), c.id(), "");
    exchange.respond("b", first, false);
    assertTrue(exchange.visibleRequests("b").isEmpty());
    assertEquals(1, exchange.visibleRequests("c").size());
    assertEquals(RequestStatus.WAITING_COLLEAGUE, exchange.visibleRequests("c").get(0).status());
    assertEquals(2, exchange.visibleRequests("a").size());
  }

  @Test
  void swapValidationFailureDoesNotPartiallyTransfer() {
    Shift a = shift("a", 1, 8), b = shift("b", 2, 10);
    exchange.swap("a", a.id(), b.id(), "");
    long id = latest();
    exchange.respond("b", id, true);
    shift("a", 2, 10);
    assertThrows(DomainException.class, () -> exchange.review("manager", id, true));
    assertEquals("a", shiftService.get(a.id()).owner());
    assertEquals("b", shiftService.get(b.id()).owner());
    assertEquals(RequestStatus.WAITING_MANAGER, exchange.request(id).status());
  }

  @Test
  void assignmentReservesFreeShiftAcrossUsers() {
    Shift s = shift(null, 1, 8);
    exchange.propose("a", List.of(s.id()), "Могу выйти");
    long id = latest();
    assertThrows(DomainException.class, () -> exchange.propose("b", List.of(s.id()), ""));
    exchange.review("manager", id, true);
    assertEquals("a", shiftService.get(s.id()).owner());
  }

  @Test
  void rejectsOverlappingAssignmentsAndPastShifts() {
    shift("b", 1, 8);
    long id = publish(shift("a", 1, 8));
    assertThrows(DomainException.class, () -> exchange.apply("b", id));
    assertThrows(DomainException.class, () -> publish(shift("a", -1, 10)));
    Shift free = shift(null, 1, 12), free2 = shift(null, 1, 12);
    assertThrows(
        DomainException.class, () -> exchange.propose("c", List.of(free.id(), free2.id()), ""));
  }

  @Test
  void employeeCannotMutateOthersOfferOrWithdrawReserved() {
    long id = publish(shift("a", 1, 8));
    assertThrows(DomainException.class, () -> exchange.withdraw("b", id));
    exchange.apply("b", id);
    assertThrows(DomainException.class, () -> exchange.withdraw("a", id));
    assertThrows(DomainException.class, () -> exchange.cancel("c", latest()));
  }

  @Test
  void notificationsReadStateIsPersonal() {
    notifications.send("a", "A");
    notifications.send("b", "B");
    notifications.markRead("a");
    assertTrue(notifications.list("a").get(0).read());
    assertFalse(notifications.list("b").get(0).read());
  }

  @Test
  void trackingRequiresOwnerAndOnlyOneActiveEntry() {
    Shift today = shift("a", 0, 8), other = shift("a", 0, 10);
    assertThrows(DomainException.class, () -> time.start("b", today.id()));
    time.start("a", today.id());
    assertThrows(DomainException.class, () -> time.start("a", other.id()));
    long id = entries.findAll().get(0).id();
    assertThrows(DomainException.class, () -> time.stop("b", id));
    time.stop("a", id);
    assertTrue(shiftService.get(today.id()).completed());
    assertNotNull(entries.find(id).orElseThrow().endedAt());
    assertThrows(DomainException.class, () -> time.stop("a", id));
  }

  @Test
  void scheduleEditsRespectLocksAndConflicts() {
    Shift target = shift(null, 1, 8);
    shift("a", 1, 8);
    assertThrows(DomainException.class, () -> schedule.assign("scheduler", target.id(), "a"));
    exchange.propose("b", List.of(target.id()), "");
    assertThrows(DomainException.class, () -> schedule.assign("scheduler", target.id(), "c"));
  }

  @Test
  void schedulerCanCreateShiftButCannotDuplicateIt() {
    java.time.LocalDate day = LocalDate.now().plusDays(8);
    if (day.getDayOfWeek() == DayOfWeek.SUNDAY) day = day.plusDays(1);
    var input = new ru.hrportal.dto.PortalDtos.NewShift(day, 1, 4, "4421", "a");
    schedule.create("scheduler", input);
    assertEquals(1, shifts.findAll().size());
    assertEquals("a", shifts.findAll().get(0).owner());
    assertThrows(DomainException.class, () -> schedule.create("scheduler", input));
  }
}
