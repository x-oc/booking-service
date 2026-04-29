package ru.vova.airbnb.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneCPaymentRequest {

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("payerEmail")
    private String payerEmail;

    @JsonProperty("payeeEmail")
    private String payeeEmail;

    @JsonProperty("description")
    private String description;
}