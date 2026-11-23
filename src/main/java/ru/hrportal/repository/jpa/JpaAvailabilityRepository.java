package ru.hrportal.repository.jpa;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.Availability;
import ru.hrportal.persistence.entity.AvailabilityEntity;
import ru.hrportal.repository.AvailabilityRepository;

@Repository
public class JpaAvailabilityRepository extends JpaEntityRepository<Availability, AvailabilityEntity>
        implements AvailabilityRepository {
    public JpaAvailabilityRepository() {
        super(AvailabilityEntity.class, AvailabilityEntity::from, AvailabilityEntity::toDomain);
    }

    public void delete(long id) {
        deleteEntity(id);
    }
}
