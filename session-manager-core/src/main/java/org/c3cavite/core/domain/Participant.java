package org.c3cavite.core.domain;

import java.util.Objects;

public class Participant {
    private final String name;
    private final int age;
    private final Gender gender;
    private final LifeStage lifeStage;
    private final AgeTier ageTier;

    public Participant(String name, int age, Gender gender, LifeStage lifeStage) {
        this(name, age, gender, lifeStage, AgeTier.fromAge(age).orElse(null));
    }

    private Participant(String name, int age, Gender gender, LifeStage lifeStage, AgeTier ageTier) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        if (age < 1) {
            throw new IllegalArgumentException("age must be greater than 0");
        }
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.lifeStage = lifeStage;
        this.ageTier = ageTier;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public Gender getGender() {
        return gender;
    }

    public LifeStage getLifeStage() {
        return lifeStage;
    }

    /** Returns the tier captured when constructed or derived for an assignment session. */
    public AgeTier getAgeTier() {
        return ageTier;
    }

    /** Resolves the tier against the supplied event without changing this value. */
    public AgeTier getAgeTier(SessionConfig config) {
        return config.ageTierFor(age).orElse(null);
    }

    /** Returns an immutable copy with this event's tier, leaving the input reusable. */
    public Participant forSession(SessionConfig config) {
        return new Participant(name, age, gender, lifeStage, getAgeTier(config));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Participant that = (Participant) other;
        return age == that.age && name.equals(that.name) && gender == that.gender && lifeStage == that.lifeStage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, gender, lifeStage);
    }

    @Override
    public String toString() {
        return "Participant[name=" + name + ", age=" + age + ", gender=" + gender + ", lifeStage=" + lifeStage + "]";
    }
}
