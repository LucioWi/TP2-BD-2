package com.batalla.fraudesito.service;

import com.batalla.fraudesito.dto.request.PersonaRequestDto;
import com.batalla.fraudesito.dto.response.CaminoGrafoDto;
import com.batalla.fraudesito.dto.response.CuentaResponseDto;
import com.batalla.fraudesito.dto.response.DispositivoResponseDto;
import com.batalla.fraudesito.dto.response.PersonaResponseDto;

import java.util.List;

public interface PersonaService {

    // ─── CRUD ──────────────────────────────────────────────────────────────────
    PersonaResponseDto crear(PersonaRequestDto dto);
    PersonaResponseDto buscarPorId(String id);
    PersonaResponseDto buscarPorDni(String dni);
    List<PersonaResponseDto> listarTodas();
    PersonaResponseDto actualizar(String id, PersonaRequestDto dto);
    void eliminar(String id);

    // ─── Relaciones ────────────────────────────────────────────────────────────
    CuentaResponseDto asignarCuenta(String personaId, String cuentaId);
    DispositivoResponseDto asignarDispositivo(String personaId, String dispositivoId);

    // ─── Fraude ────────────────────────────────────────────────────────────────
    List<PersonaResponseDto> buscarPEPs();
    List<PersonaResponseDto> buscarSancionados();
    List<PersonaResponseDto> buscarConCuentasBloqueadas();
    List<PersonaResponseDto> buscarConDispositivosSospechosos();
    List<PersonaResponseDto> buscarPersonasQueCompartenDispositivo(String personaId);
    List<PersonaResponseDto> buscarConNivelRiesgoMinimo(Integer umbral);

    // ─── Grafo ─────────────────────────────────────────────────────────────────
    CaminoGrafoDto caminoMasCorto(String idOrigen, String idDestino);
}
