package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    List<Tarea> findByEquipoIdAndActivaTrue(Long equipoId);
    List<Tarea> findByEquipoIdInAndActivaTrue(List<Long> equipoIds);
}