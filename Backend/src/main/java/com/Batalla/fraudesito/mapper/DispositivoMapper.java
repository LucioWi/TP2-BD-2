package com.batalla.fraudesito.mapper;

import com.batalla.fraudesito.domain.node.Dispositivo;
import com.batalla.fraudesito.dto.response.DispositivoResponseDto;
import org.springframework.stereotype.Component;

@Component
public class DispositivoMapper {

    public DispositivoResponseDto toDto(Dispositivo dispositivo) {
        return DispositivoResponseDto.builder()
                .id(dispositivo.getId())
                .tipoDispositivo(dispositivo.getTipoDispositivo())
                .marca(dispositivo.getMarca())
                .modelo(dispositivo.getModelo())
                .sistemaOperativo(dispositivo.getSistemaOperativo())
                .fingerprint(dispositivo.getFingerprint())
                .ipAddress(dispositivo.getIpAddress())
                .ipPais(dispositivo.getIpPais())
                .userAgent(dispositivo.getUserAgent())
                .ipEsProxy(dispositivo.isIpEsProxy())
                .ipEsVPN(dispositivo.isIpEsVPN())
                .ipEsTor(dispositivo.isIpEsTor())
                .esEmulador(dispositivo.isEsEmulador())
                .esRooteado(dispositivo.isEsRooteado())
                .esSospechoso(dispositivo.isEsSospechoso())
                .cantidadPersonasAsociadas(dispositivo.getCantidadPersonasAsociadas())
                .fechaRegistro(dispositivo.getFechaRegistro())
                .fechaUltimaActividad(dispositivo.getFechaUltimaActividad())
                .build();
    }
}
