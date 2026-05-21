package com.batalla.fraudesito.dto.request;

import com.batalla.fraudesito.domain.enums.TipoDispositivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoRequestDto {

    @NotBlank(message = "La dirección IP es obligatoria")
    @Pattern(
        regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$",
        message = "La dirección IP debe ser una IPv4 válida"
    )
    private String ipAddress;

    private String userAgent;

    @NotNull(message = "El tipo de dispositivo es obligatorio")
    private TipoDispositivo tipoDispositivo;

    private String sistemaOperativo;
    private String fingerprint;
}
