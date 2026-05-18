package com.batalla.fraudesito.domain.relationship;

import com.batalla.fraudesito.domain.node.Dispositivo;
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
public class UsadaEn {

    @RelationshipId
    private Long id;

    @TargetNode
    private Dispositivo dispositivo;

    private LocalDateTime fechaPrimeraOperacion;
    private LocalDateTime fechaUltimaOperacion;

    @Builder.Default
    private Integer cantidadOperaciones = 0;
}
