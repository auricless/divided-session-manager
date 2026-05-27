package org.c3cavite.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgeTierTest {

    @ParameterizedTest
    @CsvSource({
            "13, TEENS",
            "14, TEENS",
            "15, TEENS",
            "16, YOUTH",
            "19, YOUTH",
            "22, YOUTH",
            "23, YOUNG_ADULT",
            "30, YOUNG_ADULT",
            "99, YOUNG_ADULT"
    })
    @DisplayName("Given valid age, when fromAge is called, then return expected AgeTier")
    void testFromAge(int age, AgeTier expectedTier) {
        Optional<AgeTier> result = AgeTier.fromAge(age);
        assertTrue(result.isPresent());
        assertEquals(expectedTier, result.get());
    }

    @ParameterizedTest(name = "Given age {0}, then return Optional.empty()")
    @ValueSource(ints = {12, -1, 0})
    @DisplayName("Given age outside of supported range, when fromAge is called, then return Optional.empty()")
    void testFromAgeWithInvalidAge(int age) {
        Optional<AgeTier> result = AgeTier.fromAge(age);
        assertTrue(result.isEmpty());
    }
}
