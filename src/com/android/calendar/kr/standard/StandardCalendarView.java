package com.android.calendar.kr.standard;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.android.calendar.helper.CalendarController;
import com.android.calendar.kr.common.Calendar;
import com.android.calendar.kr.common.CalendarUtil;
import com.android.calendar.kr.common.CalendarView;
import com.android.calendar.utils.Utils;
import com.android.kr_common.Time;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE;

/**
 * 양식 1
 * 기본 view
 */
public class StandardCalendarView extends CalendarView {
    //월별현시를 위한 ViewPager 와 Adapter
    public ViewPager2 mMonthViewpager;
    StandardMonthPagerAdapter mMonthPagerAdapter;

    ViewPager2.OnPageChangeCallback mPageChangeListener = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            Calendar calendar = CalendarUtil.getFirstCalendarFromMonthViewPager(position, mDelegate);
            Calendar selectedCalendar = mDelegate.mSelectedCalendar;
            if(selectedCalendar.getYear() == calendar.getYear() && selectedCalendar.getMonth() == calendar.getMonth()) {
                calendar.setYear(selectedCalendar.getYear());
                calendar.setMonth(selectedCalendar.getMonth());
                calendar.setDay(selectedCalendar.getDay());
            }

            for (OnMonthChangeListener listener : mMonthChangeListeners){
                listener.onMonthChange(calendar.getYear(), calendar.getMonth());
            }

            Time t = new Time(calendar.getTimeInMillis());
            mController.sendEvent(CalendarController.EventType.GO_TO, t, null,
                    t, -1, CalendarController.ViewType.CURRENT, 0);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }
        @Override
        public void onPageScrollStateChanged(int state) {
            if(state == SCROLL_STATE_IDLE) {
                playTodayEventsAnimation();
            }
        }
    };

    public StandardCalendarView(@NonNull Context context) {
        this(context, null);
    }

    public StandardCalendarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StandardCalendarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //한개 날자view의 높이를 계산한다.(전체 높이÷6)
        Resources resources = getResources();
        float height = Utils.getDisplayDimensions(context).y -
                resources.getDimension(R.dimen.bottom_bar_height);
        float monthViewHeight = height - height * mDelegate.getMonthBackgroundHeightPercent() / 100 - getResources().getDimension(R.dimen.week_bar_height);
        int calendarItemHeight = (int) (monthViewHeight / 6);
        mDelegate.setCalendarItemHeight(calendarItemHeight);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mMonthViewpager = findViewById(R.id.month_view_pager);

        mMonthPagerAdapter = new StandardMonthPagerAdapter(getContext(), mDelegate, mMonthViewpager);
        mMonthViewpager.setAdapter(mMonthPagerAdapter);
        mMonthViewpager.registerOnPageChangeCallback(mPageChangeListener);

        //페지전환 animation을 주기 위해 PageTransformer 를 정의해준다.
        mMonthViewpager.setPageTransformer((page, position) -> {
            if (!(page instanceof StandardMonthViewContainer))
                return;

            StandardMonthViewContainer monthViewContainer = (StandardMonthViewContainer) page;
            monthViewContainer.setTransformAnimatorProgress(position);
        });
    }
    public void playTodayEventsAnimation() {
        DateTime dateTime = DateTime.now();

        int currentItem = mMonthViewpager.getCurrentItem();
        if(mLastItem == currentItem)
            return;

        mLastItem = currentItem;

        Calendar calendar = CalendarUtil.getFirstCalendarFromMonthViewPager(currentItem, mDelegate);
        if(dateTime.getYear() == calendar.getYear() && dateTime.getMonthOfYear() == calendar.getMonth()) {
            StandardMonthViewContainer monthViewContainer = mMonthViewpager.findViewWithTag(currentItem);
            if(monthViewContainer != null)
                monthViewContainer.getMonthView().startTodayAnimation();
        }
    }

    @Override
    public void removeTodayAnimation() {
        StandardMonthViewContainer monthViewContainer = mMonthViewpager.findViewWithTag(mLastItem);
        if(monthViewContainer != null)
            monthViewContainer.getMonthView().removeTodayAnimation();
    }

    public void setFadeAnimatorProgress(float progress) {
        int currentItem = mMonthViewpager.getCurrentItem();
        if(mMonthViewpager.findViewWithTag(currentItem) != null){
            StandardMonthViewContainer monthViewContainer = mMonthViewpager.findViewWithTag(currentItem);
            monthViewContainer.getMonthView().setAlpha(progress);
            monthViewContainer.setFadeAnimatorProgress(progress);
        }
    }

    public void scrollToCalendar(int year, int month, int day, boolean smoothScroll) {
        Calendar calendar = new Calendar();
        calendar.setYear(year);
        calendar.setMonth(month);
        calendar.setDay(day);
        calendar.setCurrentDay(calendar.equals(mDelegate.getCurrentDay()));
        mDelegate.mSelectedCalendar = calendar;
        int y = calendar.getYear() - mDelegate.getMinYear();
        int position = 12 * y + calendar.getMonth() - mDelegate.getMinYearMonth();

        mMonthViewpager.setCurrentItem(position, smoothScroll);
    }

    //월 ViewPager 재그리기
    public void notifyViewPager(){
        int currentItem = mMonthViewpager.getCurrentItem();
        for (int i = currentItem - 1; i < currentItem + 1; i ++) {
            StandardMonthViewContainer view = mMonthViewpager.findViewWithTag(i);
            if(view != null) {
                view.getMonthView().redrawMonthView();
            }
        }
    }

    public void onDestroy(){
    }
}
