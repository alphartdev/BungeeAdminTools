package io.minimum.minecraft.bat.database.daos.impl;

import io.minimum.minecraft.bat.database.daos.PlayerDao;
import io.minimum.minecraft.bat.database.data.StoredPlayer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JsonPlayerDao implements PlayerDao {
    private final ConcurrentMap<UUID, StoredPlayer> storedPlayersByUuid = new ConcurrentHashMap<>();

    @Override
    public StoredPlayer getOrCreateData(UUID uuid) {
        StoredPlayer storedPlayer = new StoredPlayer(uuid);
        StoredPlayer existed = storedPlayersByUuid.putIfAbsent(uuid, storedPlayer);
        return existed == null ? storedPlayer : existed;
    }

    @Override
    public void updateData(PendingConnection connection) {
        StoredPlayer player = getOrCreateData(connection.getUniqueId());
        if (player.getLastKnownName() == null)
            player.setLastKnownName(connection.getName());
        player.setLastOnlineTime(Calendar.getInstance());
        player.setLastServer("UNKNOWN");
    }

    @Override
    public void updateServer(ProxiedPlayer player) {
        StoredPlayer storedPlayer = getOrCreateData(player.getUniqueId());
        storedPlayer.setLastServer(player.getServer().getInfo().getName());
    }
}
