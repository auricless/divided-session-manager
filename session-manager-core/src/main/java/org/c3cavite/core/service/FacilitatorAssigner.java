package org.c3cavite.core.service;

import org.c3cavite.core.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FacilitatorAssigner {

    public List<String> assign(List<Group> groups, List<Facilitator> facilitators, SessionConfig config) {
        List<String> warnings = new ArrayList<>();
        List<Facilitator> available = new ArrayList<>(facilitators);

        // 1. Assign Leaders
        for (Group group : groups) {
            Optional<Facilitator> leader = findBestFacilitator(available, Role.LEADER, group);
            if (leader.isPresent()) {
                group.assignLeader(leader.get());
                available.remove(leader.get());
            } else {
                warnings.add("Group for " + group.getAgeTierTarget() + 
                             (group.getGenderTarget() != null ? " (" + group.getGenderTarget() + ")" : "") + 
                             " is leaderless.");
            }
        }

        // 2. Assign Assistants
        for (Group group : groups) {
            Optional<Facilitator> assistant = findBestFacilitator(available, Role.ASSISTANT, group);
            if (assistant.isPresent()) {
                group.assignAssistant(assistant.get());
                available.remove(assistant.get());
            }
        }

        return warnings;
    }

    private Optional<Facilitator> findBestFacilitator(List<Facilitator> available, Role role, Group group) {
        List<Facilitator> candidates = available.stream()
                .filter(f -> f.getRole() == role)
                .filter(f -> f.getQualifiedTiers().contains(group.getAgeTierTarget()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Prefer gender match if target is set
        if (group.getGenderTarget() != null) {
            Optional<Facilitator> genderMatch = candidates.stream()
                    .filter(f -> f.getGender() == group.getGenderTarget())
                    .findFirst();
            if (genderMatch.isPresent()) {
                return genderMatch;
            }
        }

        return candidates.stream().findFirst();
    }
}
