package ru.vova.airbnb.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Airbnb Booking Service API",
                version = "v1",
                description = "REST API for booking workflow management." +
                "All timestamps in the API are in Moscow time (Europe/Moscow, UTC+3).",
                contact = @Contact(name = "Booking Service")
        ),
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {
                @Tag(name = "Auth", description = "Public authentication endpoints"),
                @Tag(name = "Guest", description = "Endpoints available for GUEST"),
                @Tag(name = "Host", description = "Endpoints available for HOST"),
                @Tag(name = "Admin", description = "Endpoints available for ADMIN")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
