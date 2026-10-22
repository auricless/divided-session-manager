package org.c3cavite.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.c3cavite.core.domain.AgeRange;
import org.c3cavite.core.domain.AgeTier;
import org.c3cavite.core.domain.Gender;
import org.c3cavite.core.domain.GenderMode;
import org.c3cavite.core.domain.LifeStage;
import org.c3cavite.core.domain.Participant;
import org.c3cavite.core.domain.SessionConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SegmentationStrategyTest {

  private final SegmentationStrategy strategy = new SegmentationStrategy();

  @Test
  void segmentsByConfiguredTierAndLifeStageRatherThanCachedDefaultTier() {
    SessionConfig config =
        SessionConfig.builder()
            .capSize(6)
            .ageTierBoundaries(
                Map.of(
                    AgeTier.TEENS,
                    new AgeRange(13, 17),
                    AgeTier.YOUTH,
                    new AgeRange(18, 20),
                    AgeTier.YOUNG_ADULT,
                    new AgeRange(21, Integer.MAX_VALUE)))
            .build();
    Participant teen = new Participant("Teen", 16, Gender.MALE, null);
    Participant adult = new Participant("Adult", 21, Gender.MALE, LifeStage.STUDENT);

    Map<SegmentKey, List<Participant>> result = strategy.segment(List.of(teen, adult), config);

    assertEquals(List.of(teen), result.get(new SegmentKey(Gender.MALE, AgeTier.TEENS, null)));
    assertEquals(
        List.of(adult),
        result.get(new SegmentKey(Gender.MALE, AgeTier.YOUNG_ADULT, LifeStage.STUDENT)));
  }

  @Test
  void canDisableLifeStageSplit() {
    SessionConfig config =
        SessionConfig.builder()
            .capSize(6)
            .genderMode(GenderMode.MIXED)
            .lifeStageSubgroupingEnabled(false)
            .build();
    List<Participant> participants =
        List.of(
            new Participant("Student", 24, Gender.MALE, LifeStage.STUDENT),
            new Participant("Worker", 25, Gender.FEMALE, LifeStage.WORKING_PROFESSIONAL));

    Map<SegmentKey, List<Participant>> result = strategy.segment(participants, config);

    assertEquals(1, result.size());
    assertEquals(participants, result.get(new SegmentKey(null, AgeTier.YOUNG_ADULT, null)));
  }

  @Test
  @DisplayName(
      "Given SEPARATED gender mode, when segment is called, then partition by gender and tier")
  void testSegmentSeparatedGender() {
    SessionConfig config =
        SessionConfig.create(GenderMode.SEPARATED, 5, 8, List.of(AgeTier.TEENS), false);
    List<Participant> participants =
        List.of(
            new Participant("Male 1", 14, Gender.MALE, null),
            new Participant("Female 1", 15, Gender.FEMALE, null));

    Map<SegmentKey, List<Participant>> result = strategy.segment(participants, config);

    assertEquals(2, result.size());
    assertTrue(result.containsKey(new SegmentKey(Gender.MALE, AgeTier.TEENS, null)));
    assertTrue(result.containsKey(new SegmentKey(Gender.FEMALE, AgeTier.TEENS, null)));
  }

  @Test
  @DisplayName("Given MIXED gender mode, when segment is called, then partition by tier only")
  void testSegmentMixedGender() {
    SessionConfig config =
        SessionConfig.create(GenderMode.MIXED, 5, 8, List.of(AgeTier.TEENS), false);
    List<Participant> participants =
        List.of(
            new Participant("Male 1", 14, Gender.MALE, null),
            new Participant("Female 1", 15, Gender.FEMALE, null));

    Map<SegmentKey, List<Participant>> result = strategy.segment(participants, config);

    assertEquals(1, result.size());
    assertTrue(result.containsKey(new SegmentKey(null, AgeTier.TEENS, null)));
    assertEquals(2, result.get(new SegmentKey(null, AgeTier.TEENS, null)).size());
  }

  @Test
  @DisplayName(
      "Given life-stage subgrouping enabled, when YOUNG_ADULT, then partition by life stage")
  void testSegmentLifeStage() {
    SessionConfig config =
        SessionConfig.create(GenderMode.MIXED, 5, 8, List.of(AgeTier.YOUNG_ADULT), true);
    List<Participant> participants =
        List.of(
            new Participant("Student 1", 24, Gender.MALE, LifeStage.STUDENT),
            new Participant("Worker 1", 25, Gender.FEMALE, LifeStage.WORKING_PROFESSIONAL));

    Map<SegmentKey, List<Participant>> result = strategy.segment(participants, config);

    assertEquals(2, result.size());
    assertTrue(result.containsKey(new SegmentKey(null, AgeTier.YOUNG_ADULT, LifeStage.STUDENT)));
    assertTrue(
        result.containsKey(
            new SegmentKey(null, AgeTier.YOUNG_ADULT, LifeStage.WORKING_PROFESSIONAL)));
  }

  @Test
  void emptyInputProducesNoSegments() {
    assertTrue(strategy.segment(List.of(), SessionConfig.builder().capSize(6).build()).isEmpty());
  }

  @Test
  void oneAttendeeRemainsInOneSegment() {
    Participant attendee = new Participant("Attendee", 14, Gender.FEMALE, null);
    Map<SegmentKey, List<Participant>> result =
        strategy.segment(List.of(attendee), SessionConfig.builder().capSize(6).build());

    assertEquals(
        Map.of(new SegmentKey(Gender.FEMALE, AgeTier.TEENS, null), List.of(attendee)), result);
  }

  @Test
  void allThreeAgeTiersStaySeparate() {
    Participant teen = new Participant("Teen", 14, Gender.MALE, null);
    Participant youth = new Participant("Youth", 17, Gender.MALE, null);
    Participant adult = new Participant("Adult", 25, Gender.MALE, LifeStage.STUDENT);
    Map<SegmentKey, List<Participant>> result =
        strategy.segment(List.of(teen, youth, adult), SessionConfig.builder().capSize(6).build());

    assertEquals(
        Map.of(
            new SegmentKey(Gender.MALE, AgeTier.TEENS, null),
            List.of(teen),
            new SegmentKey(Gender.MALE, AgeTier.YOUTH, null),
            List.of(youth),
            new SegmentKey(Gender.MALE, AgeTier.YOUNG_ADULT, LifeStage.STUDENT),
            List.of(adult)),
        result);
  }

  @Test
  void unknownAndInactiveTiersAreExcludedFromSegmentation() {
    SessionConfig config =
        SessionConfig.builder()
            .capSize(6)
            .activeTiers(List.of(AgeTier.YOUTH))
            .ageTierBoundaries(
                Map.of(
                    AgeTier.TEENS,
                    new AgeRange(13, 15),
                    AgeTier.YOUTH,
                    new AgeRange(16, 22),
                    AgeTier.YOUNG_ADULT,
                    new AgeRange(23, 30)))
            .build();
    Participant eligible = new Participant("Youth", 17, Gender.MALE, null);
    List<Participant> input =
        List.of(
            eligible,
            new Participant("Child", 12, Gender.MALE, null),
            new Participant("Inactive", 14, Gender.MALE, null),
            new Participant("Outside", 31, Gender.MALE, LifeStage.STUDENT));

    assertEquals(
        Map.of(new SegmentKey(Gender.MALE, AgeTier.YOUTH, null), List.of(eligible)),
        strategy.segment(input, config));
  }
}
