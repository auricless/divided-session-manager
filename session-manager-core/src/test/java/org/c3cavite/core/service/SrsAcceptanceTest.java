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
import org.c3cavite.core.domain.AssignmentResult;
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
    List<Participant> roster = participants(200);
    List<Facilitator> staff = facilitators(200);
    SessionConfig separated = config(6, GenderMode.SEPARATED);
    SessionConfig mixed = config(4, GenderMode.MIXED);
    List<String> original = signature(service.assign(separated, roster, staff));
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
        assertEquals(signature(service.assign(config, roster, staff)), signature(result));
      }
    } finally {
      executor.shutdownNow();
    }
    assertEquals(original, signature(service.assign(separated, roster, staff)));
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
    assertEquals(roster.size(), result.getTotalAssignedParticipants());
    assertTrue(result.getUnassignedParticipants().isEmpty());
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
    assertEquals(new HashSet<>(roster), assigned);
    assertEquals(result.getBlockers().isEmpty(), result.isFinal());
  }

  private List<String> signature(AssignmentResult result) {
    return result.getGroups().stream()
        .map(g -> g.getParticipants().toString() + ":" + g.getLeader())
        .toList();
  }
}
