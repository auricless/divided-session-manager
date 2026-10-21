package org.c3cavite.core.service;

import org.c3cavite.core.domain.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GroupAssignmentService {
    private final SegmentationStrategy segmentationStrategy = new SegmentationStrategy();
    private final FacilitatorAssigner facilitatorAssigner = new FacilitatorAssigner();

    public AssignmentResult assign(SessionConfig config, List<Participant> participants, List<Facilitator> facilitators) {
        return assign(config, participants, facilitators, List.of());
    }

    public AssignmentResult assign(SessionConfig config, List<Participant> participants, List<Facilitator> facilitators,
                                   List<FacilitatorException> exceptions) {
        validateInputs(config, participants, facilitators, exceptions);
        FacilitatorEligibility eligibility = new FacilitatorEligibility(config, facilitators, exceptions);
        List<UnassignedParticipant> unassigned = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<Participant> eligible = new ArrayList<>();
        for (Participant input : participants) {
            Participant participant = input.forSession(config);
            AgeTier tier = participant.getAgeTier();
            if (tier == null) {
                boolean underage = participant.getAge() < AgeTier.TEENS.getMinAge();
                unassigned.add(new UnassignedParticipant(participant,
                        underage ? "Age under 13 or outside supported range" : "UNKNOWN_TIER"));
                if (underage) warnings.add("Excluded " + participant.getName() + ": age under 13.");
            } else if (!config.getActiveTiers().contains(tier)) {
                unassigned.add(new UnassignedParticipant(participant, "Age tier " + tier + " is not active for this session"));
            } else {
                eligible.add(participant);
            }
        }
        Map<SegmentKey, List<Participant>> segments = segmentationStrategy.segment(eligible, config);
        List<SegmentKey> ordered = new ArrayList<>(segments.keySet());
        ordered.sort(Comparator.comparingInt((SegmentKey key) -> key.ageTier() == AgeTier.YOUTH ? 0
                        : key.ageTier() == AgeTier.TEENS ? 1 : 2)
                .thenComparing(SegmentKey::gender, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(SegmentKey::lifeStage, Comparator.nullsFirst(Comparator.naturalOrder())));
        List<SegmentPlan> plans = new ArrayList<>();
        for (SegmentKey key : ordered) {
            List<Participant> attendees = segments.get(key);
            int count = initialGroupCount(attendees.size(), config);
            plans.add(new SegmentPlan(key, attendees, count));
        }
        StaffedGroups staffed = consolidate(plans, facilitators, eligibility, config.getCapSize());
        warnings.addAll(staffed.warnings());
        return new AssignmentResult(staffed.groups(), unassigned, warnings, config.getCapSize());
    }

    private StaffedGroups consolidate(List<SegmentPlan> plans, List<Facilitator> facilitators,
                                      FacilitatorEligibility eligibility, int cap) {
        StaffedGroups current = staff(plans, facilitators, eligibility);
        while (current.missingLeaders() > 0) {
            boolean improved = false;
            for (int i = 0; i < plans.size(); i++) {
                SegmentPlan plan = plans.get(i);
                if (plan.count() <= ceilDivide(plan.participants().size(), cap)) continue;
                List<SegmentPlan> trialPlans = new ArrayList<>(plans);
                trialPlans.set(i, new SegmentPlan(plan.key(), plan.participants(), plan.count() - 1));
                StaffedGroups trial = staff(trialPlans, facilitators, eligibility);
                if (trial.missingLeaders() < current.missingLeaders()) {
                    plans = trialPlans;
                    current = trial;
                    improved = true;
                    break;
                }
            }
            if (!improved) break;
        }
        return current;
    }

    private StaffedGroups staff(List<SegmentPlan> plans, List<Facilitator> facilitators, FacilitatorEligibility eligibility) {
        List<Group> groups = new ArrayList<>();
        for (SegmentPlan plan : plans) {
            List<Group> segmentGroups = new ArrayList<>();
            for (int i = 0; i < plan.count(); i++) {
                segmentGroups.add(new Group(plan.key().gender(), plan.key().ageTier(), plan.key().lifeStage()));
            }
            for (int i = 0; i < plan.participants().size(); i++) {
                segmentGroups.get(i % plan.count()).addParticipant(plan.participants().get(i));
            }
            groups.addAll(segmentGroups);
        }
        return new StaffedGroups(groups, facilitatorAssigner.assign(groups, facilitators, eligibility));
    }

    private void validateInputs(SessionConfig config, List<Participant> participants, List<Facilitator> facilitators,
                                List<FacilitatorException> exceptions) {
        if (config == null || participants == null || facilitators == null || exceptions == null
                || participants.stream().anyMatch(p -> p == null)) {
            throw new IllegalArgumentException("Config, input lists, and participant entries must be non-null");
        }
    }

    private int ceilDivide(int count, int divisor) { return 1 + (count - 1) / divisor; }

    private int initialGroupCount(int attendees, SessionConfig config) {
        int requiredByCap = ceilDivide(attendees, config.getCapSize());
        int desiredByTarget = ceilDivide(attendees, config.getTargetSize());
        int withoutSingletons = Math.max(1, attendees / 2);
        return Math.max(requiredByCap, Math.min(desiredByTarget, withoutSingletons));
    }

    private record SegmentPlan(SegmentKey key, List<Participant> participants, int count) { }
    private record StaffedGroups(List<Group> groups, List<String> warnings) {
        long missingLeaders() { return groups.stream().filter(g -> g.getLeader() == null).count(); }
    }
}
