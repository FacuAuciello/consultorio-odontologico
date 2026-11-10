package com.consultorio.service;

import com.consultorio.dto.request.PacienteRequestDTO;
import com.consultorio.entity.Paciente;
import com.consultorio.exception.DniYaRegistradoException;
import com.consultorio.exception.PacienteNoEncontradoException;
import com.consultorio.repository.IPacienteRepository;
import com.consultorio.repository.ITurnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 Test unitario: se prueba SOLO PacienteService. Los repositorios son "mocks" (dobles falsos) a los que se les
 indica que responder, asi el test no necesita base de datos y corre en milisegundos
 */
@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private IPacienteRepository pacienteRepository;

    @Mock
    private ITurnoRepository turnoRepository;

    @InjectMocks
    private PacienteService pacienteService;

    private PacienteRequestDTO pedidoValido() {
        return new PacienteRequestDTO("Ana", "Pérez", "30123456", "223 555-0000", "Calle 123", LocalDate.of(1990, 5, 20));
    }

    @Test
    void creaUnPacienteConSusDatosLimpios() {
        when(pacienteRepository.existsByDni("30123456")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = pacienteService.crearPaciente(pedidoValido());

        assertThat(respuesta.getNombre()).isEqualTo("Ana");
        assertThat(respuesta.getDni()).isEqualTo("30123456");
    }

    @Test
    void noPermiteCrearUnPacienteConUnDniYaRegistrado() {
        when(pacienteRepository.existsByDni("30123456")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.crearPaciente(pedidoValido()))
                .isInstanceOf(DniYaRegistradoException.class);

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void sinTextoDeBusquedaListaATodos() {
        when(pacienteRepository.findAllByOrderByApellidoAscNombreAsc()).thenReturn(List.of(new Paciente()));

        var resultado = pacienteService.listarPacientes("   ");

        assertThat(resultado).hasSize(1);
        verify(pacienteRepository, never()).buscarPorTexto(any());
    }

    @Test
    void conTextoDeBusquedaFiltraSinEspaciosSobrantes() {
        when(pacienteRepository.buscarPorTexto("gomez")).thenReturn(List.of());

        pacienteService.listarPacientes("  gomez ");

        verify(pacienteRepository).buscarPorTexto("gomez");
    }

    @Test
    void alEliminarUnPacienteBorraPrimeroSusTurnos() {
        when(pacienteRepository.existsById(7L)).thenReturn(true);

        pacienteService.eliminarPaciente(7L);

        // InOrder verifica el ORDEN de las llamadas: si se borrara primero el paciente, la clave foranea de los turnos fallaria
        InOrder orden = inOrder(turnoRepository, pacienteRepository);
        orden.verify(turnoRepository).deleteByPacienteIdPaciente(7L);
        orden.verify(pacienteRepository).deleteById(7L);
    }

    @Test
    void eliminarUnPacienteInexistenteDaError() {
        when(pacienteRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> pacienteService.eliminarPaciente(99L))
                .isInstanceOf(PacienteNoEncontradoException.class);

        verify(turnoRepository, never()).deleteByPacienteIdPaciente(any());
    }
}
