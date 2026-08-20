package vn.kir.keiba;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

final class RaceSimulator {
    record Result(long seed, List<Integer> finishOrder, Map<Integer, Double> finalProgress) { }

    Result simulate(List<HorseEntry> horses, long seed, int steps) {
        return simulate(horses, seed, steps, RaceConditions.neutral());
    }

    Result simulate(List<HorseEntry> horses, long seed, int steps, RaceConditions conditions) {
        SplittableRandom random = new SplittableRandom(seed);
        Map<Integer, Double> progress = new HashMap<>();
        Map<Integer, Double> form = new HashMap<>();
        for (HorseEntry horse : horses) {
            progress.put(horse.number(), 0.0);
            form.put(horse.number(), random.nextDouble(-5.0, 5.0));
        }
        for (int step = 0; step < steps; step++) {
            double phase = step / (double) Math.max(1, steps - 1);
            for (HorseEntry horse : horses) {
                double randomFactor = conditions.randomnessMultiplier();
                double score = horse.phaseScore(phase, conditions) + form.get(horse.number()) * randomFactor + random.nextDouble(-4.0, 4.0) * randomFactor;
                double delta = Math.max(0.001, score / (steps * 74.0));
                progress.compute(horse.number(), (key, value) -> value + delta);
            }
        }
        List<Integer> order = new ArrayList<>(progress.keySet());
        order.sort(Comparator.<Integer>comparingDouble(progress::get).reversed().thenComparingInt(Integer::intValue));
        return new Result(seed, List.copyOf(order), Map.copyOf(progress));
    }
}
