package io.minimum.minecraft.bat;

import io.minimum.minecraft.bat.utils.CalendarUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

public class CalendarUtilsTest {
    @Test
    public void simpleParseDurationTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 1);

        Calendar one = CalendarUtils.parseDuration(calendar, "1s");
        Assert.assertEquals(2, one.get(Calendar.SECOND));
    }

    @Test
    public void multiDdigitParseDurationTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 10);

        Calendar one = CalendarUtils.parseDuration(calendar, "10s");
        Assert.assertEquals(20, one.get(Calendar.SECOND));
    }

    @Test
    public void multiDurationParseDurationTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 10);
        calendar.set(Calendar.MINUTE, 1);
        // Use the first day of March 2015, as it's a nice and round time.
        // March 2016 deviates a little, but May 2015 doesn't have a Friday the 13th.
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.YEAR, 2015);
        calendar.set(Calendar.HOUR, 1);
        calendar.set(Calendar.MINUTE, 1);
        calendar.set(Calendar.SECOND, 1);
        Calendar one = CalendarUtils.parseDuration(calendar, "1y1w1m1d1h1s");

        Assert.assertEquals(2, one.get(Calendar.SECOND));
        Assert.assertEquals(2, one.get(Calendar.MINUTE));
        Assert.assertEquals(2, one.get(Calendar.HOUR));
        Assert.assertEquals(2, one.get(Calendar.WEEK_OF_MONTH));
        Assert.assertEquals(Calendar.MARCH, one.get(Calendar.MONTH));
        Assert.assertEquals(2016, one.get(Calendar.YEAR));
        Assert.assertEquals(calendar.get(Calendar.YEAR) + 1, one.get(Calendar.YEAR));
    }
}
