package com.batalla.fraudesito.service;

import com.batalla.fraudesito.dto.request.DispositivoRequestDto;
import com.batalla.fraudesito.dto.response.DispositivoResponseDto;

import java.util.List;

public interface DispositivoService {

    // ─── CRUD ──────────────────────────────────────────────────────────────────
    DispositivoResponseDto crear(DispositivoRequestDto dto);
    DispositivoResponseDto buscarPorId(String id);
    List<DispositivoResponseDto> listarTodos();
    DispositivoResponseDto actualizar(String id, DispositivoRequestDto dto);
    void eliminar(String id);

    // ─── Búsquedas ─────────────────────────────────────────────────────────────
    List<DispositivoResponseDto> buscarPorPersona(String personaId);

    // ─── Fraude ────────────────────────────────────────────────────────────────
    List<DispositivoResponseDto> buscarDeAltoRiesgo();
    List<DispositivoResponseDto> buscarCompartidosPorMultiplesPersonas(int limite);
    List<DispositivoResponseDto> buscarComunesEntrePersonas(String personaId1, String personaId2);
}
