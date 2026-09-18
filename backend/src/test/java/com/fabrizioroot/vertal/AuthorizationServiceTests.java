package com.fabrizioroot.vertal;

import com.fabrizioroot.vertal.exception.UnauthorizedOperationException;
import com.fabrizioroot.vertal.model.*;
import com.fabrizioroot.vertal.repository.*;
import com.fabrizioroot.vertal.service.AuthorizationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AuthorizationServiceTests {
    @Test void systemAdministratorCannotOperateTasksByDefault() {
        Usuario admin = new Usuario(); admin.setRol(Rol.ADMINISTRADOR_SISTEMAS);
        AuthorizationService service = new AuthorizationService(mock(UsuarioRepository.class), mock(MembresiaEquipoRepository.class), mock(AsignacionRepository.class));
        assertThrows(UnauthorizedOperationException.class, () -> service.canOperate(admin));
    }
}