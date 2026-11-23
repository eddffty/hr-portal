package ru.hrportal.repository.jpa;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.ExchangeRequest;
import ru.hrportal.persistence.entity.ExchangeRequestEntity;
import ru.hrportal.repository.RequestRepository;

@Repository
public class JpaRequestRepository
        extends JpaEntityRepository<ExchangeRequest, ExchangeRequestEntity>
        implements RequestRepository {
    public JpaRequestRepository() {
        super(
                ExchangeRequestEntity.class, ExchangeRequestEntity::from, ExchangeRequestEntity::toDomain);
    }
}
