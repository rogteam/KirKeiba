package vn.kir.keiba;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

final class RaceMapView {
    private final KirKeibaPlugin plugin;
    private final MapView[] views = new MapView[4]; // index = qx + qy*2
    private final AtomicReference<Snap> snapRef = new AtomicReference<>();

    RaceMapView(KirKeibaPlugin plugin) {
        this.plugin = plugin;
        loadOrCreate();
    }

    private void loadOrCreate() {
        Path idFile = plugin.getDataFolder().toPath().resolve("map_id.dat");
        int[] savedIds = new int[0];
        if (Files.exists(idFile)) {
            try {
                String[] parts = Files.readString(idFile).trim().split(",");
                if (parts.length == 4)
                    savedIds = Arrays.stream(parts).mapToInt(Integer::parseInt).toArray();
            } catch (Exception ignored) {}
        }

        World world = Bukkit.getWorlds().get(0);
        int[] finalIds = new int[4];
        boolean needSave = savedIds.length != 4;

        for (int i = 0; i < 4; i++) {
            MapView view = savedIds.length == 4 ? Bukkit.getMap(savedIds[i]) : null;
            if (view == null) { view = Bukkit.createMap(world); needSave = true; }
            finalIds[i] = view.getId();
            view.setUnlimitedTracking(false);
            view.setTrackingPosition(false);
            view.getRenderers().forEach(view::removeRenderer);
            view.addRenderer(new QuadrantRenderer(i % 2, i / 2, snapRef));
            views[i] = view;
        }

        if (needSave) {
            String ids = finalIds[0] + "," + finalIds[1] + "," + finalIds[2] + "," + finalIds[3];
            try { Files.writeString(idFile, ids); }
            catch (IOException e) { plugin.getLogger().warning("Không lưu được map ID: " + e.getMessage()); }
        }
    }

    List<ItemStack> getItems() {
        String[] pos = {"Trái-Trên", "Phải-Trên", "Trái-Dưới", "Phải-Dưới"};
        List<ItemStack> out = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            ItemStack item = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) item.getItemMeta();
            meta.setMapView(views[i]);
            meta.setDisplayName("§6KIR Keiba §7[" + pos[i] + "]");
            meta.setLore(List.of("§7Đặt 2×2 đúng góc để ghép màn hình lớn."));
            item.setItemMeta(meta);
            out.add(item);
        }
        return out;
    }

    void update(RaceState state, long raceId, List<HorseEntry> horses,
                Map<Integer, Double> progress, List<HorseEntry> result) {
        snapRef.set(new Snap(state, raceId, List.copyOf(horses), Map.copyOf(progress),
                             result == null ? List.of() : List.copyOf(result)));
    }

    // ── Shared state ─────────────────────────────────────────────────────────

    private record Snap(RaceState state, long raceId, List<HorseEntry> horses,
                        Map<Integer, Double> progress, List<HorseEntry> result) {}

    // ── Quadrant renderer ─────────────────────────────────────────────────────
    // Each instance draws its 128×128 slice of a virtual 256×256 canvas.

    private static final class QuadrantRenderer extends MapRenderer {
        // Virtual 256×256 layout
        private static final int LABEL_W  = 32;
        private static final int START_X  = 32;
        private static final int TRACK_L  = 36;
        private static final int TRACK_R  = 234;
        private static final int TRACK_W  = TRACK_R - TRACK_L; // 198px
        private static final int FINISH_X = 236;
        private static final int HEADER_H = 12;
        private static final int FOOTER_H = 20;
        private static final int LANE_AREA = 256 - HEADER_H - FOOTER_H; // 224px

        private static final byte C_BG     = MapPalette.matchColor(new Color(24,  70,  24));
        private static final byte C_LANE_A = MapPalette.matchColor(new Color(160, 120, 65));
        private static final byte C_LANE_B = MapPalette.matchColor(new Color(140, 100, 50));
        private static final byte C_START  = MapPalette.matchColor(Color.WHITE);
        private static final byte C_FINISH = MapPalette.matchColor(new Color(255, 215, 0));
        private static final byte[] HORSE_COLORS = {
            MapPalette.matchColor(new Color(220, 50,  50)),
            MapPalette.matchColor(new Color(60,  100, 230)),
            MapPalette.matchColor(new Color(60,  200, 60)),
            MapPalette.matchColor(new Color(230, 210, 40)),
            MapPalette.matchColor(new Color(160, 50,  220)),
            MapPalette.matchColor(new Color(40,  200, 210)),
            MapPalette.matchColor(new Color(230, 130, 40)),
            MapPalette.matchColor(new Color(230, 80,  170)),
        };

        private final int qx, qy;
        private final AtomicReference<Snap> snapRef;

        QuadrantRenderer(int qx, int qy, AtomicReference<Snap> snapRef) {
            super(false);
            this.qx = qx; this.qy = qy; this.snapRef = snapRef;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            Snap s = snapRef.get();
            if (s == null || s.horses().isEmpty()) {
                vfill(canvas, 0, 0, 256, 256, C_BG);
                vtext(canvas, 80, 120, MinecraftFont.Font, "KIR Keiba");
                return;
            }

            int n = s.horses().size();
            int laneH = LANE_AREA / n;

            vfill(canvas, 0, 0, 256, 256, C_BG);

            for (int i = 0; i < n; i++) {
                int ly = HEADER_H + i * laneH;
                vfill(canvas, TRACK_L, ly + 1, TRACK_W, laneH - 2,
                      i % 2 == 0 ? C_LANE_A : C_LANE_B);
            }

            vfill(canvas, START_X, HEADER_H, 2, LANE_AREA, C_START);
            vfill(canvas, FINISH_X, HEADER_H, 2, LANE_AREA, C_FINISH);

            vtext(canvas, 1, 2, MinecraftFont.Font,
                  "KIR Keiba #" + s.raceId() + "  " + stateTag(s.state()));

            for (int i = 0; i < n; i++) {
                HorseEntry h = s.horses().get(i);
                int ly = HEADER_H + i * laneH;
                int cy = ly + laneH / 2 - 3;
                byte color = HORSE_COLORS[i % HORSE_COLORS.length];

                vtext(canvas, 1, cy, MinecraftFont.Font, "#" + h.number() + " " + h.name());

                double prog = s.progress().getOrDefault(h.number(), 0.0);
                int hx = TRACK_L + (int)(prog * (TRACK_W - 8));
                hx = Math.max(TRACK_L, Math.min(TRACK_R - 8, hx));
                vfill(canvas, hx, ly + 3, 8, laneH - 6, color);
            }

            String footer = (s.state() == RaceState.RESULT && s.result().size() >= 3)
                ? "1: " + s.result().get(0).name()
                  + "   2: " + s.result().get(1).name()
                  + "   3: " + s.result().get(2).name()
                : stateTag(s.state());
            vtext(canvas, 1, 256 - FOOTER_H + 6, MinecraftFont.Font, footer);
        }

        private static String stateTag(RaceState st) {
            return switch (st) {
                case BETTING_OPEN   -> "Mo cuoc";
                case BETTING_LOCKED -> "Khoa cuoc";
                case RUNNING        -> "Dang dua!";
                case RESULT         -> "Ket qua";
                case WAITING        -> "Cho...";
            };
        }

        // Virtual-space helpers — clip to this quadrant's 128×128 slice

        private void vfill(MapCanvas c, int vx, int vy, int w, int h, byte color) {
            int ox = qx * 128, oy = qy * 128;
            int x1 = Math.max(vx, ox), y1 = Math.max(vy, oy);
            int x2 = Math.min(vx + w, ox + 128), y2 = Math.min(vy + h, oy + 128);
            for (int px = x1; px < x2; px++)
                for (int py = y1; py < y2; py++)
                    c.setPixel(px - ox, py - oy, color);
        }

        // Draws text starting at virtual (vx, vy); Bukkit clips out-of-canvas pixels.
        private void vtext(MapCanvas c, int vx, int vy, MapFont font, String text) {
            int cx = vx - qx * 128;
            int cy = vy - qy * 128;
            // Skip if text is entirely outside this quadrant vertically,
            // or starts so far right that nothing could be visible.
            if (cy < 0 || cy > 120) return;
            if (cx >= 128) return;
            c.drawText(cx, cy, font, text);
        }
    }
}
