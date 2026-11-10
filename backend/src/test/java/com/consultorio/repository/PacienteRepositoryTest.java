package com.consultorio.repository;

import com.consultorio.entity.Paciente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 Las consultas de IPacienteRepository y la restriccion UNIQUE del DNI contra la base de verdad (H2 en memoria).
 Los nombres de prueba son raros a proposito ("Xilofon", "Zarzamora") para que no se confundan con los pacientes de demo
 */
@SpringBootTest
@ActiveProfiles("demo")
@Transactional
class PacienteRepositoryTest {

    @Autowired
    private IPacienteRepository pacienteRepository;

    // ------------------------------------------------ busqueda ------------------------------------------------

    @Test
    void buscaPorNombreSinImportarMayusculasNiMinusculas() {
        guardar("Xilofon", "Pereyra", "41000001");

        assertThat(pacienteRepository.buscarPorTexto("xilof")).hasSize(1);
        assertThat(pacienteRepository.buscarPorTexto("XILOFON")).hasSize(1);
    }

    @Test
    void buscaPorApellido() {
        guardar("Ana", "Zarzamora", "41000002");

        List<Paciente> encontrados = pacienteRepository.buscarPorTexto("zarza");

        assertThat(encontrados).extracting(Paciente::getApellido).containsExactly("Zarzamora");
    }

    @Test
    void buscaPorUnPedazoDelDni() {
        guardar("Ana", "Perez", "41000003");

        assertThat(pacienteRepository.buscarPorTexto("1000003")).hasSize(1);
    }

    @Test
    void sinCoincidenciasDevuelveUnaListaVacia() {
        assertThat(pacienteRepository.buscarPorTexto("texto-que-no-existe-en-ningun-paciente")).isEmpty();
    }

    // la busqueda sale ordenada por apellido y despues por nombre
    @Test
    void laBusquedaSaleOrdenadaPorApellidoYNombre() {
        guardar("Beto", "Ordenado", "41000004");
        guardar("Ana", "Ordenado", "41000005");
        guardar("Carlos", "Alfa-Ordenado", "41000006");

        List<Paciente> encontrados = pacienteRepository.buscarPorTexto("ordenado");

        assertThat(encontrados).extracting(Paciente::getNombre).containsExactly("Carlos", "Ana", "Beto");
    }

    @Test
    void listaTodosOrdenadosPorApellidoYNombre() {
        guardar("Beto", "Zzz-Ultimo", "41000007");

        List<Paciente> todos = pacienteRepository.findAllByOrderByApellidoAscNombreAsc();

        assertThat(todos).isNotEmpty();
        assertThat(todos.get(todos.size() - 1).getApellido()).isEqualTo("Zzz-Ultimo");
    }

    // ------------------------------------------------ DNI ------------------------------------------------

    @Test
    void sabeSiUnDniYaExiste() {
        guardar("Ana", "Perez", "41000008");

        assertThat(pacienteRepository.existsByDni("41000008")).isTrue();
        assertThat(pacienteRepository.existsByDni("41999999")).isFalse();
    }

    // al editar, el DNI puede ser el del propio paciente pero no el de otro
    @Test
    void alEditarElDniDelPropioPacienteNoCuentaComoRepetido() {
        Paciente ana = guardar("Ana", "Perez", "41000009");
        Paciente beto = guardar("Beto", "Gomez", "41000010");

        assertThat(pacienteRepository.existsByDniAndIdPacienteNot("41000009", ana.getIdPaciente())).isFalse();
        assertThat(pacienteRepository.existsByDniAndIdPacienteNot("41000009", beto.getIdPaciente())).isTrue();
    }

    /*
     Red de seguridad: aunque dos pedidos simultaneos pasen la validacion del service, la base rechaza el segundo DNI repetido.
     El ManejadorGlobalExcepciones convierte esta excepcion en un 409
     */
    @Test
    void laBaseRechazaDosPacientesConElMismoDni() {
        guardar("Ana", "Perez", "41000011");

        assertThatThrownBy(() -> guardar("Beto", "Gomez", "41000011"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------ ayudas ------------------------------------------------

    private Paciente guardar(String nombre, String apellido, String dni) {
        Paciente paciente = new Paciente();
        paciente.setNombre(nombre);
        paciente.setApellido(apellido);
        paciente.setDni(dni);
        paciente.setTelefono("223 555-0100");
        paciente.setDireccion("Calle 1");
        paciente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        return pacienteRepository.saveAndFlush(paciente);
    }
}
