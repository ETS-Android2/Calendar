package com.android.calendar.kr.general;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.android.calendar.helper.CalendarController;
import com.android.calendar.kr.common.Calendar;
import com.android.calendar.kr.common.CalendarUtil;
import com.android.calendar.kr.common.CalendarView;
import com.android.calendar.kr.common.CustomMonthView;
import com.android.calendar.utils.Utils;
import com.android.calendar.kr.day.DayViewFragment;
import com.android.kr_common.Time;
import com.android.krcalendar.R;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE;

/**
 * 양식 2
 * 기본 view
 */
public class GeneralCalendarView extends CalendarView implements CalendarView.OnCalendarSelectListener {
    public static final int FADE_DURATION = 300;

    //끌기가 아니라 코드적으로 setCurrentItem()을 호출하여 페지가 이동하였을때 true로 설정된다.
    private boolean mManualDayChange = false;
    private boolean mManualMonthChange = false;

    //자식 View들
    ViewPager2 mMonthViewpager;
    ViewPager2 mDayViewPager;
    FrameLayout mMonthViewPagerContainer;
    FrameLayout mDayViewPagerContainer;
    GeneralMonthPagerAdapter mMonthPagerAdapter;
    GeneralDayPagerAdapter mDayPagerAdapter;

    //월, 일정 Viewpager 의 페지변화를 감지하는 listener 들
    ViewPager2.OnPageChangeCallback mMonthPageChangeCallback;
    ViewPager2.OnPageChangeCallback mDayPageChangeCallback;

    //날자가 변하는것을 감지하는 listener 들
    List<DayViewFragment.OnDayChangeListener> mDayChangeListeners = new ArrayList<>();

    public GeneralCalendarView(@NonNull Context context) {
        this(context, null);
    }

    public GeneralCalendarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GeneralCalendarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //한개 날자의 높이를 계산한다. (전체 높이 ÷ 6)
        Resources resources = getResources();
        float height = Utils.getDisplayDimensions(context).y -
                resources.getDimension(R.dimen.action_bar_height) -
                resources.getDimension(R.dimen.bottom_bar_height);
        float monthViewHeight = height * mDelegate.getMonthProportion() - getResources().getDimension(R.dimen.week_bar_height);
        int calendarItemHeight = (int) (monthViewHeight / 6);
        mDelegate.setCalendarItemHeight(calendarItemHeight);

        //월 Viewpager의 페지변화를 감지하는 callback
        mMonthPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Calendar calendar;
                if(mManualMonthChange) {
                    calendar = mDelegate.mSelectedCalendar;
                }
                else {
                    calendar = CalendarUtil.getFirstCalendarFromMonthViewPager(position, mDelegate);
                }

                int year = calendar.getYear();
                int month = calendar.getMonth();
                int day = calendar.getDay();

                final boolean tempMonthChange = mManualMonthChange;

                if(mManualMonthChange)
                    mManualMonthChange = false;
                else
                    mDelegate.setSelectedDay(year, month, day);

                for (OnMonthChangeListener listener : mMonthChangeListeners){
                    listener.onMonthChange(year, month);
                }

                Time t = new Time(calendar.getTimeInMillis());
                mController.sendEvent(CalendarController.EventType.GO_TO, t, null,
                        t, -1, CalendarController.ViewType.CURRENT, 0);

                //이전, 현재, 다음 페지들 재그리기
                post(() -> {
                    int page = mMonthViewpager.getCurrentItem();
                    int[] pages = new int[]{page - 1, page, page + 1};

                    for (int page_number : pages) {
                        View view = mMonthViewpager.findViewWithTag(page_number);
                        if (view instanceof GeneralMonthViewContainer) {
                            CustomMonthView monthView = ((GeneralMonthViewContainer) view).getMonthView();
                            monthView.getCurrentItem();

                            mMainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    monthView.invalidate();
                                }
                            });
                        }
                    }

                    if(!tempMonthChange){
                        mManualDayChange = true;
                        setSelectedOnDay(year, month, day, false);
                        mDayViewPager.setAlpha(0.0f);
                        mDayViewPager.animate().alpha(1.0f).setDuration(FADE_DURATION).start();
                    }
                });
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

        //일 Viewpager의 페지변화를 감지하는 callback
        mDayPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if(mManualDayChange) {
                    mManualDayChange = false;
                    return;
                }

                final DateTime minDate = new DateTime(mDelegate.getMinYear(), 1, 1, 0, 0);
                final DateTime curDate = minDate.plusDays(position);
                final Calendar calendar = mDelegate.mSelectedCalendar;

                final int year = curDate.getYear();
                final int month = curDate.getMonthOfYear();
                final int day = curDate.getDayOfMonth();

                for (DayViewFragment.OnDayChangeListener listener : mDayChangeListeners){
                    listener.onDayChange(curDate.getYear(), curDate.getMonthOfYear(), curDate.getDayOfMonth());
                }

                if(curDate.getYear() != calendar.getYear() || curDate.getMonthOfYear() != calendar.getMonth()) {
                    mManualMonthChange = true;
                    setSelectedOnMonth(year, month, day, false);

                    mMonthViewpager.setAlpha(0.5f);
                    mMonthViewpager.animate().alpha(1f).setDuration(FADE_DURATION).start();

                    //오늘의 일정화상교체 animation 을 시작한다.
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            playTodayEventsAnimation();
                        }
                    }, 500);
                }
                else {
                    mDelegate.setSelectedDay(year, month, day);

                    int page = mMonthViewpager.getCurrentItem();
                    View view = mMonthViewpager.findViewWithTag(page);
                    if(view instanceof GeneralMonthViewContainer) {
                        CustomMonthView monthView = ((GeneralMonthViewContainer)view).getMonthView();
                        monthView.getCurrentItem();
                        monthView.invalidate();
                    }
                }
            }
        };

        mMainActivity.addDayChangeListeners(this);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mMonthViewPagerContainer = findViewById(R.id.month_view_pager_container);
        mMonthViewpager = findViewById(R.id.month_view_pager);
        mDayViewPagerContainer = findViewById(R.id.day_view_pager_container);
        mDayViewPager = findViewById(R.id.day_view_pager);

        mMonthPagerAdapter = new GeneralMonthPagerAdapter(getContext(), mDelegate, mMonthViewpager);
        mMonthViewpager.setAdapter(mMonthPagerAdapter);
        mMonthViewpager.registerOnPageChangeCallback(mMonthPageChangeCallback);

        mDayPagerAdapter = new GeneralDayPagerAdapter(getContext(), mDelegate);
        mDayViewPager.setAdapter(mDayPagerAdapter);
        mDayViewPager.registerOnPageChangeCallback(mDayPageChangeCallback);
    }

    public void notifyViewPager(){
        //월, 일 Viewpager를 재그리기한다.
        mMonthPagerAdapter.notifyDataSetChanged();
        mDayPagerAdapter.notifyDataSetChanged();
    }

    public void addDayChangeListener(DayViewFragment.OnDayChangeListener listener){
        mDayChangeListeners.add(listener);
    }

    /**
     * 오늘날자에서 일정화상 바뀌는 animation
     */
    public void playTodayEventsAnimation() {
        DateTime dateTime = DateTime.now();

        int currentItem = mMonthViewpager.getCurrentItem();
        if(mLastItem == currentItem)
            return;

        mLastItem = currentItem;

        Calendar calendar = CalendarUtil.getFirstCalendarFromMonthViewPager(currentItem, mDelegate);
        if(dateTime.getYear() == calendar.getYear() && dateTime.getMonthOfYear() == calendar.getMonth()) {
            GeneralMonthViewContainer monthViewContainer = mMonthViewpager.findViewWithTag(currentItem);
            if(monthViewContainer != null)
                monthViewContainer.getMonthView().startTodayAnimation();
        }
    }

    /**
     * 오늘날자 일정 animation 없애기
     */
    @Override
    public void removeTodayAnimation() {
        GeneralMonthViewContainer monthViewContainer = mMonthViewpager.findViewWithTag(mLastItem);
        if(monthViewContainer != null)
            monthViewContainer.getMonthView().removeTodayAnimation();
    }

    public void setSelectedOnDay(int year, int month, int day, boolean smoothScroll){
        DateTime minDate = new DateTime(DateTimeZone.UTC).withDate(mDelegate.getMinYear(), 1, 1).withMillisOfDay(0);
        DateTime curDate = new DateTime(DateTimeZone.UTC).withDate(year, month, day).withMillisOfDay(0);
        int dayPosition = (int) ((curDate.getMillis() - minDate.getMillis())/CalendarUtil.ONE_DAY);
        mDayViewPager.setCurrentItem(dayPosition, smoothScroll);
    }

    public void setSelected(int year, int month, int day, boolean smoothScroll){
        if(!smoothScroll)
            mManualMonthChange = true;

        setSelectedOnMonth(year, month, day, smoothScroll);
        setSelectedOnDay(year, month, day, false);
    }

    public void setManualMonthChange(boolean value) {
        mManualMonthChange = value;
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

    public ViewPager2 getMonthViewpager() {
        return mMonthViewpager;
    }

    public void onDestroy(){
        mMonthViewpager.unregisterOnPageChangeCallback(mMonthPageChangeCallback);
        mDayViewPager.unregisterOnPageChangeCallback(mDayPageChangeCallback);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);

        //월, 일정 현시부분들의 높이들을 계산하여 설정한다.
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int monthHeight = (int) ((Utils.getDisplayDimensions(getContext()).y -
                getResources().getDimension(R.dimen.action_bar_height) -
                getResources().getDimension(R.dimen.bottom_bar_height)) * mDelegate.getMonthProportion());

        mMonthViewPagerContainer.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(monthHeight, MeasureSpec.EXACTLY));
        mDayViewPagerContainer.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height - monthHeight, MeasureSpec.EXACTLY));
    }

    /**
     * 날자선택이 변할때 호출된다.
     * @param calendar 날자
     */
    @Override
    public void onCalendarSelect(Calendar calendar) {
        Calendar selected = mDelegate.mSelectedCalendar;
        if(calendar.equals(selected))
            return;

        final int year = calendar.getYear();
        final int month = calendar.getMonth();
        final int day = calendar.getDay();

        for (DayViewFragment.OnDayChangeListener listener : mDayChangeListeners){
            listener.onDayChange(year, month, day);
        }

        final boolean toOtherMonth = selected.getYear() != year || selected.getMonth() != month;

        /* 월 ViewPager를 선택된 날자페지로 이동 */
        //다른 달(이전/다음 달)의 날자가 선택되였을때
        if(toOtherMonth) {
            mManualMonthChange = true;
            setSelectedOnMonth(year, month, day, true);
        }

        //현재 달의 날자가 선택되였을때
        else {
            mDelegate.setSelectedDay(year, month, day);

            int page = mMonthViewpager.getCurrentItem();
            View view = mMonthViewpager.findViewWithTag(page);
            if(view instanceof GeneralMonthViewContainer) {
                ((GeneralMonthViewContainer)view).getMonthView().invalidate();
            }
        }

        //일 ViewPager를 선택된 날자페지로 이동
        final DateTime minDate, curDate;
        minDate = new DateTime(DateTimeZone.UTC).withDate(mDelegate.getMinYear(), 1, 1).withMillisOfDay(0);
        curDate = new DateTime(DateTimeZone.UTC).withDate(year, month, day).withMillisOfDay(0);
        int newDayPos = (int) ((curDate.getMillis() - minDate.getMillis())/CalendarUtil.ONE_DAY);
        int oldDayPos = mDayViewPager.getCurrentItem();

        mManualDayChange = true;
        if(!toOtherMonth && Math.abs(newDayPos - oldDayPos) <= 1) {
            setSelectedOnDay(year, month, day, true);
        }
        else {
            setSelectedOnDay(year, month, day, false);
            mDayViewPager.setAlpha(0.0f);
            mDayViewPager.animate().alpha(1.0f).setDuration(FADE_DURATION).start();
        }
    }
}
