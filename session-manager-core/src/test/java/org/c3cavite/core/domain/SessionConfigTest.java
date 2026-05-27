package org.c3cavite.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionConfigTest {

    @Test
    @DisplayName("Given valid parameters, when create is called, then return SessionConfig")
    void testCreateValidConfig() {
        List<AgeTier> activeTiers = List.of(AgeTier.TEENS, AgeTier.YOUTH);
        SessionConfig config = SessionConfig.create(GenderMode.SEPARATED, 5, 8, activeTiers, false);

        assertNotNull(config);
        assertEquals(GenderMode.SEPARATED, config.getGenderMode());
        assertEquals(5, config.getTargetSize());
        assertEquals(8, config.getCapSize());
        assertEquals(activeTiers, config.getActiveTiers());
        assertFalse(config.isLifeStageSubgroupingEnabled());
    }

    @Test
    @DisplayName("Given targetSize <= 0, when create is called, then throw IllegalArgumentException")
    void testCreateWithInvalidTargetSize() {
        assertThrows(IllegalArgumentException.class, () -> 
            SessionConfig.create(GenderMode.MIXED, 0, 8, List.of(AgeTier.TEENS), false)
        );
    }

    @Test
    @DisplayName("Given targetSize > capSize, when create is called, then throw IllegalArgumentException")
    void testCreateWithTargetGreaterThanCap() {
        assertThrows(IllegalArgumentException.class, () -> 
            SessionConfig.create(GenderMode.MIXED, 10, 8, List.of(AgeTier.TEENS), false)
        );
    }

    @Test
    @DisplayName("Given null activeTiers, when create is called, then throw IllegalArgumentException")
    void testCreateWithNullActiveTiers() {
        assertThrows(IllegalArgumentException.class, () -> 
            SessionConfig.create(GenderMode.MIXED, 5, 8, null, false)
        );
    }

    @Test
    @DisplayName("Given empty activeTiers, when create is called, then throw IllegalArgumentException")
    void testCreateWithEmptyActiveTiers() {
        assertThrows(IllegalArgumentException.class, () -> 
            SessionConfig.create(GenderMode.MIXED, 5, 8, Collections.emptyList(), false)
        );
    }
}
