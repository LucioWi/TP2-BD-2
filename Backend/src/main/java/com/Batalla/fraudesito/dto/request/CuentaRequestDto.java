package com.batalla.fraudesito.dto.request;

import com.batalla.fraudesito.domain.enums.EstadoCuenta;
import com.batalla.fraudesito.domain.enums.TipoCuenta;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaRequestDto {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10,22}$", message = "El número de cuenta debe tener entre 10 y 22 dígitos")
    private String numeroCuenta;

    @Pattern(regexp = "^[0-9]{22}$", message = "El CBU/CVU debe tener exactamente 22 dígitos")
    private String cbvu;

    @Size(max = 20, message = "El alias no puede superar los 20 caracteres")
    private String alias;

    @Size(max = 100, message = "El nombre del banco no puede superar los 100 caracteres")
    private String banco;

    @NotNull(message = "El tipo de cuenta es obligatorio")
    private TipoCuenta tipoCuenta;

    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "El saldo debe ser un número válido con máximo 2 decimales")
    private String saldo;

    @NotBlank(message = "La moneda es obligatoria")
    @Size(min = 3, max = 3, message = "La moneda debe ser un código ISO 4217 de 3 letras (ej: ARS, USD)")
    private String moneda;

    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "El límite debe ser un número válido con máximo 2 decimales")
    private String limiteTransferenciaDiaria;

    private EstadoCuenta estado;
}
