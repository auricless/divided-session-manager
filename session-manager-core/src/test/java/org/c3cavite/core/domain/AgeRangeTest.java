package org.c3cavite.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AgeRangeTest {
  @ParameterizedTest
  @CsvSource({"12, 15", "-1, 15", "18, 17"})
  void rejectsUnderageOrReversedRange(int min, int max) {
    assertThrows(IllegalArgumentException.class, () -> new AgeRange(min, max));
  }

  @ParameterizedTest
  @CsvSource({"12, false", "13, true", "17, true", "18, false"})
  void containsInclusiveAges(int age, boolean expected) {
    assertEquals(expected, new AgeRange(13, 17).contains(age));
  }
}
