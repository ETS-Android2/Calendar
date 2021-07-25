package com.android.calendar.kr.big;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.kr.common.Calendar;
import com.android.calendar.kr.common.CalendarUtil;
import com.android.calendar.kr.common.CalendarView;
import com.android.calendar.utils.Utils;
import com.android.calendar.kr.day.DayViewFragment;
import com.android.krcalendar.R;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

/**
 * 양식 3
 * 기본 view
 */
public class BigCalendarView extends LinearLayout implements BigMonthMainViewAdapter.BigCalendarSelectListener {
    static final int DAYS_PER_ROW = 4;
    static final int DAYS_PER_COLUMN = 4;
    static final int DAYS_ONE_PAGE = DAYS_PER_ROW * DAYS_PER_COLUMN;
    static final int FADE_DURATION = 300;

    //그리기, 날자들을 보관하는 Delegate변수
    BigCalendarViewDelegate mDelegate;

    //자식 view들
    ViewPager2 mMonthPager, mDayPager;
    FrameLayout mMonthPagerContainer;
    FrameLayout mDayPagerContainer;

    BigMonthPagerAdapter mMonthAdapter;
    BigDayPagerAdapter mDayAdapter;
    ViewPager2.OnPageChangeCallback mMonthPageChangeCallback, mDayPageChangeCallback;

    //월, 일이 변하는것을 감지하는 listener들
    List<CalendarView.OnMonthChangeListener> mMonthChangeListeners = new ArrayList<>();
    List<DayViewFragment.OnDayChangeListener> mDayChangeListeners = new ArrayList<>();

    //현재 페지의 년, 월
    private int mYear;
    private int mMonth;

    //끌기가 아니라 코드적으로 setCurrentItem()을 호출하여 페지가 이동하였을때 true로 설정된다.
    private boolean mManualMonthChange = false;
    private boolean mManualDayChange = false;

    public BigCalendarView(Context context) {
        this(context, null);
    }

    public BigCalendarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigCalendarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDelegate = new BigCalendarViewDelegate(context, attrs);

        //기정적으로 오늘이 선택되도록 한다.
        DateTime dateTime = DateTime.now();
        mDelegate.setCurrentDate(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());

        //월 ViewPager의 페지 Scroll변화를 감지하는 callback
        mMonthPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                //페지가 변하였을때
                final int year = position / 12 + mDelegate.getMinYear();
                final int month = position % 12 + 1;

                for (CalendarView.OnMonthChangeListener listener : mMonthChangeListeners){
                    listener.onMonthChange(year, month);
                }

                Calendar selected = mDelegate.getSelectedDate();
                if(year != selected.getYear() || month != selected.getMonth()) {
                    mDelegate.setSelectedDate(year, month, 1);
                }

                mYear = year;
                mMonth = month;

                if(mManualMonthChange) {
                    mManualMonthChange = false;
                    return;
                }
                onCalendarSelect(year, month, 1, false);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if(state == ViewPager2.SCROLL_STATE_IDLE) {
                    //페지가 완전히 이동하였을때
                    int position = mMonthPager.getCurrentItem();
                    Calendar calendar = mDelegate.getSelectedDate();
                    final int year = calendar.getYear();
                    final int month = calendar.getMonth();
                    final int day = calendar.getDay();

                    if(mYear == year && mMonth == month) {
                        post(() -> {
                            //량옆페지들을 재그리기한다.
                            mMonthAdapter.notifyItemChanged(position - 1);
                            mMonthAdapter.notifyItemChanged(position + 1);

                            int page = mMonthPager.getCurrentItem();
                            BigMonthViewContainer view = mMonthPager.findViewWithTag(page);
                            view.invalidateViewPager();
                            view.gotoPosition(day - 1, false); //오늘날자로 이동한다.

                            int oldDayPosition = mDayPager.getCurrentItem();

                            DateTime minDate = new DateTime(DateTimeZone.UTC).withDate(mDelegate.getMinYear(), 1, 1).withMillisOfDay(0);
                            DateTime curDate = new DateTime(DateTimeZone.UTC).withDate(year, month, day).withMillisOfDay(0);
                            int dayPosition = (int) ((curDate.getMillis() - minDate.getMillis())/ CalendarUtil.ONE_DAY);

                            if(oldDayPosition == dayPosition)
                                return;

                            post(new Runnable() {
                                @Override
                                public void run() {
                                    mManualDayChange = true;
                                    mDayPager.setCurrentItem(dayPosition, false);
                                }
                            });
                        });
                    }
                }
            }
        };

        //일 ViewPager의 페지 Scroll변화를 감지하는 callback
        mDayPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                //페지가 변하였을때
                DateTime minDate = new DateTime(mDelegate.getMinYear(), 1, 1, 0, 0);
                DateTime curDate = minDate.plusDays(position);
                Calendar selected = mDelegate.getSelectedDate();

                for (DayViewFragment.OnDayChangeListener listener : mDayChangeListeners){
                    listener.onDayChange(curDate.getYear(), curDate.getMonthOfYear(), curDate.getDayOfMonth());
                }

                if(mManualDayChange) {
                    mManualDayChange = false;
                    return;
                }

                int page = mMonthPager.getCurrentItem();
                BigMonthViewContainer view = mMonthPager.findViewWithTag(page);

                if(selected.getYear() != curDate.getYear() || selected.getMonth() != curDate.getMonthOfYear()) {
                    mDelegate.setSelectedDate(curDate.getYear(), curDate.getMonthOfYear(), curDate.getDayOfMonth());
                    int newPos = (curDate.getYear() - mDelegate.getMinYear())*12 + curDate.getMonthOfYear() - 1;
                    mManualMonthChange = true;
                    mMonthPager.setCurrentItem(newPos, false);

                    view = mMonthPager.findViewWithTag(newPos);
                    if(view != null){
                        view.gotoPosition(curDate.getDayOfMonth() - 1, false);
                    }

                    //Fade animation 을 준다.
                    mMonthPager.setAlpha(0.5f);
                    mMonthPager.animate().alpha(1f).setDuration(FADE_DURATION);
                }
                else {
                    mDelegate.setSelectedDate(curDate.getYear(), curDate.getMonthOfYear(), curDate.getDayOfMonth());
                    if(view != null) {
                        view.gotoPosition(curDate.getDayOfMonth() - 1, true);
                    }
                }
            }
        };

        LayoutInflater.from(context).inflate(R.layout.big_calendar_view_layout, this);

        AllInOneActivity mainActivity = AllInOneActivity.getMainActivity(context);
        mainActivity.addMonthChangeListeners(this);
        mainActivity.addDayChangeListeners(this);
        mDelegate.setCalendarSelectListener(this);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        //자식 view들 얻기
        mMonthPager = findViewById(R.id.month_view_pager);
        mDayPager = findViewById(R.id.day_view_pager);
        mMonthPagerContainer = findViewById(R.id.month_view_pager_container);
        mDayPagerContainer = findViewById(R.id.day_view_pager_container);

        //Month ViewPager의 너비, 높이를 계산하여 그것을 adapter에 넘겨준다.
        Resources resources = getResources();
        int monthWidth = (int) (resources.getDisplayMetrics().widthPixels -
                        resources.getDimension(R.dimen.big_month_container_padding_start) -
                        resources.getDimension(R.dimen.big_month_container_padding_end));
        int monthHeight = (int) ((Utils.getDisplayDimensions(getContext()).y -
                        resources.getDimension(R.dimen.action_bar_height) -
                        resources.getDimension(R.dimen.bottom_bar_height)) * mDelegate.getMonthProportion());

        mMonthAdapter = new BigMonthPagerAdapter(getContext(), mDelegate, monthWidth, monthHeight);
        mMonthPager.setAdapter(mMonthAdapter);
        mMonthPager.registerOnPageChangeCallback(mMonthPageChangeCallback);

        mDayAdapter = new BigDayPagerAdapter(getContext(), mDelegate);
        mDayPager.setAdapter(mDayAdapter);
        mDayPager.registerOnPageChangeCallback(mDayPageChangeCallback);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);

        int height = MeasureSpec.getSize(heightMeasureSpec);
        int monthHeight = (int) ((Utils.getDisplayDimensions(getContext()).y -
                getResources().getDimension(R.dimen.action_bar_height) -
                getResources().getDimension(R.dimen.bottom_bar_height)) * mDelegate.getMonthProportion());

        mMonthPagerContainer.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(monthHeight, MeasureSpec.EXACTLY));
        mDayPagerContainer.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height - monthHeight, MeasureSpec.EXACTLY));
    }

    /**
     * 날자선택
     * @param year 년
     * @param month 월
     * @param day 일
     * @param smoothScroll 페지이동할때 smooth scroll 하겠는가?
     */
    public void setSelected(int year, int month, int day, boolean smoothScroll) {
        mYear = year;
        mMonth = month;
        mDelegate.setSelectedDate(year, month, day);

        int monthPosition = (year - mDelegate.getMinYear())*12 + month - 1;
        mMonthPager.setCurrentItem(monthPosition, smoothScroll);

        DateTime minDate = new DateTime(mDelegate.getMinYear(), 1, 1, 0, 0);
        DateTime curDate = new DateTime(year, month, day, 0, 0);
        int dayPosition = (int) ((curDate.getMillis() - minDate.getMillis())/CalendarUtil.ONE_DAY);
        mDayPager.setCurrentItem(dayPosition, false);
    }

    /**
     * 전체 재그리기
     */
    public void updateViews() {
        mMonthAdapter.notifyDataSetChanged();
        mDayAdapter.notifyDataSetChanged();
    }

    /**
     * 월부분 재그리기
     */
    public void updateMonthView() {
        mMonthAdapter.notifyDataSetChanged();
    }

    public BigCalendarViewDelegate getDelegate() {
        return mDelegate;
    }

    public void addMonthChangeListener(CalendarView.OnMonthChangeListener listener){
        mMonthChangeListeners.add(listener);
    }

    public void addDayChangeListener(DayViewFragment.OnDayChangeListener listener){
        mDayChangeListeners.add(listener);
    }

    public void onDestroy() {
        mMonthPager.unregisterOnPageChangeCallback(mMonthPageChangeCallback);
        mDayPager.unregisterOnPageChangeCallback(mDayPageChangeCallback);
    }

    /**
     * Month, Day ViewPager들의 OffScreenPageLimit 을 설정한다.
     */
    public void expandOffScreenPageCount(){
        mMonthPager.setOffscreenPageLimit(3);
        mDayPager.setOffscreenPageLimit(3);
    }

    /**
     * 날자가 선택되였을때
     * @param year 년
     * @param month 월
     * @param day 일
     * @param isClick 사용자가 click을 했을때 true, 다른 경우는 false
     */
    @Override
    public void onCalendarSelect(int year, int month, int day, boolean isClick) {
        Calendar selected = mDelegate.mSelectedDate;
        if(selected.getYear() == year && selected.getMonth() == month && selected.getDay() == day)
            return;

        for (DayViewFragment.OnDayChangeListener listener : mDayChangeListeners){
            listener.onDayChange(year, month, day);
        }

        //선택된 날자 보관
        mDelegate.setSelectedDate(year, month, day);
        if(!isClick)
            return;

        int page = mMonthPager.getCurrentItem();

        BigMonthViewContainer view = mMonthPager.findViewWithTag(page);
        if(view != null) {
            view.invalidateViewPager();
        }

        DateTime minDate = new DateTime(DateTimeZone.UTC).withDate(mDelegate.getMinYear(), 1, 1).withMillisOfDay(0);
        DateTime curDate = new DateTime(DateTimeZone.UTC).withDate(year, month, day).withMillisOfDay(0);
        int oldDayPosition = mDayPager.getCurrentItem();
        int dayPosition = (int) ((curDate.getMillis() - minDate.getMillis())/CalendarUtil.ONE_DAY);
        mManualDayChange = true;

        if(Math.abs(dayPosition - oldDayPosition) <= 1)
        {
            mDayPager.setCurrentItem(dayPosition);
        }
        else {
            post(() -> {
                mDayPager.setCurrentItem(dayPosition, false);

                //Fade animation을 준다.
                mDayPager.setAlpha(0.0f);
                mDayPager.animate().alpha(1).setDuration(FADE_DURATION).start();
            });
        }
    }

    /**
     * 양식 3에 해당한 Delegate 클라스
     */
    public static class BigCalendarViewDelegate {
        private int mMinYear, mMaxYear;
        private static final int MIN_YEAR = 1900;
        private static final int MAX_YEAR = 2300;
        private int mTodayFillColor, mTodayColor;
        private int mDayColor, mWeekDayColor, mEventColor;
        private float mDaySize, mWeekDaySize, mEventSize;
        private float mEventCircleRadius;
        private int mDayLabelColor;
        private float mDayLabelSize;

        public Calendar mSelectedDate, mCurrentDate;

        public float mMonthProportion;
        public static final float DEFAULT_MONTH_PROPORTION = 0.5f;

        //날자선택변화를 감지하는 listener
        BigMonthMainViewAdapter.BigCalendarSelectListener mCalendarSelectListener;

        public BigCalendarViewDelegate(Context context, AttributeSet attrs) {
            @SuppressLint("Recycle") TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.BigCalendarView);
            mMinYear = array.getInteger(R.styleable.BigCalendarView_min_year, MIN_YEAR);
            mMaxYear = array.getInteger(R.styleable.BigCalendarView_max_year, MAX_YEAR);
            mMonthProportion = array.getFloat(R.styleable.BigCalendarView_month_proportion, DEFAULT_MONTH_PROPORTION);
            mTodayFillColor = array.getColor(R.styleable.BigCalendarView_today_fill_color, Color.YELLOW);
            mTodayColor = array.getColor(R.styleable.BigCalendarView_today_outline_color, Color.BLUE);
            mDayColor = array.getColor(R.styleable.BigCalendarView_day_color, Color.WHITE);
            mWeekDayColor = array.getColor(R.styleable.BigCalendarView_weekday_color, Color.WHITE);
            mEventColor = array.getColor(R.styleable.BigCalendarView_event_color, Color.GRAY);
            mDaySize = array.getDimension(R.styleable.BigCalendarView_day_size, Utils.convertSpToPixel(16, context));
            mWeekDaySize = array.getDimension(R.styleable.BigCalendarView_weekday_size, Utils.convertSpToPixel(16, context));
            mEventSize = array.getDimension(R.styleable.BigCalendarView_event_size, Utils.convertSpToPixel(16, context));
            mEventCircleRadius = array.getDimension(R.styleable.BigCalendarView_event_circle_radius, Utils.convertDpToPixel(1, context));
            mDayLabelColor = array.getColor(R.styleable.BigCalendarView_day_label_color, Color.BLUE);
            mDayLabelSize = array.getFloat(R.styleable.BigCalendarView_day_label_size, 16);
        }

        public int getMinYear() {
            return mMinYear;
        }

        public int getMaxYear() {
            return mMaxYear;
        }

        public float getMonthProportion() {
            return mMonthProportion;
        }

        public int getTodayFillDayColor() {
            return mTodayFillColor;
        }

        public int getTodayColor() {
            return mTodayColor;
        }

        public int getDayColor() {
            return mDayColor;
        }

        public int getWeekDayColor() {
            return mWeekDayColor;
        }

        public int getEventColor() {
            return mEventColor;
        }

        public float getDaySize() {
            return mDaySize;
        }

        public float getWeekDaySize() {
            return mWeekDaySize;
        }

        public float getEventSize() {
            return mEventSize;
        }

        public float getEventCircleRadius() {
            return mEventCircleRadius;
        }

        public int getDayLabelColor() {
            return mDayLabelColor;
        }

        public float getDayLabelSize() {
            return mDayLabelSize;
        }

        public void setCurrentDate(int year, int month, int day) {
            Calendar calendar = new Calendar();
            calendar.setYear(year);
            calendar.setMonth(month);
            calendar.setDay(day);

            mCurrentDate = calendar;
        }

        public void setSelectedDate(int year, int month, int day) {
            Calendar calendar = new Calendar();
            calendar.setYear(year);
            calendar.setMonth(month);
            calendar.setDay(day);

            mSelectedDate = calendar;
        }

        public Calendar getCurrentDate() {
            return mCurrentDate;
        }

        public Calendar getSelectedDate() {
            return mSelectedDate;
        }

        public BigMonthMainViewAdapter.BigCalendarSelectListener getCalendarSelectListener() {
            return mCalendarSelectListener;
        }

        public void setCalendarSelectListener(BigMonthMainViewAdapter.BigCalendarSelectListener listener){
            mCalendarSelectListener = listener;
        }
    }
}
