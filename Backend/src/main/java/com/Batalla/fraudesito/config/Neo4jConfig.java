package com.batalla.fraudesito.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot auto-configura el Driver, Neo4jClient, Neo4jTemplate y
 * TransactionManager usando las propiedades spring.neo4j.* de application.yml.
 * Esta clase sirve como punto de extensión para customizaciones futuras
 * (conversores de tipos, auditoría, etc).
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "com.batalla.fraudesito.repository")
@EnableTransactionManagement
public class Neo4jConfig {
}
