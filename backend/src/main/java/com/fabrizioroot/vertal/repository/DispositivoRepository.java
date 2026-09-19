package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    Optional<Dispositivo> findByIdentificadorDispositivoAndActivoTrue(String identificadorDispositivo);
    boolean existsByUsuarioIdAndActivoTrue(Long usuarioId);
}