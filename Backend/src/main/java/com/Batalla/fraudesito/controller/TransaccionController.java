package com.batalla.fraudesito.controller;

import com.batalla.fraudesito.domain.enums.TipoRelacion;
import com.batalla.fraudesito.dto.request.TransaccionRequestDto;
import com.batalla.fraudesito.dto.response.TransaccionResponseDto;
import com.batalla.fraudesito.service.TransaccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transacciones")
@RequiredArgsConstructor
@Tag(
    name = "Transacciones",
    description = """
        Gestión de operaciones financieras en el grafo de fraude.
        Al registrar una transacción, el sistema aplica un scoring automático basado en
        monto, canal y presencia de IP. Si nivelRiesgo >= 40 la transacción queda alertada.
        """
)
public class TransaccionController {

    private final TransaccionService transaccionService;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Registrar transacción",
        description = """
            Registra una nueva operación financiera.
            El sistema calcula automáticamente nivelRiesgo y, si >= 40, marca esAlertada = true.
            Reglas de scoring:
            - monto >= 100.000 ARS → +30 pts
            - monto >= 10.000 ARS → +15 pts
            - canal API o TRANSFERENCIA_AUTOMATICA → +10 pts
            - ipAddress ausente → +5 pts
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                    @ExampleObject(
                        name = "Transferencia normal (bajo riesgo)",
                        value = """
                                {
                                  "monto": "5000.00",
                                  "moneda": "ARS",
                                  "tipo": "TRANSFERENCIA",
                                  "canal": "HOME_BANKING",
                                  "cuentaOrigenId": "uuid-cuenta-origen",
                                  "cuentaDestinoId": "uuid-cuenta-destino",
                                  "descripcion": "Pago alquiler",
                                  "ipAddress": "192.168.1.100",
                                  "latitud": -34.6037,
                                  "longitud": -58.3816
                                }
                                """
                    ),
                    @ExampleObject(
                        name = "Transferencia de alto monto (alerta automática)",
                        value = """
                                {
                                  "monto": "150000.00",
                                  "moneda": "ARS",
                                  "tipo": "TRANSFERENCIA",
                                  "canal": "API",
                                  "cuentaOrigenId": "uuid-cuenta-origen",
                                  "cuentaDestinoId": "uuid-cuenta-destino",
                                  "descripcion": "Liquidación",
                                  "ipAddress": null
                                }
                                """
                    )
                }
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Transacción registrada. Revisar esAlertada y nivelRiesgo en la respuesta.",
            content = @Content(schema = @Schema(implementation = TransaccionResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Cuenta origen o destino no encontrada")
    })
    public ResponseEntity<TransaccionResponseDto> crear(@Valid @RequestBody TransaccionRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transaccionService.crear(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar transacción por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transacción encontrada",
            content = @Content(schema = @Schema(implementation = TransaccionResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Transacción no encontrada")
    })
    public ResponseEntity<TransaccionResponseDto> buscarPorId(
            @Parameter(description = "UUID de la transacción") @PathVariable String id) {
        return ResponseEntity.ok(transaccionService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las transacciones")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de transacciones (puede ser vacía)")
    })
    public ResponseEntity<List<TransaccionResponseDto>> listarTodas() {
        return ResponseEntity.ok(transaccionService.listarTodas());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar transacción")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transacción eliminada"),
        @ApiResponse(responseCode = "404", description = "Transacción no encontrada")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "UUID de la transacción") @PathVariable String id) {
        transaccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Búsquedas ─────────────────────────────────────────────────────────────

    @GetMapping("/cuenta-origen/{cuentaOrigenId}")
    @Operation(
        summary = "Transacciones por cuenta de origen",
        description = "Lista las transacciones enviadas por una cuenta, ordenadas por fecha descendente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transacciones de la cuenta (puede ser vacía)")
    })
    public ResponseEntity<List<TransaccionResponseDto>> porCuentaOrigen(
            @Parameter(description = "UUID de la cuenta origen") @PathVariable String cuentaOrigenId) {
        return ResponseEntity.ok(transaccionService.buscarPorCuentaOrigen(cuentaOrigenId));
    }

    @GetMapping("/cuenta-destino/{cuentaDestinoId}")
    @Operation(
        summary = "Transacciones por cuenta de destino",
        description = "Lista las transacciones recibidas por una cuenta, ordenadas por fecha descendente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transacciones recibidas por la cuenta (puede ser vacía)")
    })
    public ResponseEntity<List<TransaccionResponseDto>> porCuentaDestino(
            @Parameter(description = "UUID de la cuenta destino") @PathVariable String cuentaDestinoId) {
        return ResponseEntity.ok(transaccionService.buscarPorCuentaDestino(cuentaDestinoId));
    }

    // ─── Relaciones ────────────────────────────────────────────────────────────

    @PostMapping("/{id1}/relacionar/{id2}")
    @Operation(
        summary = "Relacionar dos transacciones",
        description = """
            Crea la relación RELACIONADA_CON entre dos transacciones.
            TipoRelacion disponibles: MISMO_DISPOSITIVO, MISMA_IP, MONTO_SIMILAR, PATRON_TEMPORAL, ANILLO_FRAUDE, CUENTA_PUENTE.
            puntajeSimilitud: valor entre 0.0 y 1.0 que expresa la similitud detectada.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relación creada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Una o ambas transacciones no encontradas")
    })
    public ResponseEntity<Void> relacionar(
            @Parameter(description = "UUID de la primera transacción") @PathVariable String id1,
            @Parameter(description = "UUID de la segunda transacción") @PathVariable String id2,
            @Parameter(description = "Tipo de relación de fraude", required = true) @RequestParam TipoRelacion tipoRelacion,
            @Parameter(description = "Score de similitud entre 0.0 y 1.0", example = "0.85")
            @RequestParam(defaultValue = "1.0") Double puntajeSimilitud) {
        transaccionService.relacionarTransacciones(id1, id2, tipoRelacion, puntajeSimilitud);
        return ResponseEntity.ok().build();
    }
}
