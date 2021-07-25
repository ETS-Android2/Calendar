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
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

/**
 * 양식 1,2
 * 주현시를 해주는 layout (일-토)
 * @see R.layout#cv_week_bar
 */
public class WeekBar extends LinearLayout {
    private CalendarViewDelegate mDelegate;
    private LinearLayout mWeekBarLayout;

    public WeekBar(Context context) {
        this(context, null);
    }

    public WeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.cv_week_bar, this, true);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mWeekBarLayout = findViewById(R.id.ll_week);
        setTextColor(Utils.getThemeAttribute(getContext(), R.attr.current_month_text_color));
    }

    @Override
    public void onDraw(Canvas canvas){
        canvas.drawColor(Utils.getCommonBackgroundColor(getContext()));
        super.onDraw(canvas);
    }

    /**
     * 크기, padding 설정
     * @param delegate delegate
     */
    public void setup(CalendarViewDelegate delegate) {
        this.mDelegate = delegate;
        setTextSize(mDelegate.getWeekTextSize());
        setPadding(delegate.getCalendarPadding(), 0, delegate.getCalendarPadding(), 0);
    }

    /**
     * 글자색설정
     * @param color 글자색
     */
    protected void setTextColor(int color) {
        for (int i = 1; i < mWeekBarLayout.getChildCount(); i++) {
            ((TextView) mWeekBarLayout.getChildAt(i)).setTextColor(color);
        }
    }

    /**
     * 글자크기설정
     * @param size 글자크기(pixel)
     */
    protected void setTextSize(int size) {
        for (int i = 0; i < mWeekBarLayout.getChildCount(); i++) {
            ((TextView) mWeekBarLayout.getChildAt(i)).setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //높이를 계산해서 설정해준다.
        if (mDelegate != null) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mDelegate.getWeekBarHeight(), MeasureSpec.EXACTLY);
        } else {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    (int)(getResources().getDimension(R.dimen.week_bar_height)), MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
