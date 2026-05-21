package com.batalla.fraudesito.dto.response;

import java.util.List;

public record CaminoDetalladoRecord(
        List<NodoCamino> nodos,
        List<RelacionCamino> relaciones,
        Long saltos
) {
    public record NodoCamino(
            String id,
            String numeroCuenta,
            String banco,
            String tipoCuenta,
            String estado,
            String saldo,
            String moneda,
            String alias
    ) { }

    public record RelacionCamino(
            String transaccionId,
            String origenId,
            String destinoId,
            String monto,
            String moneda,
            String canal,
            String estadoTx,
            String fecha
    ) { }
}
