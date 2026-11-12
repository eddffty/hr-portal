package ru.hrportal.repository.memory;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.ExchangeOffer;
import ru.hrportal.repository.OfferRepository;

@Repository
public class InMemoryOfferRepository extends InMemoryRepository<ExchangeOffer>
        implements OfferRepository {
    public InMemoryOfferRepository() {
        super(ExchangeOffer::id);
    }
}
