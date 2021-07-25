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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import java.util.Date;

/**
 * 양식 1,2
 * 달력 현시를 위한 설정값들을 가지고 있는 관리클라스
 */
public class CalendarViewDelegate {

    private static final int MIN_YEAR = 1600;   //최소년도(기정값)
    private static final int MAX_YEAR = 2500;   //최대년도(기정값)
    private static final int DEFAULT_MONTH_BACKGROUND_PERCENT = 46; //월화상높이:화면높이 percent

    //최대, 최소날자
    private int mMinYear, mMaxYear;
    private int mMinYearMonth, mMaxYearMonth;
    private final int mMinYearDay;
    private int mMaxYearDay;

    private final int mCurDayTextColor;         //오늘 날자의 본문색갈
    private final int mOtherMonthTextColor;     //다른달 날자의 본문색갈
    private final int mCurrentMonthTextColor;   //현재달 날자의 본문색갈
    private final int mSelectedTextColor;       //선택된 날자의 본문색갈
    private final int mCalendarPadding;         //좌우 padding
    private final int mWeekTextSize;            //요일의 본문색갈
    private final int mTodayFillColor;          //오늘 날자의 원채우기색갈

    private final int mDayTextSize;             //날자본문의 서체크기
    private int mCalendarItemHeight;            //한개 날자의 높이
    private final int mWeekBarHeight;           //Week bar의 높이
    private Calendar mCurrentDate;              //오늘날자
    private final int mMonthBackgroundHeightPercent;    //월화상높이:화면높이 percent(양식1)
    private final float mMonthProportion;       //날자부분이 화면에서 차지하는 비률 percent(양식2)

    public Calendar mSelectedCalendar;  //선택된 날자

    //날자선택변화를 감지하는 listener
    public CalendarView.OnCalendarSelectListener mCalendarSelectListener;

    public CalendarViewDelegate(Context context, @Nullable AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CalendarView);

        mCalendarPadding = (int) array.getDimension(R.styleable.CalendarView_calendar_padding, 0);

        mWeekTextSize = array.getDimensionPixelSize(R.styleable.CalendarView_week_text_size,
                CalendarUtil.dipToPx(context, 20));
        mWeekBarHeight = (int) array.getDimension(R.styleable.CalendarView_week_bar_height,
                CalendarUtil.dipToPx(context, 40));
        mCurDayTextColor = Utils.getThemeAttribute(context, R.attr.current_day_text_color);
        mTodayFillColor = array.getColor(R.styleable.CalendarView_today_fill_color, 0xFF046cea);
        mSelectedTextColor = array.getColor(R.styleable.CalendarView_selected_text_color, 0xFFFFFFFF);
        mCurrentMonthTextColor = Utils.getThemeAttribute(context, R.attr.current_month_text_color);
        mOtherMonthTextColor = Utils.getThemeAttribute(context, R.attr.other_month_text_color);
        mMinYear = array.getInt(R.styleable.CalendarView_min_year, MIN_YEAR);
        mMaxYear = array.getInt(R.styleable.CalendarView_max_year, MAX_YEAR);
        mMinYearMonth = array.getInt(R.styleable.CalendarView_min_year_month, 1);
        mMaxYearMonth = array.getInt(R.styleable.CalendarView_max_year_month, 12);
        mMinYearDay = array.getInt(R.styleable.CalendarView_min_year_day, 1);
        mMaxYearDay = array.getInt(R.styleable.CalendarView_max_year_day, -1);
        mDayTextSize = array.getDimensionPixelSize(R.styleable.CalendarView_day_text_size,
                CalendarUtil.dipToPx(context, 16));
        mCalendarItemHeight = (int) array.getDimension(R.styleable.CalendarView_calendar_height,
                CalendarUtil.dipToPx(context, 56));
        mMonthBackgroundHeightPercent = array.getInt(R.styleable.CalendarView_month_background_height_percent, DEFAULT_MONTH_BACKGROUND_PERCENT);
        mMonthProportion = array.getFloat(R.styleable.CalendarView_month_proportion, 0.6f);

        if (mMinYear <= MIN_YEAR) mMinYear = MIN_YEAR;
        if (mMaxYear >= MAX_YEAR) mMaxYear = MAX_YEAR;
        array.recycle();
        init();
    }

    private void init() {
        mCurrentDate = new Calendar();
        Date d = new Date();
        mCurrentDate.setYear(CalendarUtil.getDate("yyyy", d));
        mCurrentDate.setMonth(CalendarUtil.getDate("MM", d));
        mCurrentDate.setDay(CalendarUtil.getDate("dd", d));
        mCurrentDate.setCurrentDay(true);
        setRange(mMinYear, mMinYearMonth, mMaxYear, mMaxYearMonth);
    }


    private void setRange(int minYear, int minYearMonth,
                          int maxYear, int maxYearMonth) {
        this.mMinYear = minYear;
        this.mMinYearMonth = minYearMonth;
        this.mMaxYear = maxYear;
        this.mMaxYearMonth = maxYearMonth;
        if (this.mMaxYear < mCurrentDate.getYear()) {
            this.mMaxYear = mCurrentDate.getYear();
        }
        if (this.mMaxYearDay == -1) {
            this.mMaxYearDay = CalendarUtil.getMonthDaysCount(this.mMaxYear, mMaxYearMonth);
        }
        int y = mCurrentDate.getYear() - this.mMinYear;
    }

    /**
     * 오늘 날자의 본문색갈
     */
    public int getCurDayTextColor() {
        return mCurDayTextColor;
    }

    /**
     * 다른달 날자의 본문색갈
     */
    public int getOtherMonthTextColor() {
        return mOtherMonthTextColor;
    }

    /**
     * 현재달 날자의 본문색갈
     */
    public int getCurrentMonthTextColor() {
        return mCurrentMonthTextColor;
    }

    /**
     * 선택된 날자의 본문색갈
     */
    public int getSelectedTextColor() {
        return mSelectedTextColor;
    }

    /**
     * 오늘 날자의 원채우기색갈
     */
    public int getTodayFillColor() {
        return mTodayFillColor;
    }

    /**
     * Week bar 의 높이
     */
    public int getWeekBarHeight() {
        return mWeekBarHeight;
    }

    /* 최소, 최대 날자의 년, 월, 일 */
    public int getMinYear() {
        return mMinYear;
    }
    public int getMaxYear() {
        return mMaxYear;
    }
    public int getMinYearMonth() {
        return mMinYearMonth;
    }
    public int getMaxYearMonth() {
        return mMaxYearMonth;
    }
    public int getMinYearDay() {
        return mMinYearDay;
    }
    public int getMaxYearDay() {
        return mMaxYearDay;
    }

    /**
     * 날자본문의 서체크기
     */
    public int getDayTextSize() {
        return mDayTextSize;
    }

    /**
     * 한개 날자의 높이
     */
    public int getCalendarItemHeight() {
        return mCalendarItemHeight;
    }

    /**
     * 한개 날자의 높이 설정
     * @param height 높이
     */
    public void setCalendarItemHeight(int height) {
        mCalendarItemHeight = height;
    }

    /**
     * 요일의 본문색갈
     */
    public int getWeekTextSize() {
        return mWeekTextSize;
    }

    /**
     * 오늘날자
     */
    public Calendar getCurrentDay() {
        return mCurrentDate;
    }

    /**
     * 선택된 날자 설정
     * @param year 년
     * @param month 월
     * @param day 일
     */
    public void setSelectedDay(int year, int month, int day){
        if(mSelectedCalendar != null) {
            mSelectedCalendar.setYear(year);
            mSelectedCalendar.setMonth(month);
            mSelectedCalendar.setDay(day);
        }
        else {
            Calendar calendar = new Calendar();
            calendar.setYear(year);
            calendar.setMonth(month);
            calendar.setDay(day);
            mSelectedCalendar = calendar;
        }
    }

    /**
     * 좌우 padding
     */
    public int getCalendarPadding() {
        return mCalendarPadding;
    }

    /**
     * 월화상높이:화면높이 percent(양식1)
     */
    public int getMonthBackgroundHeightPercent(){
        return mMonthBackgroundHeightPercent;
    }

    /**
     * 날자부분이 화면에서 차지하는 비률 percent(양식2)
     */
    public float getMonthProportion() {
        return mMonthProportion;
    }

    /**
     * 최소한계날자
     */
    public final Calendar getMinRangeCalendar() {
        Calendar calendar = new Calendar();
        calendar.setYear(mMinYear);
        calendar.setMonth(mMinYearMonth);
        calendar.setDay(mMinYearDay);
        calendar.setCurrentDay(calendar.equals(mCurrentDate));
        return calendar;
    }

    /**
     * 최대한계날자
     */
    public final Calendar getMaxRangeCalendar() {
        Calendar calendar = new Calendar();
        calendar.setYear(mMaxYear);
        calendar.setMonth(mMaxYearMonth);
        calendar.setDay(mMaxYearDay);
        calendar.setCurrentDay(calendar.equals(mCurrentDate));
        return calendar;
    }
}
