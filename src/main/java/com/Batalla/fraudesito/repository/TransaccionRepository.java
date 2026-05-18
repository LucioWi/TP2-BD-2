package com.batalla.fraudesito.repository;

import com.batalla.fraudesito.domain.enums.EstadoTransaccion;
import com.batalla.fraudesito.domain.enums.TipoTransaccion;
import com.batalla.fraudesito.domain.node.Transaccion;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface TransaccionRepository extends Neo4jRepository<Transaccion, String> {

    // ─── Búsquedas básicas ──────────────────────────────────────────────────────

    List<Transaccion> findByEstado(EstadoTransaccion estado);

    List<Transaccion> findByTipo(TipoTransaccion tipo);

    List<Transaccion> findByEsAlertadaTrue();

    List<Transaccion> findByEsDuplicadaTrue();

    List<Transaccion> findByNivelRiesgoGreaterThanEqual(Integer umbral);

    // ─── Búsqueda por cuenta ─────────────────────────────────────────────────────

    @Query("""
            MATCH (t:Transaccion)-[:ORIGINADA_EN]->(c:Cuenta {id: $cuentaId})
            RETURN t ORDER BY t.fechaCreacion DESC
            """)
    List<Transaccion> findByCuentaOrigenId(@Param("cuentaId") String cuentaId);

    @Query("""
            MATCH (t:Transaccion)-[:DIRIGIDA_A]->(c:Cuenta {id: $cuentaId})
            RETURN t ORDER BY t.fechaCreacion DESC
            """)
    List<Transaccion> findByCuentaDestinoId(@Param("cuentaId") String cuentaId);

    // ─── Búsqueda por período y canal ────────────────────────────────────────────

    @Query("""
            MATCH (t:Transaccion)
            WHERE t.fechaCreacion >= $desde AND t.fechaCreacion <= $hasta
            RETURN t ORDER BY t.fechaCreacion DESC
            """)
    List<Transaccion> findByPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            MATCH (t:Transaccion)
            WHERE t.canal = $canal
              AND t.fechaCreacion >= $desde
              AND t.fechaCreacion <= $hasta
            RETURN t ORDER BY t.fechaCreacion DESC
            """)
    List<Transaccion> findByCanalYPeriodo(
            @Param("canal") String canal,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // ─── Fraude: alertadas por cuenta (origen o destino) ─────────────────────────

    @Query("""
            MATCH (t:Transaccion)-[:ORIGINADA_EN|DIRIGIDA_A]->(c:Cuenta {id: $cuentaId})
            WHERE t.esAlertada = true
            RETURN t ORDER BY t.fechaCreacion DESC
            """)
    List<Transaccion> findAlertadasPorCuenta(@Param("cuentaId") String cuentaId);

    // ─── Fraude: velocity check ───────────────────────────────────────────────────
    // Muchas transacciones desde la misma cuenta en una ventana de tiempo corta
    // son la señal más básica de automatización o fraude por volumen.

    @Query("""
            MATCH (t:Transaccion)-[:ORIGINADA_EN]->(c:Cuenta {id: $cuentaId})
            WHERE t.fechaCreacion >= $desde AND t.fechaCreacion <= $hasta
            WITH COUNT(t) AS totalTx, COLLECT(t) AS txs
            WHERE totalTx >= $umbral
            UNWIND txs AS t
            RETURN t ORDER BY t.fechaCreacion ASC
            """)
    List<Transaccion> findVelocityFraud(
            @Param("cuentaId") String cuentaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("umbral") int umbral);

    // ─── Fraude: smurfing (fragmentación de montos) ───────────────────────────────
    // Muchas transferencias de bajo monto a la misma cuenta destino en un período
    // buscan evadir los umbrales de reporte (UIF/GAFI).

    @Query("""
            MATCH (t:Transaccion)-[:DIRIGIDA_A]->(c:Cuenta {id: $cuentaDestinoId})
            WHERE t.fechaCreacion >= $desde AND t.fechaCreacion <= $hasta
              AND toFloat(t.monto) <= toFloat($montoMaximo)
            WITH COUNT(t) AS totalTx, COLLECT(t) AS txs
            WHERE totalTx >= $cantidadMinima
            UNWIND txs AS t
            RETURN t ORDER BY t.fechaCreacion ASC
            """)
    List<Transaccion> findPatronSmurfing(
            @Param("cuentaDestinoId") String cuentaDestinoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("montoMaximo") String montoMaximo,
            @Param("cantidadMinima") int cantidadMinima);

    // ─── Fraude: flujo circular (layering / A→B→C→A) ─────────────────────────────
    // El dinero regresa al punto de origen después de pasar por intermediarios.
    // Es la fase de "layering" en un esquema clásico de lavado de activos.

    @Query("""
            MATCH ciclo = (c:Cuenta {id: $cuentaId})-[:TRANSFIERE_A*2..6]->(c)
            RETURN [n IN nodes(ciclo) | n.id] AS cuentasEnCiclo,
                   length(ciclo)               AS saltos
            LIMIT $limite
            """)
    List<Map<String, Object>> findFlujoCircular(
            @Param("cuentaId") String cuentaId,
            @Param("limite") int limite);

    // ─── Fraude: anillo de fraude (red de transacciones relacionadas) ─────────────
    // Expande la red RELACIONADA_CON para mapear el alcance de un incidente.
    // Devuelve los atributos clave de cada transacción sin cargar el grafo completo.

    @Query("""
            MATCH (t:Transaccion {id: $transaccionId})-[:RELACIONADA_CON*1..4]-(tRel:Transaccion)
            WHERE tRel <> t
            RETURN DISTINCT
                   tRel.id          AS id,
                   tRel.monto       AS monto,
                   tRel.estado      AS estado,
                   tRel.nivelRiesgo AS nivelRiesgo,
                   tRel.canal       AS canal,
                   tRel.ipAddress   AS ipAddress
            ORDER BY tRel.nivelRiesgo DESC
            """)
    List<Map<String, Object>> findAnilloDeFraude(@Param("transaccionId") String transaccionId);

    // ─── Grafo: camino más corto entre dos transacciones en la red de fraude ──────
    // Determina si dos transacciones sospechosas están conectadas y a cuántos
    // saltos de RELACIONADA_CON. Útil para fusionar investigaciones separadas.

    @Query("""
            MATCH (t1:Transaccion {id: $idT1}), (t2:Transaccion {id: $idT2})
            MATCH camino = shortestPath((t1)-[:RELACIONADA_CON*..10]-(t2))
            RETURN [n IN nodes(camino) | n.id] AS transaccionesEnCamino,
                   length(camino)               AS saltos
            """)
    Optional<Map<String, Object>> findCaminoMasCortoEntreTransacciones(
            @Param("idT1") String idT1,
            @Param("idT2") String idT2);

    // ─── Análisis: distribución de transacciones por estado ──────────────────────

    @Query("""
            MATCH (t:Transaccion)
            RETURN t.estado AS estado,
                   COUNT(t) AS total
            ORDER BY total DESC
            """)
    List<Map<String, Object>> countPorEstado();

    // ─── Análisis: top cuentas por volumen enviado en un período ─────────────────

    @Query("""
            MATCH (t:Transaccion)-[:ORIGINADA_EN]->(c:Cuenta)
            WHERE t.fechaCreacion >= $desde AND t.fechaCreacion <= $hasta
            WITH c, COUNT(t) AS totalTx, SUM(toFloat(t.monto)) AS volumenTotal
            RETURN c.id          AS cuentaId,
                   c.numeroCuenta AS numeroCuenta,
                   totalTx,
                   volumenTotal
            ORDER BY volumenTotal DESC
            LIMIT $limite
            """)
    List<Map<String, Object>> findTopCuentasPorVolumen(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("limite") int limite);

    // ─── Fraude avanzado: ciclos de transferencias a nivel global ─────────────────
    // Escanea todo el grafo buscando ciclos A→B→...→A de 2 a 6 saltos (layering).
    // Cada ciclo se ancla en la cuenta con el ID lexicográficamente menor para evitar
    // reportar el mismo ciclo desde múltiples puntos de entrada (deduplicación via reduce).
    // ADVERTENCIA: query de alto costo en grafos grandes. Usar LIMIT conservador en producción.

    @Query("""
            MATCH ciclo = (c:Cuenta)-[:TRANSFIERE_A*2..6]->(c)
            WITH c, nodes(ciclo) AS nodos, length(ciclo) AS saltos
            WITH c, nodos[0..-1] AS unicos, saltos
            WHERE c.id = reduce(minId = c.id, n IN unicos |
                          CASE WHEN n.id < minId THEN n.id ELSE minId END)
            RETURN c.id                            AS anclaId,
                   [n IN unicos | n.id]            AS cuentasEnCiclo,
                   [n IN unicos | n.numeroCuenta]  AS numerosCuenta,
                   saltos
            ORDER BY saltos ASC
            LIMIT $limite
            """)
    List<Map<String, Object>> findCiclosGlobales(@Param("limite") int limite);
}
