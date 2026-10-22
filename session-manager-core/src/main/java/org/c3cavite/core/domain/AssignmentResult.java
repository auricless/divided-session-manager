package org.c3cavite.core.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable snapshot of an assignment and the actions needed before it is final. */
public class AssignmentResult {
  private final List<Group> groups;
  private final List<UnassignedParticipant> unassignedParticipants;
  private final List<String> warnings;
  private final List<AssignmentBlocker> blockers;
  private final AssignmentState state;

  /** Retained for callers without cap validation; the assignment service supplies the event cap. */
  public AssignmentResult(
      List<Group> groups,
      List<UnassignedParticipant> unassignedParticipants,
      List<String> warnings) {
    this(groups, unassignedParticipants, warnings, Integer.MAX_VALUE);
  }

  public AssignmentResult(
      List<Group> groups,
      List<UnassignedParticipant> unassignedParticipants,
      List<String> warnings,
      int capSize) {
    if (capSize <= 0) {
      throw new IllegalArgumentException("Cap size must be positive");
    }
    this.groups = groups.stream().map(Group::immutableCopy).toList();
    this.unassignedParticipants = List.copyOf(unassignedParticipants);
    this.warnings = List.copyOf(warnings);
    this.blockers = findBlockers(capSize);
    this.state = blockers.isEmpty() ? AssignmentState.FINAL : AssignmentState.NON_FINAL_PROPOSAL;
  }

  private List<AssignmentBlocker> findBlockers(int capSize) {
    List<AssignmentBlocker> found = new ArrayList<>();
    Map<DemographicSegment, Integer> segmentCounts = new HashMap<>();
    for (Group group : groups) {
      segmentCounts.merge(
          new DemographicSegment(group.getAgeTierTarget(), group.getGenderTarget()),
          1,
          Integer::sum);
    }
    for (int i = 0; i < groups.size(); i++) {
      Group group = groups.get(i);
      int number = i + 1;
      String segment =
          group.getAgeTierTarget()
              + (group.getGenderTarget() == null ? "" : " / " + group.getGenderTarget())
              + (group.getLifeStageTarget() == null ? "" : " / " + group.getLifeStageTarget());
      if (group.getLeader() == null) {
        found.add(
            new AssignmentBlocker(
                AssignmentBlocker.Code.MISSING_LEADER,
                number,
                "Assign a LEADER to group "
                    + number
                    + " ("
                    + segment
                    + "); provide eligible staff or an explicit facilitator exception and rerun."));
      } else if (group.getLeader().getRole() != Role.LEADER) {
        found.add(
            new AssignmentBlocker(
                AssignmentBlocker.Code.INVALID_LEADER_ROLE,
                number,
                "Replace the facilitator in group " + number + " with a LEADER and rerun."));
      }
      if (group.getParticipants().size() > capSize) {
        found.add(
            new AssignmentBlocker(
                AssignmentBlocker.Code.GROUP_CAP_EXCEEDED,
                number,
                "Redistribute group "
                    + number
                    + " within the configured cap of "
                    + capSize
                    + ", or change the event cap and rerun."));
      }
      if (group.getParticipants().size() < 2
          && segmentCounts.get(
                  new DemographicSegment(group.getAgeTierTarget(), group.getGenderTarget()))
              > 1) {
        found.add(
            new AssignmentBlocker(
                AssignmentBlocker.Code.MINIMUM_GROUP_SIZE,
                number,
                "Group "
                    + number
                    + " needs at least 2 participants because its tier/gender segment has multiple groups. "
                    + "Adjust attendee distribution or event configuration and rerun; keep the hard cap and demographic rules."));
      }
    }
    return List.copyOf(found);
  }

  private record DemographicSegment(AgeTier tier, Gender gender) {}

  public AssignmentState getState() {
    return state;
  }

  public boolean isFinal() {
    return state == AssignmentState.FINAL;
  }

  public List<AssignmentBlocker> getBlockers() {
    return blockers;
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
    return groups.stream().mapToInt(g -> g.getParticipants().size()).sum();
  }
}
