package com.consultorio.controller;

import com.consultorio.dto.request.ActualizarHistoriaClinicaDTO;
import com.consultorio.dto.request.PacienteRequestDTO;
import com.consultorio.dto.response.PacienteResponseDTO;
import com.consultorio.dto.response.PacienteResumenResponseDTO;
import com.consultorio.dto.response.TurnoResponseDTO;
import com.consultorio.service.PacienteService;
import com.consultorio.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/*
 Metodos HTTP usados (convencion REST):
   POST   crear              GET    consultar
   PUT    reemplazar todo    PATCH  modificar solo una parte
   DELETE eliminar
 */
@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;
    private final TurnoService turnoService;

    // POST /api/pacientes -> 201 Created + header Location con la URL del paciente nuevo
    @PostMapping
    public ResponseEntity<PacienteResponseDTO> crearPaciente(@Valid @RequestBody PacienteRequestDTO dto) {

        PacienteResponseDTO creado = pacienteService.crearPaciente(dto);

        // arma la URL ".../api/pacientes/{id}" a partir de la URL del request actual
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.getIdPaciente())
                .toUri();

        return ResponseEntity.created(ubicacion).body(creado);
    }

    // GET /api/pacientes            -> todos
    // GET /api/pacientes?busqueda=g -> filtra por nombre, apellido o DNI (el parametro es opcional)
    @GetMapping
    public ResponseEntity<List<PacienteResumenResponseDTO>> listarPacientes(
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(pacienteService.listarPacientes(busqueda));
    }

    @GetMapping("/{idPaciente}")
    public ResponseEntity<PacienteResponseDTO> obtenerPaciente(@PathVariable Long idPaciente) {
        return ResponseEntity.ok(pacienteService.obtenerPacientePorId(idPaciente));
    }

    // GET /api/pacientes/{id}/turnos -> historial de turnos de ese paciente (el mas reciente primero)
    @GetMapping("/{idPaciente}/turnos")
    public ResponseEntity<List<TurnoResponseDTO>> listarTurnosDelPaciente(@PathVariable Long idPaciente) {
        return ResponseEntity.ok(turnoService.listarTurnosPorPaciente(idPaciente));
    }

    @PutMapping("/{idPaciente}")
    public ResponseEntity<PacienteResponseDTO> editarPaciente(
            @PathVariable Long idPaciente,
            @Valid @RequestBody PacienteRequestDTO dto) {
        return ResponseEntity.ok(pacienteService.editarPaciente(idPaciente, dto));
    }

    // PATCH: cambia solo la historia clinica
    @PatchMapping("/{idPaciente}/historia-clinica")
    public ResponseEntity<PacienteResponseDTO> actualizarHistoriaClinica(
            @PathVariable Long idPaciente,
            @Valid @RequestBody ActualizarHistoriaClinicaDTO dto) {
        return ResponseEntity.ok(pacienteService.actualizarHistoriaClinica(idPaciente, dto));
    }

    // DELETE -> 204 No Content (se elimino y no hay nada que devolver)
    @DeleteMapping("/{idPaciente}")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long idPaciente) {
        pacienteService.eliminarPaciente(idPaciente);
        return ResponseEntity.noContent().build();
    }
}
