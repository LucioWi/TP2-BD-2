package com.batalla.fraudesito.service;

import com.batalla.fraudesito.dto.request.CuentaRequestDto;
import com.batalla.fraudesito.dto.response.CaminoGrafoDto;
import com.batalla.fraudesito.dto.response.CicloCircularDto;
import com.batalla.fraudesito.dto.response.CuentaResponseDto;
import com.batalla.fraudesito.dto.response.ShortestPathDto;

import java.util.List;

public interface CuentaService {

    // ─── CRUD ──────────────────────────────────────────────────────────────────
    CuentaResponseDto crear(CuentaRequestDto dto);
    CuentaResponseDto buscarPorId(String id);
    CuentaResponseDto buscarPorNumeroCuenta(String numeroCuenta);
    List<CuentaResponseDto> listarTodas();
    CuentaResponseDto actualizar(String id, CuentaRequestDto dto);
    void eliminar(String id);

    // ─── Búsquedas ─────────────────────────────────────────────────────────────
    CuentaResponseDto buscarPorAliasONumeroCuenta(String criterio);
    List<CuentaResponseDto> buscarPorPersona(String personaId);

    // ─── Fraude ────────────────────────────────────────────────────────────────
    List<CuentaResponseDto> buscarCuentasPuente(int limite);
    List<CuentaResponseDto> buscarConTransaccionesAlertadas();
    List<CuentaResponseDto> buscarConAccesoDesdeDispositivosSospechosos();
    List<CicloCircularDto> detectarFlujoCircular(String cuentaId, int limite);

    // ─── Grafo ─────────────────────────────────────────────────────────────────

    /** Camino básico: retorna solo la lista de IDs de cuentas en el camino. */
    CaminoGrafoDto caminoDeTransferencias(String idOrigen, String idDestino);

    /**
     * Camino enriquecido: retorna nodos y aristas con propiedades completas,
     * riesgo estimado y formato Cytoscape.js listo para el frontend.
     */
    ShortestPathDto shortestPathDetallado(String idOrigen, String idDestino);
}
