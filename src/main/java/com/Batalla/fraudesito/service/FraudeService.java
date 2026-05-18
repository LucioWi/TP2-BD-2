package com.batalla.fraudesito.service;

import com.batalla.fraudesito.dto.response.CicloDetalladoDto;
import com.batalla.fraudesito.dto.response.ComunidadSospechosaDto;
import com.batalla.fraudesito.dto.response.CuentaCompartidaDto;
import com.batalla.fraudesito.dto.response.CuentaMulaDto;
import com.batalla.fraudesito.dto.response.DispositivoMultiCuentaDto;
import com.batalla.fraudesito.dto.response.DispositivoTakeoverDto;
import com.batalla.fraudesito.dto.response.EstadisticaTransaccionDto;
import com.batalla.fraudesito.dto.response.IdentidadSinteticaDto;
import com.batalla.fraudesito.dto.response.PersonaConectadaDto;
import com.batalla.fraudesito.dto.response.PersonaResponseDto;
import com.batalla.fraudesito.dto.response.RiesgoRelacionalDto;
import com.batalla.fraudesito.dto.response.VolumenCuentaDto;

import java.time.LocalDateTime;
import java.util.List;

public interface FraudeService {

    // ─── Personas ──────────────────────────────────────────────────────────────

    /** PEPs que reciben transferencias >= umbral (FATF R.12 — EDD obligatorio). */
    List<PersonaResponseDto> buscarPEPsConTransferenciasAltas(double umbral);

    /** Personas en listas de sanciones que aún tienen cuentas activas (OFAC/ONU/UE). */
    List<PersonaResponseDto> buscarSancionadosConCuentasActivas();

    /** Ranking de personas por cantidad de contrapartes financieras distintas (hubs de fraude). */
    List<RiesgoRelacionalDto> rankingHubsRelacionales(int limite);

    /**
     * Comunidades financieras sospechosas: personas con muchas contrapartes directas.
     * Aproximación a community detection sin Neo4j GDS.
     */
    List<ComunidadSospechosaDto> detectarComunidadesSospechosas(int minConexiones, int limite);

    /**
     * Vecindario financiero indirecto: personas alcanzables en 2-4 saltos de transferencias.
     * Revela beneficiarios finales ocultos detrás de cuentas intermediarias.
     */
    List<PersonaConectadaDto> explorarConexionesIndirectas(String personaId, int limite);

    /**
     * Indicadores de identidad sintética: personas que comparten dispositivos con otras,
     * combinados con baja verificación y alta actividad financiera.
     */
    List<IdentidadSinteticaDto> detectarIdentidadSintetica(int minPersonasVinculadas, int limite);

    // ─── Cuentas ───────────────────────────────────────────────────────────────

    /** Pares de cuentas con distintos titulares operadas desde el mismo dispositivo (account sharing). */
    List<CuentaCompartidaDto> detectarCuentasCompartidasEnDispositivo();

    /**
     * Cuentas mula detalladas: cuentas que reciben y reenvían fondos, con volúmenes monetarios.
     * La tasa de dispersión (volumenEnviado/volumenRecibido) es el indicador central.
     */
    List<CuentaMulaDto> detectarCuentasMula(int limite);

    /**
     * Ciclos de transferencias a nivel global (layering).
     * Detecta esquemas A→B→...→A en todo el grafo. Operación costosa — usar con LIMIT.
     */
    List<CicloDetalladoDto> detectarCiclosGlobales(int limite);

    // ─── Transacciones ─────────────────────────────────────────────────────────

    /** Distribución de transacciones por estado (útil para dashboards). */
    List<EstadisticaTransaccionDto> distribucionPorEstado();

    /** Top N cuentas por volumen enviado en un período (detección de dispersión masiva). */
    List<VolumenCuentaDto> topCuentasPorVolumen(LocalDateTime desde, LocalDateTime hasta, int limite);

    // ─── Dispositivos ──────────────────────────────────────────────────────────

    /** Dispositivos vinculados a más de N titulares distintos via USADA_EN (account takeover). */
    List<DispositivoTakeoverDto> detectarAccountTakeover(int limite);

    /**
     * Dispositivos con múltiples cuentas y titulares: vista completa del ecosistema del dispositivo.
     * Más rico que detectarAccountTakeover: agrega todas las cuentas e IDs de titulares.
     */
    List<DispositivoMultiCuentaDto> dispositivosConMultiplesCuentas(int minTitulares, int limite);
}
