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
import java.util.List;
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
    void teamMemberCanCreateTask() {
        Instant dueDate = Instant.now().plusSeconds(3600);
        when(auth.current(1L)).thenReturn(creator);
        when(equipos.findById(10L)).thenReturn(Optional.of(team));
        when(tareas.save(any(Tarea.class))).thenAnswer(invocation -> {
            Tarea saved = invocation.getArgument(0);
            saved.setId(21L);
            return saved;
        });

        TaskResponseDto response = service.create(
                1L,
                new TaskRequestDto("Nueva tarea", "Descripción", dueDate, 10L));

        assertEquals(21L, response.id());
        assertEquals("Nueva tarea", response.titulo());
        assertEquals(10L, response.equipoId());
        verify(auth).memberOrManager(creator, team);
        verify(tareas).save(any(Tarea.class));
    }

    @Test
    void memberCanConsultActiveTasks() {
        when(auth.current(1L)).thenReturn(creator);
        when(equipos.findAll()).thenReturn(List.of(team));
        when(auth.isMember(1L, team)).thenReturn(true);
        when(tareas.findByEquipoIdInAndActivaTrue(List.of(10L))).thenReturn(List.of(task));

        var response = service.list(1L);

        assertEquals(1, response.size());
        assertEquals(20L, response.get(0).id());
        assertEquals("Review API", response.get(0).titulo());
    }

    @Test
    void memberCanConsultTaskDetails() {
        when(tareas.findById(20L)).thenReturn(Optional.of(task));
        when(auth.current(1L)).thenReturn(creator);

        TaskResponseDto response = service.get(1L, 20L);

        assertEquals(20L, response.id());
        assertEquals("Review API", response.titulo());
        assertEquals(10L, response.equipoId());
        verify(auth).memberOrManager(creator, team);
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
    void assignedUserCanCompleteTask() {
        Usuario assignee = user(2L, Rol.USUARIO_NORMAL);
        when(tareas.findById(20L)).thenReturn(Optional.of(task));
        when(auth.current(2L)).thenReturn(assignee);
        when(tareas.save(task)).thenReturn(task);

        TaskResponseDto response = service.complete(2L, 20L);

        assertEquals(EstadoTarea.COMPLETADA, response.estado());
        verify(auth).assignedOrCreator(assignee, task);
        verify(tareas).save(task);
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

    @Test
    void authorizedUserCanEditTask() {
        Instant dueDate = Instant.now().plusSeconds(7200);
        when(tareas.findById(20L)).thenReturn(Optional.of(task));
        when(auth.current(1L)).thenReturn(creator);
        when(tareas.save(task)).thenReturn(task);

        TaskResponseDto response = service.update(
                1L,
                20L,
                new TaskRequestDto("Updated task", "Updated description", dueDate, 10L));

        assertEquals("Updated task", response.titulo());
        assertEquals("Updated description", response.descripcion());
        assertEquals(dueDate, response.fechaVencimiento());
        verify(auth).assignedOrCreator(creator, task);
        verify(tareas).save(task);
    }

    @Test
    void authorizedUserCanDeleteTask() {
        when(tareas.findById(20L)).thenReturn(Optional.of(task));
        when(auth.current(1L)).thenReturn(creator);

        service.delete(1L, 20L);

        assertFalse(task.isActiva());
        verify(auth).assignedOrCreator(creator, task);
        verify(tareas).save(task);
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
