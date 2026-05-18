package com.batalla.fraudesito.mapper;

import com.batalla.fraudesito.domain.node.Transaccion;
import com.batalla.fraudesito.dto.response.TransaccionResponseDto;
import org.springframework.stereotype.Component;

@Component
public class TransaccionMapper {

    public TransaccionResponseDto toDto(Transaccion transaccion) {
        return TransaccionResponseDto.builder()
                .id(transaccion.getId())
                .numeroOrden(transaccion.getNumeroOrden())
                .monto(transaccion.getMonto())
                .moneda(transaccion.getMoneda())
                .tipo(transaccion.getTipo())
                .canal(transaccion.getCanal())
                .estado(transaccion.getEstado())
                .descripcion(transaccion.getDescripcion())
                .cuentaOrigenId(transaccion.getCuentaOrigen() != null
                        ? transaccion.getCuentaOrigen().getId() : null)
                .cuentaDestinoId(transaccion.getCuentaDestino() != null
                        ? transaccion.getCuentaDestino().getId() : null)
                .fechaTransaccion(transaccion.getFechaCreacion())
                .fechaProcesamiento(transaccion.getFechaProcesamiento())
                .latitud(transaccion.getLatitud())
                .longitud(transaccion.getLongitud())
                .ipAddress(transaccion.getIpAddress())
                .nivelRiesgo(transaccion.getNivelRiesgo())
                .esAlertada(transaccion.isEsAlertada())
                .motivoAlerta(transaccion.getMotivoAlerta())
                .esDuplicada(transaccion.isEsDuplicada())
                .build();
    }
}
