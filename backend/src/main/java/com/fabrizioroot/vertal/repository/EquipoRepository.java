package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    List<Equipo> findDistinctByOrganizacionIdAndActivoTrueAndIdIn(Long organizacionId, List<Long> ids);
}