package com.batalla.fraudesito.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fraudeDetectionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Detección de Fraude Financiero")
                        .description("""
                                API REST para detección de fraude financiero basada en análisis de grafos con Neo4j.
                                Permite modelar personas, cuentas, dispositivos y transacciones como nodos
                                e identificar patrones fraudulentos a través de sus relaciones.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Batalla Team")
                                .email("soporte@batalla.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentación de Spring Data Neo4j")
                        .url("https://docs.spring.io/spring-data/neo4j/reference/"));
    }
}
