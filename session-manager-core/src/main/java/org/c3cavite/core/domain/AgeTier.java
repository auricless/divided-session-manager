package org.c3cavite.core.domain;

import java.util.Arrays;
import java.util.Optional;

public enum AgeTier {
    TEENS(13, 15),
    YOUTH(16, 22),
    YOUNG_ADULT(23, Integer.MAX_VALUE);

    private final int minAge;
    private final int maxAge;

    AgeTier(int minAge, int maxAge) {
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public static Optional<AgeTier> fromAge(int age) {
        return Arrays.stream(values())
                .filter(tier -> age >= tier.minAge && age <= tier.maxAge)
                .findFirst();
    }

    public int getMinAge() {
        return minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }
}
