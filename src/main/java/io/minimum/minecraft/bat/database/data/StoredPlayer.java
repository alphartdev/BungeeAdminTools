package io.minimum.minecraft.bat.database.data;

import lombok.Data;

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
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(lastOnlineTime.getTimeInMillis());
        return calendar;
    }
}
