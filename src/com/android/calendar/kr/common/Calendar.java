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

import java.io.Serializable;

/**
 * 한개 날자를 표현하는 model클라스
 */
@SuppressWarnings("all")
public final class Calendar implements Serializable, Comparable<Calendar> {
    private static final long serialVersionUID = 141315161718191143L;

    //년
    private int year;

    //월(1-12)
    private int month;

    //일(1-31)
    private int day;

    //이번달인가?
    private boolean isCurrentMonth;

    //오늘인가?
    private boolean isCurrentDay;

    //일정색갈
    private int schemeColor;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public boolean isCurrentMonth() {
        return isCurrentMonth;
    }

    public void setCurrentMonth(boolean currentMonth) {
        this.isCurrentMonth = currentMonth;
    }

    public boolean isCurrentDay() {
        return isCurrentDay;
    }

    public void setCurrentDay(boolean currentDay) {
        isCurrentDay = currentDay;
    }

    public int getSchemeColor() {
        return schemeColor;
    }

    public void setSchemeColor(int schemeColor) {
        this.schemeColor = schemeColor;
    }

    /**
     * 같은달인가를 돌려준다
     * @param calendar 달력
     */
    public boolean isSameMonth(Calendar calendar) {
        return year == calendar.getYear() && month == calendar.getMonth();
    }

    /**
     * @return 미리초시간을 돌려준다
     */
    public long getTimeInMillis() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.YEAR, year);
        calendar.set(java.util.Calendar.MONTH, month - 1);
        calendar.set(java.util.Calendar.DAY_OF_MONTH, day);
        return calendar.getTimeInMillis();
    }

    /**
     * 다른 달력과 비교
     * @param calendar
     * @return 1,0,-1
     */
    public int compareTo(Calendar calendar) {
        if (calendar == null) {
            return 1;
        }
        return toString().compareTo(calendar.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Calendar) {
            if (((Calendar) o).getYear() == year && ((Calendar) o).getMonth() == month && ((Calendar) o).getDay() == day)
                return true;
        }
        return super.equals(o);
    }

    @Override
    public String toString() {
        return year + "" + (month < 10 ? "0" + month : month) + "" + (day < 10 ? "0" + day : day);
    }
}
