package org.c3cavite.core.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SessionConfig {
  private final GenderMode genderMode;
  private final int targetSize;
  private final int capSize;
  private final List<AgeTier> activeTiers;
  private final boolean lifeStageSubgroupingEnabled;
  private final Map<AgeTier, AgeRange> ageTierBoundaries;

  private SessionConfig(Builder builder) {
    validate(builder);
    this.genderMode = builder.genderMode == null ? GenderMode.SEPARATED : builder.genderMode;
    this.targetSize = builder.targetSize;
    this.capSize = builder.capSize;
    this.activeTiers = List.copyOf(builder.activeTiers);
    this.lifeStageSubgroupingEnabled = builder.lifeStageSubgroupingEnabled;
    this.ageTierBoundaries = Map.copyOf(builder.ageTierBoundaries);
  }

  public static SessionConfig create(
      GenderMode genderMode,
      int targetSize,
      int capSize,
      List<AgeTier> activeTiers,
      boolean lifeStageSubgroupingEnabled) {
    return builder()
        .genderMode(genderMode)
        .targetSize(targetSize)
        .capSize(capSize)
        .activeTiers(activeTiers)
        .lifeStageSubgroupingEnabled(lifeStageSubgroupingEnabled)
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  private static void validate(Builder builder) {
    if (builder.capSize == null || builder.capSize <= 0) {
      throw new IllegalArgumentException("capSize must be explicitly provided and greater than 0");
    }
    if (builder.targetSize <= 0) {
      throw new IllegalArgumentException("targetSize must be greater than 0");
    }
    if (builder.targetSize > builder.capSize) {
      throw new IllegalArgumentException("targetSize cannot be greater than capSize");
    }
    if (builder.activeTiers == null || builder.activeTiers.isEmpty()) {
      throw new IllegalArgumentException("activeTiers cannot be null or empty");
    }
    if (builder.activeTiers.stream().anyMatch(tier -> tier == null)) {
      throw new IllegalArgumentException("activeTiers cannot contain null");
    }
    if (builder.ageTierBoundaries == null
        || builder.ageTierBoundaries.size() != AgeTier.values().length) {
      throw new IllegalArgumentException("ageTierBoundaries must define every age tier");
    }
    AgeRange previous = null;
    for (AgeTier tier : AgeTier.values()) {
      AgeRange range = builder.ageTierBoundaries.get(tier);
      if (range == null) {
        throw new IllegalArgumentException("ageTierBoundaries must define a range for " + tier);
      }
      if (previous != null && (long) previous.maxAge() + 1 != range.minAge()) {
        throw new IllegalArgumentException(
            "ageTierBoundaries must be ordered without gaps or overlaps");
      }
      previous = range;
    }
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

  public Map<AgeTier, AgeRange> getAgeTierBoundaries() {
    return ageTierBoundaries;
  }

  /** Resolves a tier independently of whether it is active for this session. */
  public Optional<AgeTier> ageTierFor(int age) {
    return ageTierBoundaries.entrySet().stream()
        .filter(entry -> entry.getValue().contains(age))
        .map(Map.Entry::getKey)
        .findFirst();
  }

  public static final class Builder {
    private GenderMode genderMode = GenderMode.SEPARATED;
    private int targetSize = 4;
    private Integer capSize;
    private List<AgeTier> activeTiers = List.of(AgeTier.values());
    private boolean lifeStageSubgroupingEnabled = true;
    private Map<AgeTier, AgeRange> ageTierBoundaries = defaultBoundaries();

    private Builder() {}

    private static Map<AgeTier, AgeRange> defaultBoundaries() {
      Map<AgeTier, AgeRange> boundaries = new EnumMap<>(AgeTier.class);
      for (AgeTier tier : AgeTier.values()) {
        boundaries.put(tier, new AgeRange(tier.getMinAge(), tier.getMaxAge()));
      }
      return boundaries;
    }

    public Builder genderMode(GenderMode genderMode) {
      this.genderMode = genderMode;
      return this;
    }

    public Builder targetSize(int targetSize) {
      this.targetSize = targetSize;
      return this;
    }

    public Builder capSize(Integer capSize) {
      this.capSize = capSize;
      return this;
    }

    public Builder activeTiers(List<AgeTier> activeTiers) {
      this.activeTiers = activeTiers;
      return this;
    }

    public Builder lifeStageSubgroupingEnabled(boolean enabled) {
      this.lifeStageSubgroupingEnabled = enabled;
      return this;
    }

    public Builder ageTierBoundaries(Map<AgeTier, AgeRange> boundaries) {
      this.ageTierBoundaries = boundaries;
      return this;
    }

    public SessionConfig build() {
      return new SessionConfig(this);
    }
  }
}
