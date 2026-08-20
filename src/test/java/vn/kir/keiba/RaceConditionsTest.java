package vn.kir.keiba;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceConditionsTest {
    @Test
    void combinesEveryConditionMultiplier() {
        RaceConditions.Factor distance = factor(1.10, 1.20, 0.90, 1.30, RunningStyle.NIGE, 1.10);
        RaceConditions.Factor surface = factor(1.05, 1.00, 1.00, 1.00, RunningStyle.NIGE, 1.20);
        RaceConditions conditions = new RaceConditions(distance, surface, neutral(), neutral());

        assertEquals(1.155, conditions.powerMultiplier(), 0.000001);
        assertEquals(1.20, conditions.staminaMultiplier(), 0.000001);
        assertEquals(0.90, conditions.finishMultiplier(), 0.000001);
        assertEquals(1.32, conditions.styleMultiplier(RunningStyle.NIGE), 0.000001);
        assertEquals(1.30, conditions.randomnessMultiplier(), 0.000001);
    }

    @Test
    void conditionsChangeTheSameHorsesScore() {
        HorseEntry horse = new HorseEntry(1, "Kiểm thử", RunningStyle.NIGE, 75, 70, 60);
        RaceConditions boosted = new RaceConditions(factor(1.10, 1, 1, 1, RunningStyle.NIGE, 1.15), neutral(), neutral(), neutral());
        assertTrue(horse.phaseScore(0.10, boosted) > horse.phaseScore(0.10, RaceConditions.neutral()));
    }

    private static RaceConditions.Factor neutral() {
        return factor(1, 1, 1, 1, RunningStyle.NIGE, 1);
    }

    private static RaceConditions.Factor factor(double power, double stamina, double finish, double randomness,
                                                RunningStyle style, double styleMultiplier) {
        return new RaceConditions.Factor("TEST", "Test", 1800, power, stamina, finish, randomness, 1,
            Map.of(style, styleMultiplier));
    }
}
