package com.consultorio.repository;

import com.consultorio.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
 Spring Data arma la consulta a partir del NOMBRE del metodo (existsByDni -> "select count(*) > 0 where dni = ?"),
 asi que no hace falta escribir SQL para lo simple. Para lo que no se puede expresar con el nombre se usa @Query (JPQL)
 */
public interface IPacienteRepository extends JpaRepository<Paciente, Long> {

    // para validar el DNI al CREAR un paciente
    boolean existsByDni(String dni);

    // para validar el DNI al EDITAR: puede coincidir con el del propio paciente, pero no con el de otro
    boolean existsByDniAndIdPacienteNot(String dni, Long idPaciente);

    List<Paciente> findAllByOrderByApellidoAscNombreAsc();

    // busca por nombre, apellido o DNI sin distinguir mayusculas. El % es el comodin de LIKE ("contiene")
    @Query("""
            select p from Paciente p
            where lower(p.nombre) like lower(concat('%', :texto, '%'))
               or lower(p.apellido) like lower(concat('%', :texto, '%'))
               or p.dni like concat('%', :texto, '%')
            order by p.apellido, p.nombre
            """)
    List<Paciente> buscarPorTexto(@Param("texto") String texto);
}
