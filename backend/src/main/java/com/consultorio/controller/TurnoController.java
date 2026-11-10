package com.consultorio.controller;

import com.consultorio.dto.request.TurnoRequestDTO;
import com.consultorio.dto.response.TurnoResponseDTO;
import com.consultorio.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @PostMapping
    public ResponseEntity<TurnoResponseDTO> crearTurno(@Valid @RequestBody TurnoRequestDTO dto) {

        TurnoResponseDTO creado = turnoService.crearTurno(dto);

        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.getIdTurno())
                .toUri();

        return ResponseEntity.created(ubicacion).body(creado);
    }

    /*
     GET /api/turnos?desde=2026-03-16&hasta=2026-03-20 (ambos opcionales, ambos inclusivos)
     @DateTimeFormat(iso = DATE) le indica a Spring que el texto "2026-03-16" se convierte a LocalDate
     */
    @GetMapping
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(turnoService.listarTurnos(desde, hasta));
    }

    // GET /api/turnos/hoy. Spring prioriza la ruta fija "/hoy" sobre "/{idTurno}", no se confunden
    @GetMapping("/hoy")
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnosDeHoy() {
        return ResponseEntity.ok(turnoService.listarTurnosDeHoy());
    }

    @GetMapping("/{idTurno}")
    public ResponseEntity<TurnoResponseDTO> obtenerTurno(@PathVariable Long idTurno) {
        return ResponseEntity.ok(turnoService.obtenerTurnoPorId(idTurno));
    }

    @PutMapping("/{idTurno}")
    public ResponseEntity<TurnoResponseDTO> editarTurno(
            @PathVariable Long idTurno,
            @Valid @RequestBody TurnoRequestDTO dto) {
        return ResponseEntity.ok(turnoService.editarTurno(idTurno, dto));
    }

    @DeleteMapping("/{idTurno}")
    public ResponseEntity<Void> eliminarTurno(@PathVariable Long idTurno) {
        turnoService.eliminarTurno(idTurno);
        return ResponseEntity.noContent().build();
    }
}
