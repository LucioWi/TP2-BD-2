package com.batalla.fraudesito.domain.relationship;

import com.batalla.fraudesito.domain.node.Dispositivo;
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
public class UsaDispositivo {

    @RelationshipId
    private Long id;

    @TargetNode
    private Dispositivo dispositivo;

    private LocalDateTime ultimoUso;
    private Integer frecuenciaUso;
}
