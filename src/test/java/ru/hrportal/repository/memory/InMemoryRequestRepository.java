package ru.hrportal.repository.memory;

import ru.hrportal.model.ExchangeRequest;
import ru.hrportal.repository.RequestRepository;

public class InMemoryRequestRepository extends InMemoryRepository<ExchangeRequest>
    implements RequestRepository {
  public InMemoryRequestRepository() {
    super(ExchangeRequest::id);
  }
}
