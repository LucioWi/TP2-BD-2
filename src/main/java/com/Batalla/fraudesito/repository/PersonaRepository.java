package com.batalla.fraudesito.repository;

import com.batalla.fraudesito.domain.node.Persona;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PersonaRepository extends Neo4jRepository<Persona, String> {

    // ─── Búsquedas básicas ──────────────────────────────────────────────────────

    Optional<Persona> findByNumeroDocumento(String numeroDocumento);

    Optional<Persona> findByEmail(String email);

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByEmail(String email);

    List<Persona> findByEsPEPTrue();

    List<Persona> findByEsSancionadoTrue();

    List<Persona> findByActivaFalse();

    List<Persona> findByNivelRiesgoGreaterThanEqual(Integer umbral);

    List<Persona> findByPaisAndCiudad(String pais, String ciudad);

    // ─── Fraude: dispositivos compartidos entre personas ────────────────────────
    // Un dispositivo usado por más de una persona es señal de account sharing o
    // de que un mismo actor controla múltiples identidades.

    @Query("""
            MATCH (p1:Persona {id: $personaId})-[:USA_DISPOSITIVO]->(d:Dispositivo)
                  <-[:USA_DISPOSITIVO]-(p2:Persona)
            WHERE p1 <> p2
            RETURN DISTINCT p2
            """)
    List<Persona> findPersonasQueCompartenDispositivo(@Param("personaId") String personaId);

    // ─── Fraude: personas con cuentas bloqueadas ─────────────────────────────────

    @Query("""
            MATCH (p:Persona)-[:POSEE_CUENTA]->(c:Cuenta)
            WHERE c.estado = 'BLOQUEADA'
            RETURN DISTINCT p
            """)
    List<Persona> findPersonasConCuentasBloqueadas();

    // ─── Fraude: exceso de cuentas (mule account indicator) ─────────────────────
    // Tener más cuentas que el límite habitual es un indicador de money muling.

    @Query("""
            MATCH (p:Persona)-[:POSEE_CUENTA]->(c:Cuenta)
            WITH p, COUNT(c) AS totalCuentas
            WHERE totalCuentas > $limite
            RETURN p
            ORDER BY totalCuentas DESC
            """)
    List<Persona> findPersonasConMasDeCuentas(@Param("limite") int limite);

    // ─── Fraude: personas que usan dispositivos de alto riesgo ──────────────────

    @Query("""
            MATCH (p:Persona)-[:USA_DISPOSITIVO]->(d:Dispositivo)
            WHERE d.esEmulador = true OR d.esRooteado = true OR d.ipEsTor = true
            RETURN DISTINCT p
            """)
    List<Persona> findPersonasConDispositivosSospechosos();

    // ─── Fraude: PEPs que reciben transferencias por encima del umbral ───────────
    // Norma FATF R.12: PEPs con movimientos de alto valor requieren EDD.

    @Query("""
            MATCH (p:Persona {esPEP: true})-[:POSEE_CUENTA]->(c:Cuenta)
                  <-[:DIRIGIDA_A]-(t:Transaccion)
            WHERE toFloat(t.monto) >= $umbralMonto
            RETURN DISTINCT p
            """)
    List<Persona> findPEPsConTransferenciasDeAltoValor(@Param("umbralMonto") double umbralMonto);

    // ─── Fraude: personas sancionadas con cuentas activas ───────────────────────

    @Query("""
            MATCH (p:Persona {esSancionado: true})-[:POSEE_CUENTA]->(c:Cuenta)
            WHERE c.estado = 'ACTIVA'
            RETURN DISTINCT p
            """)
    List<Persona> findSancionadosConCuentasActivas();

    // ─── Análisis: personas con mayor número de contactos financieros ────────────
    // Personas conectadas a muchos titulares distintos a través de transferencias
    // son nodos centrales en redes de fraude (hubs).

    @Query("""
            MATCH (p:Persona)-[:POSEE_CUENTA]->(c:Cuenta)
                  -[:TRANSFIERE_A]->(c2:Cuenta)
                  <-[:POSEE_CUENTA]-(p2:Persona)
            WHERE p <> p2
            WITH p, COUNT(DISTINCT p2) AS contactosFinancieros
            ORDER BY contactosFinancieros DESC
            LIMIT $limite
            RETURN p.id AS personaId, p.nombre AS nombre, p.apellido AS apellido,
                   contactosFinancieros
            """)
    List<Map<String, Object>> findPersonasConMayorRiesgoRelacional(@Param("limite") int limite);

    // ─── Grafo: camino más corto entre dos personas ──────────────────────────────
    // Determina si dos personas están conectadas en el grafo y a cuántos saltos.
    // Útil para investigar si el sospechoso está relacionado con la víctima.

    @Query("""
            MATCH (p1:Persona {id: $idOrigen}), (p2:Persona {id: $idDestino})
            MATCH camino = shortestPath((p1)-[*..8]-(p2))
            RETURN [n IN nodes(camino) | {id: n.id, etiqueta: labels(n)[0]}] AS nodos,
                   length(camino) AS saltos
            """)
    Optional<Map<String, Object>> findCaminoMasCortoEntrePersonas(
            @Param("idOrigen") String idOrigen,
            @Param("idDestino") String idDestino);

    // ─── Fraude avanzado: comunidades financieras sospechosas ────────────────────
    // Aproximación de detección de comunidades sin GDS: encuentra personas con muchas
    // contrapartes financieras directas (grado de salida alto en el grafo de transferencias).
    // Los nodos con alto grado son los hubs de redes de fraude organizado y lavado de activos.
    // Para community detection real (Louvain/WCC), integrar Neo4j Graph Data Science.

    @Query("""
            MATCH (p:Persona)-[:POSEE_CUENTA]->(c1:Cuenta)-[:TRANSFIERE_A]->(c2:Cuenta)
                  <-[:POSEE_CUENTA]-(p2:Persona)
            WHERE p.id <> p2.id
            WITH p, COLLECT(DISTINCT p2) AS vecinos, COUNT(DISTINCT p2) AS gradoSalida
            WHERE gradoSalida >= $minConexiones
            RETURN p.id          AS centroId,
                   p.nombre      AS nombre,
                   p.apellido    AS apellido,
                   p.nivelRiesgo AS nivelRiesgo,
                   gradoSalida   AS totalConexiones,
                   [v IN vecinos | v.id] AS miembrosIds
            ORDER BY gradoSalida DESC
            LIMIT $limite
            """)
    List<Map<String, Object>> findComunidadesSospechosas(
            @Param("minConexiones") int minConexiones,
            @Param("limite") int limite);

    // ─── Fraude avanzado: red de conexiones financieras indirectas ───────────────
    // Expande la red de transferencias hasta 4 saltos desde una persona.
    // Permite identificar beneficiarios finales ocultos detrás de cuentas intermediarias.
    // Complementa al shortest path: este endpoint retorna el vecindario completo,
    // no solo el camino más corto punto a punto.

    @Query("""
            MATCH (inicio:Persona {id: $personaId})-[:POSEE_CUENTA]->(cOrigen:Cuenta)
                  -[:TRANSFIERE_A*2..4]->(cFin:Cuenta)<-[:POSEE_CUENTA]-(destino:Persona)
            WHERE destino.id <> $personaId
            RETURN DISTINCT
                   destino.id           AS personaId,
                   destino.nombre       AS nombre,
                   destino.apellido     AS apellido,
                   destino.nivelRiesgo  AS nivelRiesgo,
                   destino.esPEP        AS esPEP,
                   destino.esSancionado AS esSancionado
            ORDER BY destino.nivelRiesgo DESC, destino.esPEP DESC, destino.esSancionado DESC
            LIMIT $limite
            """)
    List<Map<String, Object>> findConexionesIndirectas(
            @Param("personaId") String personaId,
            @Param("limite") int limite);

    // ─── Fraude avanzado: indicadores de identidad sintética ─────────────────────
    // Identidad sintética: combinación de datos reales e inventados para crear
    // personas "nuevas" que no existen. Señales clave:
    //   - Dispositivo compartido con múltiples otras identidades
    //   - Identidad no verificada con alta actividad financiera
    //   - Muchas cuentas para una persona con poca historia
    // Detecta personas que comparten dispositivos con otras, lo cual es la señal
    // más directa de que un mismo actor controla múltiples identidades ficticias.

    @Query("""
            MATCH (p:Persona)-[:USA_DISPOSITIVO]->(d:Dispositivo)<-[:USA_DISPOSITIVO]-(pOtro:Persona)
            WHERE p.id <> pOtro.id
            WITH p,
                 COUNT(DISTINCT pOtro)    AS personasVinculadas,
                 COLLECT(DISTINCT pOtro.id) AS idsVinculados,
                 COUNT(DISTINCT d)        AS dispositivosCompartidos
            WHERE personasVinculadas >= $minPersonasVinculadas
            OPTIONAL MATCH (p)-[:POSEE_CUENTA]->(c:Cuenta)
            WITH p, personasVinculadas, dispositivosCompartidos, idsVinculados,
                 COUNT(c) AS totalCuentas
            RETURN p.id              AS personaId,
                   p.nombre          AS nombre,
                   p.apellido        AS apellido,
                   p.numeroDocumento AS documento,
                   p.nivelRiesgo     AS nivelRiesgo,
                   p.verificada      AS verificada,
                   personasVinculadas,
                   dispositivosCompartidos,
                   totalCuentas,
                   idsVinculados     AS personasRelacionadasIds
            ORDER BY personasVinculadas DESC, totalCuentas DESC
            LIMIT $limite
            """)
    List<Map<String, Object>> findIndicadoresIdentidadSintetica(
            @Param("minPersonasVinculadas") int minPersonasVinculadas,
            @Param("limite") int limite);
}
