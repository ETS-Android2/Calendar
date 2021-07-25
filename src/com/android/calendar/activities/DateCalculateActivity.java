package com.android.calendar.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.android.calendar.kr.dialogs.CustomDatePickerDialog;
import com.android.calendar.utils.LunarCoreHelper;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import static com.android.calendar.activities.AllInOneActivity.SELECTED_TIME;

/**
 * 날자계산화면
 */
public class DateCalculateActivity extends AppCompatActivity implements View.OnClickListener {

    //Activity가 재창조될때 복귀하여야 할 자료들의 KEY 상수값들
    private static final String SOLAR_DATE = "solar_date";
    private static final String START_DATE = "start_date";
    private static final String DAY_COUNT = "day_count";
    private static final String IS_FORWARD = "is_forward";

    /*--- 자식 View들 ---*/
    View mBackButton;
    LinearLayout mSolarDateContainer;
    TextView mSolarDateView;
    TextView mLunarDateView;

    LinearLayout mStartDateContainer;
    TextView mStartDateView;

    EditText mDayCountView;

    LinearLayout mForwardViewContainer;
    TextView mForwardView;

    TextView mResultDateView;
    Button mGoViewOnCalendarBtn;

    AppCompatButton mCalculateBtn;
    /*--- ---*/

    //시작날자, 마감날자, 결과날자, 날자수, 앞으로/뒤로 방향
    DateTime mSolarDate, mStartDate, mResultDate;
    int mDayCount;
    boolean mForward;

    //대화창이 두번이상 켜지는것을 방지하기 위한 flag변수
    boolean mDialogOpened = false;

    @Override
    protected void onCreate(Bundle icicle) {
        //Theme설정
        if(Utils.isDayTheme())
            setTheme(R.style.CalendarAppThemeDay);
        else
            setTheme(R.style.CalendarAppThemeNight);
        super.onCreate(icicle);

        initValues(icicle);

        setContentView(R.layout.date_calculate);
        initViews();
    }

    @SuppressLint("SetTextI18n")
    public void initViews(){
        //자식 view들을 얻는다
        mBackButton = findViewById(R.id.back_button);
        mSolarDateContainer = findViewById(R.id.solar_date_container);
        mSolarDateView = findViewById(R.id.solar_date);
        mLunarDateView = findViewById(R.id.lunar_date);
        mStartDateContainer = findViewById(R.id.start_date_container);
        mStartDateView = findViewById(R.id.start_date);
        mDayCountView = findViewById(R.id.day_count);
        mForwardViewContainer = findViewById(R.id.forward_view_container);
        mForwardView = findViewById(R.id.forward_view);
        mResultDateView = findViewById(R.id.result_date);
        mGoViewOnCalendarBtn = findViewById(R.id.go_view_on_calendar);
        mCalculateBtn = findViewById(R.id.go_calculate);

        //ClickListener를 추가한다.
        mBackButton.setOnClickListener(this);
        mSolarDateContainer.setOnClickListener(this);
        mStartDateContainer.setOnClickListener(this);
        mForwardViewContainer.setOnClickListener(this);
        mGoViewOnCalendarBtn.setOnClickListener(this);
        mCalculateBtn.setOnClickListener(this);

        //Multi touch를 방지하기 위하여 touch사건들을 추가한다.
        Utils.addCommonTouchListener(mBackButton);
        Utils.addCommonTouchListener(mSolarDateContainer);
        Utils.addCommonTouchListener(mStartDateContainer);
        Utils.addCommonTouchListener(mForwardViewContainer);
        Utils.addCommonTouchListener(mGoViewOnCalendarBtn);
        Utils.addCommonTouchListener(mCalculateBtn);

        //초기값들을 view들에 입력한다.
        mSolarDateView.setText(getDateString(mSolarDate));
        mStartDateView.setText(getDateString(mStartDate));
        mDayCountView.setText(Integer.toString(mDayCount));

        if(mForward){
            mForwardView.setText(R.string.forward);
        }
        else{
            mForwardView.setText(R.string.backward);
        }
        onCalculateFromSolar();
        onCalculateFromDayDiff();
    }

    /**
     * 변수들의 초기값설정
     * @param bundle {@link #onCreate}에서 보내온 Bundle
     */
    private void initValues(Bundle bundle){
        if(bundle != null) {    //Activity가 재창조될때
            mSolarDate = (DateTime) bundle.getSerializable(SOLAR_DATE);
            mStartDate = (DateTime) bundle.getSerializable(START_DATE);
            mDayCount = bundle.getInt(DAY_COUNT);
            mForward = bundle.getBoolean(IS_FORWARD);
        }
        else {    //Activity가 처음 창조될때
            final DateTime selectedDate;
            Intent intent = getIntent();
            if(intent == null || !intent.hasExtra(SELECTED_TIME)) {
                selectedDate = DateTime.now();
            } else {
                selectedDate = new DateTime().withMillis(intent.getLongExtra(SELECTED_TIME, 0));
            }

            mSolarDate = new DateTime(selectedDate);
            mStartDate = new DateTime(selectedDate);
            mDayCount = 100;
            mForward = true;
        }
    }

    /**
     * 날자수 입력칸에 초점이 가있을때 그 바깥령역에 touch하면 입력칸의 초점을 없애고 건반을 숨긴다.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mDayCountView.isFocused()) {
                Rect outRect = new Rect();
                mDayCountView.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    mDayCountView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mDayCountView.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * 양력날자를 눌렀을때
     */
    private void onSolarDate(){
        //날자선택대화창을 열고 날자를 선택하면 양력날자 TextView에 날자를 설정한다.
        CustomDatePickerDialog datePickerDialog = new CustomDatePickerDialog(mSolarDate.getYear(), mSolarDate.getMonthOfYear(), mSolarDate.getDayOfMonth(), this);
        Utils.makeBottomDialog(datePickerDialog);

        datePickerDialog.setOnDateSelectedListener(new CustomDatePickerDialog.OnDateSelectedListener(){
            @Override
            public void onDateSelected(int year, int monthOfYear, int dayOfMonth) {
                mSolarDate = new DateTime(year, monthOfYear + 1, dayOfMonth, 0, 0);
                mSolarDateView.setText(getDateString(mSolarDate));
                onCalculateFromSolar();
            }

            @Override
            public void onDateSelected(int year, int monthOfYear, int dayOfMonth, boolean isStart) {
            }
        });
        datePickerDialog.show();

        datePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDialogOpened = false;
            }
        });

        mDialogOpened = true;
    }

    /**
     * 시작날자를 눌렀을때
     */
    private void onStartDate(){
        //날자선택대화창을 열고 날자를 선택하면 시작날자 TextView에 날자를 설정한다.
        CustomDatePickerDialog datePickerDialog = new CustomDatePickerDialog(mStartDate.getYear(), mStartDate.getMonthOfYear(), mStartDate.getDayOfMonth(), this);
        Utils.makeBottomDialog(datePickerDialog);

        datePickerDialog.setOnDateSelectedListener(new CustomDatePickerDialog.OnDateSelectedListener(){
            @Override
            public void onDateSelected(int year, int monthOfYear, int dayOfMonth) {
                mStartDate = new DateTime(year, monthOfYear + 1, dayOfMonth, 0, 0);
                mStartDateView.setText(getDateString(mStartDate));
            }

            @Override
            public void onDateSelected(int year, int monthOfYear, int dayOfMonth, boolean isStart) {
            }
        });
        datePickerDialog.show();

        datePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDialogOpened = false;
            }
        });

        mDialogOpened = true;
    }

    /**
     * `앞으로/뒤로`를 눌렀을때
     */
    private void onForward(){
        //앞으로/뒤로 상태를 전환한다.
        mForward = !mForward;

        if(mForward){
            mForwardView.setText(R.string.forward);
        }
        else{
            mForwardView.setText(R.string.backward);
        }
    }

    /**
     * 날자보기단추를 눌렀을때
     */
    private void onViewCalendar(){
        long timeMillis = mResultDate.getMillis();

        Intent intent = new Intent();
        intent.putExtra(SELECTED_TIME, timeMillis);

        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * 시작날자로부터 날자수만큼 앞으로/뒤로 떨어진 날자를 계산한다.
     */
    private void onCalculateFromDayDiff(){
        mDayCount = (int)Float.parseFloat(String.valueOf(mDayCountView.getText()));
        if(mForward)
            mResultDate = mStartDate.plusDays(mDayCount);
        else
            mResultDate = mStartDate.minusDays(mDayCount);

        mResultDateView.setText(getDateString(mResultDate));
    }

    /**
     * 양력날자로부터 음력날자를 계산한다.
     */
    private void onCalculateFromSolar(){
        int[] lunar = LunarCoreHelper.convertSolar2Lunar(mSolarDate.getDayOfMonth(), mSolarDate.getMonthOfYear(), mSolarDate.getYear());
        final String lunarString;
        if(lunar[3] == 0){
            lunarString = getString(R.string.date_format, lunar[2], lunar[1], lunar[0]);
        }
        else {
            lunarString = getString(R.string.lunar_date_format_with_leap, lunar[2], lunar[1], lunar[0]);
        }
        mLunarDateView.setText(lunarString);
    }

    /**
     * 양력날자, 시작날자, 결과날자들에서의 날자문자렬 형식
     * @param dateTime: 입력날자
     * @return `년.월.일`을 돌려준다.(2021.1.1)
     */
    public String getDateString(DateTime dateTime){
        return getResources().getString(R.string.date_format, dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
    }

    /**
     * 자식 view들의 click동작들을 처리한다.
     * @param v: click사건이 일어난 자식 view
     */
    @Override
    public void onClick(View v) {
        //`뒤로 가기`단추
        if(v == mBackButton) {
            onBackPressed();
        }

        //양력날자
        else if(v == mSolarDateContainer){
            if(!mDialogOpened)
                onSolarDate();
        }

        //시작날자
        else if(v == mStartDateContainer){
            if(!mDialogOpened)
                onStartDate();
        }

        //`앞으로/뒤로`
        else if(v == mForwardViewContainer){
            onForward();
        }

        //달력보기
        else if(v == mGoViewOnCalendarBtn){
            onViewCalendar();
        }

        //날자계산
        else if(v == mCalculateBtn){
            onCalculateFromDayDiff();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //상태들 보관
        outState.putSerializable(SOLAR_DATE, mSolarDate);
        outState.putSerializable(START_DATE, mStartDate);
        outState.putInt(DAY_COUNT, mDayCount);
        outState.putBoolean(IS_FORWARD, mForward);
    }
}
