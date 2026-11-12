package ru.hrportal.dto;

import jakarta.validation.constraints.*;

import java.time.*;
import java.util.List;

import ru.hrportal.model.*;

public final class PortalDtos {
    private PortalDtos() {
    }

    public record Login(
            @NotBlank @Size(max = 100) String login, @NotBlank @Size(max = 100) String password) {
    }

    public record Decision(@NotNull Boolean accept) {
    }

    public record Proposal(
            @NotEmpty @Size(max = 48) List<@NotNull Long> shiftIds, @Size(max = 2000) String comment) {
    }

    public record Swap(
            @Positive long ownShiftId, @Positive long targetShiftId, @Size(max = 2000) String comment) {
    }

    public record Assignment(String owner) {
    }

    public record NewShift(
            @NotNull @FutureOrPresent LocalDate date,
            @Min(0) @Max(7) int slot,
            int building,
            @NotBlank @Size(max = 40) String room,
            String owner) {
    }

    public record ShiftView(
            long id,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            int slot,
            int building,
            String room,
            String owner,
            String ownerName,
            boolean completed,
            boolean locked,
            Long offerId) {
    }

    public record OfferView(
            long id,
            ShiftView shift,
            String owner,
            String ownerName,
            OfferStatus status,
            boolean own,
            boolean available) {
    }

    public record RequestView(
            long id,
            RequestType type,
            RequestStatus status,
            String initiator,
            String initiatorName,
            String recipient,
            String recipientName,
            List<ShiftView> shifts,
            String comment,
            Instant createdAt,
            boolean canRespond,
            boolean canCancel) {
    }

    public record TimeView(
            long id,
            String user,
            String userName,
            ShiftView shift,
            Instant startedAt,
            Instant endedAt,
            long minutes,
            long plannedMinutes,
            long deviationMinutes) {
    }

    public record State(
            LocalDate today,
            LocalDate weekStart,
            List<ShiftView> schedule,
            List<ShiftView> myShifts,
            List<OfferView> offers,
            List<RequestView> requests,
            List<Notification> notifications,
            List<TimeView> timeEntries,
            List<UserProfile> employees) {
    }
}
