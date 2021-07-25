package com.android.calendar.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.activities.CalendarTypeSelectActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;
import com.android.calendar.kr.day.DayViewFragment;
import com.android.calendar.kr.year.YearViewFragment;
import com.android.calendar.kr.common.CalendarView;
import com.android.calendar.kr.vertical.VerticalCalendarView;
import com.android.kr_common.Time;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.android.calendar.helper.CalendarController.ViewType.AGENDA;
import static com.android.calendar.helper.CalendarController.ViewType.DAY;
import static com.android.calendar.helper.CalendarController.ViewType.MONTH;
import static com.android.calendar.helper.CalendarController.ViewType.YEAR;

/**
 * 오늘, 양식선택단추, 년/월/일에 해당한 현재시간정보를 보여주는 TextView들을 가지고 있다.
 * @see R.id#actionbar_header
 */
public class ActionBarHeader extends FrameLayout implements
        YearViewFragment.OnYearChangeListener,
        CalendarView.OnMonthChangeListener,
        DayViewFragment.OnDayChangeListener,
        View.OnClickListener {

    //선택된 날자의 년,월,일
    private int mYear;
    private int mMonth;
    private int mDay;

    //오늘 날자의 년,월,일
    private int mThisYear;
    private int mThisMonth;
    private int mThisDay;

    //자식 view들
    TextView mCurYearTextView, mCurMonthTextView, mCurWeekDayTextView, mTodayButton;
    ImageView mArrowBack, mArrowLeft, mArrowRight, mCalendarSelect;
    LinearLayout mViewForYear, mViewForMonth, mViewForDay;

    /*
    * 일보기에서 `오늘로가기`단추
    * 년보기, 월보기에서는 `오늘`단추를 같은것을 공통적으로 리용하지만 일보기에서는 단추 위치가 아래에 있다.
    * 이 단추는 자식 view가 아니다.
     */
    View mTodayButtonForDay;

    CalendarController mController;
    AllInOneActivity mMainActivity;

    //페지전환 Animation을 하는데 포함되는 view들
    List<View> mAnimateViews = new ArrayList<>();

    public ActionBarHeader(Context context) {
        this(context, null);
    }

    public ActionBarHeader(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionBarHeader(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMainActivity = AllInOneActivity.getMainActivity(context);
        mController = CalendarController.getInstance(context);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        //자식 View 들 얻기
        mViewForYear = findViewById(R.id.view_for_year);
        mViewForMonth = findViewById(R.id.view_for_month);
        mViewForDay = findViewById(R.id.view_for_day);
        mCurYearTextView = findViewById(R.id.current_year_label);
        mCurMonthTextView = findViewById(R.id.current_month_label);
        mCurWeekDayTextView = findViewById(R.id.current_weekday_label);
        mTodayButton = findViewById(R.id.today_button);
        mArrowBack = findViewById(R.id.image_arrow_back);
        mArrowLeft = findViewById(R.id.image_arrow_left);
        mArrowRight = findViewById(R.id.image_arrow_right);
        mCalendarSelect = findViewById(R.id.display_select);

        mTodayButton.setOnClickListener(this);
        mArrowBack.setOnClickListener(this);
        mArrowLeft.setOnClickListener(this);
        mArrowRight.setOnClickListener(this);
        mCalendarSelect.setOnClickListener(this);
        mViewForMonth.setOnClickListener(this);

        Utils.addCommonTouchListener(mTodayButton);
        Utils.addCommonTouchListener(mArrowBack);
        Utils.addCommonTouchListener(mArrowLeft);
        Utils.addCommonTouchListener(mArrowRight);
        Utils.addCommonTouchListener(mCalendarSelect);
        Utils.addCommonTouchListener(mViewForMonth);
    }

    /**
     * 자식 view들 갱신
     */
    @SuppressLint("DefaultLocale")
    public void updateViews(){
        boolean showTodayButton = false;

        //년
        if(mController.getViewType() == YEAR) {
            //년 Label 갱신
            String curYearText = "" + mYear;
            mCurYearTextView.setText(curYearText);
            showTodayButton = mYear != mThisYear;
        }

        //월
        else if(mController.getViewType() == CalendarController.ViewType.MONTH) {
            //년.월 Label 갱신
            String curMonthText = String.format("%1$d.%2$02d", mYear, mMonth);
            mCurMonthTextView.setText(curMonthText);

            if(Utils.getCalendarTypePreference(getContext()) == Utils.CALENDAR_TYPE4) {
                if(VerticalCalendarView.VerticalCalendarViewDelegate.isExpanded())
                    showTodayButton = mYear != mThisYear || mMonth != mThisMonth;
                else
                    showTodayButton = mYear != mThisYear || mMonth != mThisMonth || mDay != mThisDay;
            }
            else {
                showTodayButton = mYear != mThisYear || mMonth != mThisMonth;
            }
        }

        //일
        else if(mController.getViewType() == CalendarController.ViewType.DAY){
            //요일 현시
            DateTime dateTime = new DateTime(mYear, mMonth, mDay, 0, 0);
            int weekday = dateTime.getDayOfWeek();
            String weekDayString = Utils.getWeekDayString(getContext(), weekday, false);

            mCurWeekDayTextView.setText(weekDayString);
            showTodayButton = mYear != mThisYear || mMonth != mThisMonth || mDay != mThisDay;
        }

        //일정목록
        else if(mController.getViewType() == AGENDA){
            showTodayButton = mTodayButton.getVisibility() == VISIBLE;
        }

        final View todayButton = mController.getViewType() == CalendarController.ViewType.DAY ?
                mTodayButtonForDay : mTodayButton;
        if(todayButton != null) {
            todayButton.setVisibility(showTodayButton ? VISIBLE : INVISIBLE);

            if(showTodayButton &&
                    (Utils.isYearToMonthTransition() || Utils.isMonthToYearTransition() ||
                            Utils.isMonthToDayTransition() || Utils.isDayToMonthTransition() ||
                            Utils.isToAgendaTransition())
                    && !Utils.isTodayBothVisible())
                todayButton.setAlpha(0);
        }
    }

    /**
     * 일보기에서 `오늘단추`를 얻는다
     * @param view 오늘단추
     */
    public void setTodayButtonForDay(View view) {
        mTodayButtonForDay = view;
        if(view != null)
            mTodayButtonForDay.setOnClickListener(this);
    }

    /**
     * Animation을 진행할 view들을 얻는다
     * @param viewType 보기형태 (년/월/일/일정목록)
     */
    public void getAnimateViews(int viewType){
        mAnimateViews.clear();

        mViewForYear.setVisibility(GONE);
        mViewForMonth.setVisibility(GONE);
        mViewForDay.setVisibility(GONE);

        switch (viewType){
            case YEAR:
                mAnimateViews.add(mViewForYear);
                mViewForYear.setVisibility(VISIBLE);
                if(mTodayButton.getVisibility() == VISIBLE && !Utils.isTodayBothVisible())
                    mAnimateViews.add(mTodayButton);
                break;
            case MONTH:
                mAnimateViews.add(mViewForMonth);
                mViewForMonth.setVisibility(VISIBLE);
                if(mTodayButton.getVisibility() == VISIBLE && !Utils.isTodayBothVisible())
                    mAnimateViews.add(mTodayButton);
                break;
            case DAY:
                mAnimateViews.add(mViewForDay);
                mViewForDay.setVisibility(VISIBLE);
                if(mTodayButtonForDay != null && mTodayButtonForDay.getVisibility() == VISIBLE)
                    mAnimateViews.add(mTodayButtonForDay);
                break;
            case AGENDA:
                break;
        }
    }

    /**
     * mAnimateViews 목록을 지운다.
     */
    public void clearAnimateViews() {
        mAnimateViews.clear();
    }

    /**
     * 년 <-> 월, 월 <-> 일 전환시 animation 설정함수
     * @param progress animation속성 값
     */
    public void setFadeAnimationProgress(float progress) {
        if(mAnimateViews.isEmpty())
            return;

        for (View view:mAnimateViews){
            view.setAlpha(progress * progress);
        }
    }

    @Override
    public void onYearChange(int year) {
        mYear = year;

        if (!Utils.isMonthToDayTransition() && !Utils.isYearToMonthTransition() && !Utils.isMonthToYearTransition()) {
            onViewTypeChange();
            updateViews();
        }
    }

    @Override
    public void onMonthChange(int year, int month) {
        mYear = year;
        mMonth = month;

        DateTime dateTime = new DateTime().withMillis(mController.getTime());
        DateTime curTime = new DateTime(year, month, 1, dateTime.getHourOfDay(), dateTime.getMinuteOfHour());
        mController.setTime(curTime.getMillis());

        if (!Utils.isMonthToDayTransition() && !Utils.isMonthToYearTransition() && !Utils.isToAgendaTransition()) {
            if(Utils.isYearToMonthTransition()) {
                updateViews();
            }
            else {
                onViewTypeChange();
                updateViews();
            }
        }
    }

    @Override
    public void onDayChange(int year, int month, int day) {
        mYear = year;
        mMonth = month;
        mDay = day;

        DateTime dateTime = new DateTime().withMillis(mController.getTime());
        DateTime curTime = new DateTime(year, month, day, dateTime.getHourOfDay(), dateTime.getMinuteOfHour());
        mController.setTime(curTime.getMillis());

        if (!Utils.isMonthToDayTransition() && !Utils.isYearToMonthTransition() && !Utils.isMonthToYearTransition() && !Utils.isToAgendaTransition()) {
            onViewTypeChange();
            updateViews();
        }
    }

    //View초기화
    public void initialize(){
        Calendar calendar = Calendar.getInstance();
        mThisYear = calendar.get(Calendar.YEAR);
        mThisMonth = calendar.get(Calendar.MONTH) + 1;
        mThisDay = calendar.get(Calendar.DATE);

        onViewTypeChange();
        updateViews();
    }

    public View getViewForMonth() {
        return mViewForMonth;
    }

    @Override
    public void onClick(View v) {
        //`오늘`단추를 눌렀을때
        if(v == mTodayButton || v == mTodayButtonForDay) {
            long extras = CalendarController.EXTRA_GOTO_DATE | CalendarController.EXTRA_GOTO_TODAY;
            Time t = new Time();
            t.setToNow();
            mController.sendEvent(CalendarController.EventType.GO_TO, t, null,
                    t, -1, CalendarController.ViewType.CURRENT, extras);
        }

        //년보기 - `전해`단추를 눌렀을때
        else if(v == mArrowLeft){
            long extras = CalendarController.EXTRA_GOTO_DATE;

            DateTime dateTime = new DateTime(mController.getTime());
            Time t = new Time(dateTime.minusYears(1).getMillis());
            mController.sendEvent(CalendarController.EventType.GO_TO, t, null,
                    t, -1, CalendarController.ViewType.CURRENT, extras);
        }

        //년보기 - `다음해`단추를 눌렀을때
        else if(v == mArrowRight){
            long extras = CalendarController.EXTRA_GOTO_DATE;;

            DateTime dateTime = new DateTime(mController.getTime());
            Time t = new Time(dateTime.plusYears(1).getMillis());
            mController.sendEvent(CalendarController.EventType.GO_TO, t, null,
                    t, -1, CalendarController.ViewType.CURRENT, extras);
        }

        //일정목록 - `뒤로`단추를 눌렀을때
        else if(v == mArrowBack) {
            mMainActivity.onBackPressed();
        }

        //`양식선택`단추를 눌렀을때
        else if(v == mCalendarSelect) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(mMainActivity.getApplicationContext(), CalendarTypeSelectActivity.class);
            mMainActivity.startActivity(intent);
        }

        //월보기 - 월 TextView를 눌렀을때
        else if (v == mViewForMonth) {
            mController.sendEvent(CalendarController.EventType.LAUNCH_MONTH_PICKER, null, null, null, -1,
                    YEAR, 0);
        }
    }

    /**
     * 보기방식이 변하였을때
     * @see CalendarController.ViewType#YEAR
     * @see CalendarController.ViewType#MONTH
     * @see CalendarController.ViewType#DAY
     * @see CalendarController.ViewType#AGENDA
     */
    public void onViewTypeChange(){
        mViewForYear.setVisibility(GONE);
        mViewForMonth.setVisibility(GONE);
        mViewForDay.setVisibility(GONE);
        mTodayButton.setVisibility(VISIBLE);
        mViewForYear.setAlpha(1);
        mViewForMonth.setAlpha(1);
        mViewForDay.setAlpha(1);
        mTodayButton.setAlpha(1);

        int viewType = mController.getViewType();
        switch (viewType){
            case YEAR:
                mViewForYear.setVisibility(VISIBLE);
                break;
            case MONTH:
                mViewForMonth.setVisibility(VISIBLE);
                break;
            case DAY:
                mViewForDay.setVisibility(VISIBLE);
                mTodayButton.setVisibility(INVISIBLE);
                break;
            case AGENDA:
                mTodayButton.setVisibility(INVISIBLE);
                break;
        }
    }
}

