package org.c3cavite.core.domain;

public class Participant {
    private final String name;
    private final int age;
    private final Gender gender;
    private final LifeStage lifeStage;
    private final AgeTier ageTier;

    public Participant(String name, int age, Gender gender, LifeStage lifeStage) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.lifeStage = lifeStage;
        this.ageTier = AgeTier.fromAge(age).orElse(null);
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

    public AgeTier getAgeTier() {
        return ageTier;
    }
}
