package com.batalla.fraudesito.dto.response;

import com.batalla.fraudesito.domain.enums.EstadoCuenta;
import com.batalla.fraudesito.domain.enums.TipoCuenta;

public record PasoCamino(
        String tipo,
        String id,
        String numeroCuenta,
        String banco,
        TipoCuenta tipoCuenta,
        EstadoCuenta estado,
        String saldo,
        String moneda,
        String alias,
        String transaccionId,
        String origenId,
        String destinoId,
        String monto,
        String canal,
        String estadoTx,
        String fecha
) { }
