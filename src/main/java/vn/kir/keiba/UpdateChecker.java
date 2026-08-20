package vn.kir.keiba;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class UpdateChecker {
    private final KirKeibaPlugin plugin;
    private volatile Result cached;
    private volatile CompletableFuture<Result> running;

    UpdateChecker(KirKeibaPlugin plugin) {
        this.plugin = plugin;
        this.cached = new Result(State.UNKNOWN, currentVersion(), "", releasePage(), "", 0);
    }

    void checkOnStartup() {
        if (!plugin.getConfig().getBoolean("update-check.enabled", true)
                || !plugin.getConfig().getBoolean("update-check.check-on-startup", true)) return;
        check(false).thenAccept(result -> {
            if (result.state == State.AVAILABLE) plugin.getLogger().info("Có KirKeiba " + result.latest + ": " + result.url);
            else if (result.state == State.FAILED) plugin.getLogger().warning("Không thể kiểm tra cập nhật: " + result.error);
        });
    }

    void report(CommandSender sender, boolean force) {
        sender.sendMessage(Text.color(render("update-check.messages.current", "&7Phiên bản KirKeiba hiện tại: &f{current}", cached)));
        if (!plugin.getConfig().getBoolean("update-check.enabled", true)) {
            sender.sendMessage(Text.color(render("update-check.messages.disabled", "&8Kiểm tra cập nhật đang tắt trong config.", cached)));
            return;
        }
        Result before = cached;
        if (!force && before.state != State.UNKNOWN && !stale(before)) {
            sender.sendMessage(Text.color(message(before)));
            return;
        }
        sender.sendMessage(Text.color(render("update-check.messages.checking", "&eĐang kiểm tra bản mới từ GitHub...", before)));
        check(force).thenAccept(result -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!(sender instanceof Player player) || player.isOnline()) sender.sendMessage(Text.color(message(result)));
        }));
    }

    synchronized CompletableFuture<Result> check(boolean force) {
        if (!plugin.getConfig().getBoolean("update-check.enabled", true)) return CompletableFuture.completedFuture(cached);
        if (!force && cached.state != State.UNKNOWN && !stale(cached)) return CompletableFuture.completedFuture(cached);
        if (running != null && !running.isDone()) return running;
        int connectSeconds = Math.max(1, plugin.getConfig().getInt("update-check.connect-timeout-seconds", 5));
        int requestSeconds = Math.max(1, plugin.getConfig().getInt("update-check.request-timeout-seconds", 10));
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(connectSeconds)).build();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(apiUrl()))
                    .timeout(Duration.ofSeconds(requestSeconds))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "KirKeiba/" + currentVersion())
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .GET().build();
        } catch (RuntimeException error) {
            cached = failed("Repository GitHub không hợp lệ");
            return CompletableFuture.completedFuture(cached);
        }
        running = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200
                        ? fromJson(currentVersion(), response.body(), releasePage())
                        : failed("GitHub HTTP " + response.statusCode()))
                .exceptionally(error -> failed(error.getCause() == null ? error.getMessage() : error.getCause().getMessage()))
                .thenApply(result -> { cached = result; return result; });
        return running;
    }

    private boolean stale(Result result) {
        long minutes = Math.max(1, plugin.getConfig().getLong("update-check.cache-minutes", 30));
        return System.currentTimeMillis() - result.checkedAt > Duration.ofMinutes(minutes).toMillis();
    }

    private String message(Result result) {
        return switch (result.state) {
            case LATEST -> render("update-check.messages.latest", "&aBạn đang dùng bản mới nhất: &f{current}", result);
            case AVAILABLE -> render("update-check.messages.available", "&eCó bản mới &f{latest}&e: &b{url}", result);
            case FAILED -> render("update-check.messages.failed", "&cKhông thể kiểm tra cập nhật: &7{error}", result);
            case UNKNOWN -> render("update-check.messages.checking", "&eĐang kiểm tra bản mới từ GitHub...", result);
        };
    }

    private String render(String path, String fallback, Result result) {
        return Text.render(plugin.getConfig().getString(path, fallback), Map.of(
                "current", result.current,
                "latest", result.latest.isBlank() ? "—" : result.latest,
                "url", result.url,
                "error", result.error.isBlank() ? "—" : result.error));
    }

    private Result failed(String error) {
        return new Result(State.FAILED, currentVersion(), "", releasePage(), error == null ? "Lỗi không xác định" : error, System.currentTimeMillis());
    }

    private String currentVersion() { return plugin.getPluginMeta().getVersion(); }
    private String repository() {
        String repository = plugin.getConfig().getString("update-check.repository", "rogteam/KirKeiba");
        return repository == null ? "rogteam/KirKeiba" : repository.trim();
    }
    private String apiUrl() {
        String repository = repository();
        if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Invalid GitHub repository");
        return "https://api.github.com/repos/" + repository + "/releases/latest";
    }
    private String releasePage() {
        String repository = repository();
        if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) repository = "rogteam/KirKeiba";
        return "https://github.com/" + repository + "/releases/latest";
    }

    static Result fromJson(String current, String json, String fallbackUrl) {
        String latest = ReleaseVersion.tagFromJson(json), page = ReleaseVersion.urlFromJson(json, fallbackUrl);
        if (latest.isBlank()) return new Result(State.FAILED, current, "", fallbackUrl, "Phản hồi GitHub thiếu tag_name", System.currentTimeMillis());
        State state = ReleaseVersion.compare(latest, current) > 0 ? State.AVAILABLE : State.LATEST;
        return new Result(state, current, latest, page, "", System.currentTimeMillis());
    }

    enum State { UNKNOWN, LATEST, AVAILABLE, FAILED }
    record Result(State state, String current, String latest, String url, String error, long checkedAt) { }
}
