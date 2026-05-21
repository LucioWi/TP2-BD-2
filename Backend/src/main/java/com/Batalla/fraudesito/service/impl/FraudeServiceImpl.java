package com.batalla.fraudesito.service.impl;

import com.batalla.fraudesito.dto.response.CicloDetalladoDto;
import com.batalla.fraudesito.dto.response.ComunidadSospechosaDto;
import com.batalla.fraudesito.dto.response.CuentaCompartidaDto;
import com.batalla.fraudesito.dto.response.CuentaMulaDto;
import com.batalla.fraudesito.dto.response.CuentaSelectDto;
import com.batalla.fraudesito.dto.response.DispositivoMultiCuentaDto;
import com.batalla.fraudesito.dto.response.DispositivoTakeoverDto;
import com.batalla.fraudesito.dto.response.EstadisticaTransaccionDto;
import com.batalla.fraudesito.dto.response.IdentidadSinteticaDto;
import com.batalla.fraudesito.dto.response.PersonaConectadaDto;
import com.batalla.fraudesito.dto.response.PersonaResponseDto;
import com.batalla.fraudesito.dto.response.RiesgoRelacionalDto;
import com.batalla.fraudesito.dto.response.VolumenCuentaDto;
import com.batalla.fraudesito.mapper.PersonaMapper;
import com.batalla.fraudesito.repository.CuentaRepository;
import com.batalla.fraudesito.repository.DispositivoRepository;
import com.batalla.fraudesito.repository.PersonaRepository;
import com.batalla.fraudesito.repository.TransaccionRepository;
import com.batalla.fraudesito.service.FraudeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FraudeServiceImpl implements FraudeService {

    private final PersonaRepository personaRepository;
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final DispositivoRepository dispositivoRepository;
    private final PersonaMapper personaMapper;

    // ─── Personas ──────────────────────────────────────────────────────────────

    @Override
    public List<PersonaResponseDto> buscarPEPsConTransferenciasAltas(double umbral) {
        return personaRepository.findPEPsConTransferenciasDeAltoValor(umbral).stream()
                .map(personaMapper::toDto).toList();
    }

    @Override
    public List<PersonaResponseDto> buscarSancionadosConCuentasActivas() {
        return personaRepository.findSancionadosConCuentasActivas().stream()
                .map(personaMapper::toDto).toList();
    }

    @Override
    public List<RiesgoRelacionalDto> rankingHubsRelacionales(int limite) {
        return personaRepository.findPersonasConMayorRiesgoRelacional(limite).stream()
                .map(row -> RiesgoRelacionalDto.builder()
                        .personaId(str(row.get("personaId")))
                        .nombre(str(row.get("nombre")))
                        .apellido(str(row.get("apellido")))
                        .contactosFinancieros(toInt(row.get("contactosFinancieros")))
                        .build())
                .toList();
    }

    @Override
    public List<ComunidadSospechosaDto> detectarComunidadesSospechosas(int minConexiones, int limite) {
        return personaRepository.findComunidadesSospechosas(minConexiones, limite).stream()
                .map(row -> ComunidadSospechosaDto.builder()
                        .centroId(str(row.get("centroId")))
                        .nombre(str(row.get("nombre")))
                        .apellido(str(row.get("apellido")))
                        .nivelRiesgo(toInt(row.get("nivelRiesgo")))
                        .totalConexiones(toInt(row.get("totalConexiones")))
                        .miembrosIds(toStringList(row.get("miembrosIds")))
                        .build())
                .toList();
    }

    @Override
    public List<PersonaConectadaDto> explorarConexionesIndirectas(String personaId, int limite) {
        return personaRepository.findConexionesIndirectas(personaId, limite).stream()
                .map(row -> PersonaConectadaDto.builder()
                        .personaId(str(row.get("personaId")))
                        .nombre(str(row.get("nombre")))
                        .apellido(str(row.get("apellido")))
                        .nivelRiesgo(toInt(row.get("nivelRiesgo")))
                        .esPEP(toBool(row.get("esPEP")))
                        .esSancionado(toBool(row.get("esSancionado")))
                        .build())
                .toList();
    }

    @Override
    public List<IdentidadSinteticaDto> detectarIdentidadSintetica(int minPersonasVinculadas, int limite) {
        return personaRepository.findIndicadoresIdentidadSintetica(minPersonasVinculadas, limite);
    }

    // ─── Cuentas ───────────────────────────────────────────────────────────────

    @Override
    public List<CuentaCompartidaDto> detectarCuentasCompartidasEnDispositivo() {
        return cuentaRepository.findCuentasCompartidasEnDispositivo().stream()
                .map(row -> CuentaCompartidaDto.builder()
                        .cuenta1Id(str(row.get("cuenta1Id")))
                        .cuenta2Id(str(row.get("cuenta2Id")))
                        .dispositivoId(str(row.get("dispositivoId")))
                        .fingerprint(str(row.get("fingerprint")))
                        .titular1Id(str(row.get("titular1Id")))
                        .titular2Id(str(row.get("titular2Id")))
                        .build())
                .toList();
    }

    @Override
    public List<CuentaMulaDto> detectarCuentasMula(int limite) {
        return cuentaRepository.findCuentasMulaDetalladas(limite).stream()
                .map(row -> CuentaMulaDto.builder()
                        .cuentaId(str(row.get("cuentaId")))
                        .numeroCuenta(str(row.get("numeroCuenta")))
                        .banco(str(row.get("banco")))
                        .tipoCuenta(str(row.get("tipoCuenta")))
                        .totalRecibidas(toLong(row.get("totalRecibidas")))
                        .totalEnviadas(toLong(row.get("totalEnviadas")))
                        .actividadTotal(toLong(row.get("actividadTotal")))
                        .volumenRecibido(toDouble(row.get("volumenRecibido")))
                        .volumenEnviado(toDouble(row.get("volumenEnviado")))
                        .build())
                .toList();
    }

    @Override
    public List<CicloDetalladoDto> detectarCiclosGlobales(int limite) {
        return transaccionRepository.findCiclosGlobales(limite).stream()
                .map(row -> CicloDetalladoDto.builder()
                        .anclaId(str(row.get("anclaId")))
                        .cuentasEnCiclo(toStringList(row.get("cuentasEnCiclo")))
                        .numerosCuenta(toStringList(row.get("numerosCuenta")))
                        .saltos(toInt(row.get("saltos")))
                        .build())
                .toList();
    }

    // ─── Transacciones ─────────────────────────────────────────────────────────

    @Override
    public List<EstadisticaTransaccionDto> distribucionPorEstado() {
        return transaccionRepository.countPorEstado().stream()
                .map(row -> EstadisticaTransaccionDto.builder()
                        .estado(str(row.get("estado")))
                        .total(toLong(row.get("total")))
                        .build())
                .toList();
    }

    @Override
    public List<VolumenCuentaDto> topCuentasPorVolumen(LocalDateTime desde, LocalDateTime hasta, int limite) {
        return transaccionRepository.findTopCuentasPorVolumen(desde, hasta, limite).stream()
                .map(row -> VolumenCuentaDto.builder()
                        .cuentaId(str(row.get("cuentaId")))
                        .numeroCuenta(str(row.get("numeroCuenta")))
                        .totalTx(toLong(row.get("totalTx")))
                        .volumenTotal(toDouble(row.get("volumenTotal")))
                        .build())
                .toList();
    }

    // ─── Dispositivos ──────────────────────────────────────────────────────────

    @Override
    public List<DispositivoTakeoverDto> detectarAccountTakeover(int limite) {
        return dispositivoRepository.findDispositivosConCuentasDeMultiplesTitulares(limite).stream()
                .map(row -> DispositivoTakeoverDto.builder()
                        .dispositivoId(str(row.get("dispositivoId")))
                        .fingerprint(str(row.get("fingerprint")))
                        .ipAddress(str(row.get("ipAddress")))
                        .titularesDistintos(toInt(row.get("titularesDistintos")))
                        .titularIds(toStringList(row.get("titularIds")))
                        .build())
                .toList();
    }

    @Override
    public List<DispositivoMultiCuentaDto> dispositivosConMultiplesCuentas(int minTitulares, int limite) {
        return dispositivoRepository.findDispositivosConMultiplesCuentasDetallado(minTitulares, limite);
    }

    // ─── Selectores filtrados para UI ────────────────────────────────────────

    @Override
    public List<CuentaSelectDto> cuentasConCiclosActivos() {
        return cuentaRepository.findCuentasConCiclosActivos();
    }

    @Override
    public List<CuentaSelectDto> cuentasOrigenSospechosas() {
        return cuentaRepository.findCuentasOrigenSospechosas();
    }

    @Override
    public List<CuentaSelectDto> cuentasDestinoHubs() {
        return cuentaRepository.findCuentasDestinoHubs();
    }

    // ─── Helpers de conversión ─────────────────────────────────────────────────

    private String str(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Integer toInt(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    private Long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }

    private Double toDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }

    private Boolean toBool(Object value) {
        if (value instanceof Boolean b) return b;
        if (value != null) return Boolean.parseBoolean(String.valueOf(value));
        return null;
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
