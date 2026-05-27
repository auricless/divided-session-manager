package org.c3cavite.core.service;

import org.c3cavite.core.domain.*;

import java.util.*;
import java.util.stream.Collectors;

public class GroupAssignmentService {

    private final SegmentationStrategy segmentationStrategy = new SegmentationStrategy();
    private final FacilitatorAssigner facilitatorAssigner = new FacilitatorAssigner();

    public AssignmentResult assign(SessionConfig config, List<Participant> participants, List<Facilitator> facilitators) {
        List<UnassignedParticipant> unassigned = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Separate valid participants
        List<Participant> validParticipants = new ArrayList<>();
        for (Participant p : participants) {
            if (p.getAgeTier() == null) {
                unassigned.add(new UnassignedParticipant(p, "Age under 13 or outside supported range"));
            } else if (!config.getActiveTiers().contains(p.getAgeTier())) {
                unassigned.add(new UnassignedParticipant(p, "Age tier " + p.getAgeTier() + " is not active for this session"));
            } else {
                validParticipants.add(p);
            }
        }

        // 2. Segment participants
        Map<SegmentKey, List<Participant>> segmented = segmentationStrategy.segment(validParticipants, config);

        // 3. Process segments (Youth first)
        List<SegmentKey> keys = new ArrayList<>(segmented.keySet());
        keys.sort((k1, k2) -> {
            if (k1.ageTier() == AgeTier.YOUTH && k2.ageTier() != AgeTier.YOUTH) return -1;
            if (k1.ageTier() != AgeTier.YOUTH && k2.ageTier() == AgeTier.YOUTH) return 1;
            return 0;
        });

        List<Group> allGroups = new ArrayList<>();
        List<Facilitator> availableLeaders = facilitators.stream()
                .filter(f -> f.getRole() == Role.LEADER)
                .collect(Collectors.toCollection(ArrayList::new));

        for (SegmentKey key : keys) {
            List<Participant> segmentParticipants = segmented.get(key);
            int size = segmentParticipants.size();
            
            // Initial groups based on targetSize
            int groupCount = (int) Math.ceil((double) size / config.getTargetSize());

            // Consolidation logic: if we have more groups than qualified leaders, try to reduce groupCount
            long qualifiedLeaderCount = availableLeaders.stream()
                    .filter(f -> f.getQualifiedTiers().contains(key.ageTier()))
                    .count();
            
            if (groupCount > qualifiedLeaderCount && qualifiedLeaderCount > 0) {
                int consolidatedGroups = (int) Math.max(qualifiedLeaderCount, Math.ceil((double) size / config.getCapSize()));
                if (consolidatedGroups < groupCount) {
                    groupCount = consolidatedGroups;
                }
            }

            // Create groups and distribute participants
            List<Group> segmentGroups = new ArrayList<>();
            for (int i = 0; i < groupCount; i++) {
                segmentGroups.add(new Group(key.gender(), key.ageTier(), key.lifeStage()));
            }

            for (int i = 0; i < size; i++) {
                segmentGroups.get(i % groupCount).addParticipant(segmentParticipants.get(i));
            }
            
            allGroups.addAll(segmentGroups);
        }

        // 4. Assign Facilitators
        warnings.addAll(facilitatorAssigner.assign(allGroups, facilitators, config));

        return new AssignmentResult(allGroups, unassigned, warnings);
    }
}
