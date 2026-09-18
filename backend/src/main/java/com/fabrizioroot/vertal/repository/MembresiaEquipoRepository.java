package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.MembresiaEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface MembresiaEquipoRepository extends JpaRepository<MembresiaEquipo, Long> {
    Optional<MembresiaEquipo> findByUsuarioIdAndEquipoId(Long usuarioId, Long equipoId);
    List<MembresiaEquipo> findByUsuarioIdAndActivaTrue(Long usuarioId);
    boolean existsByUsuarioIdAndEquipoIdAndActivaTrue(Long usuarioId, Long equipoId);
}