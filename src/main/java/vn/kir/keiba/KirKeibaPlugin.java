package vn.kir.keiba;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;

public final class KirKeibaPlugin extends JavaPlugin {
    private RaceRepository repository;
    private RaceManager races;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        var provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            getLogger().severe("Không tìm thấy Vault Economy.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            repository = new RaceRepository(getDataFolder().toPath());
            races = new RaceManager(this, provider.getProvider(), repository);
            var menu = new KeibaMenu(this, races);
            getServer().getPluginManager().registerEvents(menu, this);
            var command = getCommand("keiba");
            var executor = new KeibaCommand(this, races, menu);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
            races.start();
            getLogger().info("KirKeiba " + getPluginMeta().getVersion() + " đã bật.");
        } catch (Exception error) {
            getLogger().severe("Không thể bật KirKeiba: " + error.getMessage());
            error.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (races != null) races.shutdown();
        if (repository != null) try { repository.close(); } catch (Exception ignored) { }
    }
}
