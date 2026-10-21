package org.c3cavite.core.domain;

import java.util.List;
import java.util.Objects;

public class Facilitator {
    private final String name;
    private final Role role;
    private final Gender gender;
    private final List<AgeTier> qualifiedTiers;

    public Facilitator(String name, Role role, Gender gender, List<AgeTier> qualifiedTiers) {
        if (name == null || name.isBlank() || role == null || gender == null
                || qualifiedTiers == null || qualifiedTiers.isEmpty() || qualifiedTiers.stream().anyMatch(tier -> tier == null)) {
            throw new IllegalArgumentException("Facilitator name, role, gender, and non-empty valid qualifications are required");
        }
        this.name = name;
        this.role = role;
        this.gender = gender;
        this.qualifiedTiers = List.copyOf(qualifiedTiers);
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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Facilitator that = (Facilitator) other;
        return name.equals(that.name) && role == that.role && gender == that.gender && qualifiedTiers.equals(that.qualifiedTiers);
    }

    @Override
    public int hashCode() { return Objects.hash(name, role, gender, qualifiedTiers); }

    @Override
    public String toString() {
        return "Facilitator[name=" + name + ", role=" + role + ", gender=" + gender + ", qualifiedTiers=" + qualifiedTiers + "]";
    }
}
