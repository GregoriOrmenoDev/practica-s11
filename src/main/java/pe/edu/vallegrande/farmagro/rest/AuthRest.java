package pe.edu.vallegrande.farmagro.rest;

import pe.edu.vallegrande.farmagro.dto.AuthResponseDTO;
import pe.edu.vallegrande.farmagro.dto.LoginRequestDTO;
import pe.edu.vallegrande.farmagro.dto.RegisterRequestDTO;
import pe.edu.vallegrande.farmagro.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthRest {

    private final AuthService authService;

    @PostMapping("/login")
    public Mono<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return authService.login(dto);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        return authService.register(dto);
    }
}
