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
import android.util.AttributeSet;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;

import org.joda.time.DateTime;

import java.util.List;

/**
 * @see CustomMonthView
 */
public abstract class BaseMonthView extends BaseView {

    //년
    protected int mYear;
    //월
    protected int mMonth;
    //일
    protected int mLineCount;

    /**
     * 下个月偏移的数量
     */
    protected int mNextDiff;

    List<EventManager.OneEvent> mEventList = null;

    //Main activity
    AllInOneActivity mMainActivity;

    public BaseMonthView(Context context) {
        this(context, null);
    }

    public BaseMonthView(Context context, AttributeSet attrs){
        super(context, attrs);
        mMainActivity = AllInOneActivity.getMainActivity(context);
    }

    /**
     * 년, 월 설정
     * @param year 년
     * @param month 월
     */
    public final void initMonthWithDate(int year, int month) {
        mYear = year;
        mMonth = month;
        initCalendar();

        redrawMonthView();

        if(Utils.isDayToMonthTransition()) {
            DateTime selectedTime = new DateTime(mMainActivity.getCalendarController().getTime());
            if(selectedTime.getYear() == mYear && selectedTime.getMonthOfYear() == mMonth) {
                setAlpha(0);
            }
        }
    }

    /**
     * thread를 통해 일정을 얻은 다음 재그리기를 진행
     */
    public void redrawMonthView() {
        DateTime dateTime = new DateTime(mYear, mMonth, 1, 0, 0);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mEventList = EventManager.getEvents(getContext(), dateTime.getMillis(), EventManager.MONTH);
                invalidate();
            }
        });
        thread.start();
    }

    /**
     * 날자변수들 초기화
     */
    @SuppressLint("WrongConstant")
    private void initCalendar() {

        mNextDiff = CalendarUtil.getMonthEndDiff(mYear, mMonth);
        int preDiff = CalendarUtil.getMonthViewStartDiff(mYear, mMonth);
        int monthDayCount = CalendarUtil.getMonthDaysCount(mYear, mMonth);

        mItems = CalendarUtil.initCalendarForMonthView(mYear, mMonth, mDelegate.getCurrentDay());

        if (mItems.contains(mDelegate.getCurrentDay())) {
            mCurrentItem = mItems.indexOf(mDelegate.getCurrentDay());
        } else {
            mCurrentItem = mItems.indexOf(mDelegate.mSelectedCalendar);
        }

        mLineCount = (preDiff + monthDayCount + mNextDiff) / 7;
        invalidate();
    }

    public void getCurrentItem() {
        mCurrentItem = mItems.indexOf(mDelegate.mSelectedCalendar);
    }

    /**
     * 获取点击选中的日期
     *
     * @return return
     */
    protected Calendar getIndex() {
        if (mItemWidth == 0 || mItemHeight == 0) {
            return null;
        }
        int indexX = (int) (mX - mDelegate.getCalendarPadding()) / mItemWidth;
        if (indexX >= 7) {
            indexX = 6;
        }
        final int indexY;
        if(Utils.getCalendarTypePreference(getContext()) == Utils.CALENDAR_TYPE1)
            indexY = (int) (mY - mDelegate.getWeekBarHeight()) / mItemHeight;
        else
            indexY = (int) mY / mItemHeight;

        int position = indexY * 7 + indexX;// 选择项
        if (position >= 0 && position < mItems.size())
            return mItems.get(position);
        return null;
    }

    /**
     * 记录已经选择的日期
     *
     * @param calendar calendar
     */
    public final void setSelectedCalendar(Calendar calendar) {
        mCurrentItem = mItems.indexOf(calendar);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mLineCount != 0) {
            if(Utils.getCalendarTypePreference(getContext()) == Utils.CALENDAR_TYPE1){
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        mDelegate.getCalendarItemHeight() * 6 + mDelegate.getWeekBarHeight(),
                        MeasureSpec.EXACTLY);
            }
            else {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(mDelegate.getCalendarItemHeight() * 6, MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
