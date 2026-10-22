package org.c3cavite.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.c3cavite.core.domain.AgeRange;
import org.c3cavite.core.domain.AgeTier;
import org.c3cavite.core.domain.AssignmentBlocker;
import org.c3cavite.core.domain.AssignmentResult;
import org.c3cavite.core.domain.AssignmentState;
import org.c3cavite.core.domain.Facilitator;
import org.c3cavite.core.domain.FacilitatorException;
import org.c3cavite.core.domain.Gender;
import org.c3cavite.core.domain.GenderMode;
import org.c3cavite.core.domain.Group;
import org.c3cavite.core.domain.LifeStage;
import org.c3cavite.core.domain.Participant;
import org.c3cavite.core.domain.Role;
import org.c3cavite.core.domain.SessionConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GroupAssignmentServiceTest {

  private final GroupAssignmentService service = new GroupAssignmentService();

  @Test
  void emptyQualificationsRequireAnExplicitExceptionForThatRun() {
    Facilitator leader = new Facilitator("Unqualified leader", Role.LEADER, Gender.MALE, List.of());
    SessionConfig config = SessionConfig.builder().capSize(6).build();
    List<Participant> participants = attendees(2, 17, null);

    AssignmentResult strict = service.assign(config, participants, List.of(leader));
    AssignmentResult allowed =
        service.assign(
            config,
            participants,
            List.of(leader),
            List.of(new FacilitatorException(leader, true, false)));
    AssignmentResult withdrawn = service.assign(config, participants, List.of(leader));

    assertFalse(strict.isFinal());
    assertNull(strict.getGroups().get(0).getLeader());
    assertTrue(allowed.isFinal());
    assertEquals(leader, allowed.getGroups().get(0).getLeader());
    assertTrue(allowed.getWarnings().stream().anyMatch(warning -> warning.contains("exception")));
    assertFalse(withdrawn.isFinal());
    assertTrue(leader.getQualifiedTiers().isEmpty());
  }

  @Test
  void filtersAndAssignsUsingTheCurrentSessionsAgeBoundaries() {
    SessionConfig config =
        SessionConfig.builder()
            .capSize(6)
            .activeTiers(List.of(AgeTier.TEENS))
            .ageTierBoundaries(
                Map.of(
                    AgeTier.TEENS,
                    new AgeRange(13, 17),
                    AgeTier.YOUTH,
                    new AgeRange(18, 25),
                    AgeTier.YOUNG_ADULT,
                    new AgeRange(26, Integer.MAX_VALUE)))
            .build();
    Participant attendee = new Participant("Attendee", 17, Gender.MALE, null);
    Participant inactive = new Participant("Inactive", 18, Gender.MALE, null);
    Facilitator leader =
        new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS));

    AssignmentResult result = service.assign(config, List.of(attendee, inactive), List.of(leader));

    assertEquals(1, result.getTotalAssignedParticipants());
    assertEquals(List.of(attendee), result.getGroups().get(0).getParticipants());
    assertEquals(AgeTier.TEENS, result.getGroups().get(0).getAgeTierTarget());
    assertEquals(AgeTier.TEENS, result.getGroups().get(0).getParticipants().get(0).getAgeTier());
    assertEquals(AgeTier.YOUTH, attendee.getAgeTier());
    assertEquals(leader, result.getGroups().get(0).getLeader());
    assertEquals(inactive, result.getUnassignedParticipants().get(0).participant());
  }

  @Test
  void reportsUnknownTierOutsideConfiguredBoundaries() {
    SessionConfig config =
        SessionConfig.builder()
            .capSize(6)
            .ageTierBoundaries(
                Map.of(
                    AgeTier.TEENS,
                    new AgeRange(14, 17),
                    AgeTier.YOUTH,
                    new AgeRange(18, 25),
                    AgeTier.YOUNG_ADULT,
                    new AgeRange(26, 70)))
            .build();
    Participant attendee = new Participant("Outside range", 71, Gender.MALE, LifeStage.STUDENT);

    AssignmentResult result = service.assign(config, List.of(attendee), List.of());

    assertEquals(0, result.getTotalAssignedParticipants());
    assertEquals("UNKNOWN_TIER", result.getUnassignedParticipants().get(0).reason());
    assertNull(result.getUnassignedParticipants().get(0).participant().getAgeTier());
  }

  @Test
  void reusingInputInAnotherSessionDoesNotChangeEarlierResults() {
    Participant attendee = new Participant("Attendee", 17, Gender.MALE, null);
    SessionConfig custom =
        SessionConfig.builder()
            .capSize(6)
            .ageTierBoundaries(
                Map.of(
                    AgeTier.TEENS,
                    new AgeRange(13, 17),
                    AgeTier.YOUTH,
                    new AgeRange(18, 25),
                    AgeTier.YOUNG_ADULT,
                    new AgeRange(26, Integer.MAX_VALUE)))
            .build();

    AssignmentResult customResult = service.assign(custom, List.of(attendee), List.of());
    AssignmentResult defaultResult =
        service.assign(SessionConfig.builder().capSize(6).build(), List.of(attendee), List.of());

    assertEquals(
        AgeTier.TEENS, customResult.getGroups().get(0).getParticipants().get(0).getAgeTier());
    assertEquals(
        AgeTier.YOUTH, defaultResult.getGroups().get(0).getParticipants().get(0).getAgeTier());
    assertEquals(AgeTier.YOUTH, attendee.getAgeTier());
  }

  @Test
  @DisplayName(
      "Given valid participants and facilitators, when assign is called, then return successful result")
  void testHappyPath() {
    SessionConfig config =
        SessionConfig.create(GenderMode.MIXED, 5, 8, List.of(AgeTier.TEENS), false);
    List<Participant> participants =
        List.of(
            new Participant("P1", 14, Gender.MALE, null),
            new Participant("P2", 14, Gender.FEMALE, null));
    List<Facilitator> facilitators =
        List.of(new Facilitator("L1", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS)));

    AssignmentResult result = service.assign(config, participants, facilitators);

    assertEquals(1, result.getTotalGroups());
    assertEquals(2, result.getTotalAssignedParticipants());
    assertEquals(2, result.getGroups().get(0).getParticipants().size());
    assertEquals(AssignmentState.FINAL, result.getState());
  }

  @Test
  @DisplayName("Given underage participant, when assign is called, then move to unassigned")
  void testUnderageParticipant() {
    SessionConfig config =
        SessionConfig.create(GenderMode.MIXED, 5, 8, List.of(AgeTier.TEENS), false);
    List<Participant> participants = List.of(new Participant("Child", 12, Gender.MALE, null));

    AssignmentResult result = service.assign(config, participants, List.of());

    assertEquals(0, result.getTotalAssignedParticipants());
    assertEquals(1, result.getUnassignedParticipants().size());
    assertTrue(result.getUnassignedParticipants().get(0).reason().contains("under 13"));
  }

  @Test
  @DisplayName(
      "Given leader shortage, when assign is called, then consolidate groups up to capSize")
  void testConsolidation() {
    SessionConfig config =
        SessionConfig.create(GenderMode.MIXED, 3, 10, List.of(AgeTier.TEENS), false);
    List<Participant> participants =
        List.of(
            new Participant("P1", 14, Gender.MALE, null),
            new Participant("P2", 14, Gender.MALE, null),
            new Participant("P3", 14, Gender.MALE, null),
            new Participant("P4", 14, Gender.MALE, null),
            new Participant("P5", 14, Gender.MALE, null),
            new Participant("P6", 14, Gender.MALE, null),
            new Participant("P7", 14, Gender.MALE, null),
            new Participant("P8", 14, Gender.MALE, null),
            new Participant("P9", 14, Gender.MALE, null),
            new Participant("P10", 14, Gender.MALE, null));
    List<Facilitator> facilitators =
        List.of(
            new Facilitator("L1", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS)),
            new Facilitator("L2", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS)));

    AssignmentResult result = service.assign(config, participants, facilitators);

    assertEquals(2, result.getTotalGroups());
    assertEquals(5, result.getGroups().get(0).getParticipants().size());
    assertEquals(5, result.getGroups().get(1).getParticipants().size());
  }

  @Test
  void shortageReturnsAProposalWithAGroupBlocker() {
    AssignmentResult result =
        service.assign(
            SessionConfig.builder().capSize(6).build(),
            List.of(new Participant("Attendee", 17, Gender.MALE, null)),
            List.of());

    assertEquals(AssignmentState.NON_FINAL_PROPOSAL, result.getState());
    assertEquals(AssignmentBlocker.Code.MISSING_LEADER, result.getBlockers().get(0).getCode());
    assertEquals(1, result.getBlockers().get(0).getGroupNumber());
  }

  @Test
  void emptyInputReturnsAnEmptyFinalResult() {
    AssignmentResult result =
        service.assign(SessionConfig.builder().capSize(6).build(), List.of(), List.of());

    assertTrue(result.isFinal());
    assertTrue(result.getGroups().isEmpty());
    assertTrue(result.getBlockers().isEmpty());
  }

  @Test
  void genderMismatchCannotSilentlyFinalizeASeparatedGroup() {
    AssignmentResult result =
        service.assign(
            SessionConfig.builder().capSize(6).build(),
            List.of(new Participant("Attendee", 17, Gender.MALE, null)),
            List.of(new Facilitator("Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.YOUTH))));

    assertFalse(result.isFinal());
    assertNull(result.getGroups().get(0).getLeader());
  }

  @Test
  void explicitExceptionsResolveShortageOnlyForTheCurrentRun() {
    SessionConfig config = SessionConfig.builder().capSize(6).build();
    List<Participant> participants = List.of(new Participant("Attendee", 17, Gender.MALE, null));
    Facilitator leader =
        new Facilitator("Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.TEENS));

    AssignmentResult allowed =
        service.assign(
            config,
            participants,
            List.of(leader),
            List.of(new FacilitatorException(leader, true, true)));
    AssignmentResult unchanged = service.assign(config, participants, List.of(leader));

    assertTrue(allowed.isFinal());
    assertEquals(leader, allowed.getGroups().get(0).getLeader());
    assertFalse(unchanged.isFinal());
    assertTrue(allowed.getWarnings().stream().anyMatch(w -> w.contains("exception")));
    assertEquals(List.of(AgeTier.TEENS), leader.getQualifiedTiers());
  }

  @Test
  void consolidatesLifeStageSegmentsUsingTheSharedLeaderPool() {
    List<Participant> participants = new java.util.ArrayList<>(attendees(5, 25, LifeStage.STUDENT));
    participants.addAll(attendees(5, 25, LifeStage.WORKING_PROFESSIONAL));
    AssignmentResult result =
        service.assign(
            SessionConfig.builder().targetSize(3).capSize(6).build(),
            participants,
            leaders(2, AgeTier.YOUNG_ADULT));

    assertTrue(result.isFinal());
    assertEquals(2, result.getTotalGroups());
    assertTrue(result.getGroups().stream().allMatch(g -> g.getParticipants().size() == 5));
    assertEquals(2, result.getGroups().stream().map(Group::getLeader).distinct().count());
  }

  @Test
  void flexibleLeadersDoNotStarveTierSpecificGroups() {
    List<Participant> participants = new java.util.ArrayList<>(attendees(4, 17, null));
    participants.addAll(attendees(4, 14, null));
    Facilitator flexible =
        new Facilitator(
            "Flexible", Role.LEADER, Gender.MALE, List.of(AgeTier.YOUTH, AgeTier.TEENS));
    Facilitator youthOnly =
        new Facilitator("Youth only", Role.LEADER, Gender.MALE, List.of(AgeTier.YOUTH));

    AssignmentResult result =
        service.assign(
            SessionConfig.builder().capSize(6).build(), participants, List.of(flexible, youthOnly));

    assertTrue(result.isFinal());
    assertEquals(youthOnly, result.getGroups().get(0).getLeader());
    assertEquals(AgeTier.YOUTH, result.getGroups().get(0).getAgeTierTarget());
  }

  @Test
  void avoidsSingletonGroupsEvenWhenTargetIsOne() {
    AssignmentResult result =
        service.assign(
            SessionConfig.builder().targetSize(1).capSize(6).build(),
            attendees(5, 17, null),
            leaders(5, AgeTier.YOUTH));

    assertTrue(result.isFinal());
    assertEquals(2, result.getTotalGroups());
    assertTrue(result.getGroups().stream().allMatch(g -> g.getParticipants().size() >= 2));
  }

  @Test
  void impossibleMinimumSizeReturnsProposalWithoutBreakingTheCap() {
    AssignmentResult result =
        service.assign(
            SessionConfig.builder().targetSize(2).capSize(2).build(),
            attendees(3, 17, null),
            leaders(2, AgeTier.YOUTH));

    assertFalse(result.isFinal());
    assertEquals(3, result.getTotalAssignedParticipants());
    assertTrue(result.getGroups().stream().allMatch(g -> g.getParticipants().size() <= 2));
    assertTrue(
        result.getBlockers().stream()
            .anyMatch(b -> b.getCode() == AssignmentBlocker.Code.MINIMUM_GROUP_SIZE));
  }

  @Test
  void lifeStageSeparationIsNotRelaxedToHideASingleton() {
    List<Participant> participants =
        List.of(
            new Participant("Student", 25, Gender.MALE, LifeStage.STUDENT),
            new Participant("Worker", 25, Gender.MALE, LifeStage.WORKING_PROFESSIONAL));
    AssignmentResult result =
        service.assign(
            SessionConfig.builder().capSize(6).build(),
            participants,
            leaders(2, AgeTier.YOUNG_ADULT));

    assertFalse(result.isFinal());
    assertEquals(2, result.getTotalGroups());
  }

  @Test
  void shortageConsolidatesOnlyWithinTheHardCap() {
    AssignmentResult result =
        service.assign(
            SessionConfig.builder().capSize(6).build(), attendees(9, 17, null), List.of());

    assertFalse(result.isFinal());
    assertEquals(2, result.getTotalGroups());
    assertEquals(9, result.getTotalAssignedParticipants());
    assertTrue(result.getGroups().stream().allMatch(g -> g.getParticipants().size() <= 6));
  }

  @Test
  void underThirteenExclusionIncludesAWarning() {
    AssignmentResult result =
        service.assign(
            SessionConfig.builder().capSize(6).build(), attendees(1, 12, null), List.of());

    assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("under 13")));
  }

  @Test
  void nullInputsAreRejectedBeforeAssignment() {
    SessionConfig config = SessionConfig.builder().capSize(6).build();
    assertThrows(IllegalArgumentException.class, () -> service.assign(null, List.of(), List.of()));
    assertThrows(IllegalArgumentException.class, () -> service.assign(config, null, List.of()));
    assertThrows(IllegalArgumentException.class, () -> service.assign(config, List.of(), null));
    assertThrows(
        IllegalArgumentException.class, () -> service.assign(config, List.of(), List.of(), null));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.assign(config, java.util.Arrays.asList((Participant) null), List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.assign(config, List.of(), java.util.Arrays.asList((Facilitator) null)));
  }

  @Test
  void staffedSegmentsStayNearTargetWhenConsolidationCannotHelpAnotherTier() {
    List<Participant> participants = new java.util.ArrayList<>(attendees(4, 17, null));
    participants.addAll(attendees(8, 14, null));

    AssignmentResult result =
        service.assign(
            SessionConfig.builder().capSize(8).build(), participants, leaders(2, AgeTier.TEENS));

    assertFalse(result.isFinal());
    assertEquals(12, result.getTotalAssignedParticipants());
    List<Group> teens =
        result.getGroups().stream().filter(g -> g.getAgeTierTarget() == AgeTier.TEENS).toList();
    assertEquals(2, teens.size());
    assertTrue(teens.stream().allMatch(g -> g.getSize() == 4 && g.getLeader() != null));
    assertNull(result.getGroups().get(0).getLeader());
  }

  @Test
  void sizingPreservesAttendeesCapsAndBalancedGroupsAcrossSmallConfigurations() {
    for (int count = 1; count <= 25; count++) {
      for (int target = 1; target <= 5; target++) {
        for (int cap = target; cap <= 7; cap++) {
          AssignmentResult result =
              service.assign(
                  SessionConfig.builder().targetSize(target).capSize(cap).build(),
                  attendees(count, 17, null),
                  leaders(count, AgeTier.YOUTH));
          assertEquals(count, result.getTotalAssignedParticipants());
          int smallest =
              result.getGroups().stream()
                  .mapToInt(g -> g.getParticipants().size())
                  .min()
                  .orElseThrow();
          int largest =
              result.getGroups().stream()
                  .mapToInt(g -> g.getParticipants().size())
                  .max()
                  .orElseThrow();
          assertTrue(largest <= cap);
          assertTrue(largest - smallest <= 1);
          assertEquals(result.getTotalGroups() == 1 || smallest >= 2, result.isFinal());
        }
      }
    }
  }

  private List<Participant> attendees(int count, int age, LifeStage stage) {
    return IntStream.range(0, count)
        .mapToObj(i -> new Participant(stage + " attendee " + i, age, Gender.MALE, stage))
        .toList();
  }

  private List<Facilitator> leaders(int count, AgeTier tier) {
    return IntStream.range(0, count)
        .mapToObj(i -> new Facilitator("Leader " + i, Role.LEADER, Gender.MALE, List.of(tier)))
        .toList();
  }
}
