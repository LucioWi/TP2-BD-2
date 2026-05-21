package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaminoGrafoDto {
    private String origenId;
    private String destinoId;
    private int saltos;
    /** Nodos con etiqueta de tipo (solo para caminos entre Personas). */
    private List<NodoGrafoDto> nodos;
    /** IDs en orden para caminos entre Cuentas o Transacciones. */
    private List<String> entidades;
}
