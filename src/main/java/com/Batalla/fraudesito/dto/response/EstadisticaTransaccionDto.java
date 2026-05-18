package com.batalla.fraudesito.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstadisticaTransaccionDto {
    private String estado;
    private Long total;
}
