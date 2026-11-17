package pe.edu.vallegrande.farmagro.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PaginationValidator {

    private static final int MIN_PAGE_NUMBER = 0;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SUPPORTED_OFFSET = Integer.MAX_VALUE;

    private PaginationValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static int offset(int page, int size) {
        if (page < MIN_PAGE_NUMBER || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La página no puede ser negativa y el tamaño debe estar entre " + MIN_PAGE_SIZE + " y " + MAX_PAGE_SIZE);
        }
        long offset = (long) page * size;
        if (offset > MAX_SUPPORTED_OFFSET) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La página solicitada es demasiado grande");
        }
        return (int) offset;
    }
}
