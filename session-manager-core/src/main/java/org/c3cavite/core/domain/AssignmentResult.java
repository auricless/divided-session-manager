package org.c3cavite.core.domain;

import java.util.Collections;
import java.util.List;

public class AssignmentResult {
    private final List<Group> groups;
    private final List<UnassignedParticipant> unassignedParticipants;
    private final List<String> warnings;

    public AssignmentResult(List<Group> groups, List<UnassignedParticipant> unassignedParticipants, List<String> warnings) {
        this.groups = Collections.unmodifiableList(groups);
        this.unassignedParticipants = Collections.unmodifiableList(unassignedParticipants);
        this.warnings = Collections.unmodifiableList(warnings);
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<UnassignedParticipant> getUnassignedParticipants() {
        return unassignedParticipants;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public int getTotalGroups() {
        return groups.size();
    }

    public int getTotalAssignedParticipants() {
        return groups.stream()
                .mapToInt(g -> g.getParticipants().size())
                .sum();
    }
}
