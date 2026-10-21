package org.c3cavite.core.service;

import org.c3cavite.core.domain.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class FacilitatorAssigner {

    public List<String> assign(List<Group> groups, List<Facilitator> facilitators, SessionConfig config) {
        return assign(groups, facilitators, config, List.of());
    }

    public List<String> assign(List<Group> groups, List<Facilitator> facilitators, SessionConfig config,
                               List<FacilitatorException> exceptions) {
        return assign(groups, facilitators, new FacilitatorEligibility(config, facilitators, exceptions));
    }

    List<String> assign(List<Group> groups, List<Facilitator> facilitators, FacilitatorEligibility eligibility) {
        List<String> warnings = new ArrayList<>();
        List<Facilitator> available = new ArrayList<>(facilitators);
        assignRole(groups, available, eligibility, Role.LEADER, warnings);
        assignRole(groups, available, eligibility, Role.ASSISTANT, warnings);
        return warnings;
    }

    private void assignRole(List<Group> groups, List<Facilitator> available, FacilitatorEligibility eligibility,
                            Role role, List<String> warnings) {
        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);
            Optional<Facilitator> selected = available.stream()
                    .filter(f -> eligibility.permits(f, role, group.getAgeTierTarget(), group.getGenderTarget()))
                    .min(Comparator.comparingInt(f -> eligibility.exceptionCount(f,
                            group.getAgeTierTarget(), group.getGenderTarget())));
            if (selected.isEmpty()) {
                if (role == Role.LEADER) {
                    warnings.add("Group " + (i + 1) + " for " + group.getAgeTierTarget() + " is leaderless.");
                }
                continue;
            }
            Facilitator facilitator = selected.get();
            if (role == Role.LEADER) group.assignLeader(facilitator);
            else group.assignAssistant(facilitator);
            available.remove(facilitator);
            if (eligibility.exceptionCount(facilitator, group.getAgeTierTarget(), group.getGenderTarget()) > 0) {
                warnings.add("Organizer exception used for " + role + " " + facilitator.getName() + " in group "
                        + (i + 1) + ": " + eligibility.exceptionDescription(facilitator,
                                group.getAgeTierTarget(), group.getGenderTarget()) + ".");
            }
        }
    }
}
