package com.fabrizioroot.vertal.repository;
import com.fabrizioroot.vertal.model.Recordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {
    List<Recordatorio> findByTareaIdAndActivoTrue(Long tareaId);
}