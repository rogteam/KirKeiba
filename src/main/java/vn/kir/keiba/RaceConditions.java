package vn.kir.keiba;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

record RaceConditions(Factor distance, Factor surface, Factor track, Factor weather) {
    static RaceConditions select(KirKeibaPlugin plugin, long seed) {
        if (!plugin.getConfig().getBoolean("race-conditions.enabled", true)) return neutral();
        SplittableRandom random = new SplittableRandom(seed ^ 0x524143454B49524CL);
        return new RaceConditions(
            choose(plugin, "race-conditions.distances", random, new Factor("MIDDLE", "Trung bình", 1800, 1, 1, 1, 1, 1, Map.of())),
            choose(plugin, "race-conditions.surfaces", random, new Factor("TURF", "Cỏ", 0, 1, 1, 1, 1, 1, Map.of())),
            choose(plugin, "race-conditions.track-states", random, new Factor("GOOD", "Tốt", 0, 1, 1, 1, 1, 1, Map.of())),
            choose(plugin, "race-conditions.weather", random, new Factor("CLEAR", "Nắng", 0, 1, 1, 1, 1, 1, Map.of()))
        );
    }

    static RaceConditions neutral() {
        Factor neutral = new Factor("NONE", "Không áp dụng", 1800, 1, 1, 1, 1, 1, Map.of());
        return new RaceConditions(neutral, neutral, neutral, neutral);
    }

    double powerMultiplier() { return distance.power() * surface.power() * track.power() * weather.power(); }
    double staminaMultiplier() { return distance.stamina() * surface.stamina() * track.stamina() * weather.stamina(); }
    double finishMultiplier() { return distance.finish() * surface.finish() * track.finish() * weather.finish(); }
    double styleMultiplier(RunningStyle style) { return distance.style(style) * surface.style(style) * track.style(style) * weather.style(style); }
    double randomnessMultiplier() { return distance.randomness() * surface.randomness() * track.randomness() * weather.randomness(); }
    int meters() { return distance.meters(); }

    private static Factor choose(KirKeibaPlugin plugin, String path, SplittableRandom random, Factor fallback) {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection(path);
        if (root == null) return fallback;
        List<WeightedFactor> factors = new ArrayList<>();
        double total = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            double weight = Math.max(0, section.getDouble("weight", 1));
            if (weight == 0) continue;
            EnumMap<RunningStyle, Double> styles = new EnumMap<>(RunningStyle.class);
            for (RunningStyle style : RunningStyle.values()) styles.put(style, section.getDouble("style-multipliers." + style.name(), 1));
            Factor factor = new Factor(id.toUpperCase(Locale.ROOT), section.getString("display-name", id), section.getInt("meters", fallback.meters()),
                section.getDouble("power-multiplier", 1), section.getDouble("stamina-multiplier", 1),
                section.getDouble("finish-multiplier", 1), section.getDouble("randomness-multiplier", 1),
                weight, Map.copyOf(styles));
            factors.add(new WeightedFactor(factor, weight));
            total += weight;
        }
        if (factors.isEmpty()) return fallback;
        double roll = random.nextDouble(total);
        for (WeightedFactor item : factors) {
            roll -= item.weight();
            if (roll < 0) return item.factor();
        }
        return factors.get(factors.size() - 1).factor();
    }

    record Factor(String id, String displayName, int meters, double power, double stamina, double finish,
                  double randomness, double weight, Map<RunningStyle, Double> styles) {
        double style(RunningStyle runningStyle) { return styles.getOrDefault(runningStyle, 1.0); }
    }
    private record WeightedFactor(Factor factor, double weight) { }
}
