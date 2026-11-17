package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.dto.AuthResponseDTO;
import pe.edu.vallegrande.farmagro.dto.LoginRequestDTO;
import pe.edu.vallegrande.farmagro.dto.RegisterRequestDTO;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import pe.edu.vallegrande.farmagro.service.AuthService;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciales incorrectas: correo o contraseña no válidos";
    private static final String INACTIVE_USER_MESSAGE = "Usuario inactivo o suspendido en el sistema";

    private final PersonRepository personRepository;
    private final PasswordHasher passwordHasher;
    private final PersonValidator personValidator;
    private final PersonIdGenerator personIdGenerator;
    private final PersonEntityFactory personEntityFactory;
    private final AuthResponseMapper authResponseMapper;

    @Override
    public Mono<AuthResponseDTO> login(LoginRequestDTO dto) {
        String email = normalizedEmail(dto.getEmail());
        String rawPassword = Objects.requireNonNullElse(dto.getPassword(), AppConstants.EMPTY_TEXT);
        return personRepository.findByEmailIgnoreCase(email)
                .switchIfEmpty(Mono.error(invalidCredentials()))
                .flatMap(person -> validateUserStatus(person)
                        .then(verifyPassword(person, rawPassword))
                        .then(recordLogin(person))
                        .map(authResponseMapper::toAuthResponse));
    }

    @Override
    public Mono<AuthResponseDTO> register(RegisterRequestDTO dto) {
        String email = normalizedEmail(dto.getEmail());
        return personValidator.validateDocumentFormat(dto.getDocumentType(), dto.getDocumentNumber().trim())
                .then(personValidator.validateUniqueEmail(email, null))
                .then(personValidator.validateUniqueDocument(
                        dto.getDocumentType(), dto.getDocumentNumber().trim(), null))
                .then(personValidator.requireGeographicLocation(
                        PersonEntityFactory.registrationLocation(dto.getGeographicLocationId())))
                .then(personIdGenerator.insertNew(null, personId -> personEntityFactory.fromRegistration(dto, email, personId)))
                .map(authResponseMapper::toAuthResponse);
    }

    private String normalizedEmail(String email) {
        return Objects.requireNonNullElse(TextNormalizer.email(email), AppConstants.EMPTY_TEXT);
    }

    private Mono<Void> validateUserStatus(Person person) {
        return Boolean.TRUE.equals(person.getStatus())
                ? Mono.empty()
                : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, INACTIVE_USER_MESSAGE));
    }

    // Legacy plain-text passwords are upgraded to BCrypt on first login.
    private Mono<Void> verifyPassword(Person person, String rawPassword) {
        String storedPassword = person.getPasswordHash();
        if (!passwordHasher.matches(rawPassword, storedPassword)) {
            return Mono.error(invalidCredentials());
        }
        if (!passwordHasher.isHashed(storedPassword)) {
            person.setPasswordHash(passwordHasher.encode(rawPassword));
        }
        return Mono.empty();
    }

    private Mono<Person> recordLogin(Person person) {
        person.setLastLogin(LocalDateTime.now());
        return personRepository.save(person);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
    }
}
