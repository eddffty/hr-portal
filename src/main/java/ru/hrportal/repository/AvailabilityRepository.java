package ru.hrportal.repository;

import ru.hrportal.model.Availability;

public interface AvailabilityRepository extends EntityRepository<Availability> {
  void delete(long id);
}
