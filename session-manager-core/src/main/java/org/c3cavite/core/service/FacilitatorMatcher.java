package org.c3cavite.core.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.c3cavite.core.domain.Facilitator;
import org.c3cavite.core.domain.Group;
import org.c3cavite.core.domain.Role;

/** Maximum matching prevents a flexible facilitator from stranding a more restricted group. */
final class FacilitatorMatcher {
  Map<Group, Facilitator> match(
      List<Group> groups, List<Facilitator> roster, FacilitatorEligibility eligibility, Role role) {
    Map<Group, List<Facilitator>> candidates = new IdentityHashMap<>();
    for (Group group : groups) {
      candidates.put(
          group,
          roster.stream()
              .filter(
                  f ->
                      eligibility.permits(
                          f, role, group.getAgeTierTarget(), group.getGenderTarget()))
              .sorted(
                  Comparator.comparingInt(
                      f ->
                          eligibility.exceptionCount(
                              f, group.getAgeTierTarget(), group.getGenderTarget())))
              .toList());
    }
    Map<Facilitator, Group> occupied = new IdentityHashMap<>();
    Map<Group, List<Facilitator>> normalCandidates = new IdentityHashMap<>();
    for (Group group : groups) {
      normalCandidates.put(
          group,
          candidates.get(group).stream()
              .filter(
                  f ->
                      eligibility.exceptionCount(
                              f, group.getAgeTierTarget(), group.getGenderTarget())
                          == 0)
              .toList());
    }
    for (Group group : groups) {
      assign(group, normalCandidates, occupied, Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    // Try exceptions only after exhausting normal coverage; never introduce one into a fully
    // staffed result.
    for (Group group : groups) {
      if (!occupied.containsValue(group)) {
        assign(group, candidates, occupied, Collections.newSetFromMap(new IdentityHashMap<>()));
      }
    }
    Map<Group, Facilitator> result = new IdentityHashMap<>();
    occupied.forEach((facilitator, group) -> result.put(group, facilitator));
    return result;
  }

  private boolean assign(
      Group group,
      Map<Group, List<Facilitator>> candidates,
      Map<Facilitator, Group> occupied,
      Set<Facilitator> visited) {
    for (Facilitator candidate : candidates.get(group)) {
      if (!visited.add(candidate)) {
        continue;
      }
      Group previous = occupied.get(candidate);
      // Move an existing assignment only if its group can still be staffed.
      if (previous == null || assign(previous, candidates, occupied, visited)) {
        occupied.put(candidate, group);
        return true;
      }
    }
    return false;
  }
}
