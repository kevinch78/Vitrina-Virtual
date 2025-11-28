package com.vitrina.vitrinaVirtual.infraestructura.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Vitrina Virtual API")
                        .description("Una vitrina virtual de moda para generar tus outfits en un pestañeo. " +
                                "Esta API permite gestionar usuarios, tiendas, productos y generar recomendaciones de outfits usando IA.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo Vitrina Virtual")
                                .email("contacto@vitrinavirtual.com")
                                .url("https://vitrinavirtual.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de desarrollo"),
                        new Server()
                                .url("https://api.vitrinavirtual.com")
                                .description("Servidor de producción")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresa tu token JWT obtenido del endpoint /api/auth/login")));
    }
}
