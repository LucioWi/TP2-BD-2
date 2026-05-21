package com.batalla.fraudesito.controller;

import com.batalla.fraudesito.dto.request.PersonaRequestDto;
import com.batalla.fraudesito.dto.response.CuentaResponseDto;
import com.batalla.fraudesito.dto.response.DispositivoResponseDto;
import com.batalla.fraudesito.dto.response.PersonaResponseDto;
import com.batalla.fraudesito.service.PersonaService;
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
@RequestMapping("/api/v1/personas")
@RequiredArgsConstructor
@Tag(name = "Personas", description = "Gestión de personas físicas — nodo central del grafo de fraude")
public class PersonaController {

    private final PersonaService personaService;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Crear persona",
        description = "Registra una nueva persona. DNI y email son únicos en el sistema.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Persona completa",
                    value = """
                            {
                              "nombre": "Juan Carlos",
                              "apellido": "Pérez",
                              "dni": "28456123",
                              "email": "jcperez@email.com",
                              "telefono": "+541145678901",
                              "fechaNacimiento": "1985-03-15"
                            }
                            """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Persona creada exitosamente",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (validación fallida)"),
        @ApiResponse(responseCode = "409", description = "DNI o email ya registrado en el sistema")
    })
    public ResponseEntity<PersonaResponseDto> crear(@Valid @RequestBody PersonaRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personaService.crear(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar persona por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Persona encontrada",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Persona no encontrada")
    })
    public ResponseEntity<PersonaResponseDto> buscarPorId(
            @Parameter(description = "UUID de la persona") @PathVariable String id) {
        return ResponseEntity.ok(personaService.buscarPorId(id));
    }

    @GetMapping("/dni/{dni}")
    @Operation(summary = "Buscar persona por DNI")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Persona encontrada",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Persona no encontrada con ese DNI")
    })
    public ResponseEntity<PersonaResponseDto> buscarPorDni(
            @Parameter(description = "Número de DNI (7 u 8 dígitos)", example = "28456123")
            @PathVariable String dni) {
        return ResponseEntity.ok(personaService.buscarPorDni(dni));
    }

    @GetMapping
    @Operation(summary = "Listar todas las personas")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de personas (puede ser vacía)")
    })
    public ResponseEntity<List<PersonaResponseDto>> listarTodas() {
        return ResponseEntity.ok(personaService.listarTodas());
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Actualizar persona",
        description = "Actualiza los datos de una persona existente. Valida unicidad de DNI y email al cambiarlos.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Actualización de datos de contacto",
                    value = """
                            {
                              "nombre": "Juan Carlos",
                              "apellido": "Pérez García",
                              "dni": "28456123",
                              "email": "jcperez.nuevo@email.com",
                              "telefono": "+541155443322",
                              "fechaNacimiento": "1985-03-15"
                            }
                            """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Persona actualizada",
            content = @Content(schema = @Schema(implementation = PersonaResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Persona no encontrada"),
        @ApiResponse(responseCode = "409", description = "El nuevo DNI o email ya pertenece a otra persona")
    })
    public ResponseEntity<PersonaResponseDto> actualizar(
            @Parameter(description = "UUID de la persona a actualizar") @PathVariable String id,
            @Valid @RequestBody PersonaRequestDto dto) {
        return ResponseEntity.ok(personaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar persona", description = "Elimina la persona y todas sus relaciones del grafo.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Persona eliminada"),
        @ApiResponse(responseCode = "404", description = "Persona no encontrada")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "UUID de la persona a eliminar") @PathVariable String id) {
        personaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Relaciones ────────────────────────────────────────────────────────────

    @PostMapping("/{personaId}/cuentas/{cuentaId}")
    @Operation(
        summary = "Asignar cuenta a persona",
        description = """
            Crea la relación POSEE_CUENTA entre la persona y la cuenta.
            Falla con 409 si la relación ya existe.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuenta asignada — devuelve los datos de la cuenta",
            content = @Content(schema = @Schema(implementation = CuentaResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Persona o cuenta no encontrada"),
        @ApiResponse(responseCode = "409", description = "La cuenta ya está asignada a esta persona")
    })
    public ResponseEntity<CuentaResponseDto> asignarCuenta(
            @Parameter(description = "UUID de la persona") @PathVariable String personaId,
            @Parameter(description = "UUID de la cuenta") @PathVariable String cuentaId) {
        return ResponseEntity.ok(personaService.asignarCuenta(personaId, cuentaId));
    }

    @PostMapping("/{personaId}/dispositivos/{dispositivoId}")
    @Operation(
        summary = "Vincular dispositivo a persona",
        description = "Crea la relación USA_DISPOSITIVO entre la persona y el dispositivo. Falla con 409 si ya existe."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dispositivo vinculado — devuelve los datos del dispositivo",
            content = @Content(schema = @Schema(implementation = DispositivoResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Persona o dispositivo no encontrado"),
        @ApiResponse(responseCode = "409", description = "El dispositivo ya está vinculado a esta persona")
    })
    public ResponseEntity<DispositivoResponseDto> asignarDispositivo(
            @Parameter(description = "UUID de la persona") @PathVariable String personaId,
            @Parameter(description = "UUID del dispositivo") @PathVariable String dispositivoId) {
        return ResponseEntity.ok(personaService.asignarDispositivo(personaId, dispositivoId));
    }
}
