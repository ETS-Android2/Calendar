/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar.recurrencepicker;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;

import com.android.calendar.utils.Utils;
import com.android.kr_common.Time;
import android.util.TimeFormatException;

import com.android.calendarcommon2.EventRecurrence;

import java.util.Calendar;

import com.android.krcalendar.R;

/**
 * 반복설정 문자렬생성을 위한 클라스
 */
public class EventRecurrenceFormatter
{
    //`n번째 s요일`형식의 문자렬 resouce, 문자렬들 보관
    private static int[] mMonthRepeatByDayOfWeekIds;
    private static String[][] mMonthRepeatByDayOfWeekStrs;

    /**
     * 반복설정 문자렬을 돌려준다.( 2주간격(수요일) )
     * @param context Context
     * @param r Resources
     * @param recurrence 반목정보
     * @param includeEndString 반복정보에 마감날자 혹은 회수설정이 있는가?
     */
    public static String getRepeatString(Context context, Resources r, EventRecurrence recurrence,
            boolean includeEndString) {
        String endString = "";
        if (includeEndString) {
            StringBuilder sb = new StringBuilder();
            if (recurrence.until != null) {
                try {
                    Time t = new Time();
                    t.parse(recurrence.until);
                    final String dateStr = DateUtils.formatDateTime(context,
                            t.toMillis(false), DateUtils.FORMAT_SHOW_DATE);
                    sb.append(", ");
                    sb.append(r.getString(R.string.endByDate, dateStr));
                } catch (TimeFormatException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (recurrence.count > 0) {
                sb.append(", ");
                sb.append(r.getQuantityString(R.plurals.endByCount, recurrence.count,
                        recurrence.count));
            }
            endString = sb.toString();
        }

        // TODO Implement "Until" portion of string, as well as custom settings
        int interval = Math.max(recurrence.interval, 1);
        StringBuilder sb = new StringBuilder();

        switch (recurrence.freq) {
            case EventRecurrence.DAILY:
                if(interval == 1)
                    return r.getString(R.string.label_every_day) + endString;
                else
                    return r.getString(R.string.daily_interval_other, interval) + endString;

            case EventRecurrence.WEEKLY: {
                final String string;
                if (recurrence.repeatsOnEveryWeekDay()) {
                    string = "(" + r.getString(R.string.every_weekday) + ")";
                }
                else {
                    int dayOfWeekLength = DateUtils.LENGTH_MEDIUM;
                    if (recurrence.bydayCount == 1) {
                        dayOfWeekLength = DateUtils.LENGTH_LONG;
                    }

                    StringBuilder days = new StringBuilder();

                    // Do one less iteration in the loop so the last element is added out of the
                    // loop. This is done so the comma is not placed after the last item.

                    boolean isEveryWeek = false;
                    if(recurrence.interval == 0 || recurrence.interval == 1) {
                        if(recurrence.bydayCount == 0)
                            isEveryWeek = true;
                        else if(recurrence.bydayCount == 1 && recurrence.byday != null && recurrence.byday.length == 1){
                            int weekDay = EventRecurrence.day2TimeDay(recurrence.byday[0]);
                            if(recurrence.startDate != null)
                                isEveryWeek = recurrence.startDate.weekDay == weekDay;
                        }
                    }

                    if (!isEveryWeek) {
                        int count = recurrence.bydayCount - 1;

                        days.append("(");
                        for (int i = 0; i < count; i++) {
                            days.append(dayToString(recurrence.byday[i], dayOfWeekLength));
                            days.append(", ");
                        }
                        days.append(dayToString(recurrence.byday[count], dayOfWeekLength));
                        days.append(")");

                        string = days.toString();
                    } else {
                        // There is no "BYDAY" specifier, so use the day of the
                        // first event.  For this to work, the setStartDate()
                        // method must have been used by the caller to set the
                        // date of the first event in the recurrence.
                        if (recurrence.startDate == null) {
                            return null;
                        }

                        return r.getString(R.string.label_every_week) + endString;
                    }
                }

                if(interval == 1)
                    return r.getString(R.string.weekly_interval_one, string) + endString;
                else
                    return r.getString(R.string.weekly_interval_other, interval, string) + endString;
            }
            case EventRecurrence.MONTHLY: {
                if (recurrence.bydayCount == 1) {
                    int weekday = EventRecurrence.day2TimeDay(recurrence.byday[0]);

                    // Cache this stuff so we won't have to redo work again later.
                    cacheMonthRepeatStrings(r, weekday);

                    final int dayNumber;
                    if(Time.isLastWeek(recurrence.startDate))
                        dayNumber = 4;
                    else
                        dayNumber = (recurrence.startDate.monthDay - 1) / 7;

                    if(interval == 1)
                        sb.append(r.getString(R.string.label_every_month));
                    else
                        sb.append(r.getString(R.string.monthly_interval_other, interval));

                    String weekDayString = mMonthRepeatByDayOfWeekStrs[weekday][dayNumber];
                    sb.append(" (");
                    sb.append(weekDayString);
                    sb.append(")");
                    sb.append(endString);

                    return sb.toString();
                }
                else {
                    if(interval == 1)
                        sb.append(r.getString(R.string.label_every_month));
                    else
                        sb.append(r.getString(R.string.monthly_interval_other, interval));

                    String monthDayString = Utils.getUpperString(getMonthlyRepeatByDayString(r, recurrence.bymonthday[0], true));
                    sb.append(" (");
                    sb.append(monthDayString);
                    sb.append(")");
                    sb.append(endString);

                    return sb.toString();
                }
            }
            case EventRecurrence.YEARLY:
                if(interval == 1)
                    return Utils.getLowerString(r.getString(R.string.label_every_year)) + endString;
                else
                    return r.getString(R.string.yearly_interval_other, interval) + endString;
        }

        return null;
    }

    /**
     * 반복설정 설명 문자렬을 돌려준다.(이번 일정은 매월 2번째주 수요일마다 반복합니다)
     * @param context Context
     * @param r Resources
     * @param recurrence 반복정보
     * @param includeEndString 반복정보에 마감날자 혹은 회수설정이 있는가?
     */
    public static String getFullRepeatString(Context context, Resources r, EventRecurrence recurrence,
                                             boolean includeEndString) {
        String startString = "", endString = "";

        if (includeEndString) {
            StringBuilder sb = new StringBuilder();
            if (recurrence.until != null) {
                try {
                    Time t = new Time();
                    t.parse(recurrence.until);
                    final String dateStr = DateUtils.formatDateTime(context, t.toMillis(false),
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);
                    sb.append(" ");
                    sb.append(r.getString(R.string.endByDate, dateStr));
                } catch (TimeFormatException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (recurrence.count > 0) {
                sb.append(" ");
                sb.append(r.getQuantityString(R.plurals.endByCount, recurrence.count,
                        recurrence.count));
            }
            endString = sb.toString();
        }

        int interval = Math.max(recurrence.interval, 1);
        switch (recurrence.freq) {
            case EventRecurrence.DAILY:
                if(interval == 1)
                    startString = r.getString(R.string.every_day_combination);
                else
                    startString = r.getString(R.string.other_day_combination, interval);
                break;

            case EventRecurrence.WEEKLY: {
                final String string;
                if (recurrence.repeatsOnEveryWeekDay()) {
                    string = r.getString(R.string.every_weekday);
                }
                else {
                    int dayOfWeekLength = DateUtils.LENGTH_MEDIUM;
                    if (recurrence.bydayCount == 1) {
                        dayOfWeekLength = DateUtils.LENGTH_LONG;
                    }

                    StringBuilder days = new StringBuilder();

                    //요일이 여러개 있는 경우 `,`으로 구분하여 붙여준다.
                    if (recurrence.bydayCount > 0) {
                        int count = recurrence.bydayCount - 1;

                        if(count > 0) {
                            for (int i = 0; i < count; i++) {
                                days.append(dayToString(recurrence.byday[i], dayOfWeekLength));
                                days.append(", ");
                            }
                        }
                        days.append(dayToString(recurrence.byday[count], dayOfWeekLength));

                        string = days.toString();
                    } else {
                        //반복요일 이 없는 경우에는 시작날자의 요일을 반복요일로 보여준다.
                        if (recurrence.startDate == null) { //시작날자는 null이 되지 말아야 한다.
                            return null;
                        }

                        int day = EventRecurrence.timeDay2Day(recurrence.startDate.weekDay);
                        string = dayToString(day, DateUtils.LENGTH_LONG);
                    }
                }
                if(interval == 1) {
                    startString = r.getString(R.string.every_week_combination, string);
                }
                else {
                    startString = r.getString(R.string.other_week_combination, interval, string);
                }
            }
            break;

            case EventRecurrence.MONTHLY: {
                if (recurrence.bydayCount == 1) {
                    int weekday = EventRecurrence.day2TimeDay(recurrence.byday[0]);

                    //요일반복문자렬들을 모두 얻어서 보관한다.
                    cacheMonthRepeatStrings(r, weekday);

                    final int dayNumber;
                    if(Time.isLastWeek(recurrence.startDate))
                        dayNumber = 4;
                    else
                        dayNumber = (recurrence.startDate.monthDay - 1) / 7;

                    String weekDayString = Utils.getLowerString(mMonthRepeatByDayOfWeekStrs[weekday][dayNumber]);

                    if(interval == 1)
                        startString = r.getString(R.string.every_month_combination_weekday, weekDayString);
                    else
                        startString = r.getString(R.string.other_month_combination_weekday, interval, weekDayString);
                }
                else {
                    if(recurrence.bymonthday != null && recurrence.bymonthday.length > 0) {
                        String monthDayString = getMonthlyRepeatByDayString(r, recurrence.bymonthday[0], true);
                        if(interval == 1)
                            startString = r.getString(R.string.every_month_combination_monthday, monthDayString);
                        else
                            startString = r.getString(R.string.other_month_combination_monthday, interval, monthDayString);
                    }
                }
            }
            break;

            case EventRecurrence.YEARLY:
                if(recurrence.startDate != null)
                {
                    StringBuilder sb = new StringBuilder();

                    final String dateStr = DateUtils.formatDateTime(context, recurrence.startDate.toMillis(false),
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR);
                    if(interval == 1)
                        sb.append(r.getString(R.string.every_year_combination, dateStr));
                    else
                        sb.append(r.getString(R.string.other_year_combination, interval, dateStr));

                    startString = sb.toString();
                }
                else {
                    if(interval == 1)
                        startString = Utils.getLowerString(r.getString(R.string.label_every_year));
                    else
                        startString = r.getString(R.string.yearly_interval_other, interval);
                }
        }

        return r.getString(R.string.recurrence_full_string_format, (startString + endString));
    }

    /**
     * 월반복(날자마다) 문자렬을 돌려준다.
     * @param resources Resources
     * @param monthDay 날자
     * @param lowerCase 소문자?
     */
    public static String getMonthlyRepeatByDayString(Resources resources, int monthDay, boolean lowerCase) {
        final String repeatString;
        switch (monthDay % 10) {
            case 1:
                repeatString = resources.getString(R.string.repeat_monthly_on_day1, monthDay);
                break;
            case 2:
                repeatString = resources.getString(R.string.repeat_monthly_on_day2, monthDay);
                break;
            case 3:
                repeatString = resources.getString(R.string.repeat_monthly_on_day3, monthDay);
                break;
            default:
                repeatString = resources.getString(R.string.repeat_monthly_on_day4, monthDay);
                break;
        }

        if(lowerCase)
            return Utils.getLowerString(repeatString);
        return repeatString;
    }

    /**
     * 모든 요일들에 대한 반복요일 문자렬들을 모두 얻는다.(한번만 얻는다)
     * @param r Resources
     * @param weekday 요일
     */
    private static void cacheMonthRepeatStrings(Resources r, int weekday) {
        if (mMonthRepeatByDayOfWeekIds == null) {
            mMonthRepeatByDayOfWeekIds = new int[7];
            mMonthRepeatByDayOfWeekIds[0] = R.array.repeat_by_nth_sun;
            mMonthRepeatByDayOfWeekIds[1] = R.array.repeat_by_nth_mon;
            mMonthRepeatByDayOfWeekIds[2] = R.array.repeat_by_nth_tues;
            mMonthRepeatByDayOfWeekIds[3] = R.array.repeat_by_nth_wed;
            mMonthRepeatByDayOfWeekIds[4] = R.array.repeat_by_nth_thurs;
            mMonthRepeatByDayOfWeekIds[5] = R.array.repeat_by_nth_fri;
            mMonthRepeatByDayOfWeekIds[6] = R.array.repeat_by_nth_sat;
        }
        if (mMonthRepeatByDayOfWeekStrs == null) {
            mMonthRepeatByDayOfWeekStrs = new String[7][];
        }

        mMonthRepeatByDayOfWeekStrs[weekday] =
                r.getStringArray(mMonthRepeatByDayOfWeekIds[weekday]);
    }

    /**
     * 요일문자렬을 돌려준다.
     * @param day 요일
     * @param dayOfWeekLength 길이(금요일|금, Sunday|Sun\Su\S) {@link DateUtils#LENGTH_LONG}
     */
    private static String dayToString(int day, int dayOfWeekLength) {
        return DateUtils.getDayOfWeekString(dayToUtilDay(day), dayOfWeekLength);
    }

    /**
     * EventRecurrence 의 요일을 Calendar 의 요일로 변환한다.
     * @param day EventRecurrence 의 요일
     */
    private static int dayToUtilDay(int day) {
        switch (day) {
        case EventRecurrence.SU: return Calendar.SUNDAY;
        case EventRecurrence.MO: return Calendar.MONDAY;
        case EventRecurrence.TU: return Calendar.TUESDAY;
        case EventRecurrence.WE: return Calendar.WEDNESDAY;
        case EventRecurrence.TH: return Calendar.THURSDAY;
        case EventRecurrence.FR: return Calendar.FRIDAY;
        case EventRecurrence.SA: return Calendar.SATURDAY;
        default: throw new IllegalArgumentException("bad day argument: " + day);
        }
    }
}
