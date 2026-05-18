package com.batalla.fraudesito.domain.relationship;

import com.batalla.fraudesito.domain.enums.CanalTransaccion;
import com.batalla.fraudesito.domain.enums.EstadoTransaccion;
import com.batalla.fraudesito.domain.node.Cuenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransfiereA {

    @RelationshipId
    private Long id;

    /**
     * OUTGOING desde CuentaA → apunta a CuentaB (destino).
     * INCOMING desde CuentaB → apunta a CuentaA (origen).
     * SDN invierte el rol de @TargetNode según la dirección de carga.
     */
    @TargetNode
    private Cuenta cuentaVinculada;

    private String transaccionId;
    private String monto;
    private String moneda;
    private CanalTransaccion canal;
    private String descripcion;
    private LocalDateTime fecha;
    private EstadoTransaccion estado;
}
