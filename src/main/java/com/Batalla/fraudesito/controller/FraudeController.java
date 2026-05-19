package com.batalla.fraudesito.controller;

import com.batalla.fraudesito.dto.response.AnilloDeFraudeDto;
import com.batalla.fraudesito.dto.response.CicloDetalladoDto;
import com.batalla.fraudesito.dto.response.ComunidadSospechosaDto;
import com.batalla.fraudesito.dto.response.CuentaCompartidaDto;
import com.batalla.fraudesito.dto.response.CuentaMulaDto;
import com.batalla.fraudesito.dto.response.CuentaResponseDto;
import com.batalla.fraudesito.dto.response.DispositivoMultiCuentaDto;
import com.batalla.fraudesito.dto.response.DispositivoResponseDto;
import com.batalla.fraudesito.dto.response.DispositivoTakeoverDto;
import com.batalla.fraudesito.dto.response.EstadisticaTransaccionDto;
import com.batalla.fraudesito.dto.response.IdentidadSinteticaDto;
import com.batalla.fraudesito.dto.response.PersonaConectadaDto;
import com.batalla.fraudesito.dto.response.PersonaResponseDto;
import com.batalla.fraudesito.dto.response.RiesgoRelacionalDto;
import com.batalla.fraudesito.dto.response.TransaccionResponseDto;
import com.batalla.fraudesito.dto.response.VolumenCuentaDto;
import com.batalla.fraudesito.service.CuentaService;
import com.batalla.fraudesito.service.DispositivoService;
import com.batalla.fraudesito.service.FraudeService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fraude")
@RequiredArgsConstructor
@Tag(
    name = "Fraude",
    description = """
        Motor de detección de fraude financiero sobre el grafo Neo4j.
        Expone alertas, patrones AML y análisis avanzados que combinan
        personas, cuentas, transacciones y dispositivos.
        Los endpoints de velocity y smurfing están bajo /transacciones.
        Los de shortest path y ciclos de cuentas específicas están en /api/v1/grafo.
        """
)
public class FraudeController {

    private final FraudeService fraudeService;
    private final PersonaService personaService;
    private final CuentaService cuentaService;
    private final TransaccionService transaccionService;
    private final DispositivoService dispositivoService;

    // ─── Alertas de personas ───────────────────────────────────────────────────

    @GetMapping("/personas/peps")
    @Operation(
        summary = "PEPs registrados",
        description = "Lista personas políticamente expuestas (PEP). Sujetas a controles AML reforzados (FATF R.12)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de PEPs",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class)))
    })
    public ResponseEntity<List<PersonaResponseDto>> peps() {
        return ResponseEntity.ok(personaService.buscarPEPs());
    }

    @GetMapping("/personas/peps-alto-valor")
    @Operation(
        summary = "PEPs con transferencias de alto valor",
        description = """
            PEPs que reciben transferencias >= umbral.
            Norma FATF R.12: requieren Debida Diligencia Ampliada (EDD).
            Umbral expresado en la moneda base del sistema (ARS).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PEPs con movimientos de alto valor",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class)))
    })
    public ResponseEntity<List<PersonaResponseDto>> pepAltoValor(
            @Parameter(description = "Monto mínimo recibido en una sola transferencia (ARS)", example = "50000")
            @RequestParam(defaultValue = "50000") double umbral) {
        return ResponseEntity.ok(fraudeService.buscarPEPsConTransferenciasAltas(umbral));
    }

    @GetMapping("/personas/sancionados")
    @Operation(
        summary = "Personas sancionadas",
        description = "Personas en listas internacionales de sanciones (OFAC, ONU, UE)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de sancionados",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class)))
    })
    public ResponseEntity<List<PersonaResponseDto>> sancionados() {
        return ResponseEntity.ok(personaService.buscarSancionados());
    }

    @GetMapping("/personas/sancionados-activos")
    @Operation(
        summary = "Sancionados con cuentas activas",
        description = "Sancionados internacionales que aún operan cuentas ACTIVAS — incumplimiento regulatorio crítico."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sancionados con actividad financiera activa",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class)))
    })
    public ResponseEntity<List<PersonaResponseDto>> sancionadosActivos() {
        return ResponseEntity.ok(fraudeService.buscarSancionadosConCuentasActivas());
    }

    @GetMapping("/personas/cuentas-bloqueadas")
    @Operation(
        summary = "Personas con cuentas bloqueadas",
        description = "Personas titulares de al menos una cuenta en estado BLOQUEADA."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Personas con cuentas bloqueadas",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class)))
    })
    public ResponseEntity<List<PersonaResponseDto>> cuentasBloqueadas() {
        return ResponseEntity.ok(personaService.buscarConCuentasBloqueadas());
    }

    @GetMapping("/personas/dispositivos-sospechosos")
    @Operation(
        summary = "Personas con dispositivos de alto riesgo",
        description = "Personas que usan emuladores, dispositivos rooteados o conexiones Tor."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de personas con dispositivos sospechosos",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class)))
    })
    public ResponseEntity<List<PersonaResponseDto>> personasDispositivosSospechosos() {
        return ResponseEntity.ok(personaService.buscarConDispositivosSospechosos());
    }

    @GetMapping("/personas/riesgo")
    @Operation(
        summary = "Personas por nivel de riesgo mínimo",
        description = "Filtra personas cuyo score de riesgo (0–100) alcanza o supera el umbral indicado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Personas con riesgo elevado",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class)))
    })
    public ResponseEntity<List<PersonaResponseDto>> porNivelRiesgo(
            @Parameter(description = "Score mínimo de riesgo (0–100)", example = "50")
            @RequestParam(defaultValue = "50") Integer umbral) {
        return ResponseEntity.ok(personaService.buscarConNivelRiesgoMinimo(umbral));
    }

    @GetMapping("/personas/{id}/dispositivo-compartido")
    @Operation(
        summary = "Personas que comparten dispositivo",
        description = """
            Otras personas que usan al menos un dispositivo en común con la persona indicada.
            Señal de account sharing o de que un mismo actor controla múltiples identidades.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Personas vinculadas por dispositivo compartido",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Persona no encontrada")
    })
    public ResponseEntity<List<PersonaResponseDto>> dispositivoCompartido(
            @Parameter(description = "ID de la persona a analizar") @PathVariable String id) {
        return ResponseEntity.ok(personaService.buscarPersonasQueCompartenDispositivo(id));
    }

    @GetMapping("/personas/hubs-relacionales")
    @Operation(
        summary = "Ranking de hubs relacionales",
        description = """
            Personas ordenadas por cantidad de contrapartes financieras únicas.
            Los nodos con muchas conexiones son los hubs más probables en redes de fraude organizado.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ranking de personas por riesgo relacional",
            content = @Content(schema = @Schema(implementation = RiesgoRelacionalDto.class)))
    })
    public ResponseEntity<List<RiesgoRelacionalDto>> hubsRelacionales(
            @Parameter(description = "Cantidad máxima de resultados", example = "10")
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(fraudeService.rankingHubsRelacionales(limite));
    }

    @GetMapping("/personas/comunidades")
    @Operation(
        summary = "Comunidades financieras sospechosas",
        description = """
            Detecta clusters de personas fuertemente interconectadas por transferencias directas.
            Cada comunidad se representa por su hub (persona con más contrapartes únicas) y la
            lista de miembros conectados a él.
            Técnica: grado de salida en el grafo de transferencias (ego-network).
            Para community detection avanzada (Louvain, WCC), se requiere Neo4j Graph Data Science.
            minConexiones: umbral mínimo de contrapartes distintas para considerar un hub.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Comunidades sospechosas detectadas",
            content = @Content(
                schema = @Schema(implementation = ComunidadSospechosaDto.class),
                examples = @ExampleObject(
                    name = "Hub con 5 miembros",
                    value = """
                            [
                              {
                                "centroId": "uuid-persona-hub",
                                "nombre": "Carlos",
                                "apellido": "Méndez",
                                "nivelRiesgo": 75,
                                "totalConexiones": 5,
                                "miembrosIds": [
                                  "uuid-p1", "uuid-p2", "uuid-p3", "uuid-p4", "uuid-p5"
                                ]
                              }
                            ]
                            """
                )
            )
        )
    })
    public ResponseEntity<List<ComunidadSospechosaDto>> comunidades(
            @Parameter(description = "Mínimo de contrapartes financieras únicas para ser hub", example = "3")
            @RequestParam(defaultValue = "3") int minConexiones,
            @Parameter(description = "Cantidad máxima de comunidades a devolver", example = "10")
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(fraudeService.detectarComunidadesSospechosas(minConexiones, limite));
    }

    @GetMapping("/personas/{id}/conexiones-indirectas")
    @Operation(
        summary = "Red de conexiones financieras indirectas",
        description = """
            Expande la red de transferencias 2 a 4 saltos desde la persona indicada.
            Devuelve todas las personas alcanzables a través de cuentas intermediarias,
            ordenadas por nivelRiesgo descendente (primero los más peligrosos).
            Uso: identificar beneficiarios finales ocultos, mapear el entorno de un sospechoso.
            Diferencia con /grafo/personas/camino: este endpoint retorna el vecindario completo,
            no el camino mínimo a un destino específico.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Personas en el vecindario financiero indirecto",
            content = @Content(
                schema = @Schema(implementation = PersonaConectadaDto.class),
                examples = @ExampleObject(
                    name = "Vecindario de 3 saltos",
                    value = """
                            [
                              {
                                "personaId": "uuid-destino-1",
                                "nombre": "María",
                                "apellido": "González",
                                "nivelRiesgo": 85,
                                "esPEP": false,
                                "esSancionado": true
                              },
                              {
                                "personaId": "uuid-destino-2",
                                "nombre": "Roberto",
                                "apellido": "Suárez",
                                "nivelRiesgo": 40,
                                "esPEP": false,
                                "esSancionado": false
                              }
                            ]
                            """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Persona no encontrada")
    })
    public ResponseEntity<List<PersonaConectadaDto>> conexionesIndirectas(
            @Parameter(description = "ID de la persona de inicio") @PathVariable String id,
            @Parameter(description = "Cantidad máxima de personas a devolver", example = "50")
            @RequestParam(defaultValue = "50") int limite) {
        return ResponseEntity.ok(fraudeService.explorarConexionesIndirectas(id, limite));
    }

    @GetMapping("/personas/identidad-sintetica")
    @Operation(
        summary = "Indicadores de identidad sintética",
        description = """
            Detecta personas que comparten dispositivos con otras, señal principal de identidad sintética.
            La identidad sintética combina datos reales e inventados para crear personas ficticias
            que pasan controles KYC básicos pero están controladas por el mismo actor fraudulento.
            Señales evaluadas:
            - personasVinculadas: otras identidades que usan el mismo dispositivo
            - dispositivosCompartidos: cantidad de dispositivos en común con otras personas
            - verificada: false en identidades sintéticas (evitan la verificación manual)
            - totalCuentas: alta cantidad de cuentas para identidades recientes
            minPersonasVinculadas: umbral mínimo de identidades vinculadas por dispositivo compartido.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Personas con indicadores de identidad sintética",
            content = @Content(
                schema = @Schema(implementation = IdentidadSinteticaDto.class),
                examples = @ExampleObject(
                    name = "Identidad sintética detectada",
                    value = """
                            [
                              {
                                "personaId": "uuid-persona",
                                "nombre": "Juan",
                                "apellido": "Perez",
                                "documento": "28456123",
                                "nivelRiesgo": 90,
                                "verificada": false,
                                "personasVinculadas": 4,
                                "dispositivosCompartidos": 2,
                                "totalCuentas": 5,
                                "personasRelacionadasIds": [
                                  "uuid-p1", "uuid-p2", "uuid-p3", "uuid-p4"
                                ]
                              }
                            ]
                            """
                )
            )
        )
    })
    public ResponseEntity<List<IdentidadSinteticaDto>> identidadSintetica(
            @Parameter(description = "Mínimo de personas vinculadas por dispositivo compartido", example = "1")
            @RequestParam(defaultValue = "1") int minPersonasVinculadas,
            @Parameter(description = "Cantidad máxima de resultados", example = "20")
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(fraudeService.detectarIdentidadSintetica(minPersonasVinculadas, limite));
    }

    // ─── Alertas de cuentas ────────────────────────────────────────────────────

    @GetMapping("/cuentas/puente")
    @Operation(
        summary = "Cuentas puente (mule accounts — básico)",
        description = """
            Cuentas que tanto reciben como envían transferencias.
            Son candidatas a ser cuentas mula usadas como intermediarias para dispersar fondos ilícitos.
            Ordenadas por actividad total descendente. Ver /cuentas/mula para métricas de volumen.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Top cuentas puente",
            content = @Content(schema = @Schema(implementation = CuentaResponseDto.class)))
    })
    public ResponseEntity<List<CuentaResponseDto>> cuentasPuente(
            @Parameter(description = "Cantidad máxima de resultados", example = "20")
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(cuentaService.buscarCuentasPuente(limite));
    }

    @GetMapping("/cuentas/mula")
    @Operation(
        summary = "Cuentas mula con métricas de volumen",
        description = """
            Cuentas que reciben y reenvían fondos, con volúmenes monetarios detallados.
            La tasa de dispersión (volumenEnviado / volumenRecibido) es el KPI central:
            una cuenta mula típica reenvía el 90–100% de lo que recibe.
            Diferencia con /cuentas/puente: incluye volumenRecibido y volumenEnviado.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Cuentas mula con actividad detallada",
            content = @Content(
                schema = @Schema(implementation = CuentaMulaDto.class),
                examples = @ExampleObject(
                    name = "Cuenta mula con alto volumen",
                    value = """
                            [
                              {
                                "cuentaId": "uuid-cuenta",
                                "numeroCuenta": "0720034888000078901234",
                                "banco": "Banco Nación",
                                "tipoCuenta": "AHORRO",
                                "totalRecibidas": 15,
                                "totalEnviadas": 14,
                                "actividadTotal": 29,
                                "volumenRecibido": 450000.00,
                                "volumenEnviado": 447500.00
                              }
                            ]
                            """
                )
            )
        )
    })
    public ResponseEntity<List<CuentaMulaDto>> cuentasMula(
            @Parameter(description = "Cantidad máxima de resultados", example = "20")
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(fraudeService.detectarCuentasMula(limite));
    }

    @GetMapping("/cuentas/alertadas")
    @Operation(
        summary = "Cuentas con transacciones alertadas",
        description = "Cuentas involucradas (como origen o destino) en al menos una transacción marcada como alertada."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuentas con historial de alertas",
            content = @Content(schema = @Schema(implementation = CuentaResponseDto.class)))
    })
    public ResponseEntity<List<CuentaResponseDto>> cuentasAlertadas() {
        return ResponseEntity.ok(cuentaService.buscarConTransaccionesAlertadas());
    }

    @GetMapping("/cuentas/acceso-sospechoso")
    @Operation(
        summary = "Cuentas accedidas desde dispositivos de alto riesgo",
        description = "Cuentas operadas desde emuladores, dispositivos rooteados o conexiones Tor."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuentas con acceso desde dispositivos sospechosos",
            content = @Content(schema = @Schema(implementation = CuentaResponseDto.class)))
    })
    public ResponseEntity<List<CuentaResponseDto>> cuentasAccesoSospechoso() {
        return ResponseEntity.ok(cuentaService.buscarConAccesoDesdeDispositivosSospechosos());
    }

    @GetMapping("/cuentas/compartidas")
    @Operation(
        summary = "Cuentas compartidas en dispositivo (account sharing)",
        description = """
            Pares de cuentas con distintos titulares que fueron operadas desde el mismo dispositivo.
            Es el indicador principal de account takeover o control de múltiples identidades.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pares de cuentas compartidas con su dispositivo común",
            content = @Content(schema = @Schema(implementation = CuentaCompartidaDto.class)))
    })
    public ResponseEntity<List<CuentaCompartidaDto>> cuentasCompartidas() {
        return ResponseEntity.ok(fraudeService.detectarCuentasCompartidasEnDispositivo());
    }

    @GetMapping("/cuentas/ciclos-globales")
    @Operation(
        summary = "Ciclos de transferencias globales (layering)",
        description = """
            Escanea todo el grafo en busca de ciclos A→B→...→A de 2 a 6 saltos (layering).
            El layering es la segunda fase del lavado de activos: el dinero circula entre cuentas
            para dificultar el rastreo de su origen ilícito.
            Cada ciclo se reporta una sola vez (deduplicado por ID mínimo del nodo ancla).
            Para ciclos anclados a una cuenta específica, usar GET /api/v1/grafo/cuentas/{id}/ciclos.
            ADVERTENCIA: operación costosa en grafos grandes. Mantener limite <= 50 en producción.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Ciclos de layering detectados en el grafo completo",
            content = @Content(
                schema = @Schema(implementation = CicloDetalladoDto.class),
                examples = @ExampleObject(
                    name = "Ciclo triangular A→B→C→A",
                    value = """
                            [
                              {
                                "anclaId": "uuid-cuenta-a",
                                "cuentasEnCiclo": [
                                  "uuid-cuenta-a",
                                  "uuid-cuenta-b",
                                  "uuid-cuenta-c"
                                ],
                                "numerosCuenta": [
                                  "0720034888000078901234",
                                  "2850590940090418135201",
                                  "0720034000000078904321"
                                ],
                                "saltos": 3
                              }
                            ]
                            """
                )
            )
        )
    })
    public ResponseEntity<List<CicloDetalladoDto>> ciclosGlobales(
            @Parameter(description = "Cantidad máxima de ciclos a devolver (recomendado <= 50)", example = "20")
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(fraudeService.detectarCiclosGlobales(limite));
    }

    // ─── Alertas de transacciones ──────────────────────────────────────────────

    @GetMapping("/transacciones/alertadas")
    @Operation(
        summary = "Transacciones alertadas",
        description = "Todas las transacciones cuyo scoring automático superó el umbral de alerta (nivelRiesgo >= 40)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transacciones marcadas como alertadas",
            content = @Content(schema = @Schema(implementation = TransaccionResponseDto.class)))
    })
    public ResponseEntity<List<TransaccionResponseDto>> transaccionesAlertadas() {
        return ResponseEntity.ok(transaccionService.buscarAlertadas());
    }

    @GetMapping("/transacciones/velocity/{cuentaId}")
    @Operation(
        summary = "Velocity fraud — ráfaga de envíos",
        description = """
            Detecta transacciones enviadas por una cuenta dentro de una ventana de tiempo corta.
            Retorna la lista de transacciones si la cantidad supera el umbral; lista vacía si no hay patrón.
            Solo devuelve resultados si el total de transacciones en la ventana >= umbral.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transacciones en la ventana temporal",
            content = @Content(schema = @Schema(implementation = TransaccionResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<List<TransaccionResponseDto>> velocity(
            @Parameter(description = "ID de la cuenta origen a analizar") @PathVariable String cuentaId,
            @Parameter(description = "Ventana de análisis en minutos", example = "60")
            @RequestParam(defaultValue = "60") int ventanaMinutos,
            @Parameter(description = "Cantidad mínima de transacciones para activar alerta", example = "5")
            @RequestParam(defaultValue = "5") int umbral) {
        return ResponseEntity.ok(transaccionService.detectarVelocity(cuentaId, ventanaMinutos, umbral));
    }

    @GetMapping("/transacciones/smurfing/{cuentaDestinoId}")
    @Operation(
        summary = "Smurfing — fragmentación de montos",
        description = """
            Detecta múltiples transferencias de bajo monto hacia la misma cuenta destino en las últimas 24 horas.
            El smurfing busca evadir los umbrales de reporte obligatorio (UIF/GAFI).
            Solo devuelve resultados si la cantidad de transacciones >= cantidadMinima.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transacciones que conforman el patrón de smurfing",
            content = @Content(schema = @Schema(implementation = TransaccionResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Cuenta destino no encontrada")
    })
    public ResponseEntity<List<TransaccionResponseDto>> smurfing(
            @Parameter(description = "ID de la cuenta destino a analizar") @PathVariable String cuentaDestinoId,
            @Parameter(description = "Monto máximo por transacción (ARS)", example = "10000")
            @RequestParam(defaultValue = "10000") String montoMaximo,
            @Parameter(description = "Cantidad mínima de transacciones para confirmar el patrón", example = "3")
            @RequestParam(defaultValue = "3") int cantidadMinima) {
        return ResponseEntity.ok(transaccionService.detectarSmurfing(cuentaDestinoId, montoMaximo, cantidadMinima));
    }

    @GetMapping("/transacciones/{id}/anillo")
    @Operation(
        summary = "Anillo de fraude — expansión RELACIONADA_CON",
        description = """
            Expande la red RELACIONADA_CON a partir de una transacción, hasta 4 saltos.
            Devuelve el conjunto de transacciones relacionadas ordenadas por nivelRiesgo descendente.
            Útil para mapear el alcance de un incidente de fraude.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transacciones en el anillo de fraude",
            content = @Content(schema = @Schema(implementation = AnilloDeFraudeDto.class))),
        @ApiResponse(responseCode = "404", description = "Transacción no encontrada")
    })
    public ResponseEntity<List<AnilloDeFraudeDto>> anillo(
            @Parameter(description = "ID de la transacción de entrada al anillo") @PathVariable String id) {
        return ResponseEntity.ok(transaccionService.anilloDeFraude(id));
    }

    @GetMapping("/transacciones/distribucion-estado")
    @Operation(
        summary = "Distribución de transacciones por estado",
        description = "Conteo de transacciones agrupado por EstadoTransaccion. Útil para dashboards de monitoreo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distribución por estado",
            content = @Content(schema = @Schema(implementation = EstadisticaTransaccionDto.class)))
    })
    public ResponseEntity<List<EstadisticaTransaccionDto>> distribucionEstado() {
        return ResponseEntity.ok(fraudeService.distribucionPorEstado());
    }

    @GetMapping("/transacciones/top-volumen")
    @Operation(
        summary = "Top cuentas por volumen enviado",
        description = """
            Cuentas con mayor volumen (suma de montos) de transferencias enviadas en un período.
            Un salto súbito en el volumen de una cuenta es señal de dispersión masiva de fondos.
            Fechas en formato ISO-8601: 2024-01-01T00:00:00
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ranking de cuentas por volumen",
            content = @Content(schema = @Schema(implementation = VolumenCuentaDto.class)))
    })
    public ResponseEntity<List<VolumenCuentaDto>> topVolumen(
            @Parameter(description = "Inicio del período (ISO-8601)", example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @Parameter(description = "Fin del período (ISO-8601)", example = "2024-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @Parameter(description = "Cantidad máxima de resultados", example = "10")
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(fraudeService.topCuentasPorVolumen(desde, hasta, limite));
    }

    // ─── Alertas de dispositivos ───────────────────────────────────────────────

    @GetMapping("/dispositivos/alto-riesgo")
    @Operation(
        summary = "Dispositivos de alto riesgo",
        description = "Emuladores, dispositivos rooteados, conexiones Tor, VPN o marcados como sospechosos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dispositivos con indicadores de riesgo",
            content = @Content(schema = @Schema(implementation = DispositivoResponseDto.class)))
    })
    public ResponseEntity<List<DispositivoResponseDto>> dispositivosAltoRiesgo() {
        return ResponseEntity.ok(dispositivoService.buscarDeAltoRiesgo());
    }

    @GetMapping("/dispositivos/account-takeover")
    @Operation(
        summary = "Account takeover — dispositivos con múltiples titulares (via USADA_EN)",
        description = """
            Dispositivos vinculados a cuentas de más de N titulares distintos.
            Es el indicador más directo de account takeover: el atacante controla el dispositivo
            y lo usa para operar cuentas de distintas personas.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dispositivos con posible account takeover",
            content = @Content(schema = @Schema(implementation = DispositivoTakeoverDto.class)))
    })
    public ResponseEntity<List<DispositivoTakeoverDto>> accountTakeover(
            @Parameter(description = "Umbral mínimo de titulares distintos por dispositivo", example = "1")
            @RequestParam(defaultValue = "1") int limite) {
        return ResponseEntity.ok(fraudeService.detectarAccountTakeover(limite));
    }

    @GetMapping("/dispositivos/multi-cuenta")
    @Operation(
        summary = "Dispositivos con múltiples cuentas y titulares (ecosistema completo)",
        description = """
            Vista enriquecida de dispositivos que concentran cuentas de múltiples titulares.
            A diferencia de /dispositivos/account-takeover (que usa USADA_EN), este endpoint
            une las rutas USADA_EN y POSEE_CUENTA para retornar el ecosistema completo:
            todas las cuentas operadas, todos los titulares, e indicadores de riesgo del dispositivo.
            Ideal para investigar la red completa controlada desde un único dispositivo.
            minTitulares: mínimo de titulares distintos para incluir el dispositivo.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Dispositivos con ecosistema multi-titular completo",
            content = @Content(
                schema = @Schema(implementation = DispositivoMultiCuentaDto.class),
                examples = @ExampleObject(
                    name = "Dispositivo controlando 3 titulares y 5 cuentas",
                    value = """
                            [
                              {
                                "dispositivoId": "uuid-dispositivo",
                                "fingerprint": "a3f9b2c1d8e7f654a321b0c9d8e7f6a5",
                                "ipAddress": "10.0.0.1",
                                "esEmulador": true,
                                "ipEsTor": false,
                                "ipEsVPN": true,
                                "totalTitulares": 3,
                                "totalCuentas": 5,
                                "cuentaIds": [
                                  "uuid-c1", "uuid-c2", "uuid-c3", "uuid-c4", "uuid-c5"
                                ],
                                "titularIds": [
                                  "uuid-p1", "uuid-p2", "uuid-p3"
                                ]
                              }
                            ]
                            """
                )
            )
        )
    })
    public ResponseEntity<List<DispositivoMultiCuentaDto>> multiCuenta(
            @Parameter(description = "Mínimo de titulares distintos por dispositivo", example = "2")
            @RequestParam(defaultValue = "2") int minTitulares,
            @Parameter(description = "Cantidad máxima de resultados", example = "20")
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(fraudeService.dispositivosConMultiplesCuentas(minTitulares, limite));
    }

    @GetMapping("/dispositivos/compartidos")
    @Operation(
        summary = "Dispositivos compartidos por múltiples personas (via USA_DISPOSITIVO)",
        description = """
            Dispositivos usados por más de N personas distintas (USA_DISPOSITIVO).
            Puede indicar synthetic identity fraud: un mismo actor controla varias identidades.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dispositivos compartidos",
            content = @Content(schema = @Schema(implementation = DispositivoResponseDto.class)))
    })
    public ResponseEntity<List<DispositivoResponseDto>> dispositivosCompartidos(
            @Parameter(description = "Umbral mínimo de personas por dispositivo", example = "1")
            @RequestParam(defaultValue = "1") int limite) {
        return ResponseEntity.ok(dispositivoService.buscarCompartidosPorMultiplesPersonas(limite));
    }

    @GetMapping("/dispositivos/comunes")
    @Operation(
        summary = "Dispositivos en común entre dos personas",
        description = "Demuestra vinculación directa entre dos personas investigadas a través de un dispositivo compartido."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dispositivos compartidos entre las dos personas",
            content = @Content(schema = @Schema(implementation = DispositivoResponseDto.class)))
    })
    public ResponseEntity<List<DispositivoResponseDto>> dispositivosComunes(
            @Parameter(description = "ID de la primera persona") @RequestParam String p1,
            @Parameter(description = "ID de la segunda persona") @RequestParam String p2) {
        return ResponseEntity.ok(dispositivoService.buscarComunesEntrePersonas(p1, p2));
    }
}
