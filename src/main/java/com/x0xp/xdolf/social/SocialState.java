package com.x0xp.xdolf;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class SocialState {
    private static final Set<String> FRIENDS = new TreeSet<>();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("xdolf-friends.txt");

    static void load() {
        try {
            if (Files.isRegularFile(FILE)) for (String line : Files.readAllLines(FILE)) {
                if (line.matches("[A-Za-z0-9_]{1,16}")) FRIENDS.add(line.toLowerCase(Locale.ROOT));
            }
        } catch (IOException error) { LogUtils.getLogger().warn("Could not load Xdolf friends", error); }
    }
    public static boolean isFriend(String name) { return FRIENDS.contains(name.toLowerCase(Locale.ROOT)); }
    static void command(String[] parts) {
        if (parts.length==2 && parts[1].equalsIgnoreCase("clear")) {
            FRIENDS.clear();save();return;
        }
        if (parts.length == 2 && parts[1].equalsIgnoreCase("list")) {
            ClientRuntime.message("Friends: " + String.join(", ", FRIENDS));
            return;
        }
        if (parts.length < 3 || !parts[2].matches("[A-Za-z0-9_]{1,16}")) {
            ClientRuntime.message(".friend add <name> [alias] | .friend del <name> | .friend list/clear"); return;
        }
        String name = parts[2].toLowerCase(Locale.ROOT);
        if (parts[1].equalsIgnoreCase("add")) FRIENDS.add(name);
        else if (parts[1].equalsIgnoreCase("remove") || parts[1].equalsIgnoreCase("del")) FRIENDS.remove(name);
        else { ClientRuntime.message("Use add, remove or list."); return; }
        save();
    }
    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.write(FILE, FRIENDS);
            ClientRuntime.message("Friends updated.");
        } catch (IOException error) {
            LogUtils.getLogger().warn("Could not save Xdolf friends", error);
            ClientRuntime.message("Friends changed for this session, but saving failed.");
        }
    }
}
