package com.batalla.fraudesito.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodoGrafoDto {
    private String id;
    private String etiqueta;
}
