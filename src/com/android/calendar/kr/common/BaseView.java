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
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import java.util.List;

/**
 * @see CustomMonthView
 */

public abstract class BaseView extends ViewGroup implements View.OnClickListener {

    public CalendarViewDelegate mDelegate;

    /**
     * 当前月份日期的笔
     */
    protected Paint mCurMonthTextPaint = new Paint();

    /*
    * Week bar text paint
     */
    protected Paint mWeekBarTextPaint = new Paint();

    /**
     * 其它月份日期颜色
     */
    protected Paint mOtherMonthTextPaint = new Paint();

    /**
     * 被选择的日期背景色
     */
    protected Paint mSelectedPaint = new Paint();

    /**
     * 今天的背景色
     */
    protected Paint mCurrentDayPaint = new Paint();

    /**
     * 选中的文本画笔
     */
    protected Paint mSelectTextPaint = new Paint();

    /**
     * 当前日期文本颜色画笔
     */
    protected Paint mCurDayTextPaint = new Paint();

    /**
     * 日历项
     */
    List<Calendar> mItems;

    /**
     * 每一项的高度
     */
    protected int mItemHeight;

    /**
     * 每一项的宽度
     */
    protected int mItemWidth;

    /**
     * Text的基线
     */
    protected float mTextBaseLine;

    /**
     * 点击的x、y坐标
     */
    float mX, mY;

    /**
     * 是否点击
     */
    boolean isClick = true;

    /**
     * 字体大小
     */
    static final int TEXT_SIZE = 14;

    /**
     * 当前点击项
     */
    public int mCurrentItem = -1;

    final float CLICK_OFFSET = getResources().getDimension(R.dimen.day_click_offset);

    public BaseView(Context context) {
        this(context, null);
    }

    public BaseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaint(context);
    }

    /**
     * 初始化配置
     *
     * @param context context
     */
    private void initPaint(Context context) {
        mCurMonthTextPaint.setAntiAlias(true);
        mCurMonthTextPaint.setTextAlign(Paint.Align.CENTER);
        mCurMonthTextPaint.setColor(0xFF111111);
        mCurMonthTextPaint.setFakeBoldText(true);
        mCurMonthTextPaint.setTextSize(CalendarUtil.dipToPx(context, TEXT_SIZE));

        mOtherMonthTextPaint.setAntiAlias(true);
        mOtherMonthTextPaint.setTextAlign(Paint.Align.CENTER);
        mOtherMonthTextPaint.setColor(0xFFe1e1e1);
        mOtherMonthTextPaint.setFakeBoldText(true);
        mOtherMonthTextPaint.setTextSize(CalendarUtil.dipToPx(context, TEXT_SIZE));

        mSelectTextPaint.setAntiAlias(true);
        mSelectTextPaint.setStyle(Paint.Style.FILL);
        mSelectTextPaint.setTextAlign(Paint.Align.CENTER);
        mSelectTextPaint.setColor(0xffed5353);
        mSelectTextPaint.setFakeBoldText(true);
        mSelectTextPaint.setTextSize(CalendarUtil.dipToPx(context, TEXT_SIZE));

        mCurDayTextPaint.setAntiAlias(true);
        mCurDayTextPaint.setTextAlign(Paint.Align.CENTER);
        mCurDayTextPaint.setColor(Color.RED);
        mCurDayTextPaint.setFakeBoldText(true);
        mCurDayTextPaint.setTextSize(CalendarUtil.dipToPx(context, TEXT_SIZE));

        setOnClickListener(this);
    }

    /**
     * 初始化所有UI配置
     *
     * @param delegate delegate
     */
   public final void setup(CalendarViewDelegate delegate) {
        this.mDelegate = delegate;
        updateStyle();
        updateItemHeight();
    }


    public final void updateStyle(){
        if(mDelegate == null){
            return;
        }
        this.mCurDayTextPaint.setColor(mDelegate.getCurDayTextColor());
        this.mCurMonthTextPaint.setColor(mDelegate.getCurrentMonthTextColor());
        this.mOtherMonthTextPaint.setColor(mDelegate.getOtherMonthTextColor());
        this.mSelectTextPaint.setColor(mDelegate.getSelectedTextColor());
        this.mCurMonthTextPaint.setTextSize(mDelegate.getDayTextSize());
        this.mOtherMonthTextPaint.setTextSize(mDelegate.getDayTextSize());
        this.mCurDayTextPaint.setTextSize(mDelegate.getDayTextSize());
        this.mSelectTextPaint.setTextSize(mDelegate.getDayTextSize());

        mSelectedPaint.setAntiAlias(true);
        mSelectedPaint.setStyle(Paint.Style.STROKE);
        mSelectedPaint.setStrokeWidth(Utils.convertDpToPixel(2f, getContext()));
        mSelectedPaint.setColor(Utils.getThemeAttribute(getContext(), R.attr.selected_day_outline_color));

        mCurrentDayPaint.setAntiAlias(true);
        mCurrentDayPaint.setStyle(Paint.Style.FILL);
        mCurrentDayPaint.setColor(mDelegate.getTodayFillColor());
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    void updateItemHeight() {
        this.mItemHeight = mDelegate.getCalendarItemHeight();
        Paint.FontMetrics metrics = mCurMonthTextPaint.getFontMetrics();
        mTextBaseLine = mDelegate.getCalendarItemHeight() / 2 - metrics.descent + (metrics.bottom - metrics.top) / 2;
    }

    /**
     * {@link #isClick}를 설정하기 위해 touch 사건을 감지한다.
     * @param event
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mX = event.getX();
                mY = event.getY();
                isClick = true;
                break;
            case MotionEvent.ACTION_MOVE:
                float mDY;
                float mDX;
                if (isClick) {
                    mDX = event.getX() - mX;
                    mDY = event.getY() - mY;

                    //끌기한 거리가 한계를 넘으면 click사건을 받지 않도록 한다.
                    isClick = Math.abs(mDY) <= CLICK_OFFSET && Math.abs(mDX) <= CLICK_OFFSET;
                }
                break;
            case MotionEvent.ACTION_UP:
                mX = event.getX();
                mY = event.getY();
                break;
        }
        return super.onTouchEvent(event);
    }
}
