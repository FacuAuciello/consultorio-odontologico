package com.consultorio.service;

import com.consultorio.dto.request.ActualizarHistoriaClinicaDTO;
import com.consultorio.dto.request.PacienteRequestDTO;
import com.consultorio.dto.response.PacienteResponseDTO;
import com.consultorio.dto.response.PacienteResumenResponseDTO;
import com.consultorio.entity.Paciente;
import com.consultorio.exception.DniYaRegistradoException;
import com.consultorio.exception.PacienteNoEncontradoException;
import com.consultorio.mapper.PacienteMapper;
import com.consultorio.repository.IPacienteRepository;
import com.consultorio.repository.ITurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/*
 @Transactional(readOnly = true) a nivel de clase: todos los metodos corren dentro de una transaccion de SOLO LECTURA
 (mas liviana para la base). Los metodos que escriben se marcan con @Transactional a secas, que pisa a la de la clase.
 Una transaccion tambien garantiza "todo o nada": si algo falla a mitad de camino se deshace lo que se hizo
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PacienteService {

    private final IPacienteRepository pacienteRepository;
    private final ITurnoRepository turnoRepository;

    @Transactional
    public PacienteResponseDTO crearPaciente(PacienteRequestDTO dto) {

        validarDniDisponible(dto.getDni().trim(), null);

        Paciente paciente = new Paciente();
        PacienteMapper.copiarDatos(dto, paciente);

        return PacienteMapper.aResponse(pacienteRepository.save(paciente));
    }

    /*
     Lista los pacientes; si llega un texto de busqueda filtra por nombre, apellido o DNI.
     Esto es Java funcional con Optional (una "caja" que puede tener o no un valor):
       ofNullable  -> envuelve el texto (puede ser null)
       map(trim)   -> si hay texto, le saca los espacios
       filter      -> descarta el texto si quedo vacio. Predicate.not(String::isEmpty) = "no esta vacio"
       map(buscar) -> si sobrevivio, busca en la base con ese texto
       orElseGet   -> si NO hay texto (cualquiera de los pasos anteriores lo descarto), trae todos
     En vez de un if/else anidado queda una sola cadena que se lee de arriba hacia abajo
     */
    public List<PacienteResumenResponseDTO> listarPacientes(String busqueda) {

        List<Paciente> pacientes = Optional.ofNullable(busqueda)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .map(pacienteRepository::buscarPorTexto)
                .orElseGet(pacienteRepository::findAllByOrderByApellidoAscNombreAsc);

        // stream + referencia a metodo: convierte cada Paciente en su DTO sin escribir un for
        return pacientes.stream()
                .map(PacienteMapper::aResumen)
                .toList();
    }

    public PacienteResponseDTO obtenerPacientePorId(Long idPaciente) {
        return PacienteMapper.aResponse(buscarEntidad(idPaciente));
    }

    @Transactional
    public PacienteResponseDTO editarPaciente(Long idPaciente, PacienteRequestDTO dto) {

        Paciente paciente = buscarEntidad(idPaciente);

        // el DNI puede quedar igual (es el del propio paciente), pero no puede coincidir con el de OTRO
        validarDniDisponible(dto.getDni().trim(), idPaciente);

        PacienteMapper.copiarDatos(dto, paciente);

        // no hace falta llamar a save(): el paciente esta "gestionado" por JPA dentro de la transaccion
        // y Hibernate detecta los cambios y los guarda solo al terminar el metodo (dirty checking)
        return PacienteMapper.aResponse(paciente);
    }

    @Transactional
    public PacienteResponseDTO actualizarHistoriaClinica(Long idPaciente, ActualizarHistoriaClinicaDTO dto) {

        Paciente paciente = buscarEntidad(idPaciente);

        paciente.setHistoriaClinica(dto.getHistoriaClinica());

        return PacienteMapper.aResponse(paciente);
    }

    /*
     Elimina al paciente y TAMBIEN todos sus turnos (igual que la version anterior).
     Los turnos se borran primero porque tienen una clave foranea hacia el paciente.
     Gracias a @Transactional, si fallara el borrado del paciente se deshace tambien el de los turnos
     */
    @Transactional
    public void eliminarPaciente(Long idPaciente) {

        if (!pacienteRepository.existsById(idPaciente)) {
            throw new PacienteNoEncontradoException();
        }

        turnoRepository.deleteByPacienteIdPaciente(idPaciente);
        pacienteRepository.deleteById(idPaciente);
    }

    // ---- metodos privados: solo se usan dentro de esta clase ----

    private Paciente buscarEntidad(Long idPaciente) {
        return pacienteRepository.findById(idPaciente)
                .orElseThrow(PacienteNoEncontradoException::new);
    }

    // idPropio es null cuando se esta CREANDO (todavia no tiene id); en una edicion es el id del paciente que se modifica
    private void validarDniDisponible(String dni, Long idPropio) {

        boolean yaExiste = (idPropio == null)
                ? pacienteRepository.existsByDni(dni)
                : pacienteRepository.existsByDniAndIdPacienteNot(dni, idPropio);

        if (yaExiste) {
            throw new DniYaRegistradoException();
        }
    }
}
