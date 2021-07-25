package com.android.calendar.kr.big;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.kr.common.Calendar;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import static com.android.calendar.helper.CalendarController.EventType.CALENDAR_PERMISSION_GRANTED;
import static com.android.calendar.helper.CalendarController.EventType.EVENTS_CHANGED;

/**
 * 양식 3
 * Fragment
 */
public class MonthViewFragmentBig extends Fragment implements CalendarController.EventHandler{
    //초기의 년, 월, 일
    private final int mCurYear;
    private final int mCurMonth;
    private final int mCurDay;

    //현재 날자(오늘), 날이 바뀔때마다 갱신된다.
    private int mLastYear, mLastMonth, mLastDay;

    BigCalendarView mCalendarView;
    AllInOneActivity mMainActivity;

    boolean mNoTransition = true;

    public MonthViewFragmentBig() {
        DateTime dateTime = DateTime.now();
        mCurYear = dateTime.getYear();
        mCurMonth = dateTime.getMonthOfYear();
        mCurDay = dateTime.getDayOfMonth();
    }

    public MonthViewFragmentBig(long timeMillis) {
        DateTime dateTime = new DateTime(timeMillis);
        mCurYear = dateTime.getYear();
        mCurMonth = dateTime.getMonthOfYear();
        mCurDay = dateTime.getDayOfMonth();
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMainActivity = AllInOneActivity.getMainActivity(getContext());

        //오늘 날자 보관
        DateTime dateTime = DateTime.now();
        mLastYear = dateTime.getYear();
        mLastMonth = dateTime.getMonthOfYear();
        mLastDay = dateTime.getDayOfMonth();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mMainActivity.updateVisibility();

        View view = inflater.inflate(R.layout.fragment_month_view3, container, false);
        mCalendarView = view.findViewById(R.id.calendar_content_view);

        mCalendarView.setSelected(mCurYear, mCurMonth, mCurDay, false);
        return view;
    }

    @Override
    public void onDestroy() {
        mCalendarView.onDestroy();
        super.onDestroy();
    }

    @Override
    public long getSupportedEventTypes() {
        return EVENTS_CHANGED | CalendarController.EventType.GO_TO
                | CalendarController.EventType.CALENDAR_PERMISSION_GRANTED;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        //`날자가기`했을때
        if(event.eventType == CalendarController.EventType.GO_TO) {
            boolean gotoToday = (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0;
            boolean gotoDate = (event.extraLong & CalendarController.EXTRA_GOTO_DATE) != 0;
            if(gotoToday || gotoDate) {
                long timeMillis = event.startTime.toMillis(true);
                DateTime dateTime = new DateTime(timeMillis);
                Calendar calendar = new Calendar();
                calendar.setYear(dateTime.getYear());
                calendar.setMonth(dateTime.getMonthOfYear() - 1);
                calendar.setDay(dateTime.getDayOfMonth());

                mCalendarView.setSelected(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), true);
            }
        }

        //일정변화가 일어났을때
        else if (event.eventType == EVENTS_CHANGED) {
            mCalendarView.updateViews();
        }

        //사용자가 달력 읽기/쓰기 권한을 부여하였을때
        else if (event.eventType == CALENDAR_PERMISSION_GRANTED) {
            mCalendarView.updateViews();
        }
    }

    /**
     * 일정변화가 일어났을때 호출된다.
     */
    @Override
    public void eventsChanged() {
        mCalendarView.updateMonthView();
    }

    /**
     * 분이 바뀔때 호출된다.
     */
    @Override
    public void minuteChanged() {
        mCalendarView.updateMonthView();

        //날이 바뀌면 Action bar단추들을 갱신한다. (오후 11:59 -> 12:00)
        DateTime dateTime = DateTime.now();
        if(mLastYear != dateTime.getYear() || mLastMonth != dateTime.getMonthOfYear() || mLastDay != dateTime.getDayOfMonth()) {
            mMainActivity.updateVisibility();
            mLastYear = dateTime.getYear();
            mLastMonth = dateTime.getMonthOfYear();
            mLastDay = dateTime.getDayOfMonth();
        }
    }

    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        //Animation이 끝날때 아래의 runnable을 실행한다.
        mNoTransition = nextAnim == 0;
        final Runnable runnable = () -> mCalendarView.expandOffScreenPageCount();

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
}
