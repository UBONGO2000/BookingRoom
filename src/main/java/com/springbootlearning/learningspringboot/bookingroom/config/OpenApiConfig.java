package com.springbootlearning.learningspringboot.bookingroom.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "BookingRoom API",
                version = "1.0",
                description = "API REST de BookingRoom, basée sur une session HTTP. "
                        + "Les routes d'écriture exigent aussi le jeton CSRF dans l'en-tête "
                        + "X-XSRF-TOKEN (obtenu via GET /api/csrf)."
        ),
        servers = @Server(url = "/", description = "Serveur courant")
)
@SecurityScheme(
        name = "sessionAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "JSESSIONID",
        description = "Session créée par POST /api/auth/login (cookie JSESSIONID)."
)
public class OpenApiConfig {
}
