package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import pe.edu.vallegrande.farmagro.repository.sql.SaleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
final class PersonSalesRoleValidator {

    private final SaleRepository saleRepository;

    Mono<Void> validateRoleChange(Person person, PersonRole requestedRole) {
        if (requestedRole != null && requestedRole.toStorageValue().equals(person.getRole())) {
            return Mono.empty();
        }
        return validateNotHistoricalCustomer(person.getId())
                .then(validateNotHistoricalSeller(person.getId()));
    }

    private Mono<Void> validateNotHistoricalCustomer(String personId) {
        return saleRepository.existsByMongoIdCustomer(personId)
                .flatMap(isHistoricalCustomer -> isHistoricalCustomer
                        ? Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "No se puede cambiar el rol porque tiene ventas registradas como cliente"))
                        : Mono.empty());
    }

    private Mono<Void> validateNotHistoricalSeller(String personId) {
        return saleRepository.existsByMongoIdUser(personId)
                .flatMap(isHistoricalSeller -> isHistoricalSeller
                        ? Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "No se puede cambiar el rol porque tiene ventas registradas como vendedor"))
                        : Mono.empty());
    }
}
