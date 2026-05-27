package org.c3cavite.core.domain;

import java.util.Collections;
import java.util.List;

public class Facilitator {
    private final String name;
    private final Role role;
    private final Gender gender;
    private final List<AgeTier> qualifiedTiers;

    public Facilitator(String name, Role role, Gender gender, List<AgeTier> qualifiedTiers) {
        this.name = name;
        this.role = role;
        this.gender = gender;
        this.qualifiedTiers = qualifiedTiers != null ? Collections.unmodifiableList(qualifiedTiers) : Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public Gender getGender() {
        return gender;
    }

    public List<AgeTier> getQualifiedTiers() {
        return qualifiedTiers;
    }
}
