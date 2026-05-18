package com.batalla.fraudesito.service.impl;

import com.batalla.fraudesito.domain.enums.CanalTransaccion;
import com.batalla.fraudesito.domain.enums.EstadoTransaccion;
import com.batalla.fraudesito.domain.enums.TipoRelacion;
import com.batalla.fraudesito.domain.node.Cuenta;
import com.batalla.fraudesito.domain.node.Transaccion;
import com.batalla.fraudesito.domain.relationship.RelacionadaCon;
import com.batalla.fraudesito.dto.request.TransaccionRequestDto;
import com.batalla.fraudesito.dto.response.AnilloDeFraudeDto;
import com.batalla.fraudesito.dto.response.CaminoGrafoDto;
import com.batalla.fraudesito.dto.response.TransaccionResponseDto;
import com.batalla.fraudesito.exception.ResourceNotFoundException;
import com.batalla.fraudesito.mapper.TransaccionMapper;
import com.batalla.fraudesito.repository.CuentaRepository;
import com.batalla.fraudesito.repository.TransaccionRepository;
import com.batalla.fraudesito.service.TransaccionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransaccionServiceImpl implements TransaccionService {

    private static final BigDecimal UMBRAL_MONTO_ALTO   = new BigDecimal("100000");
    private static final BigDecimal UMBRAL_MONTO_MEDIO  = new BigDecimal("10000");
    private static final int        SCORE_MONTO_ALTO    = 30;
    private static final int        SCORE_MONTO_MEDIO   = 15;
    private static final int        SCORE_CANAL_AUTO    = 10;
    private static final int        SCORE_IP_AUSENTE    = 5;
    private static final int        UMBRAL_ALERTA       = 40;

    private final TransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;
    private final TransaccionMapper transaccionMapper;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public TransaccionResponseDto crear(TransaccionRequestDto dto) {
        Cuenta cuentaOrigen = cuentaRepository.findById(dto.getCuentaOrigenId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", dto.getCuentaOrigenId()));
        Cuenta cuentaDestino = cuentaRepository.findById(dto.getCuentaDestinoId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", dto.getCuentaDestinoId()));

        int nivelRiesgo = calcularNivelRiesgo(dto);
        boolean esAlertada = nivelRiesgo >= UMBRAL_ALERTA;
        String motivoAlerta = esAlertada ? buildMotivoAlerta(dto) : null;

        Transaccion transaccion = Transaccion.builder()
                .numeroOrden(generarNumeroOrden())
                .monto(dto.getMonto())
                .moneda(dto.getMoneda())
                .tipo(dto.getTipo())
                .canal(dto.getCanal())
                .descripcion(dto.getDescripcion())
                .ipAddress(dto.getIpAddress())
                .latitud(dto.getLatitud())
                .longitud(dto.getLongitud())
                .estado(EstadoTransaccion.PENDIENTE)
                .nivelRiesgo(nivelRiesgo)
                .esAlertada(esAlertada)
                .motivoAlerta(motivoAlerta)
                .cuentaOrigen(cuentaOrigen)
                .cuentaDestino(cuentaDestino)
                .fechaCreacion(LocalDateTime.now())
                .build();

        Transaccion guardada = transaccionRepository.save(transaccion);
        if (esAlertada) {
            log.warn("Transacción {} alertada — riesgo: {} — motivo: {}", guardada.getId(), nivelRiesgo, motivoAlerta);
        }
        return transaccionMapper.toDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public TransaccionResponseDto buscarPorId(String id) {
        return transaccionRepository.findById(id)
                .map(transaccionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Transaccion", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResponseDto> listarTodas() {
        return StreamSupport.stream(transaccionRepository.findAll().spliterator(), false)
                .map(transaccionMapper::toDto)
                .toList();
    }

    @Override
    public void eliminar(String id) {
        if (!transaccionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaccion", "id", id);
        }
        transaccionRepository.deleteById(id);
    }

    // ─── Búsquedas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResponseDto> buscarPorCuentaOrigen(String cuentaOrigenId) {
        return transaccionRepository.findByCuentaOrigenId(cuentaOrigenId).stream()
                .map(transaccionMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResponseDto> buscarPorCuentaDestino(String cuentaDestinoId) {
        return transaccionRepository.findByCuentaDestinoId(cuentaDestinoId).stream()
                .map(transaccionMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResponseDto> buscarAlertadas() {
        return transaccionRepository.findByEsAlertadaTrue().stream()
                .map(transaccionMapper::toDto).toList();
    }

    // ─── Relaciones ────────────────────────────────────────────────────────────

    @Override
    public void relacionarTransacciones(String transaccionId1, String transaccionId2,
                                        TipoRelacion tipoRelacion, Double puntajeSimilitud) {
        Transaccion t1 = transaccionRepository.findById(transaccionId1)
                .orElseThrow(() -> new ResourceNotFoundException("Transaccion", "id", transaccionId1));
        Transaccion t2 = transaccionRepository.findById(transaccionId2)
                .orElseThrow(() -> new ResourceNotFoundException("Transaccion", "id", transaccionId2));

        t1.getTransaccionesRelacionadas().add(RelacionadaCon.builder()
                .transaccionVinculada(t2)
                .tipoRelacion(tipoRelacion)
                .puntajeSimilitud(puntajeSimilitud)
                .fechaDeteccion(LocalDateTime.now())
                .build());

        transaccionRepository.save(t1);
    }

    // ─── Fraude ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResponseDto> detectarVelocity(String cuentaId, int minutosVentana, int umbral) {
        if (!cuentaRepository.existsById(cuentaId)) {
            throw new ResourceNotFoundException("Cuenta", "id", cuentaId);
        }
        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusMinutes(minutosVentana);

        return transaccionRepository.findVelocityFraud(cuentaId, desde, hasta, umbral).stream()
                .map(transaccionMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransaccionResponseDto> detectarSmurfing(String cuentaDestinoId, String montoMaximo,
                                                          int cantidadMinima) {
        if (!cuentaRepository.existsById(cuentaDestinoId)) {
            throw new ResourceNotFoundException("Cuenta", "id", cuentaDestinoId);
        }
        LocalDateTime hasta = LocalDateTime.now();
        LocalDateTime desde = hasta.minusHours(24);

        return transaccionRepository.findPatronSmurfing(cuentaDestinoId, desde, hasta, montoMaximo, cantidadMinima)
                .stream().map(transaccionMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnilloDeFraudeDto> anilloDeFraude(String transaccionId) {
        if (!transaccionRepository.existsById(transaccionId)) {
            throw new ResourceNotFoundException("Transaccion", "id", transaccionId);
        }
        return transaccionRepository.findAnilloDeFraude(transaccionId).stream()
                .map(row -> AnilloDeFraudeDto.builder()
                        .id(toStr(row.get("id")))
                        .monto(toStr(row.get("monto")))
                        .estado(toStr(row.get("estado")))
                        .nivelRiesgo(row.get("nivelRiesgo") != null
                                ? ((Number) row.get("nivelRiesgo")).intValue() : null)
                        .canal(toStr(row.get("canal")))
                        .ipAddress(toStr(row.get("ipAddress")))
                        .build())
                .toList();
    }

    // ─── Grafo ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CaminoGrafoDto caminoMasCorto(String idT1, String idT2) {
        if (!transaccionRepository.existsById(idT1)) {
            throw new ResourceNotFoundException("Transaccion", "id", idT1);
        }
        if (!transaccionRepository.existsById(idT2)) {
            throw new ResourceNotFoundException("Transaccion", "id", idT2);
        }

        Map<String, Object> resultado = transaccionRepository
                .findCaminoMasCortoEntreTransacciones(idT1, idT2)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe camino entre la transacción '" + idT1 + "' y '" + idT2 + "'"));

        return CaminoGrafoDto.builder()
                .origenId(idT1)
                .destinoId(idT2)
                .saltos(((Number) resultado.get("saltos")).intValue())
                .entidades(toStringList(resultado.get("transaccionesEnCamino")))
                .build();
    }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    private int calcularNivelRiesgo(TransaccionRequestDto dto) {
        int score = 0;
        BigDecimal monto = new BigDecimal(dto.getMonto());

        if (monto.compareTo(UMBRAL_MONTO_ALTO) >= 0) {
            score += SCORE_MONTO_ALTO;
        } else if (monto.compareTo(UMBRAL_MONTO_MEDIO) >= 0) {
            score += SCORE_MONTO_MEDIO;
        }

        if (CanalTransaccion.API.equals(dto.getCanal())
                || CanalTransaccion.TRANSFERENCIA_AUTOMATICA.equals(dto.getCanal())) {
            score += SCORE_CANAL_AUTO;
        }

        if (dto.getIpAddress() == null || dto.getIpAddress().isBlank()) {
            score += SCORE_IP_AUSENTE;
        }

        return Math.min(score, 100);
    }

    private String buildMotivoAlerta(TransaccionRequestDto dto) {
        List<String> motivos = new ArrayList<>();
        BigDecimal monto = new BigDecimal(dto.getMonto());

        if (monto.compareTo(UMBRAL_MONTO_ALTO) >= 0) {
            motivos.add("MONTO_ALTO");
        } else if (monto.compareTo(UMBRAL_MONTO_MEDIO) >= 0) {
            motivos.add("MONTO_MEDIO");
        }

        if (CanalTransaccion.API.equals(dto.getCanal())
                || CanalTransaccion.TRANSFERENCIA_AUTOMATICA.equals(dto.getCanal())) {
            motivos.add("CANAL_AUTOMATICO");
        }

        if (dto.getIpAddress() == null || dto.getIpAddress().isBlank()) {
            motivos.add("IP_AUSENTE");
        }

        return String.join(", ", motivos);
    }

    private String generarNumeroOrden() {
        return "TXN-" + Year.now().getValue() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String toStr(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        return ((List<?>) value).stream().map(Object::toString).toList();
    }
}
