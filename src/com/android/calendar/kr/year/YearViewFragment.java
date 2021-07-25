package com.android.calendar.kr.year;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.utils.Utils;
import com.android.kr_common.Time;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.calendar.helper.CalendarController;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import com.android.krcalendar.R;

/**
 * 년보기
 * Fragment
 */
public class YearViewFragment extends Fragment implements CalendarController.EventHandler {
    AllInOneActivity mMainActivity;

    private YearPagerAdapter mAdapter;
    private ViewPager2 mPager;
    private final int mYear;
    private final long mTimeMillis;
    private ViewPager2.OnPageChangeCallback mPageChangeCallBack;

    private CalendarController mController;

    //년변화를 감지하는 listener들
    List<OnYearChangeListener> mListeners = new ArrayList<>();

    //오늘날자, 날자가 변하면 이것들도 갱신된다.
    private int mLastYear, mLastMonth, mLastDay;
    boolean mNoTransition = true;

    //최대, 최소 년도
    int mMinYear, mMaxYear;

    public YearViewFragment() {
        this(System.currentTimeMillis());
    }
    public YearViewFragment(long timeMillis) {
        DateTime dateTime = new DateTime(timeMillis);
        mYear = dateTime.getYear();
        mTimeMillis = timeMillis;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mMinYear = Utils.getThemeAttribute(context, R.attr.min_year);
        mMaxYear = Utils.getThemeAttribute(context, R.attr.max_year);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPager.unregisterOnPageChangeCallback(mPageChangeCallBack);
        mPager.setAdapter(null);
        mPager = null;
        mAdapter = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMainActivity = AllInOneActivity.getMainActivity(getContext());
        mMainActivity.addYearChangeListeners(this);
        mController = CalendarController.getInstance(getContext());

        //오늘날자 보관
        DateTime dateTime = DateTime.now();
        mLastYear = dateTime.getYear();
        mLastMonth = dateTime.getMonthOfYear();
        mLastDay = dateTime.getDayOfMonth();

        // Inflate layout
        final View fragmentView = inflater.inflate(R.layout.fragment_year_view, container,false);
        mPager = fragmentView.findViewById(R.id.yearViewPager);

        //페지선택변화를 감지하는 callback
        mPageChangeCallBack = new ViewPager2.OnPageChangeCallback(){
            @Override
            public void onPageSelected(int position) {
                DateTime dateTime = new DateTime(mTimeMillis);
                dateTime = dateTime.plusYears(position + mMinYear - mYear);
                for (OnYearChangeListener listener : mListeners){
                    listener.onYearChange(dateTime.getYear());
                }

                Time t = new Time(dateTime.getMillis());
                mController.sendEvent(CalendarController.EventType.GO_TO, t, null,
                        t, -1, CalendarController.ViewType.CURRENT, 0);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            @Override
            public void onPageScrollStateChanged(int state) { }
        };
        mPager.registerOnPageChangeCallback(mPageChangeCallBack);

        //30pixel의 MarginPageTransformer설정
        mPager.setPageTransformer(new MarginPageTransformer2(30));

        mAdapter = new YearPagerAdapter(getContext(), mMinYear, mMaxYear);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(mYear - mMinYear, false);
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mMainActivity.updateVisibility();
    }

    @Override
    public long getSupportedEventTypes() {
        return CalendarController.EventType.GO_TO | CalendarController.EventType.EVENTS_CHANGED;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        //`날자가기` 혹은 `오늘`을 눌렀을때
        if(event.eventType == CalendarController.EventType.GO_TO){
            boolean gotoToday = (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0;
            boolean gotoDate = (event.extraLong & CalendarController.EXTRA_GOTO_DATE) != 0;
            if(gotoToday || gotoDate) {
                long timeMillis = event.selectedTime.toMillis(true);
                DateTime dateTime = new DateTime(timeMillis);
                if(mPager != null)
                    mPager.setCurrentItem(dateTime.getYear() - mMinYear, true);
            }
        }

        //일정변화가 일어났을때
        else if(event.eventType == CalendarController.EventType.EVENTS_CHANGED)
            eventsChanged();
    }

    @Override
    public void eventsChanged() {
        updateView();
    }

    public void updateView() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void minuteChanged() {
        //날자가 변했으면 재그리기를 진행한다
        DateTime dateTime = DateTime.now();
        if(mLastYear != dateTime.getYear() || mLastMonth != dateTime.getMonthOfYear() || mLastDay != dateTime.getDayOfMonth()) {
            updateView();
            mLastYear = dateTime.getYear();
            mLastMonth = dateTime.getMonthOfYear();
            mLastDay = dateTime.getDayOfMonth();
        }
    }

    public void addYearChangeListener(OnYearChangeListener listener){
        mListeners.add(listener);
    }

    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        //Animation이 끝날때 아래의 runnable을 실행한다.
        mNoTransition = nextAnim == 0;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(mPager != null)
                    mPager.setOffscreenPageLimit(3);
            }
        };

        //Animation이 없을때는 runnable을 즉시에 실행한다.
        if(mNoTransition) {
            Handler handler = new Handler();
            handler.post(runnable);
            return null;
        }

        //Animation이 끝날때를 감지
        Animator animator = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
        if (animator != null && enter) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Handler handler = new Handler();
                    handler.post(runnable);
                }
            });
        }
        return animator;
    }

    public long getTimeMillis() {
        return mTimeMillis;
    }

    public interface OnYearChangeListener {
        void onYearChange(int year);
    }
}
