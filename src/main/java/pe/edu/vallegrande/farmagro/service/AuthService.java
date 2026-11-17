package pe.edu.vallegrande.farmagro.service;

import pe.edu.vallegrande.farmagro.dto.AuthResponseDTO;
import pe.edu.vallegrande.farmagro.dto.LoginRequestDTO;
import pe.edu.vallegrande.farmagro.dto.RegisterRequestDTO;
import reactor.core.publisher.Mono;


public interface AuthService {


    Mono<AuthResponseDTO> login(LoginRequestDTO dto);


    Mono<AuthResponseDTO> register(RegisterRequestDTO dto);
}
