package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DispositivoTakeoverDto {
    private String dispositivoId;
    private String fingerprint;
    private String ipAddress;
    private Integer titularesDistintos;
    private List<String> titularIds;
}
