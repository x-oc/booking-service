package ru.vova.airbnb.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DistributedTxProbeRequest {

    @NotNull
    private Long hostId;

    @NotNull
    private Long guestId;

    private Boolean forceFailure = true;

    private String probeKey;
}
