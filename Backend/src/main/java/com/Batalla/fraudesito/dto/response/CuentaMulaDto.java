package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CuentaMulaDto {
    private String cuentaId;
    private String numeroCuenta;
    private String banco;
    private String tipoCuenta;
    private Long totalRecibidas;
    private Long totalEnviadas;
    private Long actividadTotal;
    private Double volumenRecibido;
    private Double volumenEnviado;
}
