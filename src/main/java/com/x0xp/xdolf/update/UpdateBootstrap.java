package com.x0xp.xdolf.update;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tiny JDK-only helper extracted from the running mod before an update restart.
 * It waits for Minecraft to release the old JAR, swaps the files, then launches
 * the exact Java command that started the client.
 */
public final class UpdateBootstrap {
    private UpdateBootstrap() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected updater state file");
        Path stateFile = Path.of(args[0]).toAbsolutePath().normalize();
        List<String> lines = Files.readAllLines(stateFile, StandardCharsets.UTF_8);
        if (lines.size() < 8) throw new IOException("Incomplete Xdolf updater state");

        int cursor = 0;
        long parentPid = Long.parseLong(lines.get(cursor++));
        Path oldJar = Path.of(decode(lines.get(cursor++))).toAbsolutePath().normalize();
        Path downloadedJar = Path.of(decode(lines.get(cursor++))).toAbsolutePath().normalize();
        Path finalJar = Path.of(decode(lines.get(cursor++))).toAbsolutePath().normalize();
        Path workingDirectory = Path.of(decode(lines.get(cursor++))).toAbsolutePath().normalize();
        String command = decode(lines.get(cursor++));
        int argumentCount = Integer.parseInt(lines.get(cursor++));
        if (lines.size() < cursor + argumentCount)
            throw new IOException("Incomplete Xdolf restart command");

        List<String> restart = new ArrayList<>(argumentCount + 1);
        restart.add(command);
        for (int i = 0; i < argumentCount; i++) restart.add(decode(lines.get(cursor++)));

        waitForParent(parentPid);
        install(oldJar, downloadedJar, finalJar);

        new ProcessBuilder(restart)
            .directory(workingDirectory.toFile())
            .start();

        Files.deleteIfExists(stateFile);
    }

    private static void waitForParent(long pid) throws Exception {
        var process = ProcessHandle.of(pid);
        if (process.isEmpty()) return;
        try {
            process.get().onExit().get(120, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException timeout) {
            throw new IOException("Minecraft did not exit within two minutes", timeout);
        }
        Thread.sleep(450L);
    }

    private static void install(Path oldJar, Path downloadedJar, Path finalJar) throws Exception {
        IOException last = null;
        for (int attempt = 0; attempt < 80; attempt++) {
            try {
                if (!oldJar.equals(finalJar)) Files.deleteIfExists(oldJar);
                try {
                    Files.move(downloadedJar, finalJar,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(downloadedJar, finalJar, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (IOException error) {
                last = error;
                Thread.sleep(250L);
            }
        }
        throw last == null ? new IOException("Could not install Xdolf update") : last;
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
