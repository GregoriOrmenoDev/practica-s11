package pe.edu.vallegrande.farmagro.service.impl;

import pe.edu.vallegrande.farmagro.util.AppConstants;

import lombok.RequiredArgsConstructor;
import pe.edu.vallegrande.farmagro.dto.SaleDTO;
import pe.edu.vallegrande.farmagro.model.Person;
import pe.edu.vallegrande.farmagro.model.PersonRole;
import pe.edu.vallegrande.farmagro.model.sql.Sale;
import pe.edu.vallegrande.farmagro.repository.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Objects;

@Component
@RequiredArgsConstructor
final class SalePartyValidator {

    private final PersonRepository personRepository;

    Mono<Tuple2<Person, Person>> validateCustomerAndSeller(SaleDTO dto) {
        return Mono.zip(validateCustomer(dto.getMongoIdCustomer()), validateSeller(dto.getMongoIdUser()));
    }

    // On update only a changed party is checked again, so deactivating the original customer or
    // seller does not lock a pending sale. Returns the customer, needed for the voucher type.
    Mono<Person> validateChangedParties(Sale stored, SaleDTO dto) {
        boolean sameCustomer = Objects.equals(stored.getMongoIdCustomer(), dto.getMongoIdCustomer());
        boolean sameSeller = Objects.equals(stored.getMongoIdUser(), dto.getMongoIdUser());
        Mono<Void> sellerCheck = sameSeller ? Mono.empty() : validateSeller(dto.getMongoIdUser()).then();
        Mono<Person> customer = sameCustomer ? findCustomer(dto.getMongoIdCustomer()) : validateCustomer(dto.getMongoIdCustomer());
        return sellerCheck.then(customer);
    }

    private Mono<Person> findCustomer(String customerId) {
        return personRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El cliente no existe en el maestro de personas con ID: " + customerId)));
    }

    String determineAndValidateVoucherType(Person customer, String requestedVoucher) {
        String documentType = customer.getDocumentType();
        if (AppConstants.DOCUMENT_TYPE_RUC.equalsIgnoreCase(documentType)) {
            if (requestedVoucher != null && !AppConstants.VOUCHER_INVOICE.equalsIgnoreCase(requestedVoucher)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A un cliente con RUC le corresponde comprobante INVOICE (se envió: " + requestedVoucher + ")");
            }
            return AppConstants.VOUCHER_INVOICE;
        }
        // Receipts (boleta) can be issued to DNI, foreign-resident card (CE) and passport (PAS) holders.
        if (AppConstants.DOCUMENT_TYPE_DNI.equalsIgnoreCase(documentType)
                || AppConstants.DOCUMENT_TYPE_CE.equalsIgnoreCase(documentType)
                || AppConstants.DOCUMENT_TYPE_PASSPORT.equalsIgnoreCase(documentType)) {
            if (requestedVoucher != null && !AppConstants.VOUCHER_RECEIPT.equalsIgnoreCase(requestedVoucher)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A un cliente con documento " + documentType + " le corresponde comprobante RECEIPT, no INVOICE (la factura requiere RUC)");
            }
            return AppConstants.VOUCHER_RECEIPT;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El tipo de documento del cliente no permite emitir un comprobante: " + documentType);
    }

    private Mono<Person> validateCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id del cliente es obligatorio"));
        }
        return personRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El cliente no existe en el maestro de personas con ID: " + customerId)))
                .flatMap(person -> {
                    if (!PersonRole.CUSTOMER.toStorageValue().equalsIgnoreCase(person.getRole())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "La persona con ID " + customerId + " tiene rol " + person.getRole() + ", debe ser CUSTOMER"));
                    }
                    if (Boolean.FALSE.equals(person.getStatus())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "El cliente con ID " + customerId + " se encuentra inactivo/eliminado"));
                    }
                    return Mono.just(person);
                });
    }

    private Mono<Person> validateSeller(String userId) {
        if (userId == null || userId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id del vendedor es obligatorio"));
        }
        return personRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El vendedor/usuario no existe en el maestro de personas con ID: " + userId)))
                .flatMap(person -> {
                    if (!PersonRole.VENDOR.toStorageValue().equalsIgnoreCase(person.getRole())
                    && !PersonRole.ADMINISTRATOR.toStorageValue().equalsIgnoreCase(person.getRole())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "La persona con ID " + userId + " tiene rol " + person.getRole() + ", debe ser VEND o ADMIN para registrar una venta"));
                    }
                    if (Boolean.FALSE.equals(person.getStatus())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "El vendedor/usuario con ID " + userId + " se encuentra inactivo/eliminado"));
                    }
                    return Mono.just(person);
                });
    }
}
