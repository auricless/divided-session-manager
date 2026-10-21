package org.c3cavite.sessionmanager.api;

import org.c3cavite.core.domain.AgeTier;
import org.c3cavite.core.domain.Gender;
import org.c3cavite.core.domain.GenderMode;
import org.c3cavite.core.domain.LifeStage;
import org.c3cavite.core.domain.Role;

import java.util.List;
import java.util.Map;

public record AssignmentRequest(Config config, List<ParticipantInput> participants,
                                List<FacilitatorInput> facilitators, List<ExceptionInput> facilitatorExceptions) {
    public record Config(Integer capSize, Integer targetSize, GenderMode genderMode,
                         List<AgeTier> activeTiers, Boolean lifeStageSubgroupingEnabled,
                         Map<AgeTier, RangeInput> ageTierBoundaries) { }

    public record RangeInput(Integer minAge, Integer maxAge) { }

    public record ParticipantInput(String name, Integer age, Gender gender, LifeStage lifeStage) { }

    public record FacilitatorInput(String name, Role role, Gender gender, List<AgeTier> qualifiedTiers) { }

    public record ExceptionInput(Integer facilitatorIndex, Boolean allowAgeTierMismatch, Boolean allowGenderMismatch) { }
}
