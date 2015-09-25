package io.minimum.minecraft.bat.database.data;

import io.minimum.minecraft.bat.utils.CalendarUtils;
import lombok.Data;
import lombok.NonNull;

import java.util.Calendar;
import java.util.UUID;

/**
 * A player stored in the local database.
 */
@Data
public class StoredPlayer {
    /**
     * The player's current UUID.
     */
    @NonNull
    private final UUID uuid;
    /**
     * The player's last known name.
     */
    private String lastKnownName;
    /**
     * The last time we know that the player was online.
     */
    private Calendar lastOnlineTime;
    /**
     * The last server this player was on.
     */
    private String lastServer;

    public Calendar getLastOnlineTime() {
        return lastOnlineTime == null ? lastOnlineTime : CalendarUtils.copy(lastOnlineTime);
    }
}
