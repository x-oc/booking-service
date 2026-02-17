package ru.vova.airbnb.controller.dto;

import lombok.Data;
import ru.vova.airbnb.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;

@Data
public class StatusUpdateRequest {
    @NotNull
    private BookingStatus status;
}