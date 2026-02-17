package ru.vova.airbnb.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @PostMapping("/register/guest")
    @Operation(
            tags = {"Auth"},
            summary = "Register guest",
            description = "Public endpoint. Registers a user with GUEST role."
    )
    public ResponseEntity<MessageResponse> registerGuest(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.registerUser(registerRequest, "GUEST");
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/register/host")
    @Operation(
            tags = {"Auth"},
            summary = "Register host",
            description = "Public endpoint. Registers a user with HOST role."
    )
    public ResponseEntity<MessageResponse> registerHost(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.registerUser(registerRequest, "HOST");
        return ResponseEntity.ok(new MessageResponse("Host registered successfully!"));
    }

    @PostMapping("/register/admin")
    @Operation(
            tags = {"Auth"},
            summary = "Register admin",
            description = "Public endpoint. Registers a user with ADMIN role."
    )
    public ResponseEntity<MessageResponse> registerAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.registerUser(registerRequest, "ADMIN");
        return ResponseEntity.ok(new MessageResponse("Admin registered successfully!"));
    }
}