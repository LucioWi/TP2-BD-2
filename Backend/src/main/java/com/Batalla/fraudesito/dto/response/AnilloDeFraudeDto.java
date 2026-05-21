package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnilloDeFraudeDto {
    private String id;
    private String monto;
    private String estado;
    private Integer nivelRiesgo;
    private String canal;
    private String ipAddress;
}
