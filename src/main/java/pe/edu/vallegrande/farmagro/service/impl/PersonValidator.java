package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.vallegrande.farmagro.dto.CustomerDTO;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import pe.edu.vallegrande.farmagro.repository.sql.GeographicLocationRepository;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.TextNormalizer;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
final class PersonValidator {

    static final int DEFAULT_GEOGRAPHIC_LOCATION_ID = 1;

    private static final int DNI_LENGTH = 8;
    private static final int RUC_LENGTH = 11;
    private static final int RUC_PREFIX_LENGTH = 2;
    private static final int MIN_ALPHANUMERIC_DOCUMENT_LENGTH = 4;
    private static final int MAX_ALPHANUMERIC_DOCUMENT_LENGTH = 12;
    private static final String RUC_ALLOWED_PREFIX_PATTERN = "(10|15|17|20)";
    private static final String DNI_PATTERN = "^[0-9]{" + DNI_LENGTH + "}$";
    private static final String RUC_PATTERN = "^" + RUC_ALLOWED_PREFIX_PATTERN
            + "[0-9]{" + (RUC_LENGTH - RUC_PREFIX_LENGTH) + "}$";
    private static final String ALPHANUMERIC_DOCUMENT_PATTERN = "^[A-Za-z0-9]{"
            + MIN_ALPHANUMERIC_DOCUMENT_LENGTH + "," + MAX_ALPHANUMERIC_DOCUMENT_LENGTH + "}$";
    private static final String DNI_FORMAT_MESSAGE =
            "El DNI debe tener exactamente " + DNI_LENGTH + " dígitos numéricos";
    private static final String RUC_FORMAT_MESSAGE =
            "El RUC debe tener " + RUC_LENGTH + " dígitos numéricos y comenzar con 10, 15, 17 o 20";
    private static final String ALPHANUMERIC_FORMAT_MESSAGE = "El número de %s debe ser alfanumérico entre "
            + MIN_ALPHANUMERIC_DOCUMENT_LENGTH + " y " + MAX_ALPHANUMERIC_DOCUMENT_LENGTH + " caracteres";
    private static final Map<String, DocumentFormatRule> DOCUMENT_FORMAT_RULES = Map.of(
            AppConstants.DOCUMENT_TYPE_DNI, new DocumentFormatRule(DNI_PATTERN, DNI_FORMAT_MESSAGE),
            AppConstants.DOCUMENT_TYPE_RUC, new DocumentFormatRule(RUC_PATTERN, RUC_FORMAT_MESSAGE),
            AppConstants.DOCUMENT_TYPE_CE, new DocumentFormatRule(ALPHANUMERIC_DOCUMENT_PATTERN,
                    ALPHANUMERIC_FORMAT_MESSAGE.formatted(AppConstants.DOCUMENT_TYPE_CE)),
            AppConstants.DOCUMENT_TYPE_PASSPORT, new DocumentFormatRule(ALPHANUMERIC_DOCUMENT_PATTERN,
                    ALPHANUMERIC_FORMAT_MESSAGE.formatted(AppConstants.DOCUMENT_TYPE_PASSPORT)));
    private static final String LAST_NAMES_REQUIRED_MESSAGE = "El apellido es obligatorio para personas naturales ("
            + AppConstants.DOCUMENT_TYPE_DNI + ", " + AppConstants.DOCUMENT_TYPE_CE + ", "
            + AppConstants.DOCUMENT_TYPE_PASSPORT + ")";

    private record DocumentFormatRule(String pattern, String violationMessage) {
    }

    private final PersonRepository personRepository;
    private final GeographicLocationRepository geographicLocationRepository;

    Mono<Void> validate(PersonDTO person, String excludedId) {
        return validateCustomerData(person)
                .then(validateDocumentFormat(person.getDocumentType(), person.getDocumentNumber()))
                .then(validateLastNames(person))
                .then(resolveGeographicLocation(person.getCustomer()))
                .then(validateUniqueEmail(person.getEmail(), excludedId))
                .then(validateUniqueDocument(person.getDocumentType(), person.getDocumentNumber(), excludedId));
    }

    Mono<Void> validateUniqueEmail(String email, String excludedId) {
        Mono<Boolean> emailTaken = excludedId == null
                ? personRepository.existsByEmailIgnoreCase(email)
                : personRepository.existsByEmailIgnoreCaseAndIdNot(email, excludedId);
        return emailTaken.flatMap(exists -> exists
                ? badRequest("Ya existe una persona con el correo " + email)
                : Mono.empty());
    }

    Mono<Void> validateUniqueDocument(String documentType, String documentNumber, String excludedId) {
        Mono<Boolean> documentTaken = excludedId == null
                ? personRepository.existsByDocumentTypeAndDocumentNumber(documentType, documentNumber)
                : personRepository.existsByDocumentTypeAndDocumentNumberAndIdNot(
                        documentType, documentNumber, excludedId);
        return documentTaken.flatMap(exists -> exists
                ? badRequest("Ya existe una persona con el documento " + documentType + " " + documentNumber)
                : Mono.empty());
    }

    Mono<Void> requireGeographicLocation(Integer geographicLocationId) {
        return geographicLocationRepository.existsById(geographicLocationId)
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : badRequest("El ubigeo " + geographicLocationId + " no existe en el catálogo"));
    }

    private Mono<Void> validateCustomerData(PersonDTO person) {
        if (person.getRole() == null) {
            return badRequest("El rol es obligatorio");
        }
        boolean isCustomer = PersonRole.CUSTOMER.equals(person.getRole());
        if (isCustomer && person.getCustomer() == null) {
            return badRequest("Los datos de cliente (dirección y ubigeo) son obligatorios para el rol cliente");
        }
        if (!isCustomer && person.getCustomer() != null) {
            return badRequest("Los datos de cliente solo se registran para personas con rol cliente");
        }
        return Mono.empty();
    }

    Mono<Void> validateDocumentFormat(String documentType, String documentNumber) {
        if (documentType == null || documentNumber == null) {
            return Mono.empty();
        }
        DocumentFormatRule rule = DOCUMENT_FORMAT_RULES.get(documentType.toUpperCase(Locale.ROOT));
        if (rule == null || documentNumber.matches(rule.pattern())) {
            return Mono.empty();
        }
        return badRequest(rule.violationMessage());
    }

    private Mono<Void> validateLastNames(PersonDTO person) {
        boolean naturalPerson = !AppConstants.DOCUMENT_TYPE_RUC.equalsIgnoreCase(person.getDocumentType());
        return naturalPerson && TextNormalizer.isBlank(person.getLastNames())
                ? badRequest(LAST_NAMES_REQUIRED_MESSAGE)
                : Mono.empty();
    }

    private Mono<Void> resolveGeographicLocation(CustomerDTO customer) {
        if (customer == null || customer.getGeographicLocationId() == null) {
            return Mono.empty();
        }
        // An unknown UBIGEO is rejected, never replaced: a silent default would send deliveries elsewhere.
        return requireGeographicLocation(customer.getGeographicLocationId());
    }

    private static Mono<Void> badRequest(String message) {
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
    }
}
