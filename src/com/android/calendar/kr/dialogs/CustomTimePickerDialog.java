package com.android.calendar.kr.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;
import com.lany.picker.HourMinutePicker;

import java.util.Objects;

/**
 * 시간선택대화창
 * @see CustomDatePickerDialog
 */
public class CustomTimePickerDialog extends Dialog implements View.OnClickListener {
    private HourMinutePicker mTimePicker;
    private final int mHour;
    private final int mMinute;

    boolean mIsStart;
    private OnTimeSelectedListener mTimeSelectedListener;

    public CustomTimePickerDialog(int hour, int minute, boolean isStart, @NonNull Context context) {
        super(context);
        mHour = hour;
        mMinute = minute;
        mIsStart = isStart;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_timepicker);

        Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //자식 View 들 얻기
        Button btnYes = findViewById(R.id.btn_yes);
        Button btnNo = findViewById(R.id.btn_no);
        mTimePicker = findViewById(R.id.hour_minute_picker);

        //시간, 분을 설정한다.
        mTimePicker.setCurrentHour(mHour);
        mTimePicker.setCurrentMinute(mMinute);

        btnYes.setOnClickListener(this);
        btnNo.setOnClickListener(this);

        Utils.addCommonTouchListener(btnYes);
        Utils.addCommonTouchListener(btnNo);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.btn_yes) {    //확인단추를 눌렀을때
            dismiss();

            if(mTimeSelectedListener != null) {
                mTimeSelectedListener.onTimeSelected(mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute(), mIsStart);
            }
        } else if(id == R.id.btn_no) {  //취소단추를 눌렀을때
            dismiss();
        }
    }

    public void setOnTimeSelectedListener(OnTimeSelectedListener listener) {
        mTimeSelectedListener = listener;
    }

    /**
     * 시간선택변화 Listener
     */
    public interface OnTimeSelectedListener {
        void onTimeSelected(int hour, int minute, boolean isStart);
    }
}
