package org.c3cavite.core.service;

import org.c3cavite.core.domain.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SegmentationStrategy {

    public Map<SegmentKey, List<Participant>> segment(List<Participant> participants, SessionConfig config) {
        Map<SegmentKey, List<Participant>> segments = new HashMap<>();

        for (Participant p : participants) {
            if (p.getAgeTier() == null || !config.getActiveTiers().contains(p.getAgeTier())) {
                continue;
            }

            Gender segmentGender = (config.getGenderMode() == GenderMode.SEPARATED) ? p.getGender() : null;
            AgeTier segmentTier = p.getAgeTier();
            LifeStage segmentLifeStage = (config.isLifeStageSubgroupingEnabled() && segmentTier == AgeTier.YOUNG_ADULT) 
                    ? p.getLifeStage() : null;

            SegmentKey key = new SegmentKey(segmentGender, segmentTier, segmentLifeStage);
            segments.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        return segments;
    }
}
