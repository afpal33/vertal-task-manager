package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.VinculacionDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
public interface VinculacionDispositivoRepository extends JpaRepository<VinculacionDispositivo, Long> {
    Optional<VinculacionDispositivo> findByDispositivoIdentificadorDispositivoAndActivaTrue(String identificador);
    List<VinculacionDispositivo> findByUsuarioIdAndActivaTrue(Long usuarioId);
}