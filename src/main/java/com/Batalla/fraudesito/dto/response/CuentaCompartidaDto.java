package com.batalla.fraudesito.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CuentaCompartidaDto {
    private String cuenta1Id;
    private String cuenta2Id;
    private String dispositivoId;
    private String fingerprint;
    private String titular1Id;
    private String titular2Id;
}
