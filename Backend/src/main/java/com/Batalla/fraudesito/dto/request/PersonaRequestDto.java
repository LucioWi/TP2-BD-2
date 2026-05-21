package com.batalla.fraudesito.dto.request;

import com.batalla.fraudesito.domain.enums.TipoDocumento;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PersonaRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String apellido;

    @NotNull(message = "El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Pattern(regexp = "^[0-9]{7,8}$", message = "El documento debe tener 7 u 8 dígitos numéricos")
    private String dni;

    @Email(message = "El email debe ser válido")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "El teléfono debe ser un número válido")
    private String telefono;

    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;
}
