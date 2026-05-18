package com.batalla.fraudesito.mapper;

import com.batalla.fraudesito.domain.node.Cuenta;
import com.batalla.fraudesito.dto.response.CuentaResponseDto;
import org.springframework.stereotype.Component;

@Component
public class CuentaMapper {

    public CuentaResponseDto toDto(Cuenta cuenta) {
        return CuentaResponseDto.builder()
                .id(cuenta.getId())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .cbvu(cuenta.getCbvu())
                .alias(cuenta.getAlias())
                .banco(cuenta.getBanco())
                .tipoCuenta(cuenta.getTipoCuenta())
                .estado(cuenta.getEstado())
                .saldo(cuenta.getSaldo())
                .moneda(cuenta.getMoneda())
                .limiteTransferenciaDiaria(cuenta.getLimiteTransferenciaDiaria())
                .fechaApertura(cuenta.getFechaApertura())
                .fechaUltimaTransaccion(cuenta.getFechaUltimaTransaccion())
                .build();
    }
}
