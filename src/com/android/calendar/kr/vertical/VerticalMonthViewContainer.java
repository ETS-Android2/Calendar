package com.android.calendar.kr.vertical;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

/**
 * 양식 4
 * 한개 월 - 월표시부분 - 한개 페지의 content view
 * 한달 날자들과 주요일현시
 */
public class VerticalMonthViewContainer extends LinearLayout {
    private static final float MONTH_VIEW_END_PADDING = 10;

    VerticalCalendarView.VerticalCalendarViewDelegate mDelegate;

    //주요일 View
    VerticalWeekBar mWeekBar;
    //날자 View
    VerticalMonthView mMonthView;

    public VerticalMonthViewContainer(Context context) {
        this(context, null);
    }

    public VerticalMonthViewContainer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalMonthViewContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        mWeekBar = findViewById(R.id.week_view);
        mMonthView = findViewById(R.id.month_view);
    }

    public void setup(VerticalCalendarView.VerticalCalendarViewDelegate delegate, int year, int month) {
        mDelegate = delegate;

        mWeekBar.setup(delegate);
        mWeekBar.setWeekBarExpandListener(mDelegate.getWeekBarExpandListener());

        mMonthView.setup(delegate, year, month);
        mMonthView.setCalendarSelectListener(mDelegate.getCalendarSelectListener());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float weekBarWidth = mDelegate.getMonthItemWidth();
        mWeekBar.measure(MeasureSpec.makeMeasureSpec((int) weekBarWidth, MeasureSpec.EXACTLY), heightMeasureSpec);

        float monthViewWidth = mDelegate.getMonthItemWidth()*3 + Utils.convertDpToPixel(MONTH_VIEW_END_PADDING, getContext());
        mMonthView.measure(MeasureSpec.makeMeasureSpec((int) monthViewWidth, MeasureSpec.EXACTLY), heightMeasureSpec);

        float width = weekBarWidth + monthViewWidth;
        setMeasuredDimension((int) width, MeasureSpec.getSize(heightMeasureSpec));
    }

    public void moveWeekBar(float distance) {
        mWeekBar.setTranslationX(distance);
    }

    public VerticalMonthView getMonthView() {
        return mMonthView;
    }
}
