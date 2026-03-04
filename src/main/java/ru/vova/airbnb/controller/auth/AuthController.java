package ru.vova.airbnb.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.vova.airbnb.controller.auth.dto.JwtResponse;
import ru.vova.airbnb.controller.auth.dto.LoginRequest;
import ru.vova.airbnb.controller.auth.dto.MessageResponse;
import ru.vova.airbnb.controller.auth.dto.RegisterRequest;
import ru.vova.airbnb.service.AuthService;

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
    public JwtResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticateUser(loginRequest);
    }

    @PostMapping("/register")
    @Operation(
            tags = {"Auth"},
            summary = "Register user",
            description = "Public endpoint. Registers a user with selected role (GUEST/HOST/ADMIN)."
    )
    public MessageResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.registerUser(registerRequest);
        return new MessageResponse("User registered successfully!");
    }
}