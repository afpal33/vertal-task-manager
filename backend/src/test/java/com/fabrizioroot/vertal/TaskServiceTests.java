package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.dto.*;
import com.fabrizioroot.vertal.exception.BadRequestException;
import com.fabrizioroot.vertal.exception.ConflictException;
import com.fabrizioroot.vertal.exception.UnauthorizedOperationException;
import com.fabrizioroot.vertal.model.*;
import com.fabrizioroot.vertal.repository.*;
import com.fabrizioroot.vertal.service.AuthorizationService;
import com.fabrizioroot.vertal.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskServiceTests {
    private TareaRepository tareas;
    private EquipoRepository equipos;
    private UsuarioRepository usuarios;
    private AsignacionRepository asignaciones;
    private RecordatorioRepository recordatorios;
    private AuthorizationService auth;
    private TaskService service;
    private Usuario creator;
    private Equipo team;
    private Tarea task;

    @BeforeEach
    void setUp() {
        tareas = mock(TareaRepository.class);
        equipos = mock(EquipoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        asignaciones = mock(AsignacionRepository.class);
        recordatorios = mock(RecordatorioRepository.class);
        auth = mock(AuthorizationService.class);
        service = new TaskService(tareas, equipos, usuarios, asignaciones, recordatorios, auth);
        creator = user(1L, Rol.USUARIO_NORMAL);
        team = new Equipo();
        team.setId(10L);
        team.setCreador(creator);
        team.setOrganizacion(creator.getOrganizacion());
        task = new Tarea();
        task.setId(20L);
        task.setTitulo("Review API");
        task.setEquipo(team);
        task.setCreador(creator);
        task.setActiva(true);
    }

    @Test
    void createTaskRequiresTeamMembership() {
        when(auth.current(1L)).thenReturn(creator);
        when(equipos.findById(10L)).thenReturn(Optional.of(team));
        doThrow(new UnauthorizedOperationException("no pertenece")).when(auth).memberOrManager(creator, team);

        assertThrows(UnauthorizedOperationException.class, () -> service.create(1L, new TaskRequestDto("Task", "Description", null, 10L)));
        verify(tareas, never()).save(any());
    }

    @Test
    void assignmentRequiresTeamMemberAndPersistsAssignment() {
        Usuario assignee = user(2L, Rol.USUARIO_NORMAL);
        when(tareas.findById(20L)).thenReturn(Optional.of(task));
        when(auth.current(1L)).thenReturn(creator);
        when(usuarios.findByNombreUsuario("user-2")).thenReturn(Optional.of(assignee));
        when(auth.isMember(2L, team)).thenReturn(true);
        when(asignaciones.existsByTareaIdAndUsuarioAsignadoId(20L, 2L)).thenReturn(false);

        service.assign(1L, 20L, new AssignmentRequestDto("user-2"));

        verify(asignaciones).save(any(Asignacion.class));
    }

    @Test
    void duplicateAssignmentIsRejected() {
        Usuario assignee = user(2L, Rol.USUARIO_NORMAL);
        when(tareas.findById(20L)).thenReturn(Optional.of(task));
        when(auth.current(1L)).thenReturn(creator);
        when(usuarios.findByNombreUsuario("user-2")).thenReturn(Optional.of(assignee));
        when(auth.isMember(2L, team)).thenReturn(true);
        when(asignaciones.existsByTareaIdAndUsuarioAsignadoId(20L, 2L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.assign(1L, 20L, new AssignmentRequestDto("user-2")));
        verify(asignaciones, never()).save(any());
    }

    @Test
    void assignedUserCanUpdateStatus() {
        when(tareas.findById(20L)).thenReturn(Optional.of(task));
        when(auth.current(2L)).thenReturn(user(2L, Rol.USUARIO_NORMAL));
        when(tareas.save(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponseDto response = service.status(2L, 20L, new StatusUpdateRequestDto(EstadoTarea.EN_PROGRESO));

        assertEquals(EstadoTarea.EN_PROGRESO, response.estado());
        verify(auth).assignedOrCreator(any(Usuario.class), eq(task));
    }

    @Test
    void reminderIsStoredForAuthorizedUser() {
        Usuario assignee = user(2L, Rol.USUARIO_NORMAL);
        when(tareas.findById(20L)).thenReturn(Optional.of(task));
        when(auth.current(2L)).thenReturn(assignee);
        when(recordatorios.save(any(Recordatorio.class))).thenAnswer(invocation -> {
            Recordatorio reminder = invocation.getArgument(0);
            reminder.setId(30L);
            return reminder;
        });

        ReminderResponseDto response = service.reminder(2L, 20L, new ReminderRequestDto(Instant.now().plusSeconds(3600)));

        assertEquals(30L, response.id());
        assertEquals(20L, response.tareaId());
        verify(recordatorios).save(any(Recordatorio.class));
    }

    @Test
    void assignmentToNonMemberIsRejected() {
        Usuario assignee = user(2L, Rol.USUARIO_NORMAL);
        when(tareas.findById(20L)).thenReturn(Optional.of(task));
        when(auth.current(1L)).thenReturn(creator);
        when(usuarios.findByNombreUsuario("user-2")).thenReturn(Optional.of(assignee));
        when(auth.isMember(2L, team)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.assign(1L, 20L, new AssignmentRequestDto("user-2")));
    }

    private Usuario user(Long id, Rol role) {
        Usuario user = new Usuario();
        user.setId(id);
        user.setNombreUsuario("user-" + id);
        user.setNombreCompleto("User " + id);
        user.setRol(role);
        user.setActivo(true);
        Organizacion organization = new Organizacion();
        organization.setId(1L);
        user.setOrganizacion(organization);
        return user;
    }
}