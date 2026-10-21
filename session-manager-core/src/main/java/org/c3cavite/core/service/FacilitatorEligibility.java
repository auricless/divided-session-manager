package org.c3cavite.core.service;

import org.c3cavite.core.domain.*;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared rules for staffing counts and assignment; role requirements are never waived. */
final class FacilitatorEligibility {
    private final SessionConfig config;
    private final Map<Facilitator, FacilitatorException> exceptions = new IdentityHashMap<>();

    FacilitatorEligibility(SessionConfig config, List<Facilitator> roster, List<FacilitatorException> exceptions) {
        this.config = config;
        Set<Facilitator> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Facilitator facilitator : roster) {
            if (facilitator == null || !unique.add(facilitator)) {
                throw new IllegalArgumentException("Roster entries must be non-null, unique facilitator instances");
            }
        }
        for (FacilitatorException exception : exceptions) {
            if (exception == null || !unique.contains(exception.facilitator())) {
                throw new IllegalArgumentException("Exceptions must identify a facilitator instance in the roster");
            }
            if (this.exceptions.put(exception.facilitator(), exception) != null) {
                throw new IllegalArgumentException("Only one exception entry is allowed per facilitator");
            }
        }
    }

    boolean permits(Facilitator facilitator, Role role, AgeTier tier, Gender gender) {
        FacilitatorException exception = exceptions.get(facilitator);
        return facilitator.getRole() == role
                && (facilitator.getQualifiedTiers().contains(tier)
                    || exception != null && exception.allowAgeTierMismatch())
                && (!genderMismatch(facilitator, gender)
                    || exception != null && exception.allowGenderMismatch());
    }

    int exceptionCount(Facilitator facilitator, AgeTier tier, Gender gender) {
        return (facilitator.getQualifiedTiers().contains(tier) ? 0 : 1)
                + (genderMismatch(facilitator, gender) ? 1 : 0);
    }

    String exceptionDescription(Facilitator facilitator, AgeTier tier, Gender gender) {
        boolean qualification = !facilitator.getQualifiedTiers().contains(tier);
        boolean genderException = genderMismatch(facilitator, gender);
        return qualification && genderException ? "age-tier qualification and gender"
                : qualification ? "age-tier qualification" : "gender";
    }

    private boolean genderMismatch(Facilitator facilitator, Gender gender) {
        return config.getGenderMode() == GenderMode.SEPARATED && gender != null && facilitator.getGender() != gender;
    }
}
