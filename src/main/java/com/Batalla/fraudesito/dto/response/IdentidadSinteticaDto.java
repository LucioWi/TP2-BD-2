package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentidadSinteticaDto {
    private String personaId;
    private String nombre;
    private String apellido;
    private String documento;
    private Integer nivelRiesgo;
    private Boolean verificada;
    /** Cantidad de otras personas que comparten al menos un dispositivo con esta. */
    private Integer personasVinculadas;
    private Integer dispositivosCompartidos;
    private Integer totalCuentas;
    private List<String> personasRelacionadasIds;
}
