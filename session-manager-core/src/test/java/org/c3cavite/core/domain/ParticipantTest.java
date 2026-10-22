package org.c3cavite.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ParticipantTest {

  @Test
  void derivesTierForEachSessionWithoutChangingParticipant() {
    Participant participant = new Participant("Attendee", 17, Gender.MALE, null);
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

    assertEquals(AgeTier.TEENS, participant.getAgeTier(custom));
    assertEquals(AgeTier.YOUTH, participant.getAgeTier(SessionConfig.builder().capSize(6).build()));
    assertEquals(AgeTier.YOUTH, participant.getAgeTier());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void rejectsMissingName(String name) {
    assertThrows(
        IllegalArgumentException.class, () -> new Participant(name, 16, Gender.MALE, null));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
  void rejectsNonPositiveAge(int age) {
    assertThrows(
        IllegalArgumentException.class, () -> new Participant("Attendee", age, Gender.MALE, null));
  }

  @Test
  void retainsUnderageParticipantForReporting() {
    Participant participant = new Participant("Child", 12, Gender.FEMALE, null);

    assertNull(participant.getAgeTier());
    assertNull(participant.getLifeStage());
  }

  @Test
  void hasValueEqualityAcrossAllInputAttributes() {
    Participant first = new Participant("Attendee", 25, Gender.MALE, LifeStage.STUDENT);
    Participant same = new Participant("Attendee", 25, Gender.MALE, LifeStage.STUDENT);

    assertEquals(first, same);
    assertEquals(first.hashCode(), same.hashCode());
    assertNotEquals(
        first, new Participant("Attendee", 25, Gender.MALE, LifeStage.WORKING_PROFESSIONAL));
    assertTrue(first.toString().contains("Attendee"));
  }

  @Test
  void rejectsMissingGender() {
    assertThrows(IllegalArgumentException.class, () -> new Participant("Attendee", 17, null, null));
  }

  @Test
  void youngAdultLifeStageIsValidatedAgainstTheSessionTier() {
    Participant attendee = new Participant("Attendee", 21, Gender.MALE, null);
    SessionConfig custom =
        SessionConfig.builder()
            .capSize(6)
            .ageTierBoundaries(
                Map.of(
                    AgeTier.TEENS,
                    new AgeRange(13, 15),
                    AgeTier.YOUTH,
                    new AgeRange(16, 20),
                    AgeTier.YOUNG_ADULT,
                    new AgeRange(21, Integer.MAX_VALUE)))
            .build();
    assertThrows(IllegalArgumentException.class, () -> attendee.forSession(custom));
    assertEquals(
        AgeTier.YOUTH,
        attendee.forSession(SessionConfig.builder().capSize(6).build()).getAgeTier());
  }
}
