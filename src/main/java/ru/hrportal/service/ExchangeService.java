package ru.hrportal.service;

import static ru.hrportal.exception.DomainException.check;

import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.hrportal.config.WorkflowLock;
import ru.hrportal.exception.DomainException;
import ru.hrportal.model.*;
import ru.hrportal.repository.*;

@Service
public class ExchangeService {
  private final OfferRepository offers;
  private final RequestRepository requests;
  private final ShiftService shifts;
  private final NotificationService notifications;
  private final AuditService audit;
  private final WorkflowLock lock;

  public ExchangeService(
      OfferRepository offers,
      RequestRepository requests,
      ShiftService shifts,
      NotificationService notifications,
      AuditService audit,
      WorkflowLock lock) {
    this.offers = offers;
    this.requests = requests;
    this.shifts = shifts;
    this.notifications = notifications;
    this.audit = audit;
    this.lock = lock;
  }

  public ExchangeOffer offer(long id) {
    return offers
        .find(id)
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Предложение не найдено"));
  }

  public ExchangeRequest request(long id) {
    return requests
        .find(id)
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Заявка не найдена"));
  }

  public List<ExchangeOffer> visibleOffers(String user) {
    synchronized (lock) {
      return offers.findAll().stream()
          .filter(
              o ->
                  o.status() == OfferStatus.OPEN
                      || o.status() == OfferStatus.RESERVED && o.owner().equals(user))
          .filter(o -> o.owner().equals(user) || !o.declinedBy().contains(user))
          .filter(o -> !shifts.get(o.shiftId()).completed())
          .toList();
    }
  }

  public List<ExchangeRequest> visibleRequests(String user) {
    synchronized (lock) {
      return requests.findAll().stream()
          .filter(
              r ->
                  r.initiator().equals(user)
                      || Objects.equals(r.recipient(), user)
                          && r.status() != RequestStatus.REJECTED_COLLEAGUE)
          .sorted(Comparator.comparing(ExchangeRequest::id).reversed())
          .toList();
    }
  }

  public List<ExchangeRequest> managerRequests() {
    synchronized (lock) {
      return requests.findAll().stream()
          .filter(
              r ->
                  r.status() != RequestStatus.WAITING_COLLEAGUE
                      && r.status() != RequestStatus.REJECTED_COLLEAGUE)
          .sorted(Comparator.comparing(ExchangeRequest::id).reversed())
          .toList();
    }
  }

  public void publish(String user, long shiftId) {
    synchronized (lock) {
      Shift shift = shifts.get(shiftId);
      check(user.equals(shift.owner()), "Можно публиковать только свои смены");
      shifts.editable(shift);
      shifts.unlocked(shiftId, null);
      check(
          offers.findAll().stream()
              .noneMatch(
                  o ->
                      o.shiftId() == shiftId
                          && (o.status() == OfferStatus.OPEN
                              || o.status() == OfferStatus.RESERVED)),
          "Смена уже опубликована");
      ExchangeOffer offer =
          offers.save(
              new ExchangeOffer(offers.nextId(), shiftId, user, OfferStatus.OPEN, Set.of()));
      notifications.send(user, "Смена опубликована на бирже");
      audit.record(user, "PUBLISH_OFFER", offer.id());
    }
  }

  public void withdraw(String user, long id) {
    synchronized (lock) {
      ExchangeOffer o = offer(id);
      check(o.owner().equals(user), "Это чужое предложение");
      check(o.status() == OfferStatus.OPEN, "Предложение уже занято откликом или закрыто");
      offers.save(o.status(OfferStatus.CANCELLED));
      audit.record(user, "WITHDRAW_OFFER", id);
    }
  }

  public void decline(String user, long id) {
    synchronized (lock) {
      ExchangeOffer o = offer(id);
      check(!o.owner().equals(user), "Нельзя отклонить своё предложение");
      check(o.status() == OfferStatus.OPEN, "Предложение уже недоступно");
      offers.save(o.decline(user));
      audit.record(user, "HIDE_OFFER_PERSONALLY", id);
    }
  }

  public void apply(String user, long id) {
    synchronized (lock) {
      ExchangeOffer o = offer(id);
      check(o.status() == OfferStatus.OPEN, "Другой сотрудник уже откликнулся на эту смену");
      check(!o.owner().equals(user), "Нельзя откликнуться на свою смену");
      check(!o.declinedBy().contains(user), "Вы отказались от этого предложения");
      Shift shift = shifts.get(o.shiftId());
      shifts.editable(shift);
      shifts.unlocked(shift.id(), null);
      check(o.owner().equals(shift.owner()), "Владелец смены изменился");
      shifts.available(user, shift, Set.of(shift.id()));
      ExchangeRequest r =
          requests.save(
              new ExchangeRequest(
                  requests.nextId(),
                  RequestType.TRANSFER,
                  user,
                  o.owner(),
                  List.of(shift.id()),
                  id,
                  RequestStatus.WAITING_MANAGER,
                  "",
                  Instant.now()));
      offers.save(o.status(OfferStatus.RESERVED));
      notifications.send(user, "Отклик отправлен. Ожидается решение руководителя");
      notifications.send(o.owner(), "На вашу смену откликнулся сотрудник " + user);
      notifications.managers("Новый отклик на бирже, заявка №" + r.id());
      audit.record(user, "APPLY_OFFER", id);
    }
  }

  public void propose(String user, List<Long> ids, String comment) {
    synchronized (lock) {
      check(ids != null && !ids.isEmpty() && ids.size() <= 48, "Выберите от 1 до 48 смен");
      check(new HashSet<>(ids).size() == ids.size(), "Повторяющиеся смены");
      List<Shift> selected = ids.stream().map(shifts::get).toList();
      for (Shift shift : selected) {
        shifts.editable(shift);
        shifts.unlocked(shift.id(), null);
        check(shift.owner() == null, "Смена уже занята");
        shifts.available(user, shift, Set.of());
      }
      for (int i = 0; i < selected.size(); i++)
        for (int j = i + 1; j < selected.size(); j++)
          check(!shifts.overlaps(selected.get(i), selected.get(j)), "Выбранные смены пересекаются");
      ExchangeRequest r =
          requests.save(
              new ExchangeRequest(
                  requests.nextId(),
                  RequestType.ASSIGNMENT,
                  user,
                  null,
                  ids,
                  null,
                  RequestStatus.WAITING_MANAGER,
                  comment,
                  Instant.now()));
      notifications.send(user, "Заявка на свободные смены отправлена руководителю");
      notifications.managers("Заявка на дежурство №" + r.id());
      audit.record(user, "PROPOSE_SHIFT", r.id());
    }
  }

  public void swap(String user, long ownId, long targetId, String comment) {
    synchronized (lock) {
      Shift own = shifts.get(ownId), target = shifts.get(targetId);
      check(user.equals(own.owner()), "Первая смена должна принадлежать вам");
      check(target.owner() != null && !user.equals(target.owner()), "Выберите смену коллеги");
      for (Shift s : List.of(own, target)) {
        shifts.editable(s);
        shifts.unlocked(s.id(), null);
        check(
            offers.findAll().stream()
                .noneMatch(
                    o ->
                        o.shiftId() == s.id()
                            && (o.status() == OfferStatus.OPEN
                                || o.status() == OfferStatus.RESERVED)),
            "Сначала снимите смену с биржи");
      }
      shifts.available(user, target, Set.of(ownId, targetId));
      shifts.available(target.owner(), own, Set.of(ownId, targetId));
      ExchangeRequest r =
          requests.save(
              new ExchangeRequest(
                  requests.nextId(),
                  RequestType.SWAP,
                  user,
                  target.owner(),
                  List.of(ownId, targetId),
                  null,
                  RequestStatus.WAITING_COLLEAGUE,
                  comment,
                  Instant.now()));
      notifications.send(target.owner(), "Получено адресное предложение обмена от " + user);
      audit.record(user, "PROPOSE_SWAP", r.id());
    }
  }

  public void respond(String user, long id, boolean accept) {
    synchronized (lock) {
      ExchangeRequest r = request(id);
      check(
          r.type() == RequestType.SWAP && user.equals(r.recipient()),
          "Вы не получатель этого предложения");
      check(r.status() == RequestStatus.WAITING_COLLEAGUE, "Предложение уже обработано");
      requests.save(
          r.status(accept ? RequestStatus.WAITING_MANAGER : RequestStatus.REJECTED_COLLEAGUE));
      notifications.send(
          r.initiator(),
          accept
              ? "Коллега принял обмен. Заявка направлена руководителю"
              : "Коллега отклонил адресный обмен");
      if (accept) notifications.managers("Обмен №" + id + " ожидает согласования");
      audit.record(user, accept ? "ACCEPT_SWAP" : "REJECT_SWAP", id);
    }
  }

  public void cancel(String user, long id) {
    synchronized (lock) {
      ExchangeRequest r = request(id);
      check(r.initiator().equals(user), "Отменить заявку может только инициатор");
      check(r.status().pending(), "Заявка уже обработана");
      requests.save(r.status(RequestStatus.CANCELLED));
      release(r, false);
      notifications.send(r.recipient(), "Инициатор отменил заявку №" + id);
      audit.record(user, "CANCEL_REQUEST", id);
    }
  }

  private void release(ExchangeRequest r, boolean declined) {
    if (r.offerId() != null) {
      ExchangeOffer o = offer(r.offerId()).status(OfferStatus.OPEN);
      if (declined) o = o.decline(r.initiator());
      offers.save(o);
    }
  }

  public void review(String manager, long id, boolean accept) {
    review(manager, id, accept, null);
  }

  public void review(String manager, long id, boolean accept, String reason) {
    synchronized (lock) {
      ExchangeRequest r = request(id);
      check(
          r.status() == RequestStatus.WAITING_MANAGER, "Заявка уже обработана или ожидает коллегу");
      check(
          !manager.equals(r.initiator()) && !manager.equals(r.recipient()),
          "Нельзя согласовать собственную заявку");
      if (accept) {
        List<Shift> selected = r.shiftIds().stream().map(shifts::get).toList();
        Set<Long> excluded = new HashSet<>(r.shiftIds());
        for (Shift s : selected) {
          shifts.editable(s);
          shifts.unlocked(s.id(), r.id());
        }
        switch (r.type()) {
          case TRANSFER -> {
            Shift s = selected.get(0);
            ExchangeOffer o = offer(r.offerId());
            check(
                o.status() == OfferStatus.RESERVED && Objects.equals(s.owner(), r.recipient()),
                "Предложение или владелец смены изменился");
            shifts.available(r.initiator(), s, excluded);
          }
          case ASSIGNMENT -> {
            for (Shift s : selected) {
              check(s.owner() == null, "Смена уже занята");
              shifts.available(r.initiator(), s, excluded);
            }
          }
          case SWAP -> {
            check(
                Objects.equals(selected.get(0).owner(), r.initiator())
                    && Objects.equals(selected.get(1).owner(), r.recipient()),
                "Владельцы смен изменились");
            shifts.available(r.initiator(), selected.get(1), excluded);
            shifts.available(r.recipient(), selected.get(0), excluded);
          }
        }
        // All checks finish before either assignment is changed.
        if (r.type() == RequestType.SWAP) {
          shifts.assign(selected.get(0).id(), r.recipient());
          shifts.assign(selected.get(1).id(), r.initiator());
        } else for (Shift s : selected) shifts.assign(s.id(), r.initiator());
        if (r.offerId() != null) offers.save(offer(r.offerId()).status(OfferStatus.COMPLETED));
      } else release(r, true);
      ExchangeRequest resolved =
          r.status(accept ? RequestStatus.APPROVED : RequestStatus.REJECTED_MANAGER);
      if (!accept && reason != null && !reason.isBlank())
        resolved =
            new ExchangeRequest(
                resolved.id(),
                resolved.type(),
                resolved.initiator(),
                resolved.recipient(),
                resolved.shiftIds(),
                resolved.offerId(),
                resolved.status(),
                (resolved.comment() == null ? "" : resolved.comment() + "\n")
                    + "Причина отказа: "
                    + reason.trim(),
                resolved.createdAt());
      requests.save(resolved);
      String message =
          "Руководитель "
              + (accept ? "одобрил" : "отклонил")
              + " заявку №"
              + id
              + (!accept && reason != null && !reason.isBlank()
                  ? ". Причина: " + reason.trim()
                  : "");
      notifications.send(r.initiator(), message);
      notifications.send(r.recipient(), message);
      audit.record(manager, accept ? "APPROVE_REQUEST" : "REJECT_REQUEST", id);
    }
  }
}
