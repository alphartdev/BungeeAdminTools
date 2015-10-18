package io.minimum.minecraft.bat.database.daos.impl;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.bat.database.daos.BanHandler;
import io.minimum.minecraft.bat.database.data.Ban;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class JsonBanHandler implements BanHandler {
    private final ConcurrentMap<UUID, List<Ban>> storedPlayersByUuid = new ConcurrentHashMap<>();

    @Override
    public List<Ban> getMostRecentBans(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        List<Ban> bans = storedPlayersByUuid.get(uuid);
        List<Ban> applicable = new ArrayList<>();
        if (bans != null) {
            for (Ban ban : bans) {
                if (ban.isApplicable()) {
                    applicable.add(ban);
                }
            }
        }
        return applicable;
    }

    @Override
    public void createBan(Ban ban) {
        Preconditions.checkNotNull(ban, "ban");
        List<Ban> avail = new CopyOnWriteArrayList<>();
        List<Ban> bans = storedPlayersByUuid.putIfAbsent(ban.getTarget(), avail);
        if (bans == null) {
            avail.add(ban);
        } else {
            bans.add(ban);
        }
    }

    @Override
    public void unban(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        List<Ban> bans = storedPlayersByUuid.get(uuid);
        if (bans != null) {
            for (Ban ban : bans) {
                if (ban.isApplicable()) {
                    ban.setManuallyUnbanned(true);
                }
            }
        }
    }
}
