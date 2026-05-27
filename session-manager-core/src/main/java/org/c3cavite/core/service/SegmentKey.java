package org.c3cavite.core.service;

import org.c3cavite.core.domain.AgeTier;
import org.c3cavite.core.domain.Gender;
import org.c3cavite.core.domain.LifeStage;

public record SegmentKey(Gender gender, AgeTier ageTier, LifeStage lifeStage) {
}
