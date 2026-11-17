package pe.edu.vallegrande.farmagro.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import pe.edu.vallegrande.farmagro.model.sql.Purchase;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import pe.edu.vallegrande.farmagro.repository.SupplierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
final class PurchasePartyValidator {

    private static final Set<String> PURCHASE_ROLES = Set.of(
            PersonRole.ADMINISTRATOR.toStorageValue(), PersonRole.WAREHOUSE.toStorageValue());

    private final SupplierRepository supplierRepository;
    private final PersonRepository personRepository;

    Mono<Void> validate(String supplierId, String userId) {
        return validateActiveSupplier(supplierId).then(validateActiveUser(userId));
    }

    // On update only a changed party is checked again: deactivating the original supplier or the
    // user who registered the purchase must not block editing it or confirming its reception.
    Mono<Void> validateChanged(Purchase stored, String supplierId, String userId) {
        Mono<Void> supplierCheck = Objects.equals(stored.getMongoIdSupplier(), supplierId)
                ? Mono.empty() : validateActiveSupplier(supplierId);
        Mono<Void> userCheck = Objects.equals(stored.getMongoIdUser(), userId)
                ? Mono.empty() : validateActiveUser(userId);
        return supplierCheck.then(userCheck);
    }

    private Mono<Void> validateActiveSupplier(String supplierId) {
        return supplierRepository.findById(supplierId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Proveedor no encontrado en MongoDB con id: " + supplierId)))
                .flatMap(supplier -> Boolean.FALSE.equals(supplier.getStatus())
                        ? Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El proveedor '" + supplier.getBusinessName() + "' está inactivo"))
                        : Mono.empty());
    }

    private Mono<Void> validateActiveUser(String userId) {
        return personRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El usuario con id " + userId + " no existe en MongoDB")))
                .flatMap(person -> validateUserCanPurchase(person, userId));
    }

    private Mono<Void> validateUserCanPurchase(Person person, String userId) {
        if (Boolean.FALSE.equals(person.getStatus())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario '" + person.getFirstNames() + " " + person.getLastNames()
                            + "' con id " + userId + " se encuentra inactivo"));
        }
        String role = person.getRole() == null ? "" : person.getRole().toUpperCase(Locale.ROOT);
        if (!PURCHASE_ROLES.contains(role)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario con id " + userId + " tiene rol " + person.getRole()
                            + ". Solo pueden registrar compras usuarios con rol ADMIN o ALMAC"));
        }
        return Mono.empty();
    }
}
