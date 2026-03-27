package core.domain;

public enum AgeTier {

    // TODO: add more Age tiers
    TEEN;

    public static AgeTier fromAge(int age) {
        if (age >= 13 && age <= 15) {
            return TEEN;
        }

        throw new IllegalArgumentException("This age range is not supported");
    }

    @Override
    public String toString() {
        return this.name();
    }
}
