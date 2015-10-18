package io.minimum.minecraft.bat.database.daos;

import io.minimum.minecraft.bat.database.data.Ban;
import io.minimum.minecraft.bat.database.data.Server;

import java.util.List;
import java.util.UUID;

public interface BanHandler {
    List<Ban> getMostRecentBans(UUID uuid);
    void createBan(Ban ban);
    void unban(UUID uuid);
}
