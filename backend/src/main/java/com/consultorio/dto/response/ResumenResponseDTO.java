package com.consultorio.dto.response;

/*
 Numeros del panel de la agenda. proximoTurno es null cuando no hay ningun turno por delante
 */
public record ResumenResponseDTO(
        long turnosHoy,
        long turnosSemana,
        long totalPacientes,
        TurnoResponseDTO proximoTurno
) {
}
