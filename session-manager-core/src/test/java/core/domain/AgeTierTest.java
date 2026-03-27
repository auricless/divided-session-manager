package core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgeTierTest {

    @ParameterizedTest()
    @ValueSource(ints = {13, 14, 15})
    @DisplayName("Given age for teens 13 - 15, when fromAge is called, then return TEENS")
    void testFromAge(int age) {
        assertEquals(AgeTier.TEEN, AgeTier.fromAge(age));
    }

    @ParameterizedTest(name = "Given age {0}, then throw IllegalArgumentException")
    @ValueSource(ints = {12, 40, -1, 0, 999999})
    @DisplayName("Given age outside of supported range, when fromAge is called, then throw IllegalArgumentException")
    void testFromAgeWithInvalidAge(int age) {
        assertThrows(IllegalArgumentException.class, () -> AgeTier.fromAge(age));
    }

}
