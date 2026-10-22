package org.c3cavite.core.domain;

import java.util.Objects;

/** Explicit, run-scoped permission for a specific facilitator instance from the supplied roster. */
public record FacilitatorException(
    Facilitator facilitator, boolean allowAgeTierMismatch, boolean allowGenderMismatch) {
  public FacilitatorException {
    Objects.requireNonNull(facilitator, "Facilitator is required");
  }
}
