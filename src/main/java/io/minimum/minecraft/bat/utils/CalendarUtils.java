package io.minimum.minecraft.bat.utils;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Calendar;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CalendarUtils {
    /**
     * Makes a defensive copy of the specified {@link java.util.Calendar}.
     * @param calendar the Calendar to copy
     * @return a new Calendar that is equivalent to the original
     */
    public static Calendar copy(Calendar calendar) {
        Preconditions.checkNotNull(calendar, "calendar");
        Calendar newCalendar = Calendar.getInstance();
        newCalendar.setTimeInMillis(calendar.getTimeInMillis());
        newCalendar.setTimeZone(newCalendar.getTimeZone());
        return newCalendar;
    }
}
