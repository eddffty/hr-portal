package ru.hrportal.persistence.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

import ru.hrportal.model.*;

@Entity
@Table(name = "exchange_requests")
public class ExchangeRequestEntity {
    protected ExchangeRequestEntity() {
    }

    @Id
    @Column(name = "id")
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type")
    private RequestType type;

    @Column(name = "initiator")
    private String initiator;

    @Column(name = "recipient")
    private String recipient;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "exchange_requests_shift_ids",
            joinColumns = @JoinColumn(name = "entity_id"))
    @Column(name = "item_value")
    @OrderColumn(name = "position_index")
    private List<Long> shiftIds = new ArrayList<>();

    @Column(name = "offer_id")
    private Long offerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status;

    @Column(name = "request_comment", columnDefinition = "text")
    private String comment;

    @Column(name = "created_at")
    private Instant createdAt;

    public static ExchangeRequestEntity from(ExchangeRequest value) {
        ExchangeRequestEntity entity = new ExchangeRequestEntity();
        entity.id = value.id();
        entity.type = value.type();
        entity.initiator = value.initiator();
        entity.recipient = value.recipient();
        entity.shiftIds = new ArrayList<>(value.shiftIds());
        entity.offerId = value.offerId();
        entity.status = value.status();
        entity.comment = value.comment();
        entity.createdAt = value.createdAt();
        return entity;
    }

    public ExchangeRequest toDomain() {
        return new ExchangeRequest(
                id, type, initiator, recipient, shiftIds, offerId, status, comment, createdAt);
    }
}
