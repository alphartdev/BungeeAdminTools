package io.minimum.minecraft.bat.utils;

import com.google.common.base.Preconditions;
import gnu.trove.TCollections;
import gnu.trove.map.TCharIntMap;
import gnu.trove.map.hash.TCharIntHashMap;
import gnu.trove.map.hash.TCharObjectHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Calendar;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CalendarUtils {
    private static final TCharIntMap UNIT_LOOKUP_MAP;

    static {
        TCharIntHashMap map = new TCharIntHashMap();
        map.put('s', Calendar.SECOND);
        map.put('m', Calendar.MINUTE);
        map.put('h', Calendar.HOUR);
        map.put('d', Calendar.DAY_OF_MONTH);
        map.put('w', Calendar.WEEK_OF_MONTH);
        map.put('y', Calendar.YEAR);
        UNIT_LOOKUP_MAP = TCollections.unmodifiableMap(map);
    }

    /**
     * Parses a duration into text.
     * @param from the base to use
     * @param text the text to parse
     * @return the new calendar
     */
    public static Calendar parseDuration(Calendar from, String text) {
        Preconditions.checkNotNull(from, "from");
        Preconditions.checkNotNull(text, "text");

        Calendar copy = (Calendar) from.clone();

        // Parse the duration and add it.
        int s = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (UNIT_LOOKUP_MAP.containsKey(c)) {
                String n = text.substring(s, i);
                int d = Integer.parseInt(n);
                copy.add(UNIT_LOOKUP_MAP.get(c), d);
                s = i + 1;
            }
        }

        return copy;
    }
}
