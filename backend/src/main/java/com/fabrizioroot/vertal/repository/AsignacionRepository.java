package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {
    boolean existsByTareaIdAndUsuarioAsignadoId(Long tareaId, Long usuarioId);
    List<Asignacion> findByTareaId(Long tareaId);
}