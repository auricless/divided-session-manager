package org.c3cavite.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.c3cavite.core.domain.AgeTier;
import org.c3cavite.core.domain.AssignmentBlocker;
import org.c3cavite.core.domain.AssignmentResult;
import org.c3cavite.core.domain.AssignmentState;
import org.c3cavite.core.domain.Facilitator;
import org.c3cavite.core.domain.Gender;
import org.c3cavite.core.domain.GenderMode;
import org.c3cavite.core.domain.LifeStage;
import org.c3cavite.core.domain.Participant;
import org.c3cavite.core.domain.Role;
import org.c3cavite.core.domain.SessionConfig;
import org.junit.jupiter.api.Test;

class SrsAcceptanceTest {
  private final GroupAssignmentService service = new GroupAssignmentService();

  @Test
  void validBoundaryConfigurationsPreserveEveryAttendeeAndHardConstraint() {
    for (int count : List.of(0, 1, 2, 3, 7, 23, 199, 200)) {
      for (int cap : List.of(1, 2, 4, 6, Integer.MAX_VALUE)) {
        for (GenderMode mode : GenderMode.values()) {
          SessionConfig config = config(cap, mode);
          List<Participant> roster = participants(count);
          AssignmentResult result = service.assign(config, roster, facilitators(200));
          verify(config, roster, result);
        }
      }
    }
  }

  @Test
  void assignsTwoHundredAttendeesWithinFiveHundredMilliseconds() {
    List<Participant> roster = participants(200);
    for (GenderMode mode : GenderMode.values()) {
      for (int cap : List.of(1, 6, Integer.MAX_VALUE)) {
        SessionConfig config = config(cap, mode);
        for (int staffCount : List.of(4, 200)) {
          List<Facilitator> staff = facilitators(staffCount);
          for (int warmup = 0; warmup < 5; warmup++) {
            service.assign(config, roster, staff);
          }
          AssignmentResult result =
              assertTimeout(
                  Duration.ofMillis(500),
                  () -> service.assign(config, roster, staff),
                  "200 attendees, cap=" + cap + ", mode=" + mode + ", staff=" + staffCount);
          verify(config, roster, result);
        }
      }
    }
  }

  @Test
  void sharedServiceAndInputsAreIndependentAcrossConcurrentInvocations() throws Exception {
    List<Participant> roster = new ArrayList<>(participants(200));
    roster.add(new Participant("Underage attendee", 12, Gender.MALE, null));
    List<Facilitator> staff = new ArrayList<>(facilitators(20));
    staff.add(new Facilitator("Assistant", Role.ASSISTANT, Gender.MALE, List.of(AgeTier.values())));
    SessionConfig separated = config(6, GenderMode.SEPARATED);
    SessionConfig mixed = config(4, GenderMode.MIXED);
    AssignmentResult originalResult = service.assign(separated, roster, staff);
    ResultSnapshot original = signature(originalResult);
    ResultSnapshot mixedExpected = signature(service.assign(mixed, roster, staff));
    var executor = Executors.newFixedThreadPool(4);
    try {
      List<Callable<AssignmentResult>> runs = new ArrayList<>();
      for (int i = 0; i < 20; i++) {
        SessionConfig config = i % 2 == 0 ? separated : mixed;
        runs.add(() -> service.assign(config, roster, staff));
      }
      var results = executor.invokeAll(runs);
      for (int i = 0; i < results.size(); i++) {
        SessionConfig config = i % 2 == 0 ? separated : mixed;
        AssignmentResult result = results.get(i).get();
        verify(config, roster, result);
        assertEquals(i % 2 == 0 ? original : mixedExpected, signature(result));
      }
    } finally {
      executor.shutdownNow();
    }
    assertEquals(original, signature(originalResult));
    assertEquals(original, signature(service.assign(separated, roster, staff)));
  }

  @Test
  void snapshotDetectsWarningChangesEvenWhenMembershipIsUnchanged() {
    AssignmentResult original =
        service.assign(config(6, GenderMode.SEPARATED), participants(2), facilitators(4));
    AssignmentResult changed =
        new AssignmentResult(
            original.getGroups(),
            original.getUnassignedParticipants(),
            List.of("A different run leaked this warning"),
            6);
    assertNotEquals(signature(original), signature(changed));
  }

  private SessionConfig config(int cap, GenderMode mode) {
    return SessionConfig.builder()
        .capSize(cap)
        .targetSize(Math.min(4, cap))
        .genderMode(mode)
        .build();
  }

  private List<Participant> participants(int count) {
    return IntStream.range(0, count)
        .mapToObj(
            i ->
                new Participant(
                    "Attendee " + i,
                    i % 3 == 0 ? 14 : i % 3 == 1 ? 18 : 28,
                    i % 2 == 0 ? Gender.MALE : Gender.FEMALE,
                    i % 4 < 2 ? LifeStage.STUDENT : LifeStage.WORKING_PROFESSIONAL))
        .toList();
  }

  private List<Facilitator> facilitators(int count) {
    return IntStream.range(0, count)
        .mapToObj(
            i ->
                new Facilitator(
                    "Leader " + i,
                    Role.LEADER,
                    i % 2 == 0 ? Gender.MALE : Gender.FEMALE,
                    List.of(AgeTier.values())))
        .toList();
  }

  private void verify(SessionConfig config, List<Participant> roster, AssignmentResult result) {
    assertEquals(
        roster.size(),
        result.getTotalAssignedParticipants() + result.getUnassignedParticipants().size());
    var assigned = new HashSet<Participant>();
    var leaders = new HashSet<String>();
    for (var group : result.getGroups()) {
      assertTrue(group.getSize() <= config.getCapSize());
      for (Participant participant : group.getParticipants()) {
        assertTrue(assigned.add(participant), "No attendee may be assigned twice");
        assertEquals(group.getAgeTierTarget(), participant.getAgeTier(config));
        if (config.getGenderMode() == GenderMode.SEPARATED) {
          assertEquals(group.getGenderTarget(), participant.getGender());
        }
        if (group.getAgeTierTarget() == AgeTier.YOUNG_ADULT) {
          assertEquals(group.getLifeStageTarget(), participant.getLifeStage());
        }
      }
      if (group.getLeader() != null) {
        assertTrue(leaders.add(group.getLeader().getName()), "No leader may staff two groups");
        assertEquals(Role.LEADER, group.getLeader().getRole());
        if (config.getGenderMode() == GenderMode.SEPARATED) {
          assertEquals(group.getGenderTarget(), group.getLeader().getGender());
        }
      } else {
        assertFalse(result.isFinal());
        assertFalse(result.getBlockers().isEmpty());
      }
    }
    result
        .getUnassignedParticipants()
        .forEach(unassigned -> assertTrue(assigned.add(unassigned.participant())));
    assertEquals(new HashSet<>(roster), assigned);
    assertEquals(result.getBlockers().isEmpty(), result.isFinal());
  }

  private ResultSnapshot signature(AssignmentResult result) {
    return new ResultSnapshot(
        result.getState(),
        result.getTotalGroups(),
        result.getTotalAssignedParticipants(),
        result.getGroups().stream()
            .map(
                g ->
                    new GroupSnapshot(
                        g.getGenderTarget(),
                        g.getAgeTierTarget(),
                        g.getLifeStageTarget(),
                        g.getSize(),
                        g.getParticipants().stream().map(this::participantSnapshot).toList(),
                        g.getLeader(),
                        g.getAssistant()))
            .toList(),
        result.getBlockers().stream()
            .map(b -> new BlockerSnapshot(b.getCode(), b.getGroupNumber(), b.getMessage()))
            .toList(),
        result.getUnassignedParticipants().stream()
            .map(u -> new UnassignedSnapshot(participantSnapshot(u.participant()), u.reason()))
            .toList(),
        List.copyOf(result.getWarnings()));
  }

  private ParticipantSnapshot participantSnapshot(Participant participant) {
    return new ParticipantSnapshot(
        participant.getName(),
        participant.getAge(),
        participant.getGender(),
        participant.getLifeStage(),
        participant.getAgeTier());
  }

  private record ResultSnapshot(
      AssignmentState state,
      int totalGroups,
      int totalAssigned,
      List<GroupSnapshot> groups,
      List<BlockerSnapshot> blockers,
      List<UnassignedSnapshot> unassigned,
      List<String> warnings) {}

  private record GroupSnapshot(
      Gender gender,
      AgeTier tier,
      LifeStage lifeStage,
      int size,
      List<ParticipantSnapshot> participants,
      Facilitator leader,
      Facilitator assistant) {}

  private record ParticipantSnapshot(
      String name, int age, Gender gender, LifeStage lifeStage, AgeTier tier) {}

  private record BlockerSnapshot(AssignmentBlocker.Code code, int number, String message) {}

  private record UnassignedSnapshot(ParticipantSnapshot participant, String reason) {}
}
