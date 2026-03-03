package ru.vova.airbnb.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import ru.vova.airbnb.controller.auth.dto.*;
import ru.vova.airbnb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            tags = {"Auth"},
            summary = "Login",
            description = "Public endpoint. Authenticates user and returns JWT token."
    )
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
            tags = {"Auth"},
            summary = "Register user",
            description = "Public endpoint. Registers a user with selected role (GUEST/HOST/ADMIN)."
    )
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.registerUser(registerRequest);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}