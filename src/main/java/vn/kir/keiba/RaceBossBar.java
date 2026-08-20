package vn.kir.keiba;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;

final class RaceBossBar {
    private final KirKeibaPlugin plugin;
    private final BossBar bar;

    RaceBossBar(KirKeibaPlugin plugin) {
        this.plugin = plugin;
        this.bar = Bukkit.createBossBar("KIR Keiba", BarColor.GREEN, BarStyle.SEGMENTED_20);
        ConfigurationSection titles = plugin.getConfig().getConfigurationSection("bossbar.titles");
        plugin.getLogger().info("BossBar config: enabled=" + plugin.getConfig().getBoolean("bossbar.enabled", true)
            + ", titles=" + (titles == null ? "MISSING" : titles.getKeys(false)));
    }

    void update(RaceManager race) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true) || race.state() == RaceState.WAITING) {
            bar.removeAll();
            bar.setVisible(false);
            return;
        }
        String key = switch (race.state()) {
            case BETTING_OPEN -> "betting-open";
            case BETTING_LOCKED -> "betting-locked";
            case RUNNING -> "running";
            case RESULT -> "result";
            case WAITING -> "waiting";
        };
        Map<String, Object> variables = new HashMap<>();
        variables.put("race", race.raceId()); variables.put("seconds", race.secondsLeft()); variables.put("state", race.stateLabel());
        variables.put("next_race", race.raceId() + 1);
        variables.put("pool", RaceManager.money(race.totalPool()));
        variables.put("first", race.result().isEmpty() ? "—" : race.result().get(0).name());
        variables.put("second", race.result().size() < 2 ? "—" : race.result().get(1).name());
        variables.put("first_number", race.result().isEmpty() ? "—" : race.result().get(0).number());
        variables.put("second_number", race.result().size() < 2 ? "—" : race.result().get(1).number());
        variables.put("odds", RaceManager.fmt(race.lastOdds()));
        RaceConditions conditions = race.conditions();
        variables.put("distance", conditions.distance().displayName());
        variables.put("meters", conditions.meters());
        variables.put("surface", conditions.surface().displayName());
        variables.put("track_state", conditions.track().displayName());
        variables.put("weather", conditions.weather().displayName());
        bar.setTitle(Text.render(title(key), variables));
        bar.setColor(enumValue(BarColor.class, plugin.getConfig().getString("bossbar.colors." + key), BarColor.GREEN));
        bar.setStyle(enumValue(BarStyle.class, plugin.getConfig().getString("bossbar.style"), BarStyle.SEGMENTED_20));
        long duration = switch (race.state()) {
            case BETTING_OPEN -> plugin.getConfig().getLong("schedule.betting-seconds", 180);
            case BETTING_LOCKED -> plugin.getConfig().getLong("schedule.locked-seconds", 15);
            case RUNNING -> plugin.getConfig().getLong("schedule.running-seconds", 35);
            case RESULT -> plugin.getConfig().getLong("schedule.result-seconds", 30);
            case WAITING -> plugin.getConfig().getLong("schedule.interval-seconds", 300);
        };
        bar.setProgress(Math.max(0, Math.min(1, race.secondsLeft() / (double) Math.max(1, duration))));
        for (Player player : Bukkit.getOnlinePlayers()) if (!bar.getPlayers().contains(player)) bar.addPlayer(player);
        bar.setVisible(true);
    }

    void close() { bar.removeAll(); bar.setVisible(false); }

    private String title(String key) {
        ConfigurationSection titles = plugin.getConfig().getConfigurationSection("bossbar.titles");
        if (titles != null) {
            String configured = titles.getString(key);
            if (configured != null && !configured.isBlank()) return configured;
        }
        plugin.getLogger().warning("Thiếu bossbar.titles." + key + "; đang dùng tiêu đề dự phòng.");
        return "&6KIR Keiba &8| &fVòng #{race} &8| &e{state} &8| &fCòn {seconds}s";
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        try { return Enum.valueOf(type, value == null ? "" : value.toUpperCase()); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
