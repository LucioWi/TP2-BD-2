package com.batalla.fraudesito.controller;

import com.batalla.fraudesito.dto.response.CaminoGrafoDto;
import com.batalla.fraudesito.dto.response.CicloCircularDto;
import com.batalla.fraudesito.dto.response.ShortestPathDto;
import com.batalla.fraudesito.service.CuentaService;
import com.batalla.fraudesito.service.PersonaService;
import com.batalla.fraudesito.service.TransaccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grafo")
@RequiredArgsConstructor
@Tag(
    name = "Grafo",
    description = """
        Operaciones de análisis de grafos sobre el modelo Neo4j.
        Incluye shortest path entre entidades y detección de ciclos en la red de transferencias.
        Un resultado 404 significa que los nodos existen pero no están conectados en el grafo.
        """
)
public class GrafoController {

    private final PersonaService personaService;
    private final CuentaService cuentaService;
    private final TransaccionService transaccionService;

    // ─── Shortest path ─────────────────────────────────────────────────────────

    @GetMapping("/personas/camino")
    @Operation(
        summary = "Shortest path entre dos personas",
        description = """
            Encuentra el camino más corto entre dos personas en el grafo completo,
            traversando cualquier tipo de relación (POSEE_CUENTA, USA_DISPOSITIVO, etc.).
            Máximo 8 saltos. Devuelve cada nodo en el camino con su tipo (etiqueta).
            Un resultado 404 indica que no existe camino entre ambas personas en <= 8 saltos.
            Uso investigativo: ¿está el sospechoso relacionado con la víctima?
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Camino encontrado",
            content = @Content(
                schema = @Schema(implementation = CaminoGrafoDto.class),
                examples = @ExampleObject(
                    name = "Dos personas conectadas en 3 saltos",
                    value = """
                            {
                              "origenId": "a1b2c3d4-...",
                              "destinoId": "e5f6a7b8-...",
                              "saltos": 3,
                              "nodos": [
                                { "id": "a1b2c3d4-...", "etiqueta": "Persona" },
                                { "id": "cc11-...",     "etiqueta": "Cuenta"  },
                                { "id": "cc22-...",     "etiqueta": "Cuenta"  },
                                { "id": "e5f6a7b8-...", "etiqueta": "Persona" }
                              ]
                            }
                            """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Persona no encontrada o no existe camino entre ellas")
    })
    public ResponseEntity<CaminoGrafoDto> caminoPersonas(
            @Parameter(description = "ID de la persona origen", required = true) @RequestParam String origen,
            @Parameter(description = "ID de la persona destino", required = true) @RequestParam String destino) {
        return ResponseEntity.ok(personaService.caminoMasCorto(origen, destino));
    }

    @GetMapping("/cuentas/shortest-path")
    @Operation(
        summary = "Shortest path enriquecido entre dos cuentas — compatible Cytoscape.js",
        description = """
            Calcula el camino más corto DIRIGIDO de transferencias (TRANSFIERE_A) entre dos cuentas.
            Dirigido: solo sigue el flujo real del dinero (origen → destino).
            Si no existe un camino directo en ese sentido, retorna 404.

            La respuesta incluye:
            - elements.nodes: propiedades de cada cuenta en el camino (banco, estado, saldo, rol)
            - elements.edges: datos de cada transferencia (monto, canal, fecha, transaccionId)
            - metadata.riesgoEstimado: score 0-100 calculado desde estados de cuenta e intermediarios
            - metadata.factoresRiesgo: explicación textual de cada penalización
            - metadata.intermediarios: IDs de las cuentas entre origen y destino

            Integración con Cytoscape.js:
              fetch('/api/v1/grafo/cuentas/shortest-path?origen=A&destino=B')
                .then(r => r.json())
                .then(data => cytoscape({ container, elements: data.elements }))

            Para un listado básico de IDs sin metadatos, usar GET /cuentas/transferencias.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Camino encontrado — JSON listo para Cytoscape.js",
            content = @Content(
                schema = @Schema(implementation = ShortestPathDto.class),
                examples = @ExampleObject(
                    name = "Camino A→B→C con B como intermediaria",
                    value = """
                            {
                              "elements": {
                                "nodes": [
                                  {
                                    "data": {
                                      "id": "uuid-cuenta-a",
                                      "label": "PATO.VERDE.SOL",
                                      "numeroCuenta": "0720034888000078901234",
                                      "banco": "Banco Nación",
                                      "tipoCuenta": "CAJA_AHORRO",
                                      "estado": "ACTIVA",
                                      "saldo": "15000.00",
                                      "moneda": "ARS",
                                      "esOrigen": true,
                                      "esDestino": false,
                                      "esIntermediario": false,
                                      "riesgoNodo": 0
                                    }
                                  },
                                  {
                                    "data": {
                                      "id": "uuid-cuenta-b",
                                      "label": "2850590940090418135201",
                                      "numeroCuenta": "2850590940090418135201",
                                      "banco": "Mercado Pago",
                                      "tipoCuenta": "BILLETERA_VIRTUAL",
                                      "estado": "BLOQUEADA",
                                      "saldo": "0.00",
                                      "moneda": "ARS",
                                      "esOrigen": false,
                                      "esDestino": false,
                                      "esIntermediario": true,
                                      "riesgoNodo": 100
                                    }
                                  },
                                  {
                                    "data": {
                                      "id": "uuid-cuenta-c",
                                      "label": "AGUILA.RIO.LUNA",
                                      "numeroCuenta": "0720034000000078904321",
                                      "banco": "Banco Galicia",
                                      "tipoCuenta": "CUENTA_CORRIENTE",
                                      "estado": "ACTIVA",
                                      "saldo": "320000.00",
                                      "moneda": "ARS",
                                      "esOrigen": false,
                                      "esDestino": true,
                                      "esIntermediario": false,
                                      "riesgoNodo": 0
                                    }
                                  }
                                ],
                                "edges": [
                                  {
                                    "data": {
                                      "id": "edge-0",
                                      "source": "uuid-cuenta-a",
                                      "target": "uuid-cuenta-b",
                                      "transaccionId": "TXN-2024-A1B2C3D4",
                                      "monto": "150000.00",
                                      "moneda": "ARS",
                                      "canal": "API",
                                      "estadoTx": "COMPLETADA",
                                      "fecha": "2024-03-15T14:22:00"
                                    }
                                  },
                                  {
                                    "data": {
                                      "id": "edge-1",
                                      "source": "uuid-cuenta-b",
                                      "target": "uuid-cuenta-c",
                                      "transaccionId": "TXN-2024-E5F6G7H8",
                                      "monto": "149500.00",
                                      "moneda": "ARS",
                                      "canal": "TRANSFERENCIA_AUTOMATICA",
                                      "estadoTx": "COMPLETADA",
                                      "fecha": "2024-03-15T14:25:00"
                                    }
                                  }
                                ]
                              },
                              "metadata": {
                                "origenId": "uuid-cuenta-a",
                                "destinoId": "uuid-cuenta-c",
                                "saltos": 2,
                                "intermediarios": ["uuid-cuenta-b"],
                                "riesgoEstimado": 55,
                                "factoresRiesgo": [
                                  "1 cuenta(s) intermediaria(s) en la ruta (+15 pts)",
                                  "Cuenta intermediaria BLOQUEADA: 2850590940090418135201 (+40 pts)"
                                ]
                              }
                            }
                            """
                )
            )
        ),
        @ApiResponse(responseCode = "404",
            description = "Cuenta no encontrada o no existe flujo directo de transferencias entre ellas")
    })
    public ResponseEntity<ShortestPathDto> shortestPath(
            @Parameter(description = "ID de la cuenta origen", required = true) @RequestParam String origen,
            @Parameter(description = "ID de la cuenta destino", required = true) @RequestParam String destino) {
        return ResponseEntity.ok(cuentaService.shortestPathDetallado(origen, destino));
    }

    @GetMapping("/cuentas/transferencias")
    @Operation(
        summary = "Rastreo de fondos entre dos cuentas (listado básico de IDs)",
        description = """
            Shortest path de transferencias (TRANSFIERE_A) entre dos cuentas.
            Permite rastrear cómo viajaron los fondos a través de intermediarios.
            Máximo 10 saltos. Devuelve los IDs de las cuentas en orden de recorrido.
            Para una respuesta enriquecida con nodos, aristas y riesgo estimado,
            usar GET /cuentas/shortest-path (formato Cytoscape.js).
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Flujo de transferencias encontrado",
            content = @Content(
                schema = @Schema(implementation = CaminoGrafoDto.class),
                examples = @ExampleObject(
                    name = "Fondos recorrieron 3 cuentas intermediarias",
                    value = """
                            {
                              "origenId": "cuenta-origen-uuid",
                              "destinoId": "cuenta-destino-uuid",
                              "saltos": 3,
                              "entidades": [
                                "cuenta-origen-uuid",
                                "cuenta-intermedia-1-uuid",
                                "cuenta-intermedia-2-uuid",
                                "cuenta-destino-uuid"
                              ]
                            }
                            """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada o no existe flujo de transferencias entre ellas")
    })
    public ResponseEntity<CaminoGrafoDto> rastreoTransferencias(
            @Parameter(description = "ID de la cuenta origen", required = true) @RequestParam String origen,
            @Parameter(description = "ID de la cuenta destino", required = true) @RequestParam String destino) {
        return ResponseEntity.ok(cuentaService.caminoDeTransferencias(origen, destino));
    }

    @GetMapping("/transacciones/camino")
    @Operation(
        summary = "Shortest path entre dos transacciones en la red RELACIONADA_CON",
        description = """
            Determina si dos transacciones sospechosas están conectadas en la red de fraude
            y a cuántos saltos de relación RELACIONADA_CON.
            Útil para fusionar investigaciones separadas o confirmar que pertenecen al mismo incidente.
            Máximo 10 saltos.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Camino en la red de fraude encontrado",
            content = @Content(
                schema = @Schema(implementation = CaminoGrafoDto.class),
                examples = @ExampleObject(
                    name = "Dos transacciones en el mismo anillo",
                    value = """
                            {
                              "origenId": "txn-uuid-1",
                              "destinoId": "txn-uuid-2",
                              "saltos": 2,
                              "entidades": [
                                "txn-uuid-1",
                                "txn-uuid-intermedia",
                                "txn-uuid-2"
                              ]
                            }
                            """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Transacción no encontrada o no existe camino entre ellas en la red RELACIONADA_CON")
    })
    public ResponseEntity<CaminoGrafoDto> caminoTransacciones(
            @Parameter(description = "ID de la primera transacción", required = true) @RequestParam String t1,
            @Parameter(description = "ID de la segunda transacción", required = true) @RequestParam String t2) {
        return ResponseEntity.ok(transaccionService.caminoMasCorto(t1, t2));
    }

    // ─── Detección de ciclos ───────────────────────────────────────────────────

    @GetMapping("/cuentas/{id}/ciclos")
    @Operation(
        summary = "Flujos circulares en una cuenta (layering)",
        description = """
            Detecta ciclos de transferencias que parten de la cuenta indicada y regresan a ella
            después de pasar por 2 a 6 intermediarios (TRANSFIERE_A*2..6).
            Es la fase de *layering* en un esquema clásico de lavado de activos:
            el dinero circula entre cuentas para dificultar el rastreo de su origen.
            Retorna lista vacía si no se detectan ciclos.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Ciclos detectados (lista vacía si no hay)",
            content = @Content(
                schema = @Schema(implementation = CicloCircularDto.class),
                examples = @ExampleObject(
                    name = "Ciclo de 3 cuentas",
                    value = """
                            [
                              {
                                "cuentasEnCiclo": [
                                  "cuenta-a-uuid",
                                  "cuenta-b-uuid",
                                  "cuenta-c-uuid",
                                  "cuenta-a-uuid"
                                ],
                                "saltos": 3
                              }
                            ]
                            """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<List<CicloCircularDto>> ciclosEnCuenta(
            @Parameter(description = "ID de la cuenta a analizar") @PathVariable String id,
            @Parameter(description = "Número máximo de ciclos a devolver", example = "10")
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(cuentaService.detectarFlujoCircular(id, limite));
    }
}
