package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispositivoMultiCuentaDto {
    private String dispositivoId;
    private String fingerprint;
    private String ipAddress;
    private Boolean esEmulador;
    private Boolean ipEsTor;
    private Boolean ipEsVPN;
    private Long totalTitulares;
    private Long totalCuentas;
    private List<String> cuentaIds;
    private List<String> titularIds;
}
