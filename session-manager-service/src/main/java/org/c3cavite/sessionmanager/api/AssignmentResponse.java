package org.c3cavite.sessionmanager.api;

import org.c3cavite.core.domain.AgeTier;
import org.c3cavite.core.domain.AssignmentBlocker;
import org.c3cavite.core.domain.AssignmentState;
import org.c3cavite.core.domain.Gender;
import org.c3cavite.core.domain.LifeStage;
import org.c3cavite.core.domain.Role;

import java.util.List;

public record AssignmentResponse(AssignmentState state, List<GroupOutput> groups,
                                 List<BlockerOutput> blockers, List<UnassignedOutput> unassignedParticipants,
                                 List<String> warnings, int totalGroups, int totalAssignedParticipants) {
    public record GroupOutput(Gender genderTarget, AgeTier ageTierTarget, LifeStage lifeStageTarget,
                              List<ParticipantOutput> participants, FacilitatorOutput leader,
                              FacilitatorOutput assistant, int size) { }

    public record ParticipantOutput(String name, int age, Gender gender, LifeStage lifeStage, AgeTier ageTier) { }

    public record FacilitatorOutput(String name, Role role, Gender gender, List<AgeTier> qualifiedTiers) { }

    public record BlockerOutput(AssignmentBlocker.Code code, int groupNumber, String message) { }

    public record UnassignedOutput(ParticipantOutput participant, String reason) { }
}
