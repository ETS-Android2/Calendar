package com.android.calendar.kr.general;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.calendar.kr.common.CustomMonthView;
import com.android.calendar.kr.common.WeekBar;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

/**
 * 양식 2
 * 한개 월 - 월표시부분 - 한개 페지의 content view
 * 한달 날자들을 현시(1일, 마지막날에 해당한 이전, 다음 달의 날자들도 현시)
 */
public class GeneralMonthViewContainer extends LinearLayout {
    //날자 View
    private CustomMonthView mMonthView;

    CalendarViewDelegate mDelegate;

    public GeneralMonthViewContainer(Context context) {
        this(context, null);
    }

    public GeneralMonthViewContainer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GeneralMonthViewContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        mMonthView = findViewById(R.id.month_view);
    }

    public void setup(CalendarViewDelegate delegate, int year, int month, int position){
        mMonthView.setup(delegate);
        mMonthView.initMonthWithDate(year, month);
        mMonthView.setCalendarType(Utils.CALENDAR_TYPE2);
        mMonthView.setSelectedCalendar(delegate.mSelectedCalendar);
        mDelegate = delegate;

        WeekBar weekBar = findViewById(R.id.week_view);
        weekBar.setup(mDelegate);

        setTag(position);
    }

    public CustomMonthView getMonthView(){
        return mMonthView;
    }
}
