package com.batalla.fraudesito.service;

import com.batalla.fraudesito.domain.enums.TipoRelacion;
import com.batalla.fraudesito.dto.request.TransaccionRequestDto;
import com.batalla.fraudesito.dto.response.AnilloDeFraudeDto;
import com.batalla.fraudesito.dto.response.CaminoGrafoDto;
import com.batalla.fraudesito.dto.response.TransaccionResponseDto;

import java.util.List;

public interface TransaccionService {

    // ─── CRUD ──────────────────────────────────────────────────────────────────
    TransaccionResponseDto crear(TransaccionRequestDto dto);
    TransaccionResponseDto buscarPorId(String id);
    List<TransaccionResponseDto> listarTodas();
    void eliminar(String id);

    // ─── Búsquedas ─────────────────────────────────────────────────────────────
    List<TransaccionResponseDto> buscarPorCuentaOrigen(String cuentaOrigenId);
    List<TransaccionResponseDto> buscarPorCuentaDestino(String cuentaDestinoId);
    List<TransaccionResponseDto> buscarAlertadas();

    // ─── Relaciones ────────────────────────────────────────────────────────────
    void relacionarTransacciones(String transaccionId1, String transaccionId2,
                                 TipoRelacion tipoRelacion, Double puntajeSimilitud);

    // ─── Fraude ────────────────────────────────────────────────────────────────
    List<TransaccionResponseDto> detectarVelocity(String cuentaId, int minutosVentana, int umbral);
    List<TransaccionResponseDto> detectarSmurfing(String cuentaDestinoId, String montoMaximo, int cantidadMinima);
    List<AnilloDeFraudeDto> anilloDeFraude(String transaccionId);

    // ─── Grafo ─────────────────────────────────────────────────────────────────
    CaminoGrafoDto caminoMasCorto(String idT1, String idT2);
}
