package com.batalla.fraudesito.dto.response;

import com.batalla.fraudesito.domain.enums.CanalTransaccion;
import com.batalla.fraudesito.domain.enums.EstadoTransaccion;
import com.batalla.fraudesito.domain.enums.TipoTransaccion;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransaccionResponseDto {

    private String id;
    private String numeroOrden;
    private String monto;
    private String moneda;
    private TipoTransaccion tipo;
    private CanalTransaccion canal;
    private EstadoTransaccion estado;
    private String descripcion;
    private String cuentaOrigenId;
    private String cuentaDestinoId;
    private LocalDateTime fechaTransaccion;
    private LocalDateTime fechaProcesamiento;
    private Double latitud;
    private Double longitud;
    private String ipAddress;
    private Integer nivelRiesgo;
    private boolean esAlertada;
    private String motivoAlerta;
    private boolean esDuplicada;
}
