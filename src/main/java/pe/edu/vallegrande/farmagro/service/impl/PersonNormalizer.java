package pe.edu.vallegrande.farmagro.service.impl;

import org.springframework.stereotype.Component;
import pe.edu.vallegrande.farmagro.dto.PersonDTO;
import pe.edu.vallegrande.farmagro.util.AppConstants;
import pe.edu.vallegrande.farmagro.util.TextNormalizer;

@Component
final class PersonNormalizer {

    void normalize(PersonDTO person) {
        normalizeDocument(person);
        normalizeNames(person);
        person.setEmail(TextNormalizer.email(person.getEmail()));
    }

    private void normalizeDocument(PersonDTO person) {
        person.setDocumentType(TextNormalizer.code(person.getDocumentType()));
        person.setDocumentNumber(TextNormalizer.trim(person.getDocumentNumber()));
    }

    private void normalizeNames(PersonDTO person) {
        person.setFirstNames(TextNormalizer.trim(person.getFirstNames()));
        person.setLastNames(lastNamesFor(person.getDocumentType(), person.getLastNames()));
    }

    // RUC holders have no last name; the placeholder fills the required field.
    private String lastNamesFor(String documentType, String lastNames) {
        if (!TextNormalizer.isBlank(lastNames)) {
            return lastNames.trim();
        }
        return AppConstants.DOCUMENT_TYPE_RUC.equalsIgnoreCase(documentType)
                ? AppConstants.MISSING_VALUE_PLACEHOLDER
                : TextNormalizer.trim(lastNames);
    }
}
