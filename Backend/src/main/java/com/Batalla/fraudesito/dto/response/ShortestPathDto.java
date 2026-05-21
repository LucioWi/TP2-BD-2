package com.batalla.fraudesito.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Respuesta de shortest path compatible con Cytoscape.js.
 *
 * Estructura:
 *   elements.nodes  → array de nodos con sus propiedades y clasificación en el camino
 *   elements.edges  → array de aristas con los datos de cada transferencia
 *   metadata        → resumen del camino, intermediarios y riesgo estimado
 *
 * Cytoscape.js consume directamente el objeto "elements":
 *   cytoscape({ container, elements: response.elements, ... })
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortestPathDto {

    private Elements elements;
    private Metadata metadata;

    // ─── Elementos Cytoscape ───────────────────────────────────────────────────

    @Data
    @Builder
    public static class Elements {
        private List<Node> nodes;
        private List<Edge> edges;
    }

    // ─── Nodo ─────────────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class Node {
        private NodeData data;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NodeData {
        /** ID único del nodo (UUID de la cuenta). */
        private String id;
        /** Etiqueta visible: alias si existe, si no numeroCuenta. */
        private String label;
        private String numeroCuenta;
        private String banco;
        private String tipoCuenta;
        private String estado;
        private String saldo;
        private String moneda;
        /** true si es la cuenta de inicio del camino. */
        private Boolean esOrigen;
        /** true si es la cuenta de destino final. */
        private Boolean esDestino;
        /** true si está entre origen y destino (cuenta puente / intermediaria). */
        private Boolean esIntermediario;
        /**
         * Score 0-100 calculado desde el estado de la cuenta.
         * ACTIVA=0, CERRADA=40, SUSPENDIDA=60, BLOQUEADA=100.
         */
        private Integer riesgoNodo;
    }

    // ─── Arista (TRANSFIERE_A) ─────────────────────────────────────────────────

    @Data
    @Builder
    public static class Edge {
        private EdgeData data;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EdgeData {
        /** ID sintético para Cytoscape: "edge-{índice}". */
        private String id;
        /** ID del nodo origen (requerido por Cytoscape). */
        private String source;
        /** ID del nodo destino (requerido por Cytoscape). */
        private String target;
        /** ID de la Transaccion que originó esta relación TRANSFIERE_A. */
        private String transaccionId;
        private String monto;
        private String moneda;
        private String canal;
        private String estadoTx;
        private String fecha;
    }

    // ─── Metadata del camino ───────────────────────────────────────────────────

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Metadata {
        private String origenId;
        private String destinoId;
        /** Cantidad de saltos (aristas) en el camino. */
        private Integer saltos;
        /** IDs de las cuentas intermediarias (excluye origen y destino). */
        private List<String> intermediarios;
        /**
         * Riesgo estimado del camino completo: 0-100.
         * Acumula penalizaciones por intermediarios, estados de cuentas, etc.
         */
        private Integer riesgoEstimado;
        /** Explicación textual de cada factor que contribuyó al riesgoEstimado. */
        private List<String> factoresRiesgo;
    }
}
