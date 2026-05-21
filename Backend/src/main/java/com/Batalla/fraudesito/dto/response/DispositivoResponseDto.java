package com.batalla.fraudesito.dto.response;

import com.batalla.fraudesito.domain.enums.TipoDispositivo;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispositivoResponseDto {

    private String id;
    private TipoDispositivo tipoDispositivo;
    private String marca;
    private String modelo;
    private String sistemaOperativo;
    private String fingerprint;
    private String ipAddress;
    private String ipPais;
    private String userAgent;
    private boolean ipEsProxy;
    private boolean ipEsVPN;
    private boolean ipEsTor;
    private boolean esEmulador;
    private boolean esRooteado;
    private boolean esSospechoso;
    private Integer cantidadPersonasAsociadas;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaUltimaActividad;
}
