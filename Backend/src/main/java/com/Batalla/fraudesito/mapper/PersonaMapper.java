package com.batalla.fraudesito.mapper;

import com.batalla.fraudesito.domain.node.Persona;
import com.batalla.fraudesito.dto.response.PersonaResponseDto;
import org.springframework.stereotype.Component;

@Component
public class PersonaMapper {

    public PersonaResponseDto toDto(Persona persona) {
        return PersonaResponseDto.builder()
                .id(persona.getId())
                .nombre(persona.getNombre())
                .apellido(persona.getApellido())
                .dni(persona.getNumeroDocumento())
                .email(persona.getEmail())
                .telefono(persona.getTelefono())
                .fechaNacimiento(persona.getFechaNacimiento())
                .fechaCreacion(persona.getFechaCreacion())
                .activa(persona.isActiva())
                .verificada(persona.isVerificada())
                .nivelRiesgo(persona.getNivelRiesgo())
                .esPEP(persona.isEsPEP())
                .esSancionado(persona.isEsSancionado())
                .build();
    }
}
