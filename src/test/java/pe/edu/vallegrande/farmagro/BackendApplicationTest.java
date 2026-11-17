package pe.edu.vallegrande.farmagro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BackendApplicationTest {

    @Test
    @DisplayName("BackendApplication class loads without errors")
    void applicationClassShouldBeAccessible() {
        assertDoesNotThrow(() -> {
            Class<?> appClass = Class.forName("pe.edu.vallegrande.farmagro.BackendApplication");
            assertDoesNotThrow(() -> appClass.getMethod("main", String[].class));
        });
    }
}
