package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComunidadSospechosaDto {
    /** Persona con mayor grado de salida en el cluster (el hub). */
    private String centroId;
    private String nombre;
    private String apellido;
    private Integer nivelRiesgo;
    /** Cantidad de contrapartes financieras únicas en el cluster. */
    private Integer totalConexiones;
    private List<String> miembrosIds;
}
