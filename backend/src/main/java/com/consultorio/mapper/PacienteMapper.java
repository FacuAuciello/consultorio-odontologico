package com.consultorio.mapper;

import com.consultorio.dto.request.PacienteRequestDTO;
import com.consultorio.dto.response.PacienteResponseDTO;
import com.consultorio.dto.response.PacienteResumenResponseDTO;
import com.consultorio.entity.Paciente;

/*
 Convierte entre entidades y DTOs. Si esa conversion se escribiera dentro de cada metodo del service quedaria el mismo bloque repetido varias veces;
 aca vive en UN lugar. Son metodos estaticos para poder usarlos como referencia a metodo en los streams:
     pacientes.stream().map(PacienteMapper::aResumen).toList()
 */
public final class PacienteMapper {

    // clase de utilidad: no se instancia
    private PacienteMapper() {
    }

    public static PacienteResumenResponseDTO aResumen(Paciente paciente) {
        return PacienteResumenResponseDTO.builder()
                .idPaciente(paciente.getIdPaciente())
                .nombre(paciente.getNombre())
                .apellido(paciente.getApellido())
                .dni(paciente.getDni())
                .telefono(paciente.getTelefono())
                .direccion(paciente.getDireccion())
                .fechaNacimiento(paciente.getFechaNacimiento())
                .build();
    }

    public static PacienteResponseDTO aResponse(Paciente paciente) {
        return PacienteResponseDTO.builder()
                .idPaciente(paciente.getIdPaciente())
                .nombre(paciente.getNombre())
                .apellido(paciente.getApellido())
                .dni(paciente.getDni())
                .telefono(paciente.getTelefono())
                .direccion(paciente.getDireccion())
                .fechaNacimiento(paciente.getFechaNacimiento())
                .historiaClinica(paciente.getHistoriaClinica())
                .build();
    }

    // copia los datos del DTO a la entidad (sirve tanto para un paciente nuevo como para uno que se edita). trim() saca espacios sobrantes
    public static void copiarDatos(PacienteRequestDTO dto, Paciente destino) {
        destino.setNombre(dto.getNombre().trim());
        destino.setApellido(dto.getApellido().trim());
        destino.setDni(dto.getDni().trim());
        destino.setTelefono(dto.getTelefono().trim());
        destino.setDireccion(dto.getDireccion().trim());
        destino.setFechaNacimiento(dto.getFechaNacimiento());
    }
}
