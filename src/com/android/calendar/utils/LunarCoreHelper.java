package com.android.calendar.utils;

import android.util.Log;

/**
 * 양력-음력 변환을 위해 리용되는 클라스
 */
public class LunarCoreHelper {
    private static double LOCAL_TIME_ZONE = 8.0f;
    private static final double PI = Math.PI;
    private static String TAG ="LunarCoreHelper";

    /**
     * @param dd 일
     * @param mm 월
     * @param yy 년
     * @return Julian day를 돌려준다
     */
    private static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        int c = dd + (153 * m + 2) / 5 + 365 * y + y / 4;
        int jd = c - y / 100 + y / 400
                - 32045;
        if (jd < 2299161) {
            jd = c - 32083;
        }

        return jd;
    }

    /**
     * Julian day로부터 날자를 돌려준다
     * @param jd Julian Day(Int)
     * @return {년, 월, 일}
     */
    private static int[] jdToDate(int jd) {
        int a, b, c;
        if (jd > 2299160) { // After 5/10/1582, Gregorian calendar
            a = jd + 32044;
            b = (4 * a + 3) / 146097;
            c = a - (b * 146097) / 4;
        } else {
            b = 0;
            c = jd + 32082;
        }
        int d = (4 * c + 3) / 1461;
        int e = c - (1461 * d) / 4;
        int m = (5 * e + 2) / 153;
        int day = e - (153 * m + 2) / 5 + 1;
        int month = m + 3 - 12 * (m / 10);
        int year = b * 100 + d - 4800 + m / 10;
        return new int[] { day, month, year };
    }

    /**
     * Solar longitude in degrees Algorithm from: Astronomical Algorithms, by
     * Jean Meeus, 1998
     *
     * @param jdn
     *            - number of days since noon UTC on 1 January 4713 BC
     * @return
     */
    private static double SunLongitude(double jdn) {
        // return CC2K.sunLongitude(jdn);
        return SunLongitudeAA98(jdn);
    }

    private static double SunLongitudeAA98(double jdn) {
        double T = (jdn - 2415021.0) / 36525; // Time in Julian centuries from
        // 2000-01-01 12:00:00 GMT
        double T2 = T * T;
        double dr = PI / 180; // degree to radian
        double M = 357.52910 + 35999.05030 * T - 0.0001559 * T2 - 0.00000048
                * T * T2; // mean anomaly, degree
        double L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2; // mean
        // longitude,
        // degree
        double DL = (1.914600 - 0.004817 * T - 0.000014 * T2)
                * Math.sin(dr * M);
        DL = DL + (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.000290
                * Math.sin(dr * 3 * M);
        double L = L0 + DL; // true longitude, degree
        L = L - 360 * (INT(L / 360)); // Normalize to (0, 360)
        return L;
    }

    private static double NewMoon(int k) {
        // return CC2K.newMoonTime(k);
        return NewMoonAA98(k);
    }

    /**
     * Julian day number of the kth new moon after (or before) the New Moon of
     * 1900-01-01 13:51 GMT. Accuracy: 2 minutes Algorithm from: Astronomical
     * Algorithms, by Jean Meeus, 1998
     *
     * @param k
     * @return the Julian date number (number of days since noon UTC on 1
     *         January 4713 BC) of the New Moon
     */

    private static double NewMoonAA98(int k) {
        double T = k / 1236.85; // Time in Julian centuries from 1900 January
        // 0.5
        double T2 = T * T;
        double T3 = T2 * T;
        double dr = PI / 180;
        double Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2
                - 0.000000155 * T3;
        Jd1 = Jd1 + 0.00033
                * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr); // Mean
        // new
        // moon
        double M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347
                * T3; // Sun's mean anomaly
        double Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236
                * T3; // Moon's mean anomaly
        double F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239
                * T3; // Moon's argument of latitude
        double C1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021
                * Math.sin(2 * dr * M);
        C1 = C1 - 0.4068 * Math.sin(Mpr * dr) + 0.0161 * Math.sin(dr * 2 * Mpr);
        C1 = C1 - 0.0004 * Math.sin(dr * 3 * Mpr);
        C1 = C1 + 0.0104 * Math.sin(dr * 2 * F) - 0.0051
                * Math.sin(dr * (M + Mpr));
        C1 = C1 - 0.0074 * Math.sin(dr * (M - Mpr)) + 0.0004
                * Math.sin(dr * (2 * F + M));
        C1 = C1 - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006
                * Math.sin(dr * (2 * F + Mpr));
        C1 = C1 + 0.0010 * Math.sin(dr * (2 * F - Mpr)) + 0.0005
                * Math.sin(dr * (2 * Mpr + M));
        double deltat;
        if (T < -11) {
            deltat = 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3
                    - 0.000000081 * T * T3;
        } else {
            deltat = -0.000278 + 0.000265 * T + 0.000262 * T2;
        }
        ;
        return Jd1 + C1 - deltat;
    }

    private static int INT(double d) {
        return (int) Math.floor(d);
    }

    private static double getSunLongitude(int dayNumber, double timeZone) {
        return SunLongitude(dayNumber - 0.5 - timeZone / 24);
    }

    private static int getNewMoonDay(int k, double timeZone) {
        double jd = NewMoon(k);
        return INT(jd + 0.5 + timeZone / 24);
    }

    private static int getLunarMonth11(int yy, double timeZone) {
        double off = jdFromDate(31, 12, yy) - 2415021.076998695;
        int k = INT(off / 29.530588853);
        int nm = getNewMoonDay(k, timeZone);
        int sunLong = INT(getSunLongitude(nm, timeZone) / 30);
        if (sunLong >= 9) {
            nm = getNewMoonDay(k - 1, timeZone);
        }
        return nm;
    }

    private static int getLeapMonthOffset(int a11, double timeZone) {
        int k = INT(0.5 + (a11 - 2415021.076998695) / 29.530588853);
        int last=0; // Month 11 contains point of sun longutide 3*PI/2 (December
        // solstice)
        int i = 1; // We start with the month following lunar month 11
        int arc = INT(getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone) / 30);

        do {
            last = arc;
            i++;
            arc = INT(getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone) / 30);

        } while (arc != last && i < 14);
        return i - 1;
    }

    /**
     * 양력날자를 음력날자로 변환하여 돌려준다.
     * @param solarDay 년
     * @param solarMonth 월
     * @param solarYear 일
     * @return [lunarDay, lunarMonth, lunarYear, leapOrNot]의 옹근수배렬을 돌려준다.
     */
    public static int[] convertSolar2Lunar(int solarDay, int solarMonth, int solarYear) {
        return convertSolar2Lunar(solarDay, solarMonth, solarYear, LOCAL_TIME_ZONE);
    }

    /**
     * 양력날자를 음력날자로 변환하여 돌려준다.
     * @param solarDay 년
     * @param solarMonth 월
     * @param solarYear 일
     * @param timeZone 시간대 {@link #LOCAL_TIME_ZONE}
     * @return [lunarDay, lunarMonth, lunarYear, leapOrNot]의 옹근수배렬을 돌려준다.
     */
    public static int[] convertSolar2Lunar(int solarDay, int solarMonth, int solarYear,
                                           double timeZone) {
        int lunarDay, lunarMonth, lunarYear, lunarLeap;
        int dayNumber = jdFromDate(solarDay, solarMonth, solarYear);
        int k = INT((dayNumber - 2415021.076998695) / 29.530588853);
        int monthStart = getNewMoonDay(k + 1, timeZone);
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZone);
        }
        int a11 = getLunarMonth11(solarYear, timeZone);
        int b11 = a11;
        if (a11 >= monthStart) {
            lunarYear = solarYear;
            a11 = getLunarMonth11(solarYear - 1, timeZone);
        } else {
            lunarYear = solarYear + 1;
            b11 = getLunarMonth11(solarYear + 1, timeZone);
        }
        lunarDay = dayNumber - monthStart + 1;
        int diff = INT((monthStart - a11) / 29);
        lunarLeap = 0;
        lunarMonth = diff + 11;
        if (b11 - a11 > 365) {
            int leapMonthDiff = getLeapMonthOffset(a11, timeZone);

            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10;
                if (diff == leapMonthDiff) {
                    lunarLeap = 1;
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth = lunarMonth - 12;
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1;
        }
        return new int[] { lunarDay, lunarMonth, lunarYear, lunarLeap };
    }

    /**
     * 음력날자를 양력날자로 변환하여 돌려준다.
     * @param lunarDay 년
     * @param lunarMonth 월
     * @param lunarYear 일
     * @param timeZone 시간대 {@link #LOCAL_TIME_ZONE}
     * @return [solarDay, solarMonth, solarYear]의 배렬을 돌려준다
     */
    public static int[] convertLunar2Solar(int lunarDay, int lunarMonth,
                                           int lunarYear, int lunarLeap, double timeZone) {
        int a11, b11;
        if (lunarMonth < 11) {
            a11 = getLunarMonth11(lunarYear - 1, timeZone);
            b11 = getLunarMonth11(lunarYear, timeZone);
        } else {
            a11 = getLunarMonth11(lunarYear, timeZone);
            b11 = getLunarMonth11(lunarYear + 1, timeZone);
        }
        int k = INT(0.5 + (a11 - 2415021.076998695) / 29.530588853);
        int off = lunarMonth - 11;
        if (off < 0) {
            off += 12;
        }
        if (b11 - a11 > 365) {
            int leapOff = getLeapMonthOffset(a11, timeZone);
            int leapMonth = leapOff - 2;
            if (leapMonth < 0) {
                leapMonth += 12;
            }
            if (lunarLeap != 0 && lunarMonth != leapMonth) {
                Log.d(TAG,"Invalid input!");
                return new int[] { 0, 0, 0 };
            } else if (lunarLeap != 0 || off >= leapOff) {
                off += 1;
            }
        }
        int monthStart = getNewMoonDay(k + off, timeZone);
        return jdToDate(monthStart + lunarDay - 1);
    }
}