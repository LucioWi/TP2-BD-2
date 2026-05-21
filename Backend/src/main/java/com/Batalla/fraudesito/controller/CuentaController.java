package com.batalla.fraudesito.controller;

import com.batalla.fraudesito.dto.request.CuentaRequestDto;
import com.batalla.fraudesito.dto.response.CuentaResponseDto;
import com.batalla.fraudesito.service.CuentaService;
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
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas", description = "Gestión de cuentas bancarias y billeteras virtuales en el grafo de fraude")
public class CuentaController {

    private final CuentaService cuentaService;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Crear cuenta",
        description = "Registra una nueva cuenta. numeroCuenta y cbvu son únicos en el sistema.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                    @ExampleObject(
                        name = "Caja de ahorro ARS",
                        value = """
                                {
                                  "numeroCuenta": "0720034888000078901234",
                                  "cbvu": "0720034888000078901234",
                                  "alias": "PATO.VERDE.SOL",
                                  "banco": "Banco Nación",
                                  "tipoCuenta": "AHORRO",
                                  "saldo": "15000.00",
                                  "moneda": "ARS",
                                  "limiteTransferenciaDiaria": "100000.00"
                                }
                                """
                    ),
                    @ExampleObject(
                        name = "Billetera virtual USD",
                        value = """
                                {
                                  "numeroCuenta": "2850590940090418135201",
                                  "cbvu": "2850590940090418135201",
                                  "alias": "MI.WALLET.USD",
                                  "banco": "Mercado Pago",
                                  "tipoCuenta": "BILLETERA_VIRTUAL",
                                  "saldo": "500.00",
                                  "moneda": "USD",
                                  "limiteTransferenciaDiaria": "1000.00"
                                }
                                """
                    )
                }
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cuenta creada exitosamente",
            content = @Content(schema = @Schema(implementation = CuentaResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "numeroCuenta o cbvu ya registrado en el sistema")
    })
    public ResponseEntity<CuentaResponseDto> crear(@Valid @RequestBody CuentaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.crear(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cuenta por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuenta encontrada",
            content = @Content(schema = @Schema(implementation = CuentaResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<CuentaResponseDto> buscarPorId(
            @Parameter(description = "UUID de la cuenta") @PathVariable String id) {
        return ResponseEntity.ok(cuentaService.buscarPorId(id));
    }

    @GetMapping("/numero/{numeroCuenta}")
    @Operation(summary = "Buscar cuenta por número de cuenta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuenta encontrada",
            content = @Content(schema = @Schema(implementation = CuentaResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada con ese número")
    })
    public ResponseEntity<CuentaResponseDto> buscarPorNumero(
            @Parameter(description = "Número de cuenta (10 a 22 dígitos)", example = "0720034888000078901234")
            @PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.buscarPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping
    @Operation(summary = "Listar todas las cuentas")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de cuentas (puede ser vacía)")
    })
    public ResponseEntity<List<CuentaResponseDto>> listarTodas() {
        return ResponseEntity.ok(cuentaService.listarTodas());
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar cuenta",
        description = "Actualiza los datos de una cuenta. Valida unicidad de numeroCuenta y cbvu al cambiarlos.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Actualización de límite y alias",
                    value = """
                            {
                              "numeroCuenta": "0720034888000078901234",
                              "alias": "AGUILA.RIO.LUNA",
                              "banco": "Banco Nación",
                              "tipoCuenta": "CAJA_AHORRO",
                              "saldo": "15000.00",
                              "moneda": "ARS",
                              "limiteTransferenciaDiaria": "50000.00"
                            }
                            """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuenta actualizada",
            content = @Content(schema = @Schema(implementation = CuentaResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada"),
        @ApiResponse(responseCode = "409", description = "El nuevo numeroCuenta o cbvu ya pertenece a otra cuenta")
    })
    public ResponseEntity<CuentaResponseDto> actualizar(
            @Parameter(description = "UUID de la cuenta a actualizar") @PathVariable String id,
            @Valid @RequestBody CuentaRequestDto dto) {
        return ResponseEntity.ok(cuentaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cuenta", description = "Elimina la cuenta y todas sus relaciones del grafo.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cuenta eliminada"),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "UUID de la cuenta a eliminar") @PathVariable String id) {
        cuentaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Búsquedas ─────────────────────────────────────────────────────────────

    @GetMapping("/buscar")
    @Operation(
        summary = "Buscar cuenta por alias o número",
        description = "Busca una cuenta cuyo alias o numeroCuenta coincida exactamente con el parámetro 'q'."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuenta encontrada",
            content = @Content(schema = @Schema(implementation = CuentaResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Cuenta no encontrada con ese criterio")
    })
    public ResponseEntity<CuentaResponseDto> buscarPorAliasONumeroCuenta(
            @Parameter(description = "Alias o número de cuenta a buscar", example = "PATO.VERDE.SOL")
            @RequestParam("q") String q) {
        return ResponseEntity.ok(cuentaService.buscarPorAliasONumeroCuenta(q));
    }

    @GetMapping("/persona/{personaId}")
    @Operation(
        summary = "Cuentas de una persona",
        description = "Devuelve todas las cuentas sobre las que la persona tiene relación POSEE_CUENTA."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuentas de la persona (puede ser vacía)")
    })
    public ResponseEntity<List<CuentaResponseDto>> buscarPorPersona(
            @Parameter(description = "UUID de la persona") @PathVariable String personaId) {
        return ResponseEntity.ok(cuentaService.buscarPorPersona(personaId));
    }
}
