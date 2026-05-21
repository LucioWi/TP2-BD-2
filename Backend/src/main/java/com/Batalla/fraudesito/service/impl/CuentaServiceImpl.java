package com.batalla.fraudesito.service.impl;

import com.batalla.fraudesito.domain.enums.EstadoCuenta;
import com.batalla.fraudesito.domain.node.Cuenta;
import com.batalla.fraudesito.dto.request.CuentaRequestDto;
import com.batalla.fraudesito.dto.response.CaminoGrafoDto;
import com.batalla.fraudesito.dto.response.CicloCircularDto;
import com.batalla.fraudesito.dto.response.CuentaResponseDto;
import com.batalla.fraudesito.dto.response.PasoCamino;
import com.batalla.fraudesito.dto.response.ShortestPathDto;
import com.batalla.fraudesito.exception.DuplicateResourceException;
import com.batalla.fraudesito.exception.ResourceNotFoundException;
import com.batalla.fraudesito.mapper.CuentaMapper;
import com.batalla.fraudesito.repository.CuentaRepository;
import com.batalla.fraudesito.repository.TransaccionRepository;
import com.batalla.fraudesito.service.CuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final CuentaMapper cuentaMapper;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public CuentaResponseDto crear(CuentaRequestDto dto) {
        if (dto.getSaldo() == null || dto.getSaldo().isBlank()) {
            throw new IllegalArgumentException("El saldo inicial es obligatorio para crear una cuenta");
        }
        if (cuentaRepository.existsByNumeroCuenta(dto.getNumeroCuenta())) {
            throw new DuplicateResourceException("Cuenta", "numeroCuenta", dto.getNumeroCuenta());
        }
        if (dto.getCbvu() != null && cuentaRepository.existsByCbvu(dto.getCbvu())) {
            throw new DuplicateResourceException("Cuenta", "cbvu", dto.getCbvu());
        }

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(dto.getNumeroCuenta())
                .cbvu(dto.getCbvu())
                .alias(dto.getAlias())
                .banco(dto.getBanco())
                .tipoCuenta(dto.getTipoCuenta())
                .saldo(dto.getSaldo())
                .moneda(dto.getMoneda())
                .limiteTransferenciaDiaria(dto.getLimiteTransferenciaDiaria())
                .estado(EstadoCuenta.ACTIVA)
                .fechaApertura(LocalDateTime.now())
                .build();

        return cuentaMapper.toDto(cuentaRepository.save(cuenta));
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaResponseDto buscarPorId(String id) {
        return cuentaRepository.findById(id)
                .map(cuentaMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaResponseDto buscarPorNumeroCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .map(cuentaMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta", "numeroCuenta", numeroCuenta));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDto> listarTodas() {
        return StreamSupport.stream(cuentaRepository.findAll().spliterator(), false)
                .map(cuentaMapper::toDto)
                .toList();
    }

    @Override
    public CuentaResponseDto actualizar(String id, CuentaRequestDto dto) {
        Cuenta cuenta = cuentaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", id));

        if (!cuenta.getNumeroCuenta().equals(dto.getNumeroCuenta())
                && cuentaRepository.existsByNumeroCuenta(dto.getNumeroCuenta())) {
            throw new DuplicateResourceException("Cuenta", "numeroCuenta", dto.getNumeroCuenta());
        }
        if (dto.getCbvu() != null
                && !dto.getCbvu().equals(cuenta.getCbvu())
                && cuentaRepository.existsByCbvu(dto.getCbvu())) {
            throw new DuplicateResourceException("Cuenta", "cbvu", dto.getCbvu());
        }

        cuenta.setNumeroCuenta(dto.getNumeroCuenta());
        cuenta.setCbvu(dto.getCbvu());
        cuenta.setAlias(dto.getAlias());
        cuenta.setBanco(dto.getBanco());
        cuenta.setTipoCuenta(dto.getTipoCuenta());
        if (dto.getSaldo() != null && !dto.getSaldo().isBlank()) {
            cuenta.setSaldo(dto.getSaldo());
        }
        cuenta.setMoneda(dto.getMoneda());
        cuenta.setLimiteTransferenciaDiaria(dto.getLimiteTransferenciaDiaria());
        cuenta.setFechaActualizacion(LocalDateTime.now());

        return cuentaMapper.toDto(cuentaRepository.save(cuenta));
    }

    @Override
    public void eliminar(String id) {
        if (!cuentaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cuenta", "id", id);
        }
        cuentaRepository.deleteById(id);
    }

    // ─── Búsquedas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDto> buscarPorPersona(String personaId) {
        return cuentaRepository.findByPersonaId(personaId).stream()
                .map(cuentaMapper::toDto).toList();
    }

    // ─── Fraude ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDto> buscarCuentasPuente(int limite) {
        return cuentaRepository.findCuentasPuente(limite).stream()
                .map(cuentaMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDto> buscarConTransaccionesAlertadas() {
        return cuentaRepository.findCuentasConTransaccionesAlertadas().stream()
                .map(cuentaMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDto> buscarConAccesoDesdeDispositivosSospechosos() {
        return cuentaRepository.findCuentasConAccesoDesdeDispositivosSospechosos().stream()
                .map(cuentaMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CicloCircularDto> detectarFlujoCircular(String cuentaId, int limite) {
        if (!cuentaRepository.existsById(cuentaId)) {
            throw new ResourceNotFoundException("Cuenta", "id", cuentaId);
        }
        return transaccionRepository.findFlujoCircular(cuentaId, limite).stream()
                .map(rec -> CicloCircularDto.builder()
                        .cuentasEnCiclo(rec.cuentasEnCiclo())
                        .saltos(rec.saltos() != null ? rec.saltos().intValue() : 0)
                        .build())
                .toList();
    }

    // ─── Grafo: camino básico ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CaminoGrafoDto caminoDeTransferencias(String idOrigen, String idDestino) {
        if (!cuentaRepository.existsById(idOrigen)) {
            throw new ResourceNotFoundException("Cuenta", "id", idOrigen);
        }
        if (!cuentaRepository.existsById(idDestino)) {
            throw new ResourceNotFoundException("Cuenta", "id", idDestino);
        }

        Map<String, Object> resultado = cuentaRepository
                .findCaminoDeTransferencias(idOrigen, idDestino)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe flujo de transferencias entre la cuenta '" + idOrigen + "' y '" + idDestino + "'"));

        return CaminoGrafoDto.builder()
                .origenId(idOrigen)
                .destinoId(idDestino)
                .saltos(((Number) resultado.get("saltos")).intValue())
                .entidades(toStringList(resultado.get("cuentasEnCamino")))
                .build();
    }

    // ─── Grafo: shortest path enriquecido (Cytoscape.js) ──────────────────────

    @Override
    @Transactional(readOnly = true)
    public ShortestPathDto shortestPathDetallado(String idOrigen, String idDestino) {
        if (!cuentaRepository.existsById(idOrigen)) {
            throw new ResourceNotFoundException("Cuenta", "id", idOrigen);
        }
        if (!cuentaRepository.existsById(idDestino)) {
            throw new ResourceNotFoundException("Cuenta", "id", idDestino);
        }

        List<PasoCamino> pasos = cuentaRepository
                .findCaminoDeTransferenciasDetallado(idOrigen, idDestino);

        if (pasos.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No existe flujo directo de transferencias entre las cuentas indicadas");
        }

        // Separar nodos y aristas por discriminador de tipo
        List<PasoCamino> nodosRaw = pasos.stream()
                .filter(p -> "NODE".equals(p.tipo())).toList();
        List<PasoCamino> relacionesRaw = pasos.stream()
                .filter(p -> "EDGE".equals(p.tipo())).toList();

        int saltos = relacionesRaw.size();
        List<String> factoresRiesgo = new ArrayList<>();

        // ── Construir nodos Cytoscape ──────────────────────────────────────────
        List<ShortestPathDto.Node> nodes = new ArrayList<>();
        for (int i = 0; i < nodosRaw.size(); i++) {
            PasoCamino n = nodosRaw.get(i);
            boolean esOrigen       = (i == 0);
            boolean esDestino      = (i == nodosRaw.size() - 1);
            boolean esIntermediario = !esOrigen && !esDestino;
            String estado          = n.estado() != null ? n.estado().name() : null;

            String label = n.alias() != null ? n.alias() : n.numeroCuenta();

            nodes.add(ShortestPathDto.Node.builder()
                    .data(ShortestPathDto.NodeData.builder()
                            .id(n.id())
                            .label(label)
                            .numeroCuenta(n.numeroCuenta())
                            .banco(n.banco())
                            .tipoCuenta(n.tipoCuenta() != null ? n.tipoCuenta().name() : null)
                            .estado(estado)
                            .saldo(n.saldo())
                            .moneda(n.moneda())
                            .esOrigen(esOrigen)
                            .esDestino(esDestino)
                            .esIntermediario(esIntermediario)
                            .riesgoNodo(riesgoDeEstado(estado))
                            .build())
                    .build());
        }

        // ── Construir aristas Cytoscape ────────────────────────────────────────
        List<ShortestPathDto.Edge> edges = new ArrayList<>();
        for (int i = 0; i < relacionesRaw.size(); i++) {
            PasoCamino r = relacionesRaw.get(i);
            edges.add(ShortestPathDto.Edge.builder()
                    .data(ShortestPathDto.EdgeData.builder()
                            .id("edge-" + i)
                            .source(r.origenId())
                            .target(r.destinoId())
                            .transaccionId(r.transaccionId())
                            .monto(r.monto())
                            .moneda(r.moneda())
                            .canal(r.canal())
                            .estadoTx(r.estadoTx())
                            .fecha(r.fecha())
                            .build())
                    .build());
        }

        // ── Intermediarios ─────────────────────────────────────────────────────
        List<String> intermediarios = nodosRaw.size() > 2
                ? nodosRaw.subList(1, nodosRaw.size() - 1).stream()
                        .map(PasoCamino::id)
                        .toList()
                : List.of();

        // ── Riesgo estimado ────────────────────────────────────────────────────
        int riesgoEstimado = calcularRiesgoEstimado(nodosRaw, saltos, intermediarios, factoresRiesgo);

        return ShortestPathDto.builder()
                .elements(ShortestPathDto.Elements.builder()
                        .nodes(nodes)
                        .edges(edges)
                        .build())
                .metadata(ShortestPathDto.Metadata.builder()
                        .origenId(idOrigen)
                        .destinoId(idDestino)
                        .saltos(saltos)
                        .intermediarios(intermediarios.isEmpty() ? null : intermediarios)
                        .riesgoEstimado(riesgoEstimado)
                        .factoresRiesgo(factoresRiesgo.isEmpty() ? null : factoresRiesgo)
                        .build())
                .build();
    }

    // ─── Helpers privados ──────────────────────────────────────────────────────

    /**
     * Score de riesgo basado únicamente en el estado de la cuenta.
     * Los estados bloqueado/suspendido son señales directas de actividad anómala.
     */
    private int riesgoDeEstado(String estado) {
        if (estado == null) return 0;
        return switch (estado) {
            case "BLOQUEADA"   -> 100;
            case "SUSPENDIDA"  -> 60;
            case "CERRADA"     -> 40;
            default            -> 0;   // ACTIVA
        };
    }

    /**
     * Calcula el riesgo total del camino acumulando penalizaciones:
     *  +15 pts por intermediario (techo 45)
     *  +40 pts si un nodo está BLOQUEADO
     *  +25 pts si un nodo está SUSPENDIDO
     *  +15 pts si un nodo está CERRADO
     * Resultado máximo: 100.
     */
    private int calcularRiesgoEstimado(List<PasoCamino> nodos, int saltos,
                                        List<String> intermediarios, List<String> factores) {
        int riesgo = 0;

        // Penalización por cantidad de intermediarios
        if (!intermediarios.isEmpty()) {
            int pts = Math.min(intermediarios.size() * 15, 45);
            riesgo += pts;
            factores.add(intermediarios.size() + " cuenta(s) intermediaria(s) en la ruta (+" + pts + " pts)");
        }

        // Penalización por estado de cada nodo
        for (int i = 0; i < nodos.size(); i++) {
            PasoCamino nodo = nodos.get(i);
            String estado    = nodo.estado() != null ? nodo.estado().name() : "";
            String numCuenta = nodo.numeroCuenta() != null ? nodo.numeroCuenta() : "desconocida";
            String rol       = (i == 0) ? "origen" : (i == nodos.size() - 1) ? "destino" : "intermediaria";

            switch (estado) {
                case "BLOQUEADA" -> {
                    riesgo += 40;
                    factores.add("Cuenta " + rol + " BLOQUEADA: " + numCuenta + " (+40 pts)");
                }
                case "SUSPENDIDA" -> {
                    riesgo += 25;
                    factores.add("Cuenta " + rol + " SUSPENDIDA: " + numCuenta + " (+25 pts)");
                }
                case "CERRADA" -> {
                    riesgo += 15;
                    factores.add("Cuenta " + rol + " CERRADA: " + numCuenta + " (+15 pts)");
                }
                default -> { /* ACTIVA — sin penalización */ }
            }
        }

        return Math.min(riesgo, 100);
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        return ((List<?>) value).stream().map(Object::toString).toList();
    }

    private String str(Object value) {
        return value != null ? String.valueOf(value) : null;
    }
}
