package com.batalla.fraudesito.domain.node;

import com.batalla.fraudesito.domain.enums.CanalTransaccion;
import com.batalla.fraudesito.domain.enums.EstadoTransaccion;
import com.batalla.fraudesito.domain.enums.TipoTransaccion;
import com.batalla.fraudesito.domain.relationship.RelacionadaCon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nodo que representa una operación financiera individual.
 *
 * Se conecta directamente a las Cuenta origen y destino mediante relaciones
 * tipadas (ORIGINADA_EN, DIRIGIDA_A), lo que permite recorrer el flujo de
 * dinero como caminos en el grafo sin IDs cruzados ni JOINs.
 *
 * Invariantes de dominio:
 *   motivoAlerta          solo tiene valor cuando esAlertada = true.
 *   transaccionOriginalId solo tiene valor cuando esDuplicada = true.
 */
@Node("Transaccion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"cuentaOrigen", "cuentaDestino", "transaccionesRelacionadas", "relacionadasConEsta"})
public class Transaccion {

    // ─── Identificación ─────────────────────────────────────────────────────────

    @Id
    private String id;

    /** Número de secuencia legible para el usuario final, ej: "TXN-2024-000001". */
    private String numeroOrden;

    /** Código asignado por el sistema bancario externo (COELSA, BCRA, SWIFT, etc.). */
    private String referenciaExterna;

    private String codigoAutorizacion;

    // ─── Monto ──────────────────────────────────────────────────────────────────

    private String monto;

    /** Código ISO 4217: ARS, USD, EUR. */
    private String moneda;

    /** Tipo de cambio aplicado si la operación involucra moneda extranjera. */
    private String tasaCambio;

    /** Equivalente en ARS calculado al momento exacto de la transacción. */
    private String montoEnPesos;

    // ─── Clasificación ──────────────────────────────────────────────────────────

    private TipoTransaccion tipo;
    private CanalTransaccion canal;
    private EstadoTransaccion estado;

    /** Código corto de concepto: "SUELDO", "ALQUILER", "SERVICIOS", etc. */
    private String concepto;

    private String descripcion;

    // ─── Contexto de red ────────────────────────────────────────────────────────

    private String ipAddress;
    private String userAgent;

    // ─── Geolocalización ────────────────────────────────────────────────────────

    private Double latitud;
    private Double longitud;
    private String pais;
    private String ciudad;

    // ─── Indicadores de fraude ──────────────────────────────────────────────────

    /** Score 0–100 asignado por el motor de fraude en el momento del análisis. */
    @Builder.Default
    private Integer nivelRiesgo = 0;

    @Builder.Default
    private boolean esAlertada = false;

    /** Razón de la alerta. Solo tiene valor cuando esAlertada = true. */
    private String motivoAlerta;

    /** True cuando el motor detecta que esta operación duplica una anterior. */
    @Builder.Default
    private boolean esDuplicada = false;

    /** ID de la transacción original. Solo tiene valor cuando esDuplicada = true. */
    private String transaccionOriginalId;

    // ─── Timestamps ─────────────────────────────────────────────────────────────

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    /** Momento en que la entidad bancaria efectivamente acreditó la operación. */
    private LocalDateTime fechaProcesamiento;

    // ─── Relaciones ─────────────────────────────────────────────────────────────

    /**
     * Cuenta desde la que se originó esta transacción.
     * Reemplaza el campo cuentaOrigenId (String) por una arista directa en el grafo.
     */
    @Relationship(type = "ORIGINADA_EN", direction = Relationship.Direction.OUTGOING)
    private Cuenta cuentaOrigen;

    /**
     * Cuenta que recibe los fondos.
     * Reemplaza el campo cuentaDestinoId (String) por una arista directa en el grafo.
     */
    @Relationship(type = "DIRIGIDA_A", direction = Relationship.Direction.OUTGOING)
    private Cuenta cuentaDestino;

    /**
     * Transacciones que ESTA marcó como relacionadas.
     * Cada RelacionadaCon lleva el motivo y puntaje de similitud.
     */
    @Relationship(type = "RELACIONADA_CON", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<RelacionadaCon> transaccionesRelacionadas = new ArrayList<>();

    /**
     * Transacciones que marcaron a ESTA como relacionada (sentido inverso).
     * Permite recorrer el grafo de fraude en ambas direcciones sin @Query Cypher.
     */
    @Relationship(type = "RELACIONADA_CON", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<RelacionadaCon> relacionadasConEsta = new ArrayList<>();
}
