package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.dto.MemberRequestDto;
import com.fabrizioroot.vertal.dto.TeamRequestDto;
import com.fabrizioroot.vertal.exception.ConflictException;
import com.fabrizioroot.vertal.exception.UnauthorizedOperationException;
import com.fabrizioroot.vertal.model.*;
import com.fabrizioroot.vertal.repository.*;
import com.fabrizioroot.vertal.service.AuthorizationService;
import com.fabrizioroot.vertal.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TeamServiceTests {
    private EquipoRepository equipos;
    private UsuarioRepository usuarios;
    private MembresiaEquipoRepository membresias;
    private AuthorizationService auth;
    private TeamService service;
    private Usuario creator;
    private Equipo team;

    @BeforeEach
    void setUp() {
        equipos = mock(EquipoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        membresias = mock(MembresiaEquipoRepository.class);
        auth = mock(AuthorizationService.class);
        service = new TeamService(equipos, mock(OrganizacionRepository.class), usuarios, membresias, auth);
        creator = user(1L, Rol.USUARIO_NORMAL);
        team = new Equipo();
        team.setId(10L);
        team.setNombre("Backend");
        team.setCreador(creator);
        team.setOrganizacion(creator.getOrganizacion());
    }

    @Test
    void createTeamAddsCreatorAsMember() {
        when(auth.current(1L)).thenReturn(creator);
        when(equipos.save(any(Equipo.class))).thenAnswer(invocation -> {
            Equipo saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var response = service.create(1L, new TeamRequestDto("Backend"));

        assertEquals("Backend", response.nombre());
        verify(membresias).save(any(MembresiaEquipo.class));
    }

    @Test
    void memberCanConsultTeamDetails() {
        when(auth.current(1L)).thenReturn(creator);
        when(equipos.findById(10L)).thenReturn(Optional.of(team));

        var response = service.get(1L, 10L);

        assertEquals(10L, response.id());
        assertEquals("Backend", response.nombre());
        assertEquals(1L, response.creadorId());
        verify(auth).memberOrManager(creator, team);
    }

    @Test
    void creatorCanAddNewMember() {
        Usuario member = user(2L, Rol.USUARIO_NORMAL);
        when(auth.current(1L)).thenReturn(creator);
        when(equipos.findById(10L)).thenReturn(Optional.of(team));
        when(usuarios.findByNombreUsuario("user-2")).thenReturn(Optional.of(member));
        when(membresias.existsByUsuarioIdAndEquipoIdAndActivaTrue(2L, 10L)).thenReturn(false);
        when(membresias.findByUsuarioIdAndEquipoId(2L, 10L)).thenReturn(Optional.empty());

        service.add(1L, 10L, new MemberRequestDto("user-2"));

        verify(auth).managerOrCreator(creator, team);
        verify(membresias).save(argThat(saved ->
                saved.isActiva()
                        && saved.getUsuario() == member
                        && saved.getEquipo() == team));
    }

    @Test
    void duplicateMemberIsRejected() {
        Usuario member = user(2L, Rol.USUARIO_NORMAL);
        when(auth.current(1L)).thenReturn(creator);
        when(equipos.findById(10L)).thenReturn(Optional.of(team));
        when(usuarios.findByNombreUsuario("user-2")).thenReturn(Optional.of(member));
        when(membresias.existsByUsuarioIdAndEquipoIdAndActivaTrue(2L, 10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.add(1L, 10L, new MemberRequestDto("user-2")));
        verify(membresias, never()).save(any());
    }

    @Test
    void removeMemberDeactivatesMembership() {
        Usuario member = user(2L, Rol.USUARIO_NORMAL);
        MembresiaEquipo membership = new MembresiaEquipo();
        when(auth.current(1L)).thenReturn(creator);
        when(equipos.findById(10L)).thenReturn(Optional.of(team));
        when(usuarios.findByNombreUsuario("user-2")).thenReturn(Optional.of(member));
        when(membresias.findByUsuarioIdAndEquipoId(2L, 10L)).thenReturn(Optional.of(membership));

        service.remove(1L, 10L, "user-2");

        assertFalse(membership.isActiva());
        verify(membresias).save(membership);
    }

    @Test
    void systemAdministratorCannotCreateTeam() {
        Usuario admin = user(1L, Rol.ADMINISTRADOR_SISTEMAS);
        when(auth.current(1L)).thenReturn(admin);
        doThrow(new UnauthorizedOperationException("sin permisos")).when(auth).canOperate(admin);

        assertThrows(UnauthorizedOperationException.class, () -> service.create(1L, new TeamRequestDto("Admin")));
        verify(equipos, never()).save(any());
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
