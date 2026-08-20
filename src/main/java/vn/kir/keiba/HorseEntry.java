package vn.kir.keiba;

record HorseEntry(int number, String name, RunningStyle style, double power, double stamina, double finish) {
    double phaseScore(double progress) {
        return phaseScore(progress, RaceConditions.neutral());
    }

    double phaseScore(double progress, RaceConditions conditions) {
        double styleBonus = switch (style) {
            case NIGE -> 13.0 * (1.0 - progress);
            case SENKO -> 7.0 * (1.0 - Math.abs(progress - 0.35));
            case SASHI -> progress >= 0.45 ? 9.0 * progress : 0.0;
            case OIKOMI -> progress >= 0.70 ? 16.0 * progress : -3.0;
        };
        return power * conditions.powerMultiplier() * (1.0 - progress)
            + stamina * conditions.staminaMultiplier() * 0.45
            + finish * conditions.finishMultiplier() * progress
            + styleBonus * conditions.styleMultiplier(style);
    }
}
