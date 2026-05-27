package org.c3cavite.core.domain;

import java.util.Collections;
import java.util.List;

public class SessionConfig {
    private final GenderMode genderMode;
    private final int targetSize;
    private final int capSize;
    private final List<AgeTier> activeTiers;
    private final boolean lifeStageSubgroupingEnabled;

    private SessionConfig(GenderMode genderMode, int targetSize, int capSize, List<AgeTier> activeTiers, boolean lifeStageSubgroupingEnabled) {
        this.genderMode = genderMode;
        this.targetSize = targetSize;
        this.capSize = capSize;
        this.activeTiers = Collections.unmodifiableList(activeTiers);
        this.lifeStageSubgroupingEnabled = lifeStageSubgroupingEnabled;
    }

    public static SessionConfig create(GenderMode genderMode, int targetSize, int capSize, List<AgeTier> activeTiers, boolean lifeStageSubgroupingEnabled) {
        if (targetSize <= 0) {
            throw new IllegalArgumentException("targetSize must be greater than 0");
        }
        if (targetSize > capSize) {
            throw new IllegalArgumentException("targetSize cannot be greater than capSize");
        }
        if (activeTiers == null || activeTiers.isEmpty()) {
            throw new IllegalArgumentException("activeTiers cannot be null or empty");
        }
        return new SessionConfig(genderMode, targetSize, capSize, activeTiers, lifeStageSubgroupingEnabled);
    }

    public GenderMode getGenderMode() {
        return genderMode;
    }

    public int getTargetSize() {
        return targetSize;
    }

    public int getCapSize() {
        return capSize;
    }

    public List<AgeTier> getActiveTiers() {
        return activeTiers;
    }

    public boolean isLifeStageSubgroupingEnabled() {
        return lifeStageSubgroupingEnabled;
    }
}
