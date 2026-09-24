package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.Dispositivo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    @EntityGraph(attributePaths = "usuario")
    Optional<Dispositivo> findByIdentificadorDispositivoAndActivoTrue(String identificadorDispositivo);
    boolean existsByUsuarioIdAndActivoTrue(Long usuarioId);
    List<Dispositivo> findByUsuarioOrganizacionIdOrderById(Long organizacionId);
}
