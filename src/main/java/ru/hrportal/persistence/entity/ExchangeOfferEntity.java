package ru.hrportal.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import ru.hrportal.model.*;

@Entity
@Table(name = "exchange_offers")
public class ExchangeOfferEntity {
    protected ExchangeOfferEntity() {
    }

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "shift_id")
    private long shiftId;

    @Column(name = "owner")
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OfferStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "exchange_offers_declined_by",
            joinColumns = @JoinColumn(name = "entity_id"))
    @Column(name = "item_value")
    private Set<String> declinedBy = new HashSet<>();

    public static ExchangeOfferEntity from(ExchangeOffer value) {
        ExchangeOfferEntity entity = new ExchangeOfferEntity();
        entity.id = value.id();
        entity.shiftId = value.shiftId();
        entity.owner = value.owner();
        entity.status = value.status();
        entity.declinedBy = new HashSet<>(value.declinedBy());
        return entity;
    }

    public ExchangeOffer toDomain() {
        return new ExchangeOffer(id, shiftId, owner, status, declinedBy);
    }
}
