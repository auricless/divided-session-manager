package org.c3cavite.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class SessionConfigTest {

    @Test
    void appliesDefaultsWhenOnlyRequiredCapIsProvided() {
        SessionConfig config = SessionConfig.builder().capSize(6).build();

        assertEquals(GenderMode.SEPARATED, config.getGenderMode());
        assertEquals(4, config.getTargetSize());
        assertEquals(6, config.getCapSize());
        assertEquals(List.of(AgeTier.values()), config.getActiveTiers());
        assertTrue(config.isLifeStageSubgroupingEnabled());
        assertEquals(AgeTier.TEENS, config.ageTierFor(15).orElseThrow());
        assertEquals(AgeTier.YOUTH, config.ageTierFor(16).orElseThrow());
        assertEquals(AgeTier.YOUNG_ADULT, config.ageTierFor(23).orElseThrow());
    }

    @Test
    void requiresExplicitCap() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> SessionConfig.builder().build());

        assertTrue(error.getMessage().contains("capSize"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rejectsMissingOrNonPositiveCap(Integer cap) {
        assertThrows(IllegalArgumentException.class, () -> SessionConfig.builder().capSize(cap).build());
    }

    @Test
    void allowsExplicitSettingsAndCapWithoutSystemUpperBound() {
        SessionConfig config = SessionConfig.builder().capSize(Integer.MAX_VALUE).targetSize(1)
                .genderMode(GenderMode.MIXED).activeTiers(List.of(AgeTier.YOUTH))
                .lifeStageSubgroupingEnabled(false).build();

        assertEquals(GenderMode.MIXED, config.getGenderMode());
        assertEquals(1, config.getTargetSize());
        assertEquals(Integer.MAX_VALUE, config.getCapSize());
        assertEquals(List.of(AgeTier.YOUTH), config.getActiveTiers());
        assertFalse(config.isLifeStageSubgroupingEnabled());
    }

    @Test
    void rejectsDefaultTargetLargerThanCap() {
        assertThrows(IllegalArgumentException.class, () -> SessionConfig.builder().capSize(3).build());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 7})
    void validatesExplicitTarget(int target) {
        assertThrows(IllegalArgumentException.class,
                () -> SessionConfig.builder().capSize(6).targetSize(target).build());
    }

    @Test
    void usesSessionBoundariesAtInclusiveEdges() {
        SessionConfig config = SessionConfig.builder().capSize(6).ageTierBoundaries(customBoundaries()).build();

        assertTrue(config.ageTierFor(12).isEmpty());
        assertEquals(AgeTier.TEENS, config.ageTierFor(13).orElseThrow());
        assertEquals(AgeTier.TEENS, config.ageTierFor(17).orElseThrow());
        assertEquals(AgeTier.YOUTH, config.ageTierFor(18).orElseThrow());
        assertEquals(AgeTier.YOUTH, config.ageTierFor(25).orElseThrow());
        assertEquals(AgeTier.YOUNG_ADULT, config.ageTierFor(26).orElseThrow());
        assertEquals(AgeTier.YOUNG_ADULT, config.ageTierFor(Integer.MAX_VALUE).orElseThrow());
    }

    @Test
    void rejectsGapInBoundaries() {
        Map<AgeTier, AgeRange> boundaries = new HashMap<>(customBoundaries());
        boundaries.put(AgeTier.YOUTH, new AgeRange(19, 25));

        assertThrows(IllegalArgumentException.class,
                () -> SessionConfig.builder().capSize(6).ageTierBoundaries(boundaries).build());
    }

    @Test
    void rejectsOverlapInBoundaries() {
        Map<AgeTier, AgeRange> boundaries = new HashMap<>(customBoundaries());
        boundaries.put(AgeTier.YOUTH, new AgeRange(17, 25));

        assertThrows(IllegalArgumentException.class,
                () -> SessionConfig.builder().capSize(6).ageTierBoundaries(boundaries).build());
    }

    @Test
    void rejectsMissingOrNullBoundaries() {
        assertThrows(IllegalArgumentException.class,
                () -> SessionConfig.builder().capSize(6).ageTierBoundaries(null).build());
        assertThrows(IllegalArgumentException.class,
                () -> SessionConfig.builder().capSize(6).ageTierBoundaries(Map.of()).build());
        Map<AgeTier, AgeRange> boundaries = new HashMap<>(customBoundaries());
        boundaries.put(AgeTier.YOUTH, null);
        assertThrows(IllegalArgumentException.class,
                () -> SessionConfig.builder().capSize(6).ageTierBoundaries(boundaries).build());
    }

    @Test
    void freezesBoundariesAndKeepsSessionsIndependent() {
        Map<AgeTier, AgeRange> boundaries = new HashMap<>(customBoundaries());
        SessionConfig custom = SessionConfig.builder().capSize(6).ageTierBoundaries(boundaries).build();
        SessionConfig defaults = SessionConfig.builder().capSize(6).build();
        boundaries.clear();

        assertEquals(AgeTier.TEENS, custom.ageTierFor(16).orElseThrow());
        assertEquals(AgeTier.YOUTH, defaults.ageTierFor(16).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> custom.getAgeTierBoundaries().clear());
    }

    private Map<AgeTier, AgeRange> customBoundaries() {
        return Map.of(AgeTier.TEENS, new AgeRange(13, 17), AgeTier.YOUTH, new AgeRange(18, 25),
                AgeTier.YOUNG_ADULT, new AgeRange(26, Integer.MAX_VALUE));
    }

    @Test
    void defaultsMissingGenderModeToSeparated() {
        SessionConfig config = SessionConfig.create(null, 4, 6, List.of(AgeTier.values()), true);

        assertEquals(GenderMode.SEPARATED, config.getGenderMode());
    }

    @Test
    void copiesActiveTiersSoCallerCannotChangeSessionRules() {
        List<AgeTier> tiers = new ArrayList<>(List.of(AgeTier.TEENS));
        SessionConfig config = SessionConfig.create(GenderMode.MIXED, 4, 6, tiers, true);
        tiers.clear();

        assertEquals(List.of(AgeTier.TEENS), config.getActiveTiers());
        assertThrows(UnsupportedOperationException.class, () -> config.getActiveTiers().clear());
    }

    @Test
    void rejectsNullActiveTier() {
        assertThrows(IllegalArgumentException.class, () ->
                SessionConfig.create(GenderMode.MIXED, 4, 6, Arrays.asList(AgeTier.TEENS, null), true));
    }

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
