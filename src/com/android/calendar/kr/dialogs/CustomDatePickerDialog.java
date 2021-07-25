package com.android.calendar.kr.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import com.android.calendar.utils.Utils;
import com.android.kr_common.Time;

import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.lany.picker.DatePicker;

import com.android.krcalendar.R;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Objects;

/**
 * 날자선택대화창
 * @see CustomTimePickerDialog
 */
public class CustomDatePickerDialog extends Dialog implements
        android.view.View.OnClickListener {

    public Button mBtnYes, mBtnNo;  //확인, 취소단추
    public DatePicker mDatePicker;  //날자선택기

    public int mYear, mMonth, mDay; //초기 년, 월, 일

    //일정편집시 대화창을 띄울때 시작날자/마감날자 인가
    public boolean mUsedForEvent = false;
    public boolean mIsStart;

    private OnDateSelectedListener mOnDateSelectedListener;

    public CustomDatePickerDialog(Time t, Activity a) {
        super(a);

        mYear = t.year;
        mMonth = t.month + 1;
        mDay = t.monthDay;
    }

    public CustomDatePickerDialog(int year, int month, int day, Activity a) {
        super(a);

        mYear = year;
        mMonth = month;
        mDay = day;
    }

    public CustomDatePickerDialog(int year, int month, int day, boolean isAllDay, boolean isStart, Activity a) {
        super(a);

        mIsStart = isStart;
        if(isAllDay && !isStart) {
            DateTime dateTime = (new DateTime(year, month, day, 0, 0)).minusDays(1);
            mYear = dateTime.getYear();
            mMonth = dateTime.getMonthOfYear();
            mDay = dateTime.getDayOfMonth();
        }
        else {
            mYear = year;
            mMonth = month;
            mDay = day;
        }

        mUsedForEvent = true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_datepicker);

        Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mBtnYes = (Button) findViewById(R.id.btn_yes);
        mBtnNo = (Button) findViewById(R.id.btn_no);
        mDatePicker = (DatePicker)findViewById(R.id.date_picker_1);
        mBtnYes.setOnClickListener(this);
        mBtnNo.setOnClickListener(this);

        //최대, 최소 한계날자들을 계산한다.
        Calendar minDate = Calendar.getInstance();
        Calendar maxDate = Calendar.getInstance();
        int minYear = Utils.getThemeAttribute(getContext(), R.attr.min_year);
        int maxYear = Utils.getThemeAttribute(getContext(), R.attr.max_year);
        minDate.set(minYear, 0, 1);
        maxDate.set(maxYear, 11, 31);

        mDatePicker.setDayViewShown(true);
        mDatePicker.setMinDate(minDate.getTimeInMillis());
        mDatePicker.setMaxDate(maxDate.getTimeInMillis());
        mDatePicker.init(mYear, mMonth - 1, mDay);

        Utils.addCommonTouchListener(mBtnYes);
        Utils.addCommonTouchListener(mBtnNo);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.btn_yes) {    //확인단추를 눌렀을때
            dismiss();
            if(mOnDateSelectedListener != null) {
                if(mUsedForEvent) {
                    mOnDateSelectedListener.onDateSelected(mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth(), mIsStart);
                } else {
                    mOnDateSelectedListener.onDateSelected(mDatePicker.getYear(), mDatePicker.getMonth(), mDatePicker.getDayOfMonth());
                }
            }
        } else if(id == R.id.btn_no) {  //취소단추를 눌렀을때
            dismiss();
        }
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        mOnDateSelectedListener = listener;
    }

    /**
     * 날자선택변화 Listener
     */
    public interface OnDateSelectedListener {
        void onDateSelected(int year, int monthOfYear,
                           int dayOfMonth);
        void onDateSelected(int year, int monthOfYear,
                           int dayOfMonth, boolean isStart);
    }
}