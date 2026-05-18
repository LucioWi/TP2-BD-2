package com.batalla.fraudesito.domain.relationship;

import com.batalla.fraudesito.domain.node.Cuenta;
import lombok.*;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDateTime;

@RelationshipProperties
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoseeCuenta {

    @RelationshipId
    private Long id;

    @TargetNode
    private Cuenta cuenta;

    private LocalDateTime fechaAsignacion;
    private boolean activa;
}
