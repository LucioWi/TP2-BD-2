package com.batalla.fraudesito.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiesgoRelacionalDto {
    private String personaId;
    private String nombre;
    private String apellido;
    private Integer contactosFinancieros;
}
