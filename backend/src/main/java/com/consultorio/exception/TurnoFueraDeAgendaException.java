package com.consultorio.exception;

import org.springframework.http.HttpStatus;

// el turno no respeta las reglas de la agenda (dia no laboral, fuera de horario, no alineado al intervalo)
// el mensaje cambia segun la regla que se incumple, por eso se recibe por parametro
public class TurnoFueraDeAgendaException extends ConsultorioException {
    public TurnoFueraDeAgendaException(String motivo) {
        super(HttpStatus.BAD_REQUEST, motivo);
    }
}
