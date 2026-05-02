package net.craftnepal.market.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminModeManager {
    private static final Set<UUID> adminModePlayers = new HashSet<>();

    public static void toggleAdminMode(UUID uuid) {
        if (adminModePlayers.contains(uuid)) {
            adminModePlayers.remove(uuid);
        } else {
            adminModePlayers.add(uuid);
        }
    }

    public static boolean isInAdminMode(UUID uuid) {
        return adminModePlayers.contains(uuid);
    }
}
