package ru.vova.airbnb.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.vova.airbnb.controller.dto.PropertyCreateRequest;
import ru.vova.airbnb.controller.dto.PropertyResponse;
import ru.vova.airbnb.security.jwt.UserDetailsImpl;
import ru.vova.airbnb.service.PropertyService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            tags = {"Host"},
            summary = "Create property",
            description = "Role: HOST. Creates a new property."
    )
    public PropertyResponse createProperty(@Valid @RequestBody PropertyCreateRequest request,
                                           @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return propertyService.createProperty(request, currentUser.getId());
    }

    @GetMapping("/host")
    @Operation(
            tags = {"Host"},
            summary = "Get host properties",
            description = "Role: HOST. Returns properties for current host."
    )
    public List<PropertyResponse> getHostProperties(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return propertyService.getHostProperties(currentUser.getId());
    }

    @GetMapping("/{id}")
    @Operation(
            tags = {"Guest", "Host", "Admin"},
            summary = "Get property by id",
            description = "Roles: GUEST/HOST/ADMIN. Returns property details."
    )
    public PropertyResponse getPropertyById(@PathVariable Long id) {
        return propertyService.getPropertyById(id);
    }
}

