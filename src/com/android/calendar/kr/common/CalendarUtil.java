/*
 * Copyright (C) 2016 huanghaibin_dev <huanghaibin_dev@163.com>
 * WebSite https://github.com/MiracleTimes-Dev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.calendar.kr.common;

import android.annotation.SuppressLint;
import android.content.Context;

import com.android.krcalendar.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 달력 날자계산, 현시을 위한 Helper클라스
 */
public final class CalendarUtil {

    public static final long ONE_DAY = 1000 * 3600 * 24;

    @SuppressLint("SimpleDateFormat")
    public static int getDate(String formatStr, Date date) {
        SimpleDateFormat format = new SimpleDateFormat(formatStr);
        return Integer.parseInt(format.format(date));
    }

    /**
     * 월에 해당한 날자수를 계산하여 돌려준다.
     *
     * @param year  년
     * @param month 월
     */
    public static int getMonthDaysCount(int year, int month) {
        int count = 0;
        //31일
        if (month == 1 || month == 3 || month == 5 || month == 7
                || month == 8 || month == 10 || month == 12) {
            count = 31;
        }

        //30일
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            count = 30;
        }

        //2월
        if (month == 2) {
            if (isLeapYear(year)) { //윤년일때
                count = 29;
            } else {    //윤년이 아닐때
                count = 28;
            }
        }
        return count;
    }

    /**
     * 윤년인가를 돌려준다.
     *
     * @param year 년
     */
    static boolean isLeapYear(int year) {
        return ((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0);
    }

    /**
     * 월의 시작날자와 그주 일요일과의 날자수차이를 돌려준다.
     * @param year 년
     * @param month 월
     */
    static int getMonthViewStartDiff(int year, int month) {
        java.util.Calendar date = java.util.Calendar.getInstance();
        date.set(year, month - 1, 1);
        int week = date.get(java.util.Calendar.DAY_OF_WEEK);
        return week - 1;
    }

    /**
     * 월의 마감날자와 그주 토요일과의 날자수차이를 돌려준다.
     * @param year 년
     * @param month 월
     */
    static int getMonthEndDiff(int year, int month) {
        return getMonthEndDiff(year, month, getMonthDaysCount(year, month));
    }

    /**
     * 입력날자와 그주 토요일과의 날자수차이를 돌려준다.
     * @param year 년
     * @param month 월
     * @param day 일
     */
    private static int getMonthEndDiff(int year, int month, int day) {
        java.util.Calendar date = java.util.Calendar.getInstance();
        date.set(year, month - 1, day);
        int week = date.get(java.util.Calendar.DAY_OF_WEEK);
        return 7 - week;
    }

    /**
     * 입력날자가 날자범위에 있는가를 돌려준다.
     * @param calendar 날자
     * @param minYear 시작날자-년
     * @param minYearMonth 시작날자-월
     * @param minYearDay 시작날자-일
     * @param maxYear 마감날자-년
     * @param maxYearMonth 마감날자-월
     * @param maxYearDay 마감날자-일
     */
    static boolean isCalendarInRange(Calendar calendar,
                                     int minYear, int minYearMonth, int minYearDay,
                                     int maxYear, int maxYearMonth, int maxYearDay) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(minYear, minYearMonth - 1, minYearDay);
        long minTime = c.getTimeInMillis();
        c.set(maxYear, maxYearMonth - 1, maxYearDay);
        long maxTime = c.getTimeInMillis();
        c.set(calendar.getYear(), calendar.getMonth() - 1, calendar.getDay());
        long curTime = c.getTimeInMillis();
        return curTime >= minTime && curTime <= maxTime;
    }


    /**
     * 입력날자가 날자한계를 넘어서지 않았는가를 돌려준다.
     *
     * @param calendar 입력날자
     * @param delegate delegate
     */
    static boolean isCalendarInRange(Calendar calendar, CalendarViewDelegate delegate) {
        return isCalendarInRange(calendar,
                delegate.getMinYear(), delegate.getMinYearMonth(), delegate.getMinYearDay(),
                delegate.getMaxYear(), delegate.getMaxYearMonth(), delegate.getMaxYearDay());
    }

    /**
     * 시작날자가 속하는 주부터 마감날자가 속하는 주까지의 모든 날자들을 순서대로 가지고 있는 날자목록을 돌려준다.
     *
     * @param year        년
     * @param month       월
     * @param currentDate 일
     */
    public static List<Calendar> initCalendarForMonthView(int year, int month, Calendar currentDate) {
        int mPreDiff = getMonthViewStartDiff(year, month);  //시작여백날자수
        int monthDayCount = getMonthDaysCount(year, month); //마감여백날자수

        int preYear, preMonth;
        int nextYear, nextMonth;

        int size = 42;  //6주
        List<Calendar> mItems = new ArrayList<>();

        //Calendar객체를 구성하여 결과목록에 추가해준다.
        int preMonthDaysCount;
        if (month == 1) {
            preYear = year - 1;
            preMonth = 12;
            nextYear = year;
            nextMonth = month + 1;
            preMonthDaysCount = mPreDiff == 0 ? 0 : CalendarUtil.getMonthDaysCount(preYear, preMonth);
        } else if (month == 12) {
            preYear = year;
            preMonth = month - 1;
            nextYear = year + 1;
            nextMonth = 1;
            preMonthDaysCount = mPreDiff == 0 ? 0 : CalendarUtil.getMonthDaysCount(preYear, preMonth);
        } else {
            preYear = year;
            preMonth = month - 1;
            nextYear = year;
            nextMonth = month + 1;
            preMonthDaysCount = mPreDiff == 0 ? 0 : CalendarUtil.getMonthDaysCount(preYear, preMonth);
        }
        int nextDay = 1;
        for (int i = 0; i < size; i++) {
            Calendar calendarDate = new Calendar();
            if (i < mPreDiff) {
                calendarDate.setYear(preYear);
                calendarDate.setMonth(preMonth);
                calendarDate.setDay(preMonthDaysCount - mPreDiff + i + 1);
            } else if (i >= monthDayCount + mPreDiff) {
                calendarDate.setYear(nextYear);
                calendarDate.setMonth(nextMonth);
                calendarDate.setDay(nextDay);
                ++nextDay;
            } else {
                calendarDate.setYear(year);
                calendarDate.setMonth(month);
                calendarDate.setCurrentMonth(true);
                calendarDate.setDay(i - mPreDiff + 1);
            }
            if (calendarDate.equals(currentDate)) {
                calendarDate.setCurrentDay(true);
            }
            mItems.add(calendarDate);
        }
        return mItems;
    }


    /**
     * 페지의 시작날자를 돌려준다.
     * @param position 페지위치
     * @param delegate Delegate
     */
    public static Calendar getFirstCalendarFromMonthViewPager(int position, CalendarViewDelegate delegate) {
        Calendar calendar = new Calendar();
        calendar.setYear((position + delegate.getMinYearMonth() - 1) / 12 + delegate.getMinYear());
        calendar.setMonth((position + delegate.getMinYearMonth() - 1) % 12 + 1);
        calendar.setDay(1);

        if (!isCalendarInRange(calendar, delegate)) {
            if (isMinRangeEdge(calendar, delegate)) {
                calendar = delegate.getMinRangeCalendar();
            } else {
                calendar = delegate.getMaxRangeCalendar();
            }
        }

        calendar.setCurrentMonth(calendar.getYear() == delegate.getCurrentDay().getYear() &&
                calendar.getMonth() == delegate.getCurrentDay().getMonth());
        calendar.setCurrentDay(calendar.equals(delegate.getCurrentDay()));
        return calendar;
    }


    /**
     * 입력날자가 최소한계날자를 넘어섰는가를 돌려준다.
     *
     * @param calendar calendar
     * @param delegate Delegate
     */
    private static boolean isMinRangeEdge(Calendar calendar, CalendarViewDelegate delegate) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(delegate.getMinYear(), delegate.getMinYearMonth() - 1, delegate.getMinYearDay());
        long minTime = c.getTimeInMillis();
        c.set(calendar.getYear(), calendar.getMonth() - 1, calendar.getDay());
        long curTime = c.getTimeInMillis();
        return curTime < minTime;
    }

    /**
     * dp -> pixel 변환
     *
     * @param context context
     * @param dpValue dp
     * @return px
     */
    public static int dipToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 음력날자문자렬을 돌려준다.
     * @param lunar 음력날자(년, 월, 일, 윤)
     * @param showLunar `음력`문자렬을 현시하겠는가?
     * @param context Context
     * @return String
     */
    @SuppressLint("DefaultLocale")
    public static String getLunarDayString(int[] lunar, boolean showLunar, Context context){
        int day = lunar[0];
        int month = lunar[1];
        int leap = lunar[3];

        String resultString = "";
        if(leap == 1) {
            if(showLunar)
                resultString = String.format(context.getString(R.string.lunar_string_with_leap), month, day);
            else
                resultString = String.format(context.getString(R.string.lunar_string_with_leap_none_lunar), month, day);
        }
        else{
            if(showLunar)
                resultString = String.format(context.getString(R.string.lunar_string_without_leap), month, day);
            else
                resultString = String.format(context.getString(R.string.lunar_string_without_leap_none_lunar), month, day);
        }
        return resultString;
    }
}
