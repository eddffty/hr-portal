package ru.hrportal.repository.memory;

import org.springframework.stereotype.Repository;
import ru.hrportal.model.Shift;
import ru.hrportal.repository.ShiftRepository;

@Repository
public class InMemoryShiftRepository extends InMemoryRepository<Shift> implements ShiftRepository {
    public InMemoryShiftRepository() {
        super(Shift::id);
    }
}
