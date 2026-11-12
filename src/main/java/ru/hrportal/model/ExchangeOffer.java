package ru.hrportal.model;

import java.util.HashSet;
import java.util.Set;

public record ExchangeOffer(
        long id, long shiftId, String owner, OfferStatus status, Set<String> declinedBy) {
    public ExchangeOffer {
        declinedBy = Set.copyOf(declinedBy);
    }

    public ExchangeOffer status(OfferStatus next) {
        return new ExchangeOffer(id, shiftId, owner, next, declinedBy);
    }

    public ExchangeOffer decline(String login) {
        Set<String> declined = new HashSet<>(declinedBy);
        declined.add(login);
        return new ExchangeOffer(id, shiftId, owner, status, declined);
    }
}
