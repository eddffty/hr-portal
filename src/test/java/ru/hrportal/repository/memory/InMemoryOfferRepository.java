package ru.hrportal.repository.memory;

import ru.hrportal.model.ExchangeOffer;
import ru.hrportal.repository.OfferRepository;

public class InMemoryOfferRepository extends InMemoryRepository<ExchangeOffer>
    implements OfferRepository {
  public InMemoryOfferRepository() {
    super(ExchangeOffer::id);
  }
}
