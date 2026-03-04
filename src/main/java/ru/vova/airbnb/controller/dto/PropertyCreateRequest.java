package ru.vova.airbnb.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PropertyCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String address;
}

