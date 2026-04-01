package ru.vova.airbnb.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PropertyResponse {
    private Long id;
    private Long hostId;
    private String title;
    private String address;
    private BigDecimal basePricePerDay;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

