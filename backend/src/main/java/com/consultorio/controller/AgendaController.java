package com.consultorio.controller;

import com.consultorio.config.AgendaProperties;
import com.consultorio.dto.response.AgendaConfiguracionDTO;
import com.consultorio.dto.response.ResumenResponseDTO;
import com.consultorio.service.ResumenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agenda")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaProperties agenda;
    private final ResumenService resumenService;

    // el front dibuja el calendario con ESTE horario: si cambia la configuracion del back, cambia el front solo
    @GetMapping("/configuracion")
    public ResponseEntity<AgendaConfiguracionDTO> obtenerConfiguracion() {

        AgendaConfiguracionDTO configuracion = new AgendaConfiguracionDTO(
                agenda.horaInicio(),
                agenda.horaFin(),
                agenda.diasLaborales().stream().sorted().toList(), // sorted: lunes primero (orden natural del enum DayOfWeek)
                agenda.duracionesPermitidas(),
                agenda.intervaloMinutos());

        return ResponseEntity.ok(configuracion);
    }

    // GET /api/agenda/resumen -> numeros de las tarjetas de la agenda
    @GetMapping("/resumen")
    public ResponseEntity<ResumenResponseDTO> obtenerResumen() {
        return ResponseEntity.ok(resumenService.obtenerResumen());
    }
}
