package com.batalla.fraudesito.service.impl;

import com.batalla.fraudesito.domain.enums.TipoDocumento;
import com.batalla.fraudesito.domain.node.Cuenta;
import com.batalla.fraudesito.domain.node.Dispositivo;
import com.batalla.fraudesito.domain.node.Persona;
import com.batalla.fraudesito.domain.relationship.PoseeCuenta;
import com.batalla.fraudesito.domain.relationship.UsaDispositivo;
import com.batalla.fraudesito.dto.request.PersonaRequestDto;
import com.batalla.fraudesito.dto.response.CaminoGrafoDto;
import com.batalla.fraudesito.dto.response.CuentaResponseDto;
import com.batalla.fraudesito.dto.response.DispositivoResponseDto;
import com.batalla.fraudesito.dto.response.NodoGrafoDto;
import com.batalla.fraudesito.dto.response.PersonaResponseDto;
import com.batalla.fraudesito.exception.DuplicateResourceException;
import com.batalla.fraudesito.exception.ResourceNotFoundException;
import com.batalla.fraudesito.mapper.CuentaMapper;
import com.batalla.fraudesito.mapper.DispositivoMapper;
import com.batalla.fraudesito.mapper.PersonaMapper;
import com.batalla.fraudesito.repository.CuentaRepository;
import com.batalla.fraudesito.repository.DispositivoRepository;
import com.batalla.fraudesito.repository.PersonaRepository;
import com.batalla.fraudesito.service.PersonaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PersonaServiceImpl implements PersonaService {

    private final PersonaRepository personaRepository;
    private final CuentaRepository cuentaRepository;
    private final DispositivoRepository dispositivoRepository;
    private final PersonaMapper personaMapper;
    private final CuentaMapper cuentaMapper;
    private final DispositivoMapper dispositivoMapper;

    // ─── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    public PersonaResponseDto crear(PersonaRequestDto dto) {
        if (personaRepository.existsByNumeroDocumento(dto.getDni())) {
            throw new DuplicateResourceException("Persona", "DNI", dto.getDni());
        }
        if (dto.getEmail() != null && personaRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Persona", "email", dto.getEmail());
        }

        Persona persona = Persona.builder()
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .tipoDocumento(TipoDocumento.DNI)
                .numeroDocumento(dto.getDni())
                .email(dto.getEmail())
                .telefono(dto.getTelefono())
                .fechaNacimiento(dto.getFechaNacimiento())
                .fechaCreacion(LocalDateTime.now())
                .build();

        return personaMapper.toDto(personaRepository.save(persona));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponseDto buscarPorId(String id) {
        return personaRepository.findById(id)
                .map(personaMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponseDto buscarPorDni(String dni) {
        return personaRepository.findByNumeroDocumento(dni)
                .map(personaMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", "DNI", dni));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> listarTodas() {
        return StreamSupport.stream(personaRepository.findAll().spliterator(), false)
                .map(personaMapper::toDto)
                .toList();
    }

    @Override
    public PersonaResponseDto actualizar(String id, PersonaRequestDto dto) {
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", "id", id));

        if (!persona.getNumeroDocumento().equals(dto.getDni())
                && personaRepository.existsByNumeroDocumento(dto.getDni())) {
            throw new DuplicateResourceException("Persona", "DNI", dto.getDni());
        }
        if (dto.getEmail() != null
                && !dto.getEmail().equals(persona.getEmail())
                && personaRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Persona", "email", dto.getEmail());
        }

        persona.setNombre(dto.getNombre());
        persona.setApellido(dto.getApellido());
        persona.setNumeroDocumento(dto.getDni());
        persona.setEmail(dto.getEmail());
        persona.setTelefono(dto.getTelefono());
        persona.setFechaNacimiento(dto.getFechaNacimiento());
        persona.setFechaActualizacion(LocalDateTime.now());

        return personaMapper.toDto(personaRepository.save(persona));
    }

    @Override
    public void eliminar(String id) {
        if (!personaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Persona", "id", id);
        }
        personaRepository.deleteById(id);
    }

    // ─── Relaciones ────────────────────────────────────────────────────────────

    @Override
    public CuentaResponseDto asignarCuenta(String personaId, String cuentaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", "id", personaId));
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta", "id", cuentaId));

        boolean yaAsignada = persona.getCuentas().stream()
                .anyMatch(rel -> rel.getCuenta().getId().equals(cuentaId));
        if (yaAsignada) {
            throw new DuplicateResourceException("Relación Persona-Cuenta", "cuentaId", cuentaId);
        }

        persona.getCuentas().add(PoseeCuenta.builder()
                .cuenta(cuenta)
                .fechaAsignacion(LocalDateTime.now())
                .activa(true)
                .build());
        personaRepository.save(persona);

        return cuentaMapper.toDto(cuenta);
    }

    @Override
    public DispositivoResponseDto asignarDispositivo(String personaId, String dispositivoId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", "id", personaId));
        Dispositivo dispositivo = dispositivoRepository.findById(dispositivoId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispositivo", "id", dispositivoId));

        boolean yaAsignado = persona.getDispositivos().stream()
                .anyMatch(rel -> rel.getDispositivo().getId().equals(dispositivoId));
        if (yaAsignado) {
            throw new DuplicateResourceException("Relación Persona-Dispositivo", "dispositivoId", dispositivoId);
        }

        persona.getDispositivos().add(UsaDispositivo.builder()
                .dispositivo(dispositivo)
                .ultimoUso(LocalDateTime.now())
                .frecuenciaUso(1)
                .build());
        personaRepository.save(persona);

        return dispositivoMapper.toDto(dispositivo);
    }

    // ─── Fraude ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> buscarPEPs() {
        return personaRepository.findByEsPEPTrue().stream()
                .map(personaMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> buscarSancionados() {
        return personaRepository.findByEsSancionadoTrue().stream()
                .map(personaMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> buscarConCuentasBloqueadas() {
        return personaRepository.findPersonasConCuentasBloqueadas().stream()
                .map(personaMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> buscarConDispositivosSospechosos() {
        return personaRepository.findPersonasConDispositivosSospechosos().stream()
                .map(personaMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> buscarPersonasQueCompartenDispositivo(String personaId) {
        if (!personaRepository.existsById(personaId)) {
            throw new ResourceNotFoundException("Persona", "id", personaId);
        }
        return personaRepository.findPersonasQueCompartenDispositivo(personaId).stream()
                .map(personaMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonaResponseDto> buscarConNivelRiesgoMinimo(Integer umbral) {
        return personaRepository.findByNivelRiesgoGreaterThanEqual(umbral).stream()
                .map(personaMapper::toDto).toList();
    }

    // ─── Grafo ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CaminoGrafoDto caminoMasCorto(String idOrigen, String idDestino) {
        if (!personaRepository.existsById(idOrigen)) {
            throw new ResourceNotFoundException("Persona", "id", idOrigen);
        }
        if (!personaRepository.existsById(idDestino)) {
            throw new ResourceNotFoundException("Persona", "id", idDestino);
        }

        Map<String, Object> resultado = personaRepository
                .findCaminoMasCortoEntrePersonas(idOrigen, idDestino)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe camino entre la persona '" + idOrigen + "' y '" + idDestino + "'"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodosRaw = (List<Map<String, Object>>) resultado.get("nodos");
        int saltos = ((Number) resultado.get("saltos")).intValue();

        List<NodoGrafoDto> nodos = nodosRaw.stream()
                .map(n -> NodoGrafoDto.builder()
                        .id(String.valueOf(n.get("id")))
                        .etiqueta(String.valueOf(n.get("etiqueta")))
                        .build())
                .toList();

        return CaminoGrafoDto.builder()
                .origenId(idOrigen)
                .destinoId(idDestino)
                .saltos(saltos)
                .nodos(nodos)
                .build();
    }
}
