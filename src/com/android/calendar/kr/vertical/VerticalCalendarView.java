package com.android.calendar.kr.vertical;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;

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
 * 양식 4
 * 기본 view
 */
public class VerticalCalendarView extends RelativeLayout implements VerticalMonthView.CalendarSelectListener, VerticalWeekBar.ExpandWeekBarListener {
    private static final int EXPAND_ANIMATE_DURATION = 200;
    private int mCurYear, mCurMonth, mCurDay;

    //자식 view들
    ViewPager2 mMonthViewPager, mDayViewPager;
    View mMonthViewLayout;
    VerticalDayViewScrollableHost mDayViewLayout;

    //ViewPager에 련결된 adapter들
    VerticalMonthPagerAdapter mMonthAdapter;
    VerticalDayPagerAdapter mDayAdapter;

    VerticalCalendarViewDelegate mDelegate;

    //월 ViewPager, 일 ViewPager 의 페지변화를 감지하는 callback들
    ViewPager2.OnPageChangeCallback mMonthPagerCallback, mDayPagerCallback;

    //월변화를 감지하는 listener들
    List<CalendarView.OnMonthChangeListener> mMonthChangeListeners = new ArrayList<>();
    //일변화를 감지하는 listener들
    List<DayViewFragment.OnDayChangeListener> mDayChangeListeners = new ArrayList<>();

    //Expand/collapse animator
    Animator mAnimator;

    public VerticalCalendarView(Context context) {
        this(context, null);
    }

    public VerticalCalendarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mDelegate = new VerticalCalendarViewDelegate(context, attrs);
        DateTime dateTime = DateTime.now();
        mDelegate.setCurrentDate(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());

        LayoutInflater.from(getContext()).inflate(R.layout.vertical_calendar_view_layout, this);

        mMonthPagerCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                final int year = position / 12 + mDelegate.getMinYear();
                final int month = position % 12 + 1;

                for (CalendarView.OnMonthChangeListener listener : mMonthChangeListeners){
                    listener.onMonthChange(year, month);
                }

                mCurYear = year;
                mCurMonth = month;
            }
        };
        mDayPagerCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if(VerticalCalendarView.VerticalCalendarViewDelegate.isExpanded())
                    return;

                final DateTime minDate = new DateTime(mDelegate.getMinYear(), 1, 1, 0, 0);
                final DateTime curDate = minDate.plusDays(position);
                final int year = curDate.getYear();
                final int month = curDate.getMonthOfYear();
                final int day = curDate.getDayOfMonth();
                mDelegate.setSelectedDate(year, month, day);
                for (DayViewFragment.OnDayChangeListener listener : mDayChangeListeners){
                    listener.onDayChange(year, month, day);
                }

                int currentPage = mMonthViewPager.getCurrentItem();
                View view = mMonthViewPager.findViewWithTag(currentPage);
                if(view instanceof VerticalMonthViewContainer){
                    ((VerticalMonthViewContainer)view).getMonthView().invalidate();
                }

                if(mCurYear != year || mCurMonth != month) {
                    mCurYear = year;
                    mCurMonth = month;
                    mCurDay = day;

                    mDelegate.setSelectedDate(year, month, day);
                    int page = (year - mDelegate.getMinYear())*12 + month - 1;
                    mMonthViewPager.setCurrentItem(page, false);
                    mMonthAdapter.notifyDataSetChanged();
                }
            }
        };

        AllInOneActivity mainActivity = AllInOneActivity.getMainActivity(context);
        mainActivity.addMonthChangeListeners(this);
        mainActivity.addDayChangeListeners(this);
        mDelegate.setCalendarSelectListener(this);
        mDelegate.setWeekBarTouchListener(this);
    }

    public void onFinishInflate(){
        super.onFinishInflate();
        mDayViewPager = findViewById(R.id.day_view_pager);
        mMonthViewPager = findViewById(R.id.month_view_pager);
        mDayViewLayout = findViewById(R.id.day_view_layout);
        mMonthViewLayout = findViewById(R.id.month_view_layout);

        //Adapter 추가
        mMonthAdapter = new VerticalMonthPagerAdapter(getContext(), mDelegate);
        mMonthViewPager.setAdapter(mMonthAdapter);
        mDayAdapter = new VerticalDayPagerAdapter(getContext(), mDelegate);
        mDayViewPager.setAdapter(mDayAdapter);

        //월, 일정 Viewpager 의 페지변화를 감지하는 listener 들을 추가한다.
        mMonthViewPager.registerOnPageChangeCallback(mMonthPagerCallback);
        mDayViewPager.registerOnPageChangeCallback(mDayPagerCallback);

        mDayViewLayout.setWeekBarExpandListener(mDelegate.getWeekBarExpandListener());
    }

    public void onDestroy() {
        mMonthViewPager.unregisterOnPageChangeCallback(mMonthPagerCallback);
        mDayViewPager.unregisterOnPageChangeCallback(mDayPagerCallback);
    }

    public void updateViews() {
        mMonthAdapter.notifyDataSetChanged();
        mDayAdapter.notifyDataSetChanged();
    }

    public void updateMonthView() {
        mMonthAdapter.notifyDataSetChanged();
    }

    public void setSelected(int year, int month, int day, boolean smoothScroll) {
        mCurYear = year;
        mCurMonth = month;
        mCurDay = day;

        mDelegate.setSelectedDate(year, month, day);
        int position = (year - mDelegate.getMinYear())*12 + month - 1;
        mMonthViewPager.setCurrentItem(position, smoothScroll);

        if(!VerticalCalendarViewDelegate.isExpanded()) {
            setExpandProgress(0);

            DateTime minDate = new DateTime(mDelegate.getMinYear(), 1, 1, 0, 0);
            DateTime curDate = new DateTime(year, month, day, 0, 0);
            int dayPosition = (int) ((curDate.getMillis() - minDate.getMillis())/ CalendarUtil.ONE_DAY);
            mDayViewPager.setCurrentItem(dayPosition, smoothScroll);
        }
    }

    public void addMonthChangeListener(CalendarView.OnMonthChangeListener listener){
        mMonthChangeListeners.add(listener);
    }

    public void addDayChangeListener(DayViewFragment.OnDayChangeListener listener){
        mDayChangeListeners.add(listener);
    }

    public VerticalCalendarViewDelegate getDelegate() {
        return mDelegate;
    }

    /**
     * 날자를 click하여 날자가 선택되였을때
     * @param year 년
     * @param month 월
     * @param day 일
     */
    @Override
    public void onCalendarSelect(int year, int month, int day) {
        if(mAnimator != null && mAnimator.isRunning())
            return;

        //선택된 날자 보관
        mDelegate.setSelectedDate(year, month, day);

        final DateTime minDate, curDate;
        minDate = new DateTime(DateTimeZone.UTC).withDate(mDelegate.getMinYear(), 1, 1).withMillisOfDay(0);
        curDate = new DateTime(DateTimeZone.UTC).withDate(year, month, day).withMillisOfDay(0);
        int page = (int) ((curDate.getMillis() - minDate.getMillis())/CalendarUtil.ONE_DAY);
        mDayViewPager.setCurrentItem(page, false);

        mMonthViewPager.setUserInputEnabled(false);
        VerticalCalendarViewDelegate.setExpanded(false);

        //날자보기부분이 펼쳐진상태에서 접히는 animation을 창조하고 시작한다.
        Animator animator = ObjectAnimator.ofFloat(VerticalCalendarView.this, "expandProgress", 1, 0);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(EXPAND_ANIMATE_DURATION);
        animator.start();
        mAnimator = animator;

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                /* Animation 이 끝났을때 */
                //다른날의 날자가 선택했을때
                if(mCurYear != year || mCurMonth != month) {
                    int position = (year - mDelegate.getMinYear()) * 12 + month - 1;
                    mMonthViewPager.setCurrentItem(position, false);
                    mMonthAdapter.notifyDataSetChanged();
                }

                //현재 달의 날자가 선택되였을때
                else {
                    int page = mMonthViewPager.getCurrentItem();
                    View view = mMonthViewPager.findViewWithTag(page);
                    if(view instanceof VerticalMonthViewContainer) {
                        ((VerticalMonthViewContainer)view).getMonthView().invalidate();
                    }
                }

                //날자선택이 변하였다는것을 listener 들에 알려준다.
                for (DayViewFragment.OnDayChangeListener listener : mDayChangeListeners){
                    listener.onDayChange(year, month, day);
                }
            }
        });
    }

    /**
     * 날자현시부분이 접힌 상태에서 Fling 혹은 Click사건이 일어날때 호출된다.
     * @see #setExpandProgress
     */
    @Override
    public void onFlingOrClick() {
        //Animation이 이미 진행중이면 여기서 끝낸다.
        if(mAnimator != null && mAnimator.isRunning())
            return;

        //날자보기부분이 접힌 상태에서 펼쳐지는 animation을 창조하고 시작한다.
        VerticalCalendarViewDelegate.setExpanded(true);
        Animator animator = ObjectAnimator.ofFloat(this, "expandProgress", 0, 1);
        animator.setInterpolator(new LinearInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                for (CalendarView.OnMonthChangeListener listener : mMonthChangeListeners){
                    listener.onMonthChange(mCurYear, mCurMonth);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mMonthViewPager.setUserInputEnabled(true);
                mMonthAdapter.notifyDataSetChanged();
            }
        });

        animator.setDuration(EXPAND_ANIMATE_DURATION);
        animator.start();

        mAnimator = animator;
    }

    /**
     * Expand|Collapse Animation을 진행할때 호출되는 animation속성함수
     * @param value 0 ~ 1
     */
    public void setExpandProgress(float value) {
        //이동거리 계산
        float moveDistance = (1 - value) * mDelegate.getMonthItemWidth() * 3;

        //월보기부분은 이동시키고 날자일정보기부분은 Alpha값을 변경시킨다.
        mMonthViewLayout.setTranslationX(-moveDistance);
        mDayViewLayout.setAlpha(1 - value);
        if(value == 1) {
            mDayViewLayout.setVisibility(INVISIBLE);
        }
        else if (value == 0){
            mDayViewLayout.setVisibility(VISIBLE);
        }

        //요일부분은 움직이지 말아야 하기때문에 다시 반대방향으로 이동한다.
        //(우의 코드부분에서 월보기부분이 이동하면서 주요일부분도 따라 움직였다.)
        int currentPage = mMonthViewPager.getCurrentItem();
        VerticalMonthViewContainer childView = mMonthViewPager.findViewWithTag(currentPage);
        if(childView != null) {
            childView.moveWeekBar(moveDistance);
        }
    }

    public static class VerticalCalendarViewDelegate {
        private final int mMinYear;
        private final int mMaxYear;
        private static final int MIN_YEAR = 1900;
        private static final int MAX_YEAR = 2300;

        private final float mMonthItemWidth;
        private static final float MONTH_ITEM_WIDTH_DP = 20;

        private final float mMonthItemHeight;

        private final float mTextSize;
        private static final float MONTH_TEXT_SIZE = 20;

        private final int mWeekBarColor;
        private final int mOtherMonthColor;
        private final int mCurrentMonthColor;
        private final int mTodayUnderlineColor;
        private final int mSelectedDayCircleColor;
        private final int mWeekBarWeekendColor;
        private final int mMonthWeekendColor;

        private final float mTodayUnderlineHeight;
        private static final float TODAY_UNDERLINE_HEIGHT = 3;

        private final int mDayViewMainBackgroundColor;
        private final int mDayViewEventBackgroundColor;

        Calendar mSelectedDate, mCurrentDate;

        //날자현시부분이 펼쳐진 상태인가, 접혀진 상태인가
        private static boolean mExpanded = true;

        //날자가 변하는것을 감지하는 listener
        private VerticalMonthView.CalendarSelectListener mCalendarSelectListener;

        //날자현시부분이 접혀져있는 상태에서 Fling혹은 Click을 감지하는 listener
        private VerticalWeekBar.ExpandWeekBarListener mExpandWeekBarListener;

        public VerticalCalendarViewDelegate(Context context, AttributeSet attrs) {
            @SuppressLint("Recycle") TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.VerticalCalendarView);
            mMinYear = array.getInteger(R.styleable.VerticalCalendarView_min_year, MIN_YEAR);
            mMaxYear = array.getInteger(R.styleable.VerticalCalendarView_max_year, MAX_YEAR);
            mMonthItemWidth = array.getDimension(R.styleable.VerticalCalendarView_month_item_width, Utils.convertDpToPixel(MONTH_ITEM_WIDTH_DP, context));
            mTextSize = array.getDimension(R.styleable.VerticalCalendarView_text_size, Utils.convertDpToPixel(MONTH_TEXT_SIZE, context));
            mWeekBarColor = array.getColor(R.styleable.VerticalCalendarView_week_bar_color, Color.WHITE);
            mOtherMonthColor = array.getColor(R.styleable.VerticalCalendarView_other_month_color, Color.GRAY);
            mCurrentMonthColor = array.getColor(R.styleable.VerticalCalendarView_current_month_color, Color.WHITE);
            mWeekBarWeekendColor = array.getColor(R.styleable.VerticalCalendarView_week_bar_weekend_color, Color.RED);;
            mMonthWeekendColor = array.getColor(R.styleable.VerticalCalendarView_month_weekend_color, Color.RED);
            mTodayUnderlineColor = array.getColor(R.styleable.VerticalCalendarView_today_underline_color, Color.MAGENTA);
            mTodayUnderlineHeight = array.getDimension(R.styleable.VerticalCalendarView_today_underline_height,
                    Utils.convertDpToPixel(TODAY_UNDERLINE_HEIGHT, context));
            mSelectedDayCircleColor = array.getColor(R.styleable.VerticalCalendarView_selected_day_circle_color, Color.BLUE);
            mDayViewMainBackgroundColor = array.getColor(R.styleable.VerticalCalendarView_day_view_main_bg_color, 0xff0076DD);
            mDayViewEventBackgroundColor = array.getColor(R.styleable.VerticalCalendarView_day_view_event_bg_color, 0x12000000);

            //날자그리기를 위해 한개 날자의 높이를 계산한다.
            Resources resources = context.getResources();
            float monthHeight = Utils.getDisplayDimensions(context).y
                    - resources.getDimension(R.dimen.action_bar_height)
                    - resources.getDimension(R.dimen.bottom_bar_height)
                    - resources.getDimension(R.dimen.vertical_calendar_padding_bottom);
            mMonthItemHeight = monthHeight/14;
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

        public int getMinYear(){
            return mMinYear;
        }

        public int getMaxYear(){
            return mMaxYear;
        }

        public float getMonthItemWidth(){
            return mMonthItemWidth;
        }

        public float getMonthItemHeight(){
            return mMonthItemHeight;
        }

        public float getTextSize(){
            return mTextSize;
        }

        public int getWeekBarColor(){
            return mWeekBarColor;
        }

        public int getOtherMonthColor(){
            return mOtherMonthColor;
        }

        public int getCurrentMonthColor(){
            return mCurrentMonthColor;
        }

        public int getWeekBarWeekendColor() {
            return mWeekBarWeekendColor;
        }

        public int getMonthWeekendColor() {
            return mMonthWeekendColor;
        }

        public int getTodayUnderlineColor(){
            return mTodayUnderlineColor;
        }

        public float getTodayUnderlineHeight() {
            return mTodayUnderlineHeight;
        }

        public int getSelectedDayCircleColor() {
            return mSelectedDayCircleColor;
        }

        public int getDayViewMainBackgroundColor() {
            return mDayViewMainBackgroundColor;
        }

        public int getDayViewEventBackgroundColor() {
            return mDayViewEventBackgroundColor;
        }

        public Calendar getCurrentDate() {
            return mCurrentDate;
        }

        public Calendar getSelectedDate() {
            return mSelectedDate;
        }

        public static boolean isExpanded() {
            return mExpanded;
        }

        public static void setExpanded(boolean expanded) {
            mExpanded = expanded;
        }

        public VerticalMonthView.CalendarSelectListener getCalendarSelectListener() {
            return mCalendarSelectListener;
        }

        public void setCalendarSelectListener(VerticalMonthView.CalendarSelectListener listener){
            mCalendarSelectListener = listener;
        }

        public VerticalWeekBar.ExpandWeekBarListener getWeekBarExpandListener() {
            return mExpandWeekBarListener;
        }

        public void setWeekBarTouchListener(VerticalWeekBar.ExpandWeekBarListener listener) {
            mExpandWeekBarListener = listener;
        }
    }
}
