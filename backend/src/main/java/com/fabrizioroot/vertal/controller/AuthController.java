package com.fabrizioroot.vertal.controller;
import com.fabrizioroot.vertal.dto.*; import com.fabrizioroot.vertal.model.Usuario; import com.fabrizioroot.vertal.repository.UsuarioRepository; import com.fabrizioroot.vertal.service.AuthService; import com.fabrizioroot.vertal.service.AuthorizationService; import jakarta.validation.Valid; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final AuthService auth; private final UsuarioRepository usuarios; private final AuthorizationService authorization;
 public AuthController(AuthService a,UsuarioRepository u,AuthorizationService x){auth=a;usuarios=u;authorization=x;}
 @PostMapping("/linking/request") public LinkingResponseDto request(@Valid @RequestBody LinkingRequestDto dto){return auth.request(dto);}
 @PostMapping("/challenge") public ChallengeResponseDto challenge(@Valid @RequestBody ChallengeRequestDto dto){return auth.challenge(dto);}
 @PostMapping("/login") public LoginResponseDto login(@Valid @RequestBody LoginRequestDto dto){return auth.login(dto);}
 @GetMapping("/me") public UserResponseDto me(@AuthenticationPrincipal Long id){Usuario u=authorization.current(id);return new UserResponseDto(u.getId(),u.getNombreUsuario(),u.getNombreCompleto(),u.getRol(),u.isActivo());}
}