package com.batalla.fraudesito.service.impl;

import com.batalla.fraudesito.domain.node.Dispositivo;
import com.batalla.fraudesito.dto.request.DispositivoRequestDto;
import com.batalla.fraudesito.dto.response.DispositivoResponseDto;
import com.batalla.fraudesito.exception.DuplicateResourceException;
import com.batalla.fraudesito.exception.ResourceNotFoundException;
import com.batalla.fraudesito.mapper.DispositivoMapper;
import com.batalla.fraudesito.repository.DispositivoRepository;
import com.batalla.fraudesito.service.DispositivoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DispositivoServiceImpl implements DispositivoService {

    private final DispositivoRepository dispositivoRepository;
    private final DispositivoMapper dispositivoMapper;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public DispositivoResponseDto crear(DispositivoRequestDto dto) {
        if (dto.getFingerprint() != null && !dto.getFingerprint().isBlank()
                && dispositivoRepository.existsByFingerprint(dto.getFingerprint())) {
            throw new DuplicateResourceException("Dispositivo", "fingerprint", dto.getFingerprint());
        }

        Dispositivo dispositivo = Dispositivo.builder()
                .tipoDispositivo(dto.getTipoDispositivo())
                .sistemaOperativo(dto.getSistemaOperativo())
                .userAgent(dto.getUserAgent())
                .fingerprint(dto.getFingerprint())
                .ipAddress(dto.getIpAddress())
                .fechaRegistro(LocalDateTime.now())
                .build();

        return dispositivoMapper.toDto(dispositivoRepository.save(dispositivo));
    }

    @Override
    @Transactional(readOnly = true)
    public DispositivoResponseDto buscarPorId(String id) {
        return dispositivoRepository.findById(id)
                .map(dispositivoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Dispositivo", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoResponseDto> listarTodos() {
        return StreamSupport.stream(dispositivoRepository.findAll().spliterator(), false)
                .map(dispositivoMapper::toDto)
                .toList();
    }

    @Override
    public DispositivoResponseDto actualizar(String id, DispositivoRequestDto dto) {
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispositivo", "id", id));

        if (dto.getFingerprint() != null && !dto.getFingerprint().isBlank()
                && !dto.getFingerprint().equals(dispositivo.getFingerprint())
                && dispositivoRepository.existsByFingerprint(dto.getFingerprint())) {
            throw new DuplicateResourceException("Dispositivo", "fingerprint", dto.getFingerprint());
        }

        dispositivo.setTipoDispositivo(dto.getTipoDispositivo());
        dispositivo.setSistemaOperativo(dto.getSistemaOperativo());
        dispositivo.setUserAgent(dto.getUserAgent());
        dispositivo.setFingerprint(dto.getFingerprint());
        dispositivo.setIpAddress(dto.getIpAddress());
        dispositivo.setFechaActualizacion(LocalDateTime.now());

        return dispositivoMapper.toDto(dispositivoRepository.save(dispositivo));
    }

    @Override
    public void eliminar(String id) {
        if (!dispositivoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Dispositivo", "id", id);
        }
        dispositivoRepository.deleteById(id);
    }

    // ─── Búsquedas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoResponseDto> buscarPorPersona(String personaId) {
        return dispositivoRepository.findByPersonaId(personaId).stream()
                .map(dispositivoMapper::toDto).toList();
    }

    // ─── Fraude ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoResponseDto> buscarDeAltoRiesgo() {
        return dispositivoRepository.findDispositivosDeAltoRiesgo().stream()
                .map(dispositivoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoResponseDto> buscarCompartidosPorMultiplesPersonas(int limite) {
        return dispositivoRepository.findDispositivosCompartidosPorMultiplesPersonas(limite).stream()
                .map(dispositivoMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispositivoResponseDto> buscarComunesEntrePersonas(String personaId1, String personaId2) {
        return dispositivoRepository.findDispositivosComunesEntrePersonas(personaId1, personaId2).stream()
                .map(dispositivoMapper::toDto).toList();
    }
}
