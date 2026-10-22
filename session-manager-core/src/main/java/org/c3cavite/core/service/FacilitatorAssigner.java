package org.c3cavite.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.c3cavite.core.domain.Facilitator;
import org.c3cavite.core.domain.FacilitatorException;
import org.c3cavite.core.domain.Group;
import org.c3cavite.core.domain.Role;
import org.c3cavite.core.domain.SessionConfig;

public class FacilitatorAssigner {

  public List<String> assign(
      List<Group> groups, List<Facilitator> facilitators, SessionConfig config) {
    return assign(groups, facilitators, config, List.of());
  }

  public List<String> assign(
      List<Group> groups,
      List<Facilitator> facilitators,
      SessionConfig config,
      List<FacilitatorException> exceptions) {
    return assign(
        groups, facilitators, new FacilitatorEligibility(config, facilitators, exceptions));
  }

  List<String> assign(
      List<Group> groups, List<Facilitator> facilitators, FacilitatorEligibility eligibility) {
    List<String> warnings = new ArrayList<>();
    List<Facilitator> available = new ArrayList<>(facilitators);
    assignRole(groups, available, eligibility, Role.LEADER, warnings);
    assignRole(groups, available, eligibility, Role.ASSISTANT, warnings);
    return warnings;
  }

  private void assignRole(
      List<Group> groups,
      List<Facilitator> available,
      FacilitatorEligibility eligibility,
      Role role,
      List<String> warnings) {
    Map<Group, Facilitator> assignments =
        new FacilitatorMatcher().match(groups, available, eligibility, role);
    for (int i = 0; i < groups.size(); i++) {
      Group group = groups.get(i);
      Facilitator facilitator = assignments.get(group);
      if (facilitator == null) {
        if (role == Role.LEADER) {
          group.assignLeader(null);
          warnings.add("Group " + (i + 1) + " for " + group.getAgeTierTarget() + " is leaderless.");
        } else {
          group.assignAssistant(null);
        }
        continue;
      }
      if (role == Role.LEADER) {
        group.assignLeader(facilitator);
      } else {
        group.assignAssistant(facilitator);
      }
      available.removeIf(candidate -> candidate == facilitator);
      if (eligibility.exceptionCount(facilitator, group.getAgeTierTarget(), group.getGenderTarget())
          > 0) {
        warnings.add(
            "Organizer exception used for "
                + role
                + " "
                + facilitator.getName()
                + " in group "
                + (i + 1)
                + ": "
                + eligibility.exceptionDescription(
                    facilitator, group.getAgeTierTarget(), group.getGenderTarget())
                + ".");
      }
    }
  }
}
