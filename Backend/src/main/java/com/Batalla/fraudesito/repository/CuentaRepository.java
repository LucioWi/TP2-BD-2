package com.batalla.fraudesito.repository;

import com.batalla.fraudesito.domain.enums.EstadoCuenta;
import com.batalla.fraudesito.domain.enums.TipoCuenta;
import com.batalla.fraudesito.domain.node.Cuenta;
import com.batalla.fraudesito.dto.response.CuentaSelectDto;
import com.batalla.fraudesito.dto.response.PasoCamino;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface CuentaRepository extends Neo4jRepository<Cuenta, String> {

    // ─── Búsquedas básicas ──────────────────────────────────────────────────────

    Optional<Cuenta> findByCbvu(String cbvu);

    Optional<Cuenta> findByAlias(String alias);

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    boolean existsByCbvu(String cbvu);

    boolean existsByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByEstado(EstadoCuenta estado);

    List<Cuenta> findByTipoCuenta(TipoCuenta tipoCuenta);

    List<Cuenta> findByBanco(String banco);

    List<Cuenta> findByMoneda(String moneda);

    // ─── Búsqueda por titular y dispositivo ─────────────────────────────────────

    @Query("""
            MATCH (p:Persona {id: $personaId})-[:POSEE_CUENTA]->(c:Cuenta)
            RETURN c
            """)
    List<Cuenta> findByPersonaId(@Param("personaId") String personaId);

    @Query("""
            MATCH (d:Dispositivo {id: $dispositivoId})<-[:USADA_EN]-(c:Cuenta)
            RETURN c
            """)
    List<Cuenta> findByDispositivoId(@Param("dispositivoId") String dispositivoId);

    // ─── Fraude: cuentas puente / money mule ─────────────────────────────────────
    // Cuentas que tanto reciben como envían transferencias son candidatas a
    // ser usadas como intermediarias para dispersar fondos ilícitos.

    @Query("""
            MATCH (c:Cuenta)
            WITH c,
                 size([(t:Transaccion)-[:DIRIGIDA_A]->(c) | t]) AS recibidas,
                 size([(t:Transaccion)-[:ORIGINADA_EN]->(c) | t]) AS enviadas
            WHERE recibidas > 0 AND enviadas > 0
            RETURN c
            ORDER BY (recibidas + enviadas) DESC
            LIMIT $limite
            """)
    List<Cuenta> findCuentasPuente(@Param("limite") int limite);

    // ─── Fraude: cuentas con transacciones alertadas ─────────────────────────────

    @Query("""
            MATCH (c:Cuenta)<-[:ORIGINADA_EN|DIRIGIDA_A]-(t:Transaccion)
            WHERE t.esAlertada = true
            RETURN DISTINCT c
            """)
    List<Cuenta> findCuentasConTransaccionesAlertadas();

    // ─── Fraude: alto volumen de transacciones enviadas en un período ─────────────
    // Detecta rafagas de envíos que pueden indicar dispersión masiva de fondos.

    @Query("""
            MATCH (c:Cuenta)<-[:ORIGINADA_EN]-(t:Transaccion)
            WHERE t.fechaCreacion >= $desde AND t.fechaCreacion <= $hasta
            WITH c, COUNT(t) AS totalEnviadas
            WHERE totalEnviadas >= $umbral
            RETURN c.id          AS cuentaId,
                   c.numeroCuenta AS numeroCuenta,
                   c.banco        AS banco,
                   totalEnviadas
            ORDER BY totalEnviadas DESC
            """)
    List<Map<String, Object>> findCuentasConAltoVolumenEnPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("umbral") int umbral);

    // ─── Fraude: cuentas accedidas desde dispositivos sospechosos ────────────────

    @Query("""
            MATCH (c:Cuenta)-[:USADA_EN]->(d:Dispositivo)
            WHERE d.esEmulador = true OR d.esRooteado = true OR d.ipEsTor = true
            RETURN DISTINCT c
            """)
    List<Cuenta> findCuentasConAccesoDesdeDispositivosSospechosos();

    // ─── Fraude: cuentas de distintos titulares operadas desde el mismo dispositivo
    // El cruce dispositivo → múltiples titulares es el indicador principal de
    // account takeover. Se retorna el par de cuentas y el dispositivo compartido.

    @Query("""
            MATCH (c1:Cuenta)-[:USADA_EN]->(d:Dispositivo)<-[:USADA_EN]-(c2:Cuenta)
            MATCH (p1:Persona)-[:POSEE_CUENTA]->(c1)
            MATCH (p2:Persona)-[:POSEE_CUENTA]->(c2)
            WHERE p1 <> p2 AND c1 <> c2
            RETURN DISTINCT c1.id          AS cuenta1Id,
                            c2.id          AS cuenta2Id,
                            d.id           AS dispositivoId,
                            d.fingerprint  AS fingerprint,
                            p1.id          AS titular1Id,
                            p2.id          AS titular2Id
            """)
    List<Map<String, Object>> findCuentasCompartidasEnDispositivo();

    // ─── Grafo: camino más corto entre dos cuentas (rastreo de fondos) ───────────
    // Recorre la cadena de transferencias para encontrar el rastro del dinero
    // entre una cuenta origen y una destino sospechosa.

    @Query("""
            MATCH (c1:Cuenta {id: $idOrigen}), (c2:Cuenta {id: $idDestino})
            MATCH camino = shortestPath((c1)-[:TRANSFIERE_A*..10]-(c2))
            RETURN [n IN nodes(camino) | n.id] AS cuentasEnCamino,
                   length(camino)               AS saltos
            """)
    Optional<Map<String, Object>> findCaminoDeTransferencias(
            @Param("idOrigen") String idOrigen,
            @Param("idDestino") String idDestino);

    // ─── Grafo: shortest path enriquecido compatible con Cytoscape.js ────────────
    // Retorna propiedades completas de cada nodo (cuenta) Y de cada relación
    // (TRANSFIERE_A) en el camino más corto dirigido.
    // Dirigido (->): solo sigue el flujo real del dinero; si no existe un camino
    // en esa dirección devuelve lista vacía.
    // Usa UNION para aplanar nodos y relaciones en filas separadas con un
    // discriminador de tipo, evitando map projections anidados que SDN no mapea.

    @Query("""
            MATCH (c1:Cuenta {id: $idOrigen}), (c2:Cuenta {id: $idDestino})
            MATCH camino = shortestPath((c1)-[:TRANSFIERE_A*..10]->(c2))
            WITH camino
            UNWIND nodes(camino) AS n
            RETURN 'NODE' AS tipo,
                   n.id AS id,
                   n.numeroCuenta AS numeroCuenta,
                   n.banco AS banco,
                   n.tipoCuenta AS tipoCuenta,
                   n.estado AS estado,
                   toString(n.saldo) AS saldo,
                   n.moneda AS moneda,
                   n.alias AS alias,
                   null AS transaccionId,
                   null AS origenId,
                   null AS destinoId,
                   null AS monto,
                   null AS canal,
                   null AS estadoTx,
                   null AS fecha
            UNION ALL
            MATCH (c1:Cuenta {id: $idOrigen}), (c2:Cuenta {id: $idDestino})
            MATCH camino = shortestPath((c1)-[:TRANSFIERE_A*..10]->(c2))
            WITH camino
            UNWIND relationships(camino) AS r
            RETURN 'EDGE' AS tipo,
                   r.transaccionId AS id,
                   null AS numeroCuenta,
                   null AS banco,
                   null AS tipoCuenta,
                   null AS estado,
                   null AS saldo,
                   null AS moneda,
                   null AS alias,
                   r.transaccionId AS transaccionId,
                   startNode(r).id AS origenId,
                   endNode(r).id AS destinoId,
                   toString(r.monto) AS monto,
                   toString(r.canal) AS canal,
                   toString(r.estado) AS estadoTx,
                   toString(r.fecha) AS fecha
            """)
    List<PasoCamino> findCaminoDeTransferenciasDetallado(
            @Param("idOrigen") String idOrigen,
            @Param("idDestino") String idDestino);

    // ─── Análisis: conteo de transacciones por cuenta en un período ──────────────

    @Query("""
            MATCH (c:Cuenta {id: $cuentaId})<-[:ORIGINADA_EN]-(t:Transaccion)
            WHERE t.fechaCreacion >= $desde AND t.fechaCreacion <= $hasta
            RETURN COUNT(t) AS total
            """)
    Long contarTransaccionesPorCuentaEnPeriodo(
            @Param("cuentaId") String cuentaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // ─── Fraude avanzado: cuentas mula con métricas de volumen ───────────────────
    // Extiende findCuentasPuente con volúmenes monetarios para calcular la tasa de
    // dispersión (cuánto del dinero recibido se reenvía, indicador clave de money muling).
    // Una cuenta mula típica recibe fondos de pocas fuentes y los dispersa hacia muchos
    // destinos en poco tiempo, a menudo con una tasa de dispersión cercana al 100%.

    @Query("""
            MATCH (c:Cuenta)
            OPTIONAL MATCH (tR:Transaccion)-[:DIRIGIDA_A]->(c)
            WITH c,
                 COUNT(tR)                            AS totalRecibidas,
                 COALESCE(SUM(toFloat(tR.monto)), 0.0) AS volumenRecibido
            OPTIONAL MATCH (tE:Transaccion)-[:ORIGINADA_EN]->(c)
            WITH c, totalRecibidas, volumenRecibido,
                 COUNT(tE)                            AS totalEnviadas,
                 COALESCE(SUM(toFloat(tE.monto)), 0.0) AS volumenEnviado
            WHERE totalRecibidas > 0 AND totalEnviadas > 0
            RETURN c.id          AS cuentaId,
                   c.numeroCuenta AS numeroCuenta,
                   c.banco        AS banco,
                   toString(c.tipoCuenta) AS tipoCuenta,
                   totalRecibidas,
                   totalEnviadas,
                   (totalRecibidas + totalEnviadas) AS actividadTotal,
                   volumenRecibido,
                   volumenEnviado
            ORDER BY actividadTotal DESC
            LIMIT $limite
            """)
    List<Map<String, Object>> findCuentasMulaDetalladas(@Param("limite") int limite);

    // ─── Selectores filtrados para UI ────────────────────────────────────────────

    @Query("""
            MATCH (c:Cuenta)
            WHERE EXISTS { MATCH (c)-[:TRANSFIERE_A*1..5]->(c) }
            RETURN c.id AS id, c.alias AS alias, c.banco AS banco
            ORDER BY c.id
            """)
    List<CuentaSelectDto> findCuentasConCiclosActivos();

    @Query("""
            MATCH (origen:Cuenta)-[:TRANSFIERE_A]->(mula:Cuenta {id: 'c10'})
            RETURN DISTINCT origen.id AS id, origen.alias AS alias, origen.banco AS banco
            ORDER BY origen.id
            """)
    List<CuentaSelectDto> findCuentasOrigenSospechosas();

    @Query("""
            MATCH (mula:Cuenta)-[:TRANSFIERE_A]->(hub:Cuenta)
            WHERE mula.id IN ['c10', 'c11', 'c17']
            RETURN DISTINCT hub.id AS id, hub.alias AS alias, hub.banco AS banco
            ORDER BY hub.id
            """)
    List<CuentaSelectDto> findCuentasDestinoHubs();
}
