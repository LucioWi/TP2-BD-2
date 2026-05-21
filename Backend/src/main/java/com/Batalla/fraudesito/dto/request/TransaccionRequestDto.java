package com.batalla.fraudesito.dto.request;

import com.batalla.fraudesito.domain.enums.CanalTransaccion;
import com.batalla.fraudesito.domain.enums.TipoTransaccion;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionRequestDto {

    @NotBlank(message = "El monto es obligatorio")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "El monto debe ser un número válido con máximo 2 decimales")
    private String monto;

    @NotBlank(message = "La moneda es obligatoria")
    @Size(min = 3, max = 3, message = "La moneda debe ser un código ISO 4217 de 3 letras")
    private String moneda;

    @NotNull(message = "El tipo de transacción es obligatorio")
    private TipoTransaccion tipo;

    @NotNull(message = "El canal es obligatorio")
    private CanalTransaccion canal;

    @NotBlank(message = "La cuenta de origen es obligatoria")
    private String cuentaOrigenId;

    @NotBlank(message = "La cuenta de destino es obligatoria")
    private String cuentaDestinoId;

    private String descripcion;

    private String concepto;

    private String userAgent;

    @DecimalMin(value = "-90.0", message = "La latitud debe ser >= -90")
    @DecimalMax(value = "90.0", message = "La latitud debe ser <= 90")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "La longitud debe ser >= -180")
    @DecimalMax(value = "180.0", message = "La longitud debe ser <= 180")
    private Double longitud;

    private String ipAddress;
}
