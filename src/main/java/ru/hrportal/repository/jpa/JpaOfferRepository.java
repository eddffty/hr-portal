package ru.hrportal.repository.jpa;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.ExchangeOffer;
import ru.hrportal.persistence.entity.ExchangeOfferEntity;
import ru.hrportal.repository.OfferRepository;

@Repository
public class JpaOfferRepository extends JpaEntityRepository<ExchangeOffer, ExchangeOfferEntity>
        implements OfferRepository {
    public JpaOfferRepository() {
        super(ExchangeOfferEntity.class, ExchangeOfferEntity::from, ExchangeOfferEntity::toDomain);
    }
}
