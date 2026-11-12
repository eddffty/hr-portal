package ru.hrportal.repository.memory;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.ExchangeRequest;
import ru.hrportal.repository.RequestRepository;

@Repository
public class InMemoryRequestRepository extends InMemoryRepository<ExchangeRequest>
        implements RequestRepository {
    public InMemoryRequestRepository() {
        super(ExchangeRequest::id);
    }
}
