package org.c3cavite.core.domain;

/** Inclusive event-specific ages for one tier. Under-13 attendees are handled separately. */
public record AgeRange(int minAge, int maxAge) {
    public AgeRange {
        if (minAge < AgeTier.TEENS.getMinAge()) {
            throw new IllegalArgumentException("Age ranges must start at age 13 or above");
        }
        if (maxAge < minAge) {
            throw new IllegalArgumentException("maxAge cannot be less than minAge");
        }
    }

    public boolean contains(int age) {
        return age >= minAge && age <= maxAge;
    }
}
