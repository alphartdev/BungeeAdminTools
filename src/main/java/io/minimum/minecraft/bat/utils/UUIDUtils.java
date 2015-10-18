package io.minimum.minecraft.bat.utils;

import io.minimum.minecraft.bat.BungeeAdminTools;
import io.minimum.minecraft.bat.database.data.StoredPlayer;

import java.util.UUID;

public class UUIDUtils {
    public static String getName(UUID uuid) {
        StoredPlayer storedPlayer = BungeeAdminTools.get().getPlayerHandler().getData(uuid);
        if (storedPlayer == null || storedPlayer.getLastKnownName() == null) {
            // TODO: Fallback.
            return "UNKNOWN";
        }
        return storedPlayer.getLastKnownName();
    }
}
