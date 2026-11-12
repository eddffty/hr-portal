package ru.hrportal.model;

import java.time.Instant;
import java.util.List;

public record ExchangeRequest(
        long id,
        RequestType type,
        String initiator,
        String recipient,
        List<Long> shiftIds,
        Long offerId,
        RequestStatus status,
        String comment,
        Instant createdAt) {
    public ExchangeRequest {
        shiftIds = List.copyOf(shiftIds);
    }

    public ExchangeRequest status(RequestStatus next) {
        return new ExchangeRequest(
                id, type, initiator, recipient, shiftIds, offerId, next, comment, createdAt);
    }
}
