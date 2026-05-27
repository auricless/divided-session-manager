package org.c3cavite.core.service;

import org.c3cavite.core.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SegmentationStrategyTest {

    private final SegmentationStrategy strategy = new SegmentationStrategy();

    @Test
    @DisplayName("Given SEPARATED gender mode, when segment is called, then partition by gender and tier")
    void testSegmentSeparatedGender() {
        SessionConfig config = SessionConfig.create(GenderMode.SEPARATED, 5, 8, List.of(AgeTier.TEENS), false);
        List<Participant> participants = List.of(
                new Participant("Male 1", 14, Gender.MALE, null),
                new Participant("Female 1", 15, Gender.FEMALE, null)
        );

        Map<SegmentKey, List<Participant>> result = strategy.segment(participants, config);

        assertEquals(2, result.size());
        assertTrue(result.containsKey(new SegmentKey(Gender.MALE, AgeTier.TEENS, null)));
        assertTrue(result.containsKey(new SegmentKey(Gender.FEMALE, AgeTier.TEENS, null)));
    }

    @Test
    @DisplayName("Given MIXED gender mode, when segment is called, then partition by tier only")
    void testSegmentMixedGender() {
        SessionConfig config = SessionConfig.create(GenderMode.MIXED, 5, 8, List.of(AgeTier.TEENS), false);
        List<Participant> participants = List.of(
                new Participant("Male 1", 14, Gender.MALE, null),
                new Participant("Female 1", 15, Gender.FEMALE, null)
        );

        Map<SegmentKey, List<Participant>> result = strategy.segment(participants, config);

        assertEquals(1, result.size());
        assertTrue(result.containsKey(new SegmentKey(null, AgeTier.TEENS, null)));
        assertEquals(2, result.get(new SegmentKey(null, AgeTier.TEENS, null)).size());
    }

    @Test
    @DisplayName("Given life-stage subgrouping enabled, when YOUNG_ADULT, then partition by life stage")
    void testSegmentLifeStage() {
        SessionConfig config = SessionConfig.create(GenderMode.MIXED, 5, 8, List.of(AgeTier.YOUNG_ADULT), true);
        List<Participant> participants = List.of(
                new Participant("Student 1", 24, Gender.MALE, LifeStage.STUDENT),
                new Participant("Worker 1", 25, Gender.FEMALE, LifeStage.WORKING_PROFESSIONAL)
        );

        Map<SegmentKey, List<Participant>> result = strategy.segment(participants, config);

        assertEquals(2, result.size());
        assertTrue(result.containsKey(new SegmentKey(null, AgeTier.YOUNG_ADULT, LifeStage.STUDENT)));
        assertTrue(result.containsKey(new SegmentKey(null, AgeTier.YOUNG_ADULT, LifeStage.WORKING_PROFESSIONAL)));
    }
}
