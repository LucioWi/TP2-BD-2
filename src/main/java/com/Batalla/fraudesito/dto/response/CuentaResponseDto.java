package com.batalla.fraudesito.dto.response;

import com.batalla.fraudesito.domain.enums.EstadoCuenta;
import com.batalla.fraudesito.domain.enums.TipoCuenta;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CuentaResponseDto {

    private String id;
    private String numeroCuenta;
    private String cbvu;
    private String alias;
    private String banco;
    private TipoCuenta tipoCuenta;
    private EstadoCuenta estado;
    private String saldo;
    private String moneda;
    private String limiteTransferenciaDiaria;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaUltimaTransaccion;
}
