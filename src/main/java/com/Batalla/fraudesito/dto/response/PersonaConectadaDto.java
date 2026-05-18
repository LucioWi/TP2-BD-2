package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonaConectadaDto {
    private String personaId;
    private String nombre;
    private String apellido;
    private Integer nivelRiesgo;
    private Boolean esPEP;
    private Boolean esSancionado;
}
