package com.batalla.fraudesito.repository;

import com.batalla.fraudesito.domain.enums.TipoDispositivo;
import com.batalla.fraudesito.domain.node.Dispositivo;
import com.batalla.fraudesito.dto.response.DispositivoMultiCuentaDto;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface DispositivoRepository extends Neo4jRepository<Dispositivo, String> {

    // ─── Búsquedas básicas ──────────────────────────────────────────────────────

    Optional<Dispositivo> findByFingerprint(String fingerprint);

    boolean existsByFingerprint(String fingerprint);

    List<Dispositivo> findByIpAddress(String ipAddress);

    List<Dispositivo> findByTipoDispositivo(TipoDispositivo tipo);

    List<Dispositivo> findByEsEmuladorTrue();

    List<Dispositivo> findByEsRooteadoTrue();

    List<Dispositivo> findByIpEsTorTrue();

    List<Dispositivo> findByIpEsVPNTrue();

    List<Dispositivo> findByEsSospechosoTrue();

    // ─── Búsqueda por persona ────────────────────────────────────────────────────

    @Query("""
            MATCH (p:Persona {id: $personaId})-[:USA_DISPOSITIVO]->(d:Dispositivo)
            RETURN d
            """)
    List<Dispositivo> findByPersonaId(@Param("personaId") String personaId);

    // ─── Fraude: todos los indicadores de riesgo combinados ──────────────────────

    @Query("""
            MATCH (d:Dispositivo)
            WHERE d.esEmulador   = true
               OR d.esRooteado   = true
               OR d.ipEsTor      = true
               OR d.ipEsVPN      = true
               OR d.esSospechoso = true
            RETURN d
            """)
    List<Dispositivo> findDispositivosDeAltoRiesgo();

    // ─── Fraude: dispositivos compartidos por múltiples personas ─────────────────
    // Un fingerprint vinculado a más de N personas es señal directa de que
    // el mismo actor controla varias identidades (synthetic identity fraud).

    @Query("""
            MATCH (d:Dispositivo)<-[:USA_DISPOSITIVO]-(p:Persona)
            WITH d, COUNT(DISTINCT p) AS cantPersonas
            WHERE cantPersonas > $limite
            RETURN d
            ORDER BY cantPersonas DESC
            """)
    List<Dispositivo> findDispositivosCompartidosPorMultiplesPersonas(@Param("limite") int limite);

    // ─── Fraude: dispositivos comunes entre dos personas sospechosas ──────────────
    // Demuestra vinculación directa entre dos personas investigadas.

    @Query("""
            MATCH (p1:Persona {id: $personaId1})-[:USA_DISPOSITIVO]->(d:Dispositivo)
                  <-[:USA_DISPOSITIVO]-(p2:Persona {id: $personaId2})
            RETURN DISTINCT d
            """)
    List<Dispositivo> findDispositivosComunesEntrePersonas(
            @Param("personaId1") String personaId1,
            @Param("personaId2") String personaId2);

    // ─── Fraude: dispositivos que operan cuentas de múltiples titulares ──────────
    // Detecta account takeover: el mismo dispositivo accede a cuentas que
    // pertenecen a distintas personas (el atacante tiene control del dispositivo).

    @Query("""
            MATCH (d:Dispositivo)<-[:USADA_EN]-(c:Cuenta)
            MATCH (p:Persona)-[:POSEE_CUENTA]->(c)
            WITH d, COUNT(DISTINCT p) AS titularesDistintos, COLLECT(DISTINCT p.id) AS titularIds
            WHERE titularesDistintos > $limite
            RETURN d.id           AS dispositivoId,
                   d.fingerprint  AS fingerprint,
                   d.ipAddress    AS ipAddress,
                   titularesDistintos,
                   titularIds
            ORDER BY titularesDistintos DESC
            """)
    List<Map<String, Object>> findDispositivosConCuentasDeMultiplesTitulares(@Param("limite") int limite);

    // ─── Fraude: dispositivos por país de IP ─────────────────────────────────────
    // Filtra operaciones provenientes de jurisdicciones de alto riesgo.

    @Query("""
            MATCH (d:Dispositivo)
            WHERE d.ipPais = $pais
            RETURN d
            """)
    List<Dispositivo> findByIpPais(@Param("pais") String pais);

    // ─── Análisis: conteo de personas únicas por dispositivo ─────────────────────

    @Query("""
            MATCH (d:Dispositivo {id: $dispositivoId})<-[:USA_DISPOSITIVO]-(p:Persona)
            RETURN COUNT(DISTINCT p) AS cantPersonas
            """)
    Long contarPersonasPorDispositivo(@Param("dispositivoId") String dispositivoId);

    // ─── Análisis: ranking de dispositivos con más personas asociadas ─────────────

    @Query("""
            MATCH (d:Dispositivo)<-[:USA_DISPOSITIVO]-(p:Persona)
            WITH d, COUNT(DISTINCT p) AS cantPersonas
            RETURN d.id          AS dispositivoId,
                   d.fingerprint AS fingerprint,
                   d.ipAddress   AS ipAddress,
                   d.marca       AS marca,
                   d.modelo      AS modelo,
                   cantPersonas
            ORDER BY cantPersonas DESC
            LIMIT $limite
            """)
    List<Map<String, Object>> findRankingDispositivosMasUsados(@Param("limite") int limite);

    // ─── Fraude avanzado: múltiples cuentas en mismo dispositivo (enriquecido) ──
    // Agrupa todas las cuentas y titulares por dispositivo. Detecta cuando un único
    // dispositivo concentra cuentas de múltiples personas (account takeover avanzado,
    // synthetic identity y money muling coordinado). A diferencia del account takeover
    // básico (pares titular-cuenta), aquí se ve el ecosistema completo del dispositivo.

    @Query("""
            MATCH (d:Dispositivo)<-[:USADA_EN]-(c:Cuenta)<-[:POSEE_CUENTA]-(p:Persona)
            WITH d,
                 COLLECT(DISTINCT c.id) AS cuentaIds,
                 COLLECT(DISTINCT p.id) AS titularIds,
                 COUNT(DISTINCT c)      AS totalCuentas,
                 COUNT(DISTINCT p)      AS totalTitulares
            WHERE totalTitulares >= $minTitulares
            RETURN d.id          AS dispositivoId,
                   d.fingerprint AS fingerprint,
                   d.ipAddress   AS ipAddress,
                   d.esEmulador  AS esEmulador,
                   d.ipEsTor     AS ipEsTor,
                   d.ipEsVPN     AS ipEsVPN,
                   totalTitulares,
                   totalCuentas,
                   cuentaIds,
                   titularIds
            ORDER BY totalTitulares DESC
            LIMIT $limite
            """)
    List<DispositivoMultiCuentaDto> findDispositivosConMultiplesCuentasDetallado(
            @Param("minTitulares") int minTitulares,
            @Param("limite") int limite);
}
