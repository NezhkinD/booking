package com.booking.service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Booking Service API",
                version = "1.0.0",
                description = "REST API for hotel booking and user management",
                contact = @Contact(
                        name = "Hotel Booking System",
                        email = "support@booking.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8081", description = "Booking Service"),
                @Server(url = "http://localhost:8080", description = "API Gateway")
        }
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenAPIConfig {
}
