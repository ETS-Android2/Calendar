package com.android.kr_common;
import android.annotation.SuppressLint;
import android.text.format.DateUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * 체계기정 Time클라스의 Year 2038문제를 대책하기 위하여 새롭게 작성한 Time클라스
 * @see android.text.format.Time
 */
public class Time {
    public static final String TIMEZONE_UTC = "UTC";
    public static final int EPOCH_JULIAN_DAY = 2440588;

    //마당이름들
    public static final int SECOND = 1;     //초
    public static final int MINUTE = 2;     //분
    public static final int HOUR = 3;       //시간
    public static final int MONTH_DAY = 4;  //월의 날자(1-30)
    public static final int MONTH = 5;      //월(0-11)
    public static final int YEAR = 6;       //년
    public static final int WEEK_DAY = 7;   //요일(0-6)
    public static final int YEAR_DAY = 8;   //년의 날자(0-365)
    public static final int WEEK_NUM = 9;   //년의 주

    /**
     * 요일들
     * @see #weekDay
     */
    public static final int SUNDAY = 0;
    public static final int MONDAY = 1;
    public static final int TUESDAY = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY = 4;
    public static final int FRIDAY = 5;
    public static final int SATURDAY = 6;

    //월별 날자수배렬
    private static final int[] DAYS_PER_MONTH = { 31, 28, 31, 30, 31, 30, 31,
            31, 30, 31, 30, 31 };

    //하루, 한시간, 1분의 초수
    private static final int ONE_DAY_SECONDS = 86400;
    private static final int ONE_HOUR_SECONDS = 3600;
    private static final int ONE_MINUTE_SECONDS = 60;

    public int year;
    public int month;
    public int monthDay;
    public int yearDay;
    public int weekDay;
    public int hour;
    public int minute;
    public int second;
    public int isDst;
    public boolean allDay;
    public String timezone;
    public long gmtoff;

    public Time(){
        allDay = false;
        setToNow();
    }

    public Time(String timezoneId) {
        this();
        timezone = timezoneId;
    }

    public Time(Time new_time){
        year = new_time.year;
        month = new_time.month;
        monthDay = new_time.monthDay;
        hour = new_time.hour;
        minute = new_time.minute;
        second = new_time.second;
        allDay = new_time.allDay;
        timezone = new_time.timezone;
        calculate();
    }

    public Time(long timeMillis){
        set(timeMillis);
    }

    private void calculate(){
        DateTime dateTime = new DateTime(year, month + 1, monthDay, 0, 0);

        Calendar mCalendar = new GregorianCalendar();
        mCalendar.set(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
        TimeZone mTimeZone = mCalendar.getTimeZone();
        gmtoff = mTimeZone.getRawOffset()/1000;
        weekDay = dateTime.getDayOfWeek() % 7;
    }

    public void set(int new_second, int new_minute, int new_hour,
                    int new_day, int new_month, int new_year){
        second = new_second;
        minute = new_minute;
        hour = new_hour;
        monthDay = new_day;
        month = new_month;
        year = new_year;
        allDay = false;
        normalize(false);
    }
    public void set(int new_day, int new_month, int new_year){
        monthDay = new_day;
        month = new_month;
        year = new_year;
        second = 0;
        minute = 0;
        hour = 0;
        allDay = true;
        normalize(false);
    }
    public void set(Time other){
        year = other.year;
        month = other.month;
        monthDay = other.monthDay;
        hour = other.hour;
        minute = other.minute;
        second = other.second;
        allDay = other.allDay;
        calculate();
    }
    public void set(long timeMillis) {
        final DateTime dateTime = new DateTime(timeMillis);

        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        hour = dateTime.getHourOfDay();
        minute = dateTime.getMinuteOfHour();
        second = dateTime.getSecondOfMinute();
        calculate();
    }
    public void setToNow() {
        DateTime dateTime = DateTime.now();
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        hour = dateTime.getHourOfDay();
        minute = dateTime.getMinuteOfHour();
        second = dateTime.getSecondOfMinute();
        calculate();
    }
    public void plusDays(int numDays){
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute);
        dateTime = dateTime.plusDays(numDays);
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        calculate();
    }
    public void minusDays(int numDays) {
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute);
        dateTime = dateTime.minusDays(numDays);
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        calculate();
    }
    public void plusMonths(int numMonths){
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute);
        dateTime = dateTime.plusMonths(numMonths);
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        calculate();
    }
    public void minusMonths(int numMonths){
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute);
        dateTime = dateTime.minusMonths(numMonths);
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        calculate();
    }
    public void plusMinutes(int numMinutes){
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute);
        dateTime = dateTime.plusMinutes(numMinutes);
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        hour = dateTime.getHourOfDay();
        minute = dateTime.getMinuteOfHour();
        calculate();
    }
    public void plusSeconds(int numSeconds){
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute, second);
        dateTime = dateTime.plusSeconds(numSeconds);
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        hour = dateTime.getHourOfDay();
        minute = dateTime.getMinuteOfHour();
        second = dateTime.getSecondOfMinute();
        calculate();
    }

    public void minusSeconds(int numSeconds){
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute, second);
        dateTime = dateTime.minusSeconds(numSeconds);
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        hour = dateTime.getHourOfDay();
        minute = dateTime.getMinuteOfHour();
        second = dateTime.getSecondOfMinute();
        calculate();
    }
    public void plusHours(int numHours){
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute);
        dateTime = dateTime.plusHours(numHours);
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear() - 1;
        monthDay = dateTime.getDayOfMonth();
        hour = dateTime.getHourOfDay();
    }
    public long toMillis(boolean ignoreDist){
        final DateTime dateTime;
        if(allDay || (timezone != null && timezone.equals("UTC"))) {
            dateTime = new DateTime(DateTimeZone.UTC).withDate(year, month + 1, monthDay).withMillisOfDay(0);
        }
        else {
            dateTime = new DateTime(year, month + 1, monthDay, hour, minute, 0);
        }
        return dateTime.getMillis();
    }

    /**
     * Ensures the values in each field are in range. For example if the
     * current value of this calendar is March 32, normalize() will convert it
     * to April 1. It also fills in weekDay, yearDay, isDst and gmtoff.
     */
    public long normalize(boolean ignoreDist){
        //First set right hour, minute, second
        int totalSecond = hour * ONE_HOUR_SECONDS + minute * ONE_MINUTE_SECONDS + second;
        int dayCount = 0;
        if(totalSecond < 0) {
            while (totalSecond < 0)
            {
                dayCount --;
                totalSecond += ONE_DAY_SECONDS;
            }
        }
        else {
            while (totalSecond > ONE_DAY_SECONDS)
            {
                dayCount ++;
                totalSecond -= ONE_DAY_SECONDS;
            }
        }
        hour = totalSecond / ONE_HOUR_SECONDS;
        minute = (totalSecond % ONE_HOUR_SECONDS) / ONE_MINUTE_SECONDS;
        second = (totalSecond % ONE_HOUR_SECONDS) % ONE_MINUTE_SECONDS;

        //Then set day, month, year
        monthDay += dayCount;
        if(month < 0) {
            while (month < 0) {
                month += 12;
                year ++;
            }
        }
        else {
            while (month > 11){
                month -= 12;
                year --;
            }
        }

        if(monthDay < 1) {
            DateTime dateTime = new DateTime(year, month + 1, 1, 0, 0);
            dateTime = dateTime.minusDays(Math.abs(monthDay - 1));
            year = dateTime.getYear();
            month = dateTime.getMonthOfYear() - 1;
            monthDay = dateTime.getDayOfMonth();
        }
        else {
            DateTime dateTime = new DateTime(year, month + 1, 1, 0, 0);
            dateTime = dateTime.plusDays(monthDay - 1);
            year = dateTime.getYear();
            month = dateTime.getMonthOfYear() - 1;
            monthDay = dateTime.getDayOfMonth();
        }
        calculate();

        return toMillis(ignoreDist);
    }

    public static int compare(Time a, Time b) {
        DateTime time1 = new DateTime(a.year, a.month + 1, a.monthDay, a.hour, a.minute);
        DateTime time2 = new DateTime(b.year, b.month + 1, b.monthDay, b.hour, b.minute);
        if(time1.isAfter(time2))
            return 1;
        if(time1.isBefore(time2))
            return -1;
        return 0;
    }

    public boolean parse(String s) throws Exception {
        if (s == null) {
            throw new NullPointerException("time string is null");
        }
        if (parseInternal(s)) {
            timezone = TIMEZONE_UTC;
            return true;
        }
        return false;
    }

    private boolean parseInternal(String s) throws Exception {
        int len = s.length();
        if (len < 8) {
            throw new Exception("String is too short: \"" + s +
                    "\" Expected at least 8 characters.");
        }

        boolean inUtc = false;

        // year
        int n = getChar(s, 0, 1000);
        n += getChar(s, 1, 100);
        n += getChar(s, 2, 10);
        n += getChar(s, 3, 1);
        year = n;

        // month
        n = getChar(s, 4, 10);
        n += getChar(s, 5, 1);
        n--;
        month = n;

        // day of month
        n = getChar(s, 6, 10);
        n += getChar(s, 7, 1);
        monthDay = n;

        if (len > 8) {
            if (len < 15) {
                throw new Exception(
                        "String is too short: \"" + s
                                + "\" If there are more than 8 characters there must be at least"
                                + " 15.");
            }
            checkChar(s, 8, 'T');
            allDay = false;

            // hour
            n = getChar(s, 9, 10);
            n += getChar(s, 10, 1);
            hour = n;

            // min
            n = getChar(s, 11, 10);
            n += getChar(s, 12, 1);
            minute = n;

            // sec
            n = getChar(s, 13, 10);
            n += getChar(s, 14, 1);
            second = n;

            if (len > 15) {
                // Z
                checkChar(s, 15, 'Z');
                inUtc = true;
            }
        } else {
            allDay = true;
            hour = 0;
            minute = 0;
            second = 0;
        }

        weekDay = 0;
        yearDay = 0;
        isDst = -1;
        gmtoff = 0;
        return inUtc;
    }

    private static int getChar(String s, int spos, int mul) throws Exception {
        char c = s.charAt(spos);
        if (Character.isDigit(c)) {
            return Character.getNumericValue(c) * mul;
        } else {
            throw new Exception("Parse error at pos=" + spos);
        }
    }

    private void checkChar(String s, int spos, char expected) throws Exception {
        char c = s.charAt(spos);
        if (c != expected) {
            throw new Exception(String.format(
                    "Unexpected character 0x%02d at pos=%d.  Expected 0x%02d (\'%c\').",
                    (int) c, spos, (int) expected, expected));
        }
    }

    public int getActualMaximum(int field) {
        switch (field) {
            case SECOND:
                return 59; // leap seconds, bah humbug
            case MINUTE:
                return 59;
            case HOUR:
                return 23;
            case MONTH_DAY: {
                int n = DAYS_PER_MONTH[this.month];
                if (n != 28) {
                    return n;
                } else {
                    int y = this.year;
                    return ((y % 4) == 0 && ((y % 100) != 0 || (y % 400) == 0)) ? 29 : 28;
                }
            }
            case MONTH:
                return 11;
            case YEAR:
                return 2300;
            case WEEK_DAY:
                return 6;
            case YEAR_DAY: {
                int y = this.year;
                // Year days are numbered from 0, so the last one is usually 364.
                return ((y % 4) == 0 && ((y % 100) != 0 || (y % 400) == 0)) ? 365 : 364;
            }
            case WEEK_NUM:
                throw new RuntimeException("WEEK_NUM not implemented");
            default:
                throw new RuntimeException("bad field=" + field);
        }
    }

    public int getWeekNumber() {
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute);
        return dateTime.getWeekOfWeekyear();
    }

    public int getDayOfWeek(){
        DateTime dateTime = new DateTime(year, month + 1, monthDay, hour, minute);
        return dateTime.getDayOfWeek()%7;
    }

    public void clear(String timezoneId) {
        if (timezoneId == null) {
            throw new NullPointerException("timezone is null!");
        }
        this.timezone = timezoneId;
        this.allDay = false;
        this.second = 0;
        this.minute = 0;
        this.hour = 0;
        this.monthDay = 0;
        this.month = 0;
        this.year = 0;
        this.weekDay = 0;
        this.yearDay = 0;
        this.gmtoff = 0;
    }

    public void clear(){
        this.allDay = false;
        this.second = 0;
        this.minute = 0;
        this.hour = 0;
        this.monthDay = 0;
        this.month = 1;
        this.year = 0;
        this.weekDay = 0;
        this.yearDay = 0;
        this.gmtoff = 0;
    }

    public void switchTimezone(String timezone) {
        this.timezone = timezone;
        if(this.timezone.equals("UTC")){
            DateTime dateTime = new DateTime(toMillis(false), DateTimeZone.UTC);
            set(dateTime.getSecondOfMinute(), dateTime.getMinuteOfHour(), dateTime.getHourOfDay(),
                    dateTime.getDayOfMonth(), dateTime.getMonthOfYear() - 1, dateTime.getYear());
        }
    }

    @SuppressLint("DefaultLocale")
    public String format2445() {
        String result = String.format("%d%02d%02dT%02d%02d%02d",
                year, month+1, monthDay, hour, minute, second);
        if(timezone.equals("UTC"))
            result += "Z";
        return result;
    }
    public String format(String format) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat(convertToSampleFormat(format));
        Date date = new Date(toMillis(false));

        return dateFormat.format(date);
    }

    private String convertToSampleFormat(String oldFormat){
        StringBuilder newFormat = new StringBuilder();
        int i = 0;
        boolean handleToken = false;
        for (i = 0; i < oldFormat.length(); i ++){
            char c = oldFormat.charAt(i);
            if(handleToken){
                boolean realToken = true;
                switch (c){
                    case 'Y':
                        newFormat.append("YYYY");
                        break;
                    case 'y':
                        newFormat.append("YY");
                        break;
                    case 'm':
                        newFormat.append("MM");
                        break;
                    case 'd':
                        newFormat.append("dd");
                        break;
                    case 'H':
                        newFormat.append("h");
                        break;
                    case 'M':
                        newFormat.append("mm");
                        break;
                    case 'S':
                        newFormat.append("ss");
                        break;
                    case 'a':
                        newFormat.append("E");
                        break;
                    case 'A':
                        newFormat.append("EEEE");
                        break;
                    case 'b':
                    case 'h':
                        newFormat.append("MMM");
                        break;
                    case 'B':
                        newFormat.append("MMMM");
                        break;
                    case 'I':
                    case 'l':
                        newFormat.append("hh");
                        break;
                    case 'P':
                    case 'p':
                        newFormat.append("aaa");
                        break;
                    case 'k':
                        newFormat.append("HH");
                        break;
                    default:
                        newFormat.append('%');
                        realToken = false;
                        break;
                }
                handleToken = false;
                if(realToken)
                    continue;
            }
            if(c == '%'){
                handleToken = true;
            }
            else {
                newFormat.append(oldFormat.charAt(i));
            }
        }
        return newFormat.toString();
    }

    public static String getCurrentTimezone() {
        return TimeZone.getDefault().getID();
    }

    /**
     * 날자로부터 Julian day얻기
     * @param year 년
     * @param month 월
     * @param day 일
     */
    public static int getJulianDay(int year, int month, int day) {
        DateTime dateTime = new DateTime(DateTimeZone.UTC).withDate(year, month, day).withMillisOfDay(0);
        long julianDay = dateTime.getMillis() / DateUtils.DAY_IN_MILLIS;
        return (int) julianDay + EPOCH_JULIAN_DAY;
    }
    public static int getJulianDay(Time time) {
        return getJulianDay(time.year, time.month + 1, time.monthDay);
    }
    public static int getJulianDay(DateTime dateTime) {
        return getJulianDay(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
    }

    public static boolean isLastWeek(Time time) {
        Time nextWeek = new Time(time);
        nextWeek.plusDays(7);
        return nextWeek.month != time.month;
    }

    public long setJulianDay(int julianDay) {
        // Don't bother with the GMT offset since we don't know the correct
        // value for the given Julian day.  Just get close and then adjust
        // the day.
        long millis = (julianDay - EPOCH_JULIAN_DAY) * DateUtils.DAY_IN_MILLIS;
        set(millis);
        // Figure out how close we are to the requested Julian day.
        // We can't be off by more than a day.
        int approximateDay = getJulianDay(this);
        int diff = julianDay - approximateDay;
        plusDays(diff);

        // Set the time to 12am and re-normalize.
        hour = 0;
        minute = 0;
        second = 0;
        millis = normalize(true);

        return millis;
    }
}
