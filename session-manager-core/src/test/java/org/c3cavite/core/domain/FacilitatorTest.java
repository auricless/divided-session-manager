package org.c3cavite.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class FacilitatorTest {
  @Test
  void requiredFieldsAndQualificationsAreValidated() {
    for (String name : Arrays.asList(null, "", " ")) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Facilitator(name, Role.LEADER, Gender.MALE, List.of(AgeTier.YOUTH)));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> new Facilitator("Leader", null, Gender.MALE, List.of(AgeTier.YOUTH)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Facilitator("Leader", Role.LEADER, null, List.of(AgeTier.YOUTH)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Facilitator("Leader", Role.LEADER, Gender.MALE, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Facilitator("Leader", Role.LEADER, Gender.MALE, Arrays.asList((AgeTier) null)));
  }

  @Test
  void emptyQualificationsAreAValidRosterValueForExplicitOrganizerOverrides() {
    Facilitator facilitator =
        new Facilitator("Unqualified leader", Role.LEADER, Gender.MALE, List.of());
    assertTrue(facilitator.getQualifiedTiers().isEmpty());
    assertThrows(
        UnsupportedOperationException.class,
        () -> facilitator.getQualifiedTiers().add(AgeTier.YOUTH));
  }

  @Test
  void valuesHaveEqualityHashingAndReadableDescriptions() {
    Facilitator first = new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.YOUTH));
    Facilitator equal = new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.YOUTH));
    assertEquals(first, equal);
    assertEquals(first.hashCode(), equal.hashCode());
    assertNotEquals(
        first, new Facilitator("Assistant", Role.ASSISTANT, Gender.FEMALE, List.of(AgeTier.TEENS)));
    assertTrue(first.toString().contains("Leader"));
    assertTrue(first.toString().contains("YOUTH"));
  }

  @Test
  void qualificationListsAreDefensivelyCopiedAndUnmodifiable() {
    List<AgeTier> source = new ArrayList<>(List.of(AgeTier.YOUTH));
    Facilitator facilitator = new Facilitator("Leader", Role.LEADER, Gender.MALE, source);
    source.clear();
    assertEquals(List.of(AgeTier.YOUTH), facilitator.getQualifiedTiers());
    assertThrows(
        UnsupportedOperationException.class, () -> facilitator.getQualifiedTiers().clear());
  }
}
