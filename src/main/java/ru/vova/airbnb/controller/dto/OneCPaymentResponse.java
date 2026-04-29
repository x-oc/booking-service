package ru.vova.airbnb.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneCPaymentResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("documentId")
    private String documentId;

    @JsonProperty("message")
    private String message;
}