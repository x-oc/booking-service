package ru.vova.airbnb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.vova.airbnb.controller.dto.OneCPaymentRequest;
import ru.vova.airbnb.controller.dto.OneCPaymentResponse;
import ru.vova.airbnb.service.OneCIntegrationService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/integration/1c")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "1C Integration endpoints")
public class OneCController {

    private final OneCIntegrationService oneCIntegrationService;

    @GetMapping("/test")
    @Operation(
            summary = "Test connection to 1C",
            description = "Role: ADMIN. Sends GET request to 1C to check connectivity."
    )
    @PreAuthorize("hasAuthority('BOOKING_VIEW_ANY')")
    public ResponseEntity<Map<String, String>> test1CConnection() {
        String result = oneCIntegrationService.testConnection();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "1C connection test completed",
                "response", result
        ));
    }

    @PostMapping("/payment")
    @Operation(
            summary = "Send payment to 1C",
            description = "Role: ADMIN. Sends payment data to 1C accounting system."
    )
    @PreAuthorize("hasAuthority('BOOKING_VIEW_ANY')")
    public ResponseEntity<OneCPaymentResponse> sendPaymentTo1C(
            @RequestBody OneCPaymentRequest request) {
        OneCPaymentResponse response = oneCIntegrationService.sendPaymentTo1C(request);
        return ResponseEntity.ok(response);
    }
}