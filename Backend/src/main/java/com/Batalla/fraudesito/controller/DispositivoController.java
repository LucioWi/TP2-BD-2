package com.batalla.fraudesito.controller;

import com.batalla.fraudesito.dto.request.DispositivoRequestDto;
import com.batalla.fraudesito.dto.response.DispositivoResponseDto;
import com.batalla.fraudesito.service.DispositivoService;
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
@RequestMapping("/api/v1/dispositivos")
@RequiredArgsConstructor
@Tag(
    name = "Dispositivos",
    description = """
        Gestión de dispositivos físicos y virtuales desde los que se accede al sistema.
        Son el vector principal para detectar account takeover y synthetic identity fraud.
        Vinculados a personas via USA_DISPOSITIVO y a cuentas via USADA_EN.
        """
)
public class DispositivoController {

    private final DispositivoService dispositivoService;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Registrar dispositivo",
        description = "Registra un nuevo dispositivo. El fingerprint es único en el sistema (si se provee).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = {
                    @ExampleObject(
                        name = "Smartphone Android",
                        value = """
                                {
                                  "ipAddress": "200.49.130.10",
                                  "userAgent": "Mozilla/5.0 (Android 13; Mobile) Chrome/118.0",
                                  "tipoDispositivo": "MOVIL",
                                  "sistemaOperativo": "Android 13",
                                  "fingerprint": "a3f9b2c1d8e7f654a321b0c9d8e7f6a5"
                                }
                                """
                    ),
                    @ExampleObject(
                        name = "Dispositivo sospechoso (Tor + emulador)",
                        value = """
                                {
                                  "ipAddress": "10.0.0.1",
                                  "userAgent": "okhttp/4.9.1",
                                  "tipoDispositivo": "OTRO",
                                  "sistemaOperativo": "Android 11",
                                  "fingerprint": null
                                }
                                """
                    )
                }
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dispositivo registrado exitosamente",
            content = @Content(schema = @Schema(implementation = DispositivoResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (IP inválida, etc.)"),
        @ApiResponse(responseCode = "409", description = "Fingerprint ya registrado en el sistema")
    })
    public ResponseEntity<DispositivoResponseDto> crear(@Valid @RequestBody DispositivoRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dispositivoService.crear(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar dispositivo por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dispositivo encontrado",
            content = @Content(schema = @Schema(implementation = DispositivoResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Dispositivo no encontrado")
    })
    public ResponseEntity<DispositivoResponseDto> buscarPorId(
            @Parameter(description = "UUID del dispositivo") @PathVariable String id) {
        return ResponseEntity.ok(dispositivoService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los dispositivos")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de dispositivos (puede ser vacía)")
    })
    public ResponseEntity<List<DispositivoResponseDto>> listarTodos() {
        return ResponseEntity.ok(dispositivoService.listarTodos());
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar dispositivo",
        description = "Actualiza los datos de un dispositivo. Valida unicidad de fingerprint al cambiarlo.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Actualización de IP",
                    value = """
                            {
                              "ipAddress": "200.49.130.55",
                              "userAgent": "Mozilla/5.0 (Android 13; Mobile) Chrome/120.0",
                              "tipoDispositivo": "MOVIL",
                              "sistemaOperativo": "Android 13",
                              "fingerprint": "a3f9b2c1d8e7f654a321b0c9d8e7f6a5"
                            }
                            """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dispositivo actualizado",
            content = @Content(schema = @Schema(implementation = DispositivoResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Dispositivo no encontrado"),
        @ApiResponse(responseCode = "409", description = "El nuevo fingerprint ya pertenece a otro dispositivo")
    })
    public ResponseEntity<DispositivoResponseDto> actualizar(
            @Parameter(description = "UUID del dispositivo a actualizar") @PathVariable String id,
            @Valid @RequestBody DispositivoRequestDto dto) {
        return ResponseEntity.ok(dispositivoService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar dispositivo")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Dispositivo eliminado"),
        @ApiResponse(responseCode = "404", description = "Dispositivo no encontrado")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "UUID del dispositivo a eliminar") @PathVariable String id) {
        dispositivoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Búsquedas ─────────────────────────────────────────────────────────────

    @GetMapping("/persona/{personaId}")
    @Operation(
        summary = "Dispositivos de una persona",
        description = "Devuelve todos los dispositivos vinculados a la persona via USA_DISPOSITIVO."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dispositivos de la persona (puede ser vacía)")
    })
    public ResponseEntity<List<DispositivoResponseDto>> buscarPorPersona(
            @Parameter(description = "UUID de la persona") @PathVariable String personaId) {
        return ResponseEntity.ok(dispositivoService.buscarPorPersona(personaId));
    }
}
