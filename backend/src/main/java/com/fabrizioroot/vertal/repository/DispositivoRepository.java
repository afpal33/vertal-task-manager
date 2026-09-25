package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.Dispositivo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    @Override
    @EntityGraph(attributePaths = "usuario")
    Optional<Dispositivo> findById(Long id);

    Optional<Dispositivo> findByIdentificadorDispositivoAndActivoTrue(String identificadorDispositivo);
    boolean existsByUsuarioIdAndActivoTrue(Long usuarioId);
    @EntityGraph(attributePaths = "usuario")
    List<Dispositivo> findByUsuarioOrganizacionIdOrderById(Long organizacionId);
}
