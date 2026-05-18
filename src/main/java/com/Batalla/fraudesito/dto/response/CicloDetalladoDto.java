package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CicloDetalladoDto {
    /** ID de la cuenta que ancla el ciclo (la de menor ID para deduplicar). */
    private String anclaId;
    private List<String> cuentasEnCiclo;
    private List<String> numerosCuenta;
    private Integer saltos;
}
