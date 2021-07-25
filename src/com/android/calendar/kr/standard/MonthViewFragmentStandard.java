package com.android.calendar.kr.standard;

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
import com.android.calendar.kr.common.CalendarView;
import com.android.kr_common.Time;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import static com.android.calendar.helper.CalendarController.EventType.CALENDAR_PERMISSION_GRANTED;
import static com.android.calendar.helper.CalendarController.EventType.EVENTS_CHANGED;

/**
 * 양식 1
 * Fragment
 */
public class MonthViewFragmentStandard extends Fragment implements
        CalendarController.EventHandler, CalendarView.OnCalendarSelectListener {

    //초기날자의 년, 월, 일
    private final int mCurYear;
    private final int mCurMonth;
    private final int mCurDay;

    //현재 날자(오늘), 날이 바뀔때마다 갱신된다.
    private int mLastYear, mLastMonth, mLastDay;

    CalendarController mController;
    AllInOneActivity mMainActivity;

    public StandardCalendarView mCalendarView;

    /**
     * 미리초시간을 입력값으로 받는다
     * @param timeMillis 시간
     */
    public MonthViewFragmentStandard(long timeMillis) {
        DateTime dateTime = new DateTime().withMillis(timeMillis);
        mCurYear = dateTime.getYear();
        mCurMonth = dateTime.getMonthOfYear();
        mCurDay = dateTime.getDayOfMonth();
    }

    /**
     * Fragment 가 재창조될때 파라메터 없는 구성자를 요구하므로 기정의 구성자를 정의해주었다.
     */
    public MonthViewFragmentStandard() {
        DateTime dateTime = new DateTime();
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

        final View v = inflater.inflate(R.layout.fragment_month_view1, container, false);

        mCalendarView = v.findViewById(R.id.calendarView);
        mCalendarView.setOnCalendarSelectListener(this);

        mController = CalendarController.getInstance(getContext());
        mCalendarView.setSelectedOnMonth(mCurYear, mCurMonth, mCurDay, false);
        return v;
    }

    @Override
    public long getSupportedEventTypes() {
        return EVENTS_CHANGED | CalendarController.EventType.GO_TO
                | CalendarController.EventType.CALENDAR_PERMISSION_GRANTED;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        //`날자가기`했을때
        if(event.eventType == CalendarController.EventType.GO_TO){
            boolean gotoToday = (event.extraLong & CalendarController.EXTRA_GOTO_TODAY) != 0;
            boolean gotoDate = (event.extraLong & CalendarController.EXTRA_GOTO_DATE) != 0;
            if(gotoToday || gotoDate){
                long timeMillis = event.startTime.toMillis(true);
                DateTime dateTime = new DateTime(timeMillis);
                Calendar calendar = new Calendar();
                calendar.setYear(dateTime.getYear());
                calendar.setMonth(dateTime.getMonthOfYear() - 1);
                calendar.setDay(dateTime.getDayOfMonth());

                mCalendarView.mDelegate.mSelectedCalendar = calendar;
                mCalendarView.setSelectedOnMonth(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), true);
            }
        }

        //사용자가 달력 읽기/쓰기 권한을 부여하였을때
        else if(event.eventType == CALENDAR_PERMISSION_GRANTED){
            mCalendarView.notifyViewPager();
        }

        //일정변화가 일어났을때
        else if(event.eventType == EVENTS_CHANGED){
            eventsChanged();
        }
    }

    /**
     * 일정변화가 일어났을때 호출된다.
     */
    @Override
    public void eventsChanged() {
        mCalendarView.notifyViewPager();
    }

    /**
     * 분이 바뀔때 호출된다.
     */
    @Override
    public void minuteChanged() {
        mCalendarView.notifyViewPager();

        //날자가 변하면 Actionbar 를 갱신한다.
        DateTime dateTime = DateTime.now();
        if(mLastYear != dateTime.getYear() || mLastMonth != dateTime.getMonthOfYear() || mLastDay != dateTime.getDayOfMonth()) {
            mMainActivity.updateVisibility();
            mLastYear = dateTime.getYear();
            mLastMonth = dateTime.getMonthOfYear();
            mLastDay = dateTime.getDayOfMonth();
        }
    }

    @Override
    public void onCalendarSelect(Calendar calendar) {
        DateTime curDateTime = new DateTime(mController.getTime());
        DateTime dateTime = new DateTime(calendar.getYear(), calendar.getMonth(), calendar.getDay(),
                curDateTime.getHourOfDay(), curDateTime.getMinuteOfHour());
        Time goTime = new Time();
        goTime.set(dateTime.getMillis());
        mController.sendEvent(CalendarController.EventType.GO_TO, goTime, null, -1, CalendarController.ViewType.DAY,
                0);
    }

    @Override
    public void onResume(){
        super.onResume();

        mCalendarView.removeTodayAnimation();
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mCalendarView.mLastItem = -1;
                mCalendarView.playTodayEventsAnimation();
            }
        }, 500);
    }

    @Override
    public void onDestroy(){
        mCalendarView.onDestroy();
        super.onDestroy();
    }
}
