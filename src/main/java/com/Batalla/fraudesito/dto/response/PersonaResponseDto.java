package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonaResponseDto {

    private String id;
    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private LocalDateTime fechaCreacion;
    private boolean activa;
    private boolean verificada;
    private Integer nivelRiesgo;
    private boolean esPEP;
    private boolean esSancionado;
}
