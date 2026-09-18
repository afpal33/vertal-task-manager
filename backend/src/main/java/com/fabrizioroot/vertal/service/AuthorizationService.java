package com.fabrizioroot.vertal.service;

import com.fabrizioroot.vertal.exception.UnauthorizedOperationException;
import com.fabrizioroot.vertal.model.*;
import com.fabrizioroot.vertal.repository.*;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
    private final UsuarioRepository usuarios; private final MembresiaEquipoRepository membresias; private final AsignacionRepository asignaciones;
    public AuthorizationService(UsuarioRepository u, MembresiaEquipoRepository m, AsignacionRepository a) { usuarios=u; membresias=m; asignaciones=a; }
    public Usuario current(Long id) { return usuarios.findById(id).filter(Usuario::isActivo).orElseThrow(() -> new UnauthorizedOperationException("Usuario no autorizado")); }
    public boolean isManager(Usuario u) { return u.getRol() == Rol.MANAGER; }
    public boolean isAdmin(Usuario u) { return u.getRol() == Rol.ADMINISTRADOR_SISTEMAS; }
    public boolean isMember(Long userId, Equipo equipo) { return equipo.getCreador().getId().equals(userId) || membresias.existsByUsuarioIdAndEquipoIdAndActivaTrue(userId, equipo.getId()); }
    public void canOperate(Usuario u) { if (isAdmin(u)) throw new UnauthorizedOperationException("El administrador de sistemas no tiene permisos operativos por defecto"); }
    public void memberOrManager(Usuario u, Equipo e) { canOperate(u); if (!isManager(u) && !e.getCreador().getId().equals(u.getId()) && !membresias.existsByUsuarioIdAndEquipoIdAndActivaTrue(u.getId(), e.getId())) throw new UnauthorizedOperationException("No pertenece al equipo"); }
    public void managerOrCreator(Usuario u, Equipo e) { canOperate(u); if (!isManager(u) && !e.getCreador().getId().equals(u.getId())) throw new UnauthorizedOperationException("Permiso insuficiente"); }
    public void assignedOrManager(Usuario u, Tarea t) { canOperate(u); if (!isManager(u) && !asignaciones.existsByTareaIdAndUsuarioAsignadoId(t.getId(), u.getId())) throw new UnauthorizedOperationException("No está asignado a la tarea"); }
}