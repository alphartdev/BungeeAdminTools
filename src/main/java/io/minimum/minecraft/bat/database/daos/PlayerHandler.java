package io.minimum.minecraft.bat.database.daos;

import io.minimum.minecraft.bat.database.data.StoredPlayer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public interface PlayerHandler {
    StoredPlayer getOrCreateData(UUID uuid);
    void updateData(PendingConnection connection);
    void updateServer(ProxiedPlayer player);
}
