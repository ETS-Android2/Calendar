package com.android.calendar.kr.day;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;
import com.android.calendar.kr.common.Calendar;
import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.kr_common.Time;
import com.android.krcalendar.R;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import static com.android.calendar.helper.CalendarController.EXTRA_GOTO_DATE;
import static com.android.calendar.helper.CalendarController.EXTRA_GOTO_TODAY;

/**
 * 일보기(양식1)
 * Fragment
 */
public class DayViewFragment extends Fragment implements
        CalendarController.EventHandler {
    //초기의 년, 월, 일
    private int mCurYear, mCurMonth, mCurDay;

    //오늘의 년, 월, 일
    private int mLastYear, mLastMonth, mLastDay;

    CalendarController mController;
    CalendarViewDelegate mDelegate;
    AllInOneActivity mMainActivity;

    //Content View
    DayFragmentContainerView mContentView;
    ViewPager2 mDayViewPager;
    DayPagerAdapter mDayPagerAdapter;

    //일변화감지 listener
    List<OnDayChangeListener> mListeners = new ArrayList<>();

    //페지변화를 감지하는 callback
    ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            DateTime dateTime = new DateTime(mDelegate.getMinYear(), mDelegate.getMinYearMonth(), mDelegate.getMinYearDay(), 0, 0);
            dateTime = dateTime.plusDays(position);
            for (OnDayChangeListener listener : mListeners) {
                listener.onDayChange(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
            }

            mCurYear = dateTime.getYear();
            mCurMonth = dateTime.getMonthOfYear();
            mCurDay = dateTime.getDayOfMonth();

            Time t = new Time(dateTime.getMillis());
            mController.sendEvent(CalendarController.EventType.GO_TO, t, null,
                    t, -1, CalendarController.ViewType.CURRENT, 0);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

        @Override
        public void onPageScrollStateChanged(int state) { }
    };

    /**
     * 미리초시간을 입력값으로 받는다
     * @param timeMillis 시간
     */
    public DayViewFragment(long timeMillis) {
        DateTime dateTime = new DateTime(timeMillis);
        mCurYear = dateTime.getYear();
        mCurMonth = dateTime.getMonthOfYear();
        mCurDay = dateTime.getDayOfMonth();
    }

    /**
     * Fragment 가 재창조될때 파라메터 없는 구성자를 요구하므로 기정의 구성자를 정의해주었다.
     */
    public DayViewFragment() {
        DateTime dateTime = new DateTime();
        mCurYear = dateTime.getYear();
        mCurMonth = dateTime.getMonthOfYear();
        mCurDay = dateTime.getDayOfMonth();
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMainActivity = AllInOneActivity.getMainActivity(getContext());
        mController = CalendarController.getInstance(getContext());
        mMainActivity.addDayChangeListeners(this);

        if(mMainActivity.viewFromAgenda() || mMainActivity.viewToAgenda())
            mMainActivity.updateVisibility();

        //오늘날자 보관
        DateTime dateTime = DateTime.now();
        mLastYear = dateTime.getYear();
        mLastMonth = dateTime.getMonthOfYear();
        mLastDay = dateTime.getDayOfMonth();

        //Delegate 객체 초기화
        mDelegate = new CalendarViewDelegate(requireContext(), null);
        mDelegate.mSelectedCalendar = new Calendar();
        mDelegate.mSelectedCalendar.setYear(mCurYear);
        mDelegate.mSelectedCalendar.setMonth(mCurMonth);
        mDelegate.mSelectedCalendar.setDay(mCurDay);

        //Inflate layout
        mContentView = (DayFragmentContainerView) inflater.inflate(R.layout.fragment_day_view, container, false);
        mContentView.setCalendarViewDelegate(mDelegate);
        addDayChangeListener(mContentView);

        mDayViewPager = mContentView.findViewById(R.id.day_view_pager);
        mDayPagerAdapter = new DayPagerAdapter(getContext(), mDelegate);
        mDayViewPager.setAdapter(mDayPagerAdapter);
        mDayViewPager.registerOnPageChangeCallback(pageChangeCallback);

        //초기날자가 있는 페지로 이행한다.
        mDayViewPager.setCurrentItem((int) Utils.daysDiff(mDelegate.getMinYear(), mDelegate.getMinYearMonth(), mDelegate.getMinYearDay(),
                mCurYear, mCurMonth, mCurDay), false);

        return mContentView;
    }

    public void addDayChangeListener(OnDayChangeListener listener){
        mListeners.add(listener);
    }

    @Override
    public void onDestroy(){
        mDayViewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        super.onDestroy();
    }

    @Override
    public long getSupportedEventTypes() {
        return CalendarController.EventType.GO_TO | CalendarController.EventType.EVENTS_CHANGED;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        //일정변화가 일어났을때
        if(event.eventType == CalendarController.EventType.EVENTS_CHANGED)
            eventsChanged();

        //`날자로 가기`할때
        if(event.eventType == CalendarController.EventType.GO_TO){
            boolean gotoToday = (event.extraLong & EXTRA_GOTO_TODAY) != 0;
            boolean gotoDate = (event.extraLong & EXTRA_GOTO_DATE) != 0;
            if(gotoToday || gotoDate) {
                DateTime dateTime = new DateTime(event.startTime.toMillis(true));
                mCurYear = dateTime.getYear();
                mCurMonth = dateTime.getMonthOfYear();
                mCurDay = dateTime.getDayOfMonth();

                //선택된 날자가 있는 페지로 이행한다.
                if(mDayViewPager != null) {
                    mDayViewPager.setCurrentItem((int) Utils.daysDiff(mDelegate.getMinYear(), mDelegate.getMinYearMonth(), mDelegate.getMinYearDay(),
                            mCurYear, mCurMonth, mCurDay), true);
                }
            }
        }
    }

    @Override
    public void eventsChanged() {
        if (mDayPagerAdapter != null)
            mDayPagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void minuteChanged() {
        //날자가 변하였는가를 검사하고 날자가 변하였을때 Action bar를 갱신한다.
        DateTime dateTime = DateTime.now();
        if(mLastYear != dateTime.getYear() || mLastMonth != dateTime.getMonthOfYear() || mLastDay != dateTime.getDayOfMonth()) {
            mMainActivity.updateVisibility();
            mLastYear = dateTime.getYear();
            mLastMonth = dateTime.getMonthOfYear();
            mLastDay = dateTime.getDayOfMonth();
        }
    }

    public interface OnDayChangeListener {
        void onDayChange(int year, int month, int day);
    }
}
