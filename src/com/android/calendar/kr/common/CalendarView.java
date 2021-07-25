package com.android.calendar.kr.common;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;

import java.util.ArrayList;
import java.util.List;

/**
 * 양식 1-4
 * Fragment의 기본 content view로서 리용되는 layout
 */
public abstract class CalendarView extends LinearLayout {
    //Delegate to manage month view style
    public CalendarViewDelegate mDelegate;

    //Calendar controller object
    public CalendarController mController;

    //Main activity
    public AllInOneActivity mMainActivity;

    //Month change lister list
    public List<OnMonthChangeListener> mMonthChangeListeners = new ArrayList<>();

    public int mLastItem = -1;

    public CalendarView(@NonNull Context context) {
        this(context, null);
    }

    public CalendarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mController = CalendarController.getInstance(getContext());
        mMainActivity = AllInOneActivity.getMainActivity(context);
        mMainActivity.addMonthChangeListeners(this);

        //Create calendar view delegate
        mDelegate = new CalendarViewDelegate(context, attrs);
    }

    public abstract void playTodayEventsAnimation();

    public void setSelectedOnMonth(int year, int month, int day, boolean smoothScroll){
        mDelegate.setSelectedDay(year, month, day);
        scrollToCalendar(year, month, day, smoothScroll);
    }

    public abstract void scrollToCalendar(int year, int month, int day, boolean smoothScroll);

    public void addMonthChangeListener(OnMonthChangeListener listener){
        mMonthChangeListeners.add(listener);
    }

    public void setOnCalendarSelectListener(OnCalendarSelectListener listener) {
        mDelegate.mCalendarSelectListener = listener;
    }

    public abstract void onDestroy();

    public abstract void removeTodayAnimation();

    public interface OnMonthChangeListener {
        void onMonthChange(int year, int month);
    }

    public interface OnCalendarSelectListener {
        void onCalendarSelect(Calendar calendar);
    }
}
