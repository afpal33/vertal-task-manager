package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SolicitudVinculacionRepository extends JpaRepository<SolicitudVinculacion, Long> {
    List<SolicitudVinculacion> findByOrganizacionIdOrderByFechaSolicitudDesc(Long organizacionId);
}