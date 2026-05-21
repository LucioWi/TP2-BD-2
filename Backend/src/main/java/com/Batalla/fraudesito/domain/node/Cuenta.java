package com.batalla.fraudesito.domain.node;

import com.batalla.fraudesito.domain.enums.EstadoCuenta;
import com.batalla.fraudesito.domain.enums.TipoCuenta;
import com.batalla.fraudesito.domain.relationship.TransfiereA;
import com.batalla.fraudesito.domain.relationship.UsadaEn;
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
 * Representa una cuenta bancaria o billetera virtual en el grafo de fraude.
 *
 * montoAcumuladoHoy se acumula durante el día y se compara contra
 * limiteTransferenciaDiaria para detectar smurfing (fragmentación de montos).
 */
@Node("Cuenta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"titulares", "transferenciasEnviadas", "transferenciasRecibidas", "dispositivosDeAcceso"})
public class Cuenta {

    // ─── Identificación bancaria ────────────────────────────────────────────────

    @Id
    private String id;

    private String numeroCuenta;

    /** CBU (banco tradicional) o CVU (billetera virtual) — 22 dígitos, único en el sistema. */
    private String cbvu;

    private String alias;
    private String banco;
    private TipoCuenta tipoCuenta;
    private EstadoCuenta estado;

    /** Código ISO 4217: ARS, USD, EUR. */
    private String moneda;

    // ─── Saldo ─────────────────────────────────────────────────────────────────

    private String saldo;
    private String saldoMinimo;
    private String limiteTransferenciaDiaria;

    /** Suma de transferencias del día. Reset diario. */
    @Builder.Default
    private String montoAcumuladoHoy = "0";

    // ─── Contadores ─────────────────────────────────────────────────────────────

    @Builder.Default
    private Integer cantidadTransaccionesEnviadas = 0;

    @Builder.Default
    private Integer cantidadTransaccionesRecibidas = 0;

    // ─── Bloqueo ────────────────────────────────────────────────────────────────

    /** Razón del bloqueo. Solo relevante cuando estado = BLOQUEADA | SUSPENDIDA. */
    private String motivoBloqueo;

    // ─── Timestamps ─────────────────────────────────────────────────────────────

    private LocalDateTime fechaApertura;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime fechaUltimaTransaccion;

    /** Solo se setea cuando estado = CERRADA. */
    private LocalDateTime fechaCierre;

    // ─── Relaciones ─────────────────────────────────────────────────────────────

    /**
     * Personas titulares de esta cuenta.
     * Navegación simple sin propiedades: úsalo para saber quién es el dueño.
     * Para acceder a fechaAsignacion o activa de la relación, usá un @Query Cypher.
     */
    @Relationship(type = "POSEE_CUENTA", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<Persona> titulares = new ArrayList<>();

    /** Transferencias que esta cuenta envió hacia otras cuentas. */
    @Relationship(type = "TRANSFIERE_A", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<TransfiereA> transferenciasEnviadas = new ArrayList<>();

    /**
     * Transferencias que esta cuenta recibió de otras cuentas.
     * En cada TransfiereA, cuentaVinculada apunta a la cuenta ORIGEN (quien envió).
     */
    @Relationship(type = "TRANSFIERE_A", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private List<TransfiereA> transferenciasRecibidas = new ArrayList<>();

    /** Dispositivos desde los que se operó esta cuenta. */
    @Relationship(type = "USADA_EN", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<UsadaEn> dispositivosDeAcceso = new ArrayList<>();
}
