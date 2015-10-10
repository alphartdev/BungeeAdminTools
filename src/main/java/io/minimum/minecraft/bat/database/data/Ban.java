package io.minimum.minecraft.bat.database.data;

import io.minimum.minecraft.bat.utils.CalendarUtils;
import lombok.Data;
import lombok.NonNull;

import java.util.Calendar;
import java.util.UUID;

@Data
public class Ban {
    /**
     * The target of this ban.
     */
    @NonNull
    private final UUID target;
    /**
     * The creator of this ban.
     */
    @NonNull
    private final UUID creator;
    /**
     * When this ban was created.
     */
    @NonNull
    private final Calendar bannedAt;
    /**
     * When this ban expires.
     */
    private final Calendar expiresAt;
    /**
     * The reason for this ban.
     */
    private final String reason;
    /**
     * Whether or not this user was manually unbanned.
     */
    private boolean manuallyUnbanned = false;

    /**
     * Determines whether or not this ban is applicable. The exact criteria are:
     * <ul>
     *     <li>if {@code manuallyUnbanned} is true, then the user was unbanned</li>
     *     <li>if {@code bannedAt} is null, the user's ban will never expire</li>
     *     <li>otherwise, whether or not the current date is past {@code bannedAt}</li>
     * </ul>
     * @return true if the ban is still applicable, false if it is.
     */
    public boolean isApplicable() {
        return manuallyUnbanned || (bannedAt != null && bannedAt.after(Calendar.getInstance()));
    }

    public Calendar getBannedAt() {
        return (Calendar) bannedAt.clone();
    }

    public Calendar getExpiresAt() {
        return expiresAt == null ? expiresAt : (Calendar) expiresAt.clone();
    }
}
