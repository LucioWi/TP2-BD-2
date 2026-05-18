package com.batalla.fraudesito.domain.relationship;

import com.batalla.fraudesito.domain.enums.TipoRelacion;
import com.batalla.fraudesito.domain.node.Transaccion;
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
public class RelacionadaCon {

    @RelationshipId
    private Long id;

    /**
     * OUTGOING desde T1: apunta a T2 (la transacción marcada como relacionada).
     * INCOMING desde T2: apunta a T1 (quien la marcó).
     * SDN invierte el rol de @TargetNode según la dirección de carga.
     */
    @TargetNode
    private Transaccion transaccionVinculada;

    private TipoRelacion tipoRelacion;

    /** Score 0.0–1.0 de similitud calculado por el motor de fraude. */
    private Double puntajeSimilitud;

    private String descripcion;
    private LocalDateTime fechaDeteccion;
}
