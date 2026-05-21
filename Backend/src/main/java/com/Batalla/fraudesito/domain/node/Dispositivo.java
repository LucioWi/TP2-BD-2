package com.batalla.fraudesito.domain.node;

import com.batalla.fraudesito.domain.enums.TipoDispositivo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nodo que representa el dispositivo físico o virtual desde el que operan
 * personas y se accede a cuentas. Es un vector clave para detectar:
 *   - Account takeover: un dispositivo accede a cuentas de distintos titulares.
 *   - Fraude de identidad: múltiples personas operan desde el mismo dispositivo.
 *   - Automatización maliciosa: operaciones desde emuladores o redes anónimas.
 */
@Node("Dispositivo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"personas", "cuentasOperadas"})
public class Dispositivo {

    // ─── Identificación del dispositivo ─────────────────────────────────────────

    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private String id;

    private TipoDispositivo tipoDispositivo;
    private String marca;
    private String modelo;
    private String sistemaOperativo;
    private String versionSO;
    private String userAgent;

    /**
     * Hash del dispositivo derivado de atributos de hardware y software.
     * Persiste aunque el usuario cambie de app, borre cookies o use modo incógnito.
     */
    private String fingerprint;

    // ─── Red / IP ───────────────────────────────────────────────────────────────

    private String ipAddress;
    private String ipPais;
    private String ipCiudad;

    /** La IP real puede estar oculta detrás de un servidor intermediario. */
    @Builder.Default
    private boolean ipEsProxy = false;

    /** El origen geográfico real es desconocido cuando se usa una VPN. */
    @Builder.Default
    private boolean ipEsVPN = false;

    /** Nodo de salida Tor: anonimización deliberada de identidad y ubicación. */
    @Builder.Default
    private boolean ipEsTor = false;

    // ─── Teléfono ───────────────────────────────────────────────────────────────

    private String telefono;
    private String codigoPais;
    private String operador;

    // ─── Ubicación GPS ──────────────────────────────────────────────────────────

    private Double latitud;
    private Double longitud;
    private String paisGps;
    private String ciudadGps;

    /** Precisión del GPS en metros. Valores > 1000 m pueden indicar location spoofing. */
    private Double precisionGps;

    // ─── Indicadores de fraude ──────────────────────────────────────────────────

    /** Dispositivo corriendo en emulador o VM — señal de automatización o bot. */
    @Builder.Default
    private boolean esEmulador = false;

    /** Android rooteado o iOS con jailbreak — los controles de seguridad están deshabilitados. */
    @Builder.Default
    private boolean esRooteado = false;

    @Builder.Default
    private boolean esSospechoso = false;

    // ─── Contadores ─────────────────────────────────────────────────────────────

    @Builder.Default
    private Integer cantidadSesiones = 0;

    /**
     * Personas distintas que usaron este dispositivo.
     * Más de una persona por dispositivo activa alerta de account sharing o takeover.
     */
    @Builder.Default
    private Integer cantidadPersonasAsociadas = 0;

    // ─── Timestamps ─────────────────────────────────────────────────────────────

    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime fechaUltimaActividad;

    // ─── Relaciones ─────────────────────────────────────────────────────────────

    /**
     * Personas que usan este dispositivo (INCOMING desde Persona -[USA_DISPOSITIVO]->).
     * Navegación simple sin propiedades de relación.
     * Para acceder a ultimoUso o frecuenciaUso, usá un @Query Cypher.
     */
    @Relationship(type = "USA_DISPOSITIVO", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<Persona> personas = new ArrayList<>();

    /**
     * Cuentas operadas desde este dispositivo (INCOMING desde Cuenta -[USADA_EN]->).
     * Un dispositivo vinculado a muchas cuentas de distintos titulares es el
     * indicador más directo de account takeover en análisis de grafos.
     */
    @Relationship(type = "USADA_EN", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<Cuenta> cuentasOperadas = new ArrayList<>();
}
