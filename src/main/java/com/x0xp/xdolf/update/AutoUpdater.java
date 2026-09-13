package com.x0xp.xdolf.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.x0xp.xdolf.core.Xdolf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks Xdolf's GitHub releases and performs user-approved in-place updates.
 * Network work never runs on Minecraft's render thread.
 */
public final class AutoUpdater {
    public enum Phase {
        IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, VERIFYING, READY, RESTARTING, ERROR
    }

    public record ReleaseInfo(String version, String tag, String assetName, URI downloadUri,
                              URI releaseUri, long size, String sha256) { }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final URI RELEASES_API = URI.create(
        "https://api.github.com/repos/x0XP/xdolf-forge-1.21.10/releases?per_page=20");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-dev\\.(\\d+))?$");
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(8))
        .build();

    private static final AtomicBoolean CHECK_STARTED = new AtomicBoolean();
    private static volatile Phase phase = Phase.IDLE;
    private static volatile ReleaseInfo release;
    private static volatile String status = "Waiting to check for updates";
    private static volatile String errorMessage = "";
    private static volatile long downloadedBytes;
    private static volatile long totalBytes;
    private static volatile boolean prompted;

    private AutoUpdater() {}

    public static void beginCheck() {
        if (Boolean.getBoolean("xdolf.smokeTest") || !CHECK_STARTED.compareAndSet(false, true)) return;
        phase = Phase.CHECKING;
        status = "Checking for Xdolf updates...";
        CompletableFuture.runAsync(() -> {
            try {
                String current = currentVersion();
                ReleaseInfo latest = fetchLatestRelease();
                if (latest != null && compareVersions(latest.version(), current) > 0) {
                    release = latest;
                    totalBytes = latest.size();
                    phase = Phase.AVAILABLE;
                    status = "Xdolf " + latest.version() + " is available";
                    LOGGER.info("Xdolf update available: {} -> {}", current, latest.version());
                } else {
                    phase = Phase.UP_TO_DATE;
                    status = "Xdolf is up to date";
                }
            } catch (Exception error) {
                // Update checks are advisory. A network failure must never block startup.
                phase = Phase.ERROR;
                errorMessage = concise(error);
                status = "Could not check for updates";
                LOGGER.debug("Xdolf update check failed", error);
            }
        });
    }

    /** Called from the normal client tick so checking starts only after client loading has completed. */
    public static void tick(Minecraft mc) {
        if (Boolean.getBoolean("xdolf.smokeTest")) return;
        beginCheck();
        if (prompted || phase != Phase.AVAILABLE || release == null) return;
        Screen current = mc.screen;
        if (!startupScreen(current)) return;
        prompted = true;
        mc.setScreen(new UpdateScreen(current, release));
    }

    public static synchronized void downloadAndRestart() {
        if (release == null || (phase != Phase.AVAILABLE && phase != Phase.ERROR)) return;
        ReleaseInfo target = release;
        phase = Phase.DOWNLOADING;
        status = "Downloading " + target.assetName() + "...";
        errorMessage = "";
        downloadedBytes = 0L;
        totalBytes = target.size();

        CompletableFuture.runAsync(() -> {
            Path temporary = null;
            try {
                Path mods = FMLPaths.MODSDIR.get().toAbsolutePath().normalize();
                Files.createDirectories(mods);
                Path currentJar = locateCurrentJar(mods);
                Path finalJar = mods.resolve(target.assetName()).normalize();
                temporary = mods.resolve("." + target.assetName() + ".download").normalize();
                Files.deleteIfExists(temporary);

                download(target, temporary);
                phase = Phase.VERIFYING;
                status = "Verifying downloaded update...";
                verify(target, temporary);
                phase = Phase.READY;
                status = "Update verified. Restarting Xdolf...";

                Path downloaded = temporary;
                Minecraft.getInstance().execute(() -> {
                    try {
                        launchRestartHelper(currentJar, downloaded, finalJar);
                    } catch (Exception error) {
                        fail("Could not prepare automatic restart", error);
                    }
                });
            } catch (Exception error) {
                if (temporary != null) {
                    try { Files.deleteIfExists(temporary); }
                    catch (IOException ignored) { }
                }
                fail("Update failed", error);
            }
        });
    }

    public static void dismiss() {
        prompted = true;
    }

    public static Phase phase() { return phase; }
    public static String status() { return status; }
    public static String errorMessage() { return errorMessage; }
    public static long downloadedBytes() { return downloadedBytes; }
    public static long totalBytes() { return totalBytes; }
    public static ReleaseInfo release() { return release; }

    public static String currentVersion() {
        try {
            return ModList.get().getModContainerById(Xdolf.ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("0.0.0-dev.0");
        } catch (RuntimeException ignored) {
            Package pkg = AutoUpdater.class.getPackage();
            String implementation = pkg == null ? null : pkg.getImplementationVersion();
            return implementation == null ? "0.0.0-dev.0" : implementation;
        }
    }

    public static float progress() {
        long total = totalBytes;
        if (phase == Phase.VERIFYING || phase == Phase.READY || phase == Phase.RESTARTING) return 1.0f;
        if (total <= 0L) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, downloadedBytes / (float) total));
    }

    static int compareVersions(String first, String second) {
        ParsedVersion a = ParsedVersion.parse(first);
        ParsedVersion b = ParsedVersion.parse(second);
        if (a == null || b == null) return first.compareToIgnoreCase(second);
        return a.compareTo(b);
    }

    private static ReleaseInfo fetchLatestRelease() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Xdolf-Minecraft-Updater/" + currentVersion())
            .timeout(Duration.ofSeconds(12))
            .GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200)
            throw new IOException("GitHub returned HTTP " + response.statusCode());

        JsonArray releases = JsonParser.parseString(response.body()).getAsJsonArray();
        List<ReleaseInfo> candidates = new ArrayList<>();
        for (JsonElement element : releases) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (booleanValue(object, "draft")) continue;
            String tag = string(object, "tag_name");
            if (!tag.startsWith("xdolf-")) continue;
            String version = tag.substring("xdolf-".length());
            if (ParsedVersion.parse(version) == null) continue;

            String expectedAsset = "xdolf-" + version + ".jar";
            JsonArray assets = object.has("assets") && object.get("assets").isJsonArray()
                ? object.getAsJsonArray("assets") : new JsonArray();
            for (JsonElement assetElement : assets) {
                if (!assetElement.isJsonObject()) continue;
                JsonObject asset = assetElement.getAsJsonObject();
                if (!expectedAsset.equals(string(asset, "name"))) continue;
                URI download = trustedGithubUri(string(asset, "browser_download_url"));
                URI page = trustedGithubUri(string(object, "html_url"));
                long size = longValue(asset, "size");
                String digest = string(asset, "digest");
                if (digest.toLowerCase(Locale.ROOT).startsWith("sha256:")) digest = digest.substring(7);
                else digest = "";
                candidates.add(new ReleaseInfo(version, tag, expectedAsset, download, page, size, digest));
            }
        }

        return candidates.stream().max(Comparator.comparing(ReleaseInfo::version, AutoUpdater::compareVersions)).orElse(null);
    }

    private static void download(ReleaseInfo target, Path destination) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(target.downloadUri())
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "Xdolf-Minecraft-Updater/" + currentVersion())
            .timeout(Duration.ofMinutes(3))
            .GET().build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("Download returned HTTP " + response.statusCode());

        try (InputStream input = response.body();
             OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) continue;
                output.write(buffer, 0, read);
                downloadedBytes += read;
            }
        }
    }

    private static void verify(ReleaseInfo target, Path file) throws Exception {
        long actualSize = Files.size(file);
        if (target.size() > 0L && actualSize != target.size())
            throw new IOException("Downloaded size was " + actualSize + " bytes; expected " + target.size());
        if (actualSize < 64 * 1024)
            throw new IOException("Downloaded update is unexpectedly small");

        if (!target.sha256().isBlank()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read);
            }
            String actual = java.util.HexFormat.of().formatHex(digest.digest());
            if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII),
                    target.sha256().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))
                throw new SecurityException("GitHub SHA-256 verification failed");
        }
    }

    private static Path locateCurrentJar(Path mods) throws Exception {
        try {
            var source = AutoUpdater.class.getProtectionDomain().getCodeSource();
            if (source != null) {
                Path location = Path.of(source.getLocation().toURI()).toAbsolutePath().normalize();
                if (Files.isRegularFile(location) && location.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    return location;
            }
        } catch (Exception ignored) { }

        try (var stream = Files.list(mods)) {
            Optional<Path> fallback = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().matches("(?i)xdolf-.*\\.jar"))
                .findFirst();
            if (fallback.isPresent()) return fallback.get().toAbsolutePath().normalize();
        }
        throw new IOException("Could not locate the currently installed Xdolf JAR");
    }

    private static void launchRestartHelper(Path currentJar, Path downloadedJar, Path finalJar) throws Exception {
        ProcessHandle.Info info = ProcessHandle.current().info();
        String command = info.command().orElseThrow(() -> new IOException("Could not determine Java launch command"));
        String[] arguments = info.arguments().orElseThrow(() -> new IOException("Could not capture Minecraft launch arguments"));
        Path gameDirectory = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path helperRoot = gameDirectory.resolve(".xdolf-updater");
        Path helperClass = helperRoot.resolve("com/x0xp/xdolf/update/UpdateBootstrap.class");
        Path state = helperRoot.resolve("update.state");
        Path log = helperRoot.resolve("update.log");
        Files.createDirectories(helperClass.getParent());

        try (InputStream resource = AutoUpdater.class.getResourceAsStream("/com/x0xp/xdolf/update/UpdateBootstrap.class")) {
            if (resource == null) throw new IOException("Updater bootstrap class is missing from the Xdolf JAR");
            Files.copy(resource, helperClass, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        List<String> lines = new ArrayList<>();
        lines.add(Long.toString(ProcessHandle.current().pid()));
        lines.add(encode(currentJar.toString()));
        lines.add(encode(downloadedJar.toString()));
        lines.add(encode(finalJar.toString()));
        lines.add(encode(gameDirectory.toString()));
        lines.add(encode(command));
        lines.add(Integer.toString(arguments.length));
        for (String argument : arguments) lines.add(encode(argument));
        Files.write(state, lines, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path helperJava = Path.of(System.getProperty("java.home"), "bin", windows ? "javaw.exe" : "java");
        if (!Files.isRegularFile(helperJava)) helperJava = Path.of(command);

        List<String> helperCommand = List.of(helperJava.toString(), "-cp", helperRoot.toString(),
            "com.x0xp.xdolf.update.UpdateBootstrap", state.toString());
        new ProcessBuilder(helperCommand)
            .directory(gameDirectory.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile())
            .start();

        phase = Phase.RESTARTING;
        status = "Restarting Minecraft with Xdolf " + release.version() + "...";
        LOGGER.info("Xdolf update verified; restart helper launched for {}", release.version());

        Minecraft mc = Minecraft.getInstance();
        CompletableFuture.delayedExecutor(450, java.util.concurrent.TimeUnit.MILLISECONDS)
            .execute(() -> mc.execute(mc::stop));
    }

    private static boolean startupScreen(Screen screen) {
        if (screen == null) return false;
        return screen instanceof TitleScreen || screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen");
    }

    private static URI trustedGithubUri(String raw) throws IOException {
        if (raw == null || raw.isBlank()) throw new IOException("Release URL is missing");
        URI uri = URI.create(raw);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!uri.getScheme().equalsIgnoreCase("https") ||
            !(host.equals("github.com") || host.endsWith(".github.com") || host.endsWith(".githubusercontent.com")))
            throw new IOException("Untrusted update URL: " + uri);
        return uri;
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String concise(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private static void fail(String prefix, Throwable error) {
        phase = Phase.ERROR;
        errorMessage = concise(error);
        status = prefix + ": " + errorMessage;
        LOGGER.error(prefix, error);
    }

    private static String string(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return "";
        try { return object.get(key).getAsString(); }
        catch (RuntimeException ignored) { return ""; }
    }

    private static boolean booleanValue(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return false;
        try { return object.get(key).getAsBoolean(); }
        catch (RuntimeException ignored) { return false; }
    }

    private static long longValue(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return 0L;
        try { return object.get(key).getAsLong(); }
        catch (RuntimeException ignored) { return 0L; }
    }

    private record ParsedVersion(int major, int minor, int patch, Integer dev) implements Comparable<ParsedVersion> {
        static ParsedVersion parse(String value) {
            Matcher matcher = VERSION_PATTERN.matcher(value);
            if (!matcher.matches()) return null;
            try {
                Integer dev = matcher.group(4) == null ? null : Integer.parseInt(matcher.group(4));
                return new ParsedVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), dev);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        @Override
        public int compareTo(ParsedVersion other) {
            int result = Integer.compare(major, other.major);
            if (result != 0) return result;
            result = Integer.compare(minor, other.minor);
            if (result != 0) return result;
            result = Integer.compare(patch, other.patch);
            if (result != 0) return result;
            if (dev == null && other.dev == null) return 0;
            if (dev == null) return 1;
            if (other.dev == null) return -1;
            return Integer.compare(dev, other.dev);
        }
    }
}
