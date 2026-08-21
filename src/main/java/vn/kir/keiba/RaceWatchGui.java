package vn.kir.keiba;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

final class RaceWatchGui implements Listener {
    // 54-slot layout (6 rows × 9 cols)
    // Rows 0-3: horse lanes — 2 horses/row, each gets cols 0-3 (left) or 5-8 (right), col 4 = divider
    // Row 4   : race status
    // Row 5   : controls

    private static final Material[] HORSE_MATS = {
        Material.RED_CONCRETE,    Material.BLUE_CONCRETE,
        Material.LIME_CONCRETE,   Material.YELLOW_CONCRETE,
        Material.PURPLE_CONCRETE, Material.CYAN_CONCRETE,
        Material.ORANGE_CONCRETE, Material.PINK_CONCRETE,
    };
    private static final Material BG  = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material DIV = Material.BLACK_STAINED_GLASS_PANE;

    private final KirKeibaPlugin plugin;
    private final RaceManager races;
    private final Set<UUID> viewers = new HashSet<>();

    static final class WatchHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    RaceWatchGui(KirKeibaPlugin plugin, RaceManager races) {
        this.plugin = plugin;
        this.races = races;
    }

    void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 20L, 20L);
    }

    void open(Player player) {
        if (!plugin.getConfig().getBoolean("view-gui.enabled", true)) {
            player.sendMessage(Text.color(prefix() + "&cGUI xem đua chưa được bật."));
            return;
        }
        if (races.waiting()) {
            player.sendMessage(Text.color(prefix() + races.waitingMessage()));
            return;
        }
        Inventory inv = Bukkit.createInventory(new WatchHolder(), 54,
            Text.color("&0KIR Keiba &8• &6Xem đua #" + races.raceId()));
        populateInventory(inv);
        player.openInventory(inv);
        viewers.add(player.getUniqueId());
    }

    @EventHandler
    void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof WatchHolder)
            viewers.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof WatchHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        if (slot == 45) populateInventory(e.getInventory());
        else if (slot == 53) p.closeInventory();
    }

    private void refresh() {
        viewers.removeIf(uid -> {
            Player p = Bukkit.getPlayer(uid);
            return p == null || !(p.getOpenInventory().getTopInventory().getHolder() instanceof WatchHolder);
        });
        for (UUID uid : new ArrayList<>(viewers)) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) populateInventory(p.getOpenInventory().getTopInventory());
        }
    }

    private void populateInventory(Inventory inv) {
        List<HorseEntry> horses = races.horses();
        Map<Integer, Double> prog = races.watchProgress();
        RaceState state = races.state();
        List<HorseEntry> result = races.result();
        int n = Math.min(horses.size(), 8);
        int laneRows = (n + 1) / 2; // 8 horses → 4 rows

        // Background fill
        ItemStack bg = pane(BG, " "), div = pane(DIV, " "), dark = pane(DIV, " ");
        for (int row = 0; row < laneRows; row++)
            for (int col = 0; col < 9; col++)
                inv.setItem(row * 9 + col, col == 4 ? div : bg);
        for (int s = laneRows * 9; s < 36; s++) inv.setItem(s, bg);
        for (int s = 36; s < 45; s++) inv.setItem(s, dark);
        for (int s = 45; s < 54; s++) inv.setItem(s, bg);

        // Horse items
        for (int i = 0; i < n; i++) {
            HorseEntry h = horses.get(i);
            int row = i / 2, side = i % 2;
            double p = prog.getOrDefault(h.number(), 0.0);
            int laneCol = Math.min(3, (int)(p * 4.0));
            int slot = row * 9 + (side == 0 ? laneCol : 5 + laneCol);
            inv.setItem(slot, horseItem(HORSE_MATS[i % HORSE_MATS.length], h, p, state, result));
        }

        // Status row (row 4)
        RaceConditions c = races.conditions();
        inv.setItem(36, item(Material.CLOCK,
            Text.color("&6Vòng #" + races.raceId() + " &8• &e" + races.stateLabel()),
            List.of(Text.color("&7Còn lại: &e" + races.secondsLeft() + "s"),
                    Text.color("&7Pool: &f" + RaceManager.money(races.totalPool())))));
        inv.setItem(37, item(Material.GRASS_BLOCK,
            Text.color("&bĐiều kiện"),
            List.of(Text.color("&f" + c.distance().displayName() + " (" + c.meters() + "m)"),
                    Text.color("&7" + c.surface().displayName() + " • " + c.track().displayName() + " • " + c.weather().displayName()))));
        if (state == RaceState.RESULT && result.size() >= 3) {
            inv.setItem(40, item(Material.DIAMOND, Text.color("&aKết quả"),
                List.of(Text.color("&e1. &f#" + result.get(0).number() + " " + result.get(0).name()),
                        Text.color("&e2. &f#" + result.get(1).number() + " " + result.get(1).name()),
                        Text.color("&e3. &f#" + result.get(2).number() + " " + result.get(2).name()))));
        } else if (state == RaceState.RUNNING) {
            inv.setItem(40, item(Material.BLAZE_POWDER, Text.color("&cĐang đua!"),
                List.of(Text.color("&7Kết quả sau &e" + races.secondsLeft() + "s"))));
        } else {
            inv.setItem(40, item(Material.EMERALD, Text.color("&a" + races.stateLabel()),
                List.of(Text.color("&7Còn &e" + races.secondsLeft() + "s"))));
        }

        // Controls row (row 5)
        inv.setItem(45, item(Material.COMPASS, Text.color("&eLàm mới"), List.of(Text.color("&7Cập nhật vị trí ngựa"))));
        inv.setItem(53, item(Material.BARRIER, Text.color("&cĐóng"), List.of()));
    }

    private static ItemStack horseItem(Material mat, HorseEntry h, double prog,
                                       RaceState state, List<HorseEntry> result) {
        List<String> lore = new ArrayList<>();
        lore.add(Text.color("&7Lối chạy: &f" + h.style().label()));
        lore.add(Text.color("&c⚔ &f" + RaceManager.fmt(h.power())
                          + "  &a❤ &f" + RaceManager.fmt(h.stamina())
                          + "  &d✦ &f" + RaceManager.fmt(h.finish())));
        if (state == RaceState.RUNNING) {
            lore.add(Text.color("&7Tiến độ: &e" + (int)(prog * 100) + "%  " + bar(prog, 8)));
        } else if (state == RaceState.RESULT) {
            int rank = rankOf(h.number(), result);
            lore.add(rank > 0 ? Text.color("&7Hạng: &e#" + rank) : Text.color("&8Chưa xếp hạng"));
        }
        return item(mat, Text.color("&f#" + h.number() + " " + h.name()), lore);
    }

    private static String bar(double progress, int len) {
        int filled = (int)(progress * len);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(i < filled ? "§a▌" : "§8▌");
        return sb.toString();
    }

    private static int rankOf(int number, List<HorseEntry> result) {
        for (int i = 0; i < result.size(); i++)
            if (result.get(i).number() == number) return i + 1;
        return 0;
    }

    private String prefix() {
        return plugin.getConfig().getString("messages.prefix", "&6[KIR Keiba]&r ");
    }

    private static ItemStack pane(Material mat, String name) {
        return item(mat, name, List.of());
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
