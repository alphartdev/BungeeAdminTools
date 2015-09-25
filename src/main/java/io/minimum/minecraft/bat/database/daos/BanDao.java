package io.minimum.minecraft.bat.database.daos;

import io.minimum.minecraft.bat.database.data.Ban;

import java.util.UUID;

public interface BanDao {
    boolean isCurrentlyBanned(UUID uuid);
    Ban getMostRecentBan(UUID uuid);
    void createBan(Ban ban);
    void unban(UUID uuid);
}
