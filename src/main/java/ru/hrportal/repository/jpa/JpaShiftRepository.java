package ru.hrportal.repository.jpa;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.Shift;
import ru.hrportal.persistence.entity.ShiftEntity;
import ru.hrportal.repository.ShiftRepository;

@Repository
public class JpaShiftRepository extends JpaEntityRepository<Shift, ShiftEntity>
        implements ShiftRepository {
    public JpaShiftRepository() {
        super(ShiftEntity.class, ShiftEntity::from, ShiftEntity::toDomain);
    }
}
