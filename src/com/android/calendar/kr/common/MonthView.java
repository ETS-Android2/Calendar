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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;

import java.util.ArrayList;

import com.android.krcalendar.R;

import static com.android.calendar.utils.Utils.CALENDAR_TYPE1;
import static com.android.calendar.utils.Utils.CALENDAR_TYPE2;

/**
 * @see CustomMonthView
 */
public abstract class MonthView extends BaseMonthView {

    protected int mCalendarType;
    public MonthView(Context context) {
        this(context, null);
    }
    public MonthView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 그리기함수
     * @param canvas 그리기객체
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mLineCount == 0)
            return;

        canvas.save();
        if(mCalendarType == CALENDAR_TYPE1){
            //요일 그려주기
            final int[] weekTexts = new int[]{R.string.sun, R.string.mon, R.string.tue, R.string.wed,
                    R.string.thu, R.string.fri, R.string.sat};

            mWeekBarTextPaint.setTextSize(mDelegate.getWeekTextSize());
            mWeekBarTextPaint.setTextAlign(Paint.Align.CENTER);
            for (int i = 0; i < 7; i ++) {
                String text = getResources().getString(weekTexts[i]);
                int x = i * mItemWidth + mItemWidth/2 + mDelegate.getCalendarPadding();

                if(i == 0)
                    mWeekBarTextPaint.setColor(getResources().getColor(R.color.week_bar_weekend_text_color, null));
                else
                    mWeekBarTextPaint.setColor(Utils.getThemeAttribute(getContext(), R.attr.current_month_text_color));

                canvas.drawText(text, x, mTextBaseLine, mWeekBarTextPaint);
            }

            canvas.translate(0, mDelegate.getWeekBarHeight());
        }

        //날자들을 순환하면서 하나씩 그려준다.
        int count = mLineCount * 7;
        int d = 0;
        for (int i = 0; i < mLineCount; i++) {
            for (int j = 0; j < 7; j++) {
                Calendar calendar = mItems.get(d);
                if (mCalendarType == CALENDAR_TYPE1) {
                    if (d > mItems.size() - mNextDiff) {
                        return;
                    }
                    if (!calendar.isCurrentMonth()) {
                        ++d;
                        continue;
                    }
                } else if (mCalendarType == CALENDAR_TYPE2) {
                    if (d >= count) {
                        return;
                    }
                    if(calendar.getYear() < mDelegate.getMinYear() ||
                            calendar.getYear() > mDelegate.getMaxYear()) {
                        ++d;
                        continue;
                    }
                }
                draw(canvas, calendar, i, j, d);
                ++d;
            }
        }

        canvas.restore();
    }


    /**
     * 한개날자 그리기
     * @param canvas   canvas
     * @param calendar 对应日历
     * @param i        수평위치
     * @param j        수직위치
     * @param d        날자
     */
    private void draw(Canvas canvas, Calendar calendar, int i, int j, int d) {
        //양식 1에서는 이전달 마지막주, 다음달 첫주 날자들을 보여주지 않는다.
        //양식 2에서는 보여준다.

        int x = j * mItemWidth + mDelegate.getCalendarPadding();
        int y = i * mItemHeight;

        boolean isToday = calendar.isCurrentDay();
        boolean isSelected = d == mCurrentItem;

        final ArrayList<EventManager.OneEvent> ev;
        final boolean hasScheme;    //일정을 가지고 있는가?
        if(mEventList == null) {
            hasScheme = false;
            ev = null;
        }
        else {
            ev = EventManager.getEventsFromDate(mEventList, calendar.getYear(), calendar.getMonth(), calendar.getDay());
            hasScheme = ev.size() != 0;
        }

        //오늘그려주기
        if(isToday && calendar.isCurrentMonth()){
            onDrawToday(canvas, calendar, x, y);
        }
        //선택된 날자그려주기
        else if (isSelected && calendar.isCurrentMonth()) {
            onDrawSelected(canvas, calendar, x, y);
        }

        if (hasScheme && calendar.isCurrentMonth()) {
            //과거의 일정을 하나라도 포함하고 있는가를 검사한다.
            boolean hasPastEvent = true;
            for (EventManager.OneEvent event:ev){
                if(!event.pastOrFutureCurrent()){
                    hasPastEvent = false;
                    break;
                }
            }

            //과거일정을 포함하고 있으면 어두운 회색
            if(hasPastEvent){
                calendar.setSchemeColor(getResources().getColor(R.color.colorPastEvent, null));
            }

            //그렇지 않으면 파란색
            else{
                calendar.setSchemeColor(getResources().getColor(R.color.colorFutureEvent, null));
            }
            onDrawScheme(canvas, calendar, x, y);
        }
        onDrawText(canvas, calendar, x, y, isSelected);
    }

    public void setCalendarType(int calendarType){
        mCalendarType = calendarType;
    }

    /**
     * 어느 날자를 click 했는지 감지한다.
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (!isClick) {
            return;
        }
        Calendar calendar = getIndex();

        if (calendar == null) {
            return;
        }

        if(mCalendarType == CALENDAR_TYPE1 && !calendar.isCurrentMonth())
            return;

        if(calendar.getYear() < mDelegate.getMinYear() ||
                calendar.getYear() > mDelegate.getMaxYear())
            return;

        mCurrentItem = mItems.indexOf(calendar);
        if(mCurrentItem / 7 >= mLineCount)
            return;

        if(mDelegate.mCalendarSelectListener != null) {
            mDelegate.mCalendarSelectListener.onCalendarSelect(calendar);
        }
    }

    protected abstract void onDrawToday(Canvas canvas, Calendar calendar, int x, int y);
    /**
     * 绘制选中的日期
     *
     * @param canvas    canvas
     * @param calendar  日历日历calendar
     * @param x         日历Card x起点坐标
     * @param y         日历Card y起点坐标
     * @return 是否绘制onDrawScheme，true or false
     */
    protected abstract boolean onDrawSelected(Canvas canvas, Calendar calendar, int x, int y);

    /**
     * 绘制标记的日期,这里可以是背景色，标记色什么的
     *
     * @param canvas   canvas
     * @param calendar 日历calendar
     * @param x        日历Card x起点坐标
     * @param y        日历Card y起点坐标
     */
    protected abstract void onDrawScheme(Canvas canvas, Calendar calendar, int x, int y);


    /**
     * 날자그리기
     *
     * @param canvas     canvas
     * @param calendar   날자객체
     * @param x          수평좌표
     * @param y          수직좌표
     * @param isSelected 선택되였는가
     */
    protected abstract void onDrawText(Canvas canvas, Calendar calendar, int x, int y, boolean isSelected);
}
