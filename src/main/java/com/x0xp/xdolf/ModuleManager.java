package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** One authority for enablement, dependencies, conflicts and temporary suspension. */
final class ModuleManager {
    enum Activity { DISABLED, WAITING_FOR_WORLD, MISSING_DEPENDENCY, SUSPENDED, PAUSED, ACTIVE }
    record Status(Activity activity, String detail) {
        boolean active() { return activity == Activity.ACTIVE; }
    }

    private ModuleManager() {}

    static boolean setEnabled(ClientModule module, boolean enabled) {
        return enabled ? enable(module, new HashSet<>()) : disable(module);
    }

    static boolean toggle(ClientModule module) {
        return setEnabled(module, !module.enabled());
    }

    private static boolean enable(ClientModule module, Set<String> path) {
        if (module.enabled()) return true;
        if (!path.add(module.name)) {
            ClientRuntime.message("Cannot enable " + module.name + ": dependency cycle.");
            return false;
        }

        for (String name : module.dependencies()) {
            ClientModule dependency = ClientRuntime.find(name);
            if (dependency == null || !enable(dependency, path)) {
                ClientRuntime.message("Cannot enable " + module.name + ": requires " + name + ".");
                return false;
            }
        }

        var disabled = new LinkedHashSet<String>();
        for (ClientModule other : ClientRuntime.MODULES) {
            if (other == module || !other.enabled() || !conflict(module, other)) continue;
            other.applyEnabled(false);
            disabled.add(other.name);
        }
        module.applyEnabled(true);
        if (!disabled.isEmpty())
            NotificationCards.warning("Module conflict", String.join(", ", disabled) + " disabled");
        return true;
    }

    private static boolean disable(ClientModule module) {
        if (!module.enabled()) return true;
        var dependents = ClientRuntime.MODULES.stream()
            .filter(other -> other.enabled() && other.dependencies().stream().anyMatch(name -> name.equalsIgnoreCase(module.name)))
            .toList();
        for (ClientModule dependent : dependents) dependent.applyEnabled(false);
        module.applyEnabled(false);
        return true;
    }

    static boolean conflict(ClientModule first, ClientModule second) {
        return contains(first.conflicts(), second.name) || contains(second.conflicts(), first.name);
    }

    static List<String> conflicts(ClientModule module) {
        return ClientRuntime.MODULES.stream().filter(other -> other != module && conflict(module, other))
            .map(other -> other.name).toList();
    }

    static Status status(ClientModule module, Minecraft mc) {
        if (!module.enabled()) return new Status(Activity.DISABLED, "Disabled");
        if (mc == null || mc.player == null || mc.level == null || mc.getConnection() == null)
            return new Status(Activity.WAITING_FOR_WORLD, "Waiting for a world");
        for (String name : module.dependencies()) {
            ClientModule dependency = ClientRuntime.find(name);
            if (dependency == null || !dependency.enabled())
                return new Status(Activity.MISSING_DEPENDENCY, "Requires " + name);
        }
        ClientModule freecam = ClientRuntime.find("Freecam");
        if (freecam != null && freecam.enabled() && module != freecam && !module.runsDuringFreecam())
            return new Status(Activity.SUSPENDED, "Paused by Freecam");
        if (mc.isPaused() && !module.runsWhilePaused())
            return new Status(Activity.PAUSED, "Paused with the game");
        return new Status(Activity.ACTIVE, "Active");
    }

    static boolean active(ClientModule module, Minecraft mc) {
        return status(module, mc).active();
    }

    static void reconcileRestoredSelections() {
        for (ClientModule module : ClientRuntime.MODULES) {
            if (!module.enabled()) continue;
            for (ClientModule other : ClientRuntime.MODULES) {
                if (other == module || !other.enabled() || !conflict(module, other)) continue;
                other.restoreEnabled(false);
            }
        }
    }

    private static boolean contains(Set<String> values, String name) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(name));
    }
}
