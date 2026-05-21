package com.batalla.fraudesito.domain.node;

import com.batalla.fraudesito.domain.enums.Genero;
import com.batalla.fraudesito.domain.enums.TipoDocumento;
import com.batalla.fraudesito.domain.relationship.PoseeCuenta;
import com.batalla.fraudesito.domain.relationship.UsaDispositivo;
import lombok.*;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nodo central del grafo. Representa a una persona física que puede ser
 * titular de cuentas bancarias y operar desde distintos dispositivos.
 *
 * Las propiedades esPEP y esSancionado permiten aplicar filtros
 * regulatorios (OFAC, FATF) antes de ejecutar consultas Cypher de fraude.
 */
@Node("Persona")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"cuentas", "dispositivos"})
public class Persona {

    // ─── Identificación ────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
    private String id;

    private String nombre;
    private String apellido;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;     // valor del DNI, CUIT, pasaporte, etc.
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Genero genero;

    // ─── Ubicación ─────────────────────────────────────────────────────────────

    private String pais;
    private String provincia;
    private String ciudad;
    private String codigoPostal;
    private String direccion;

    // ─── Perfil de riesgo ──────────────────────────────────────────────────────

    /** Score 0–100 calculado por el motor de fraude. 0 = sin riesgo, 100 = crítico. */
    @Builder.Default
    private Integer nivelRiesgo = 0;

    /** Persona Políticamente Expuesta — sujeta a controles AML reforzados. */
    @Builder.Default
    private boolean esPEP = false;

    /** Figura en listas de sanciones internacionales (OFAC, ONU, UE). */
    @Builder.Default
    private boolean esSancionado = false;

    // ─── Estado ────────────────────────────────────────────────────────────────

    @Builder.Default
    private boolean activa = true;

    /** Indica si la identidad fue validada con documentación oficial. */
    @Builder.Default
    private boolean verificada = false;

    // ─── Auditoría ─────────────────────────────────────────────────────────────

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime fechaUltimaActividad;

    // ─── Relaciones ────────────────────────────────────────────────────────────

    @Relationship(type = "POSEE_CUENTA", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<PoseeCuenta> cuentas = new ArrayList<>();

    @Relationship(type = "USA_DISPOSITIVO", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<UsaDispositivo> dispositivos = new ArrayList<>();
}
