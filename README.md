# Trabajo Práctico Grupal 2 - Base de Datos II - Sistema de Deteccion de Fraude Financiero

Aplicación web interactiva para la detección y visualización de patrones de fraude financiero utilizando análisis de grafos sobre Neo4j.

## Integrantes y roles

- Lucio Wiesek (Backend, Testing)
- Agustín de Robles (Frontend)
- Tomás Quinteros (Backend)
- Bruno Lalomia (Documentación)
- Carmen Davila (Base de Datos)
- Lautaro Mercado (Base de Datos)

## Stack Tecnologico

| Capa | Tecnologia |
|------|------------|
| Backend | Spring Boot 3.3, Spring Data Neo4j, Java 21, Lombok, Swagger/OpenAPI |
| Base de datos | Neo4j (bolt://localhost:7687) |
| Frontend | React 19, Vite 8, Tailwind CSS 4, Cytoscape.js, Recharts, Axios, Lucide Icons |
| API | REST (/api/v1) con documentacion Swagger UI |

## Instalación y Ejecución

### Requisitos previos

- Java 21+
- Node.js 18+
- Neo4j Desktop

### 1. Preparar la base de datos

Ejecutar el script de seeding en Neo4j Browser:

```
Backend/src/main/resources/cypher/seed.cypher
```

### 2. Backend

```bash
cd Backend
./mvnw spring-boot:run
```

El servidor inicia en `http://localhost:8080`. Swagger UI disponible en `/swagger-ui.html`.

### 3. Frontend

```bash
cd Frontend
npm install
npm run dev
```

La aplicacion inicia en `http://localhost:5173` (proxy configurado hacia el backend en puerto 8080).

--- 
<h6 align="center">Universidad Tecnológica Nacional - Facultad Regional de Córdoba - Tecnicatura Universitaria en Programación - 2026</h6>
