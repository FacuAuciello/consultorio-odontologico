package com.consultorio.mapper;

import com.consultorio.dto.response.TurnoResponseDTO;
import com.consultorio.entity.Turno;

public final class TurnoMapper {

    private TurnoMapper() {
    }

    // ojo: turno.getPaciente() es LAZY, asi que esto se tiene que llamar dentro de un @Transactional (los services lo cumplen)
    public static TurnoResponseDTO aResponse(Turno turno) {
        return TurnoResponseDTO.builder()
                .idTurno(turno.getIdTurno())
                .fechaHora(turno.getFechaHora())
                .fechaHoraFin(turno.getFechaHoraFin())
                .duracionMinutos(turno.getDuracionMinutos())
                .monto(turno.getMonto())
                .notasOdontologicas(turno.getNotasOdontologicas())
                .idPaciente(turno.getPaciente().getIdPaciente())
                .nombrePaciente(turno.getPaciente().getNombre())
                .apellidoPaciente(turno.getPaciente().getApellido())
                .telefonoPaciente(turno.getPaciente().getTelefono())
                .build();
    }
}
