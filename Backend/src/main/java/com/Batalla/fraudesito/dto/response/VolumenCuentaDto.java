package com.batalla.fraudesito.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VolumenCuentaDto {
    private String cuentaId;
    private String numeroCuenta;
    private Long totalTx;
    private Double volumenTotal;
}
