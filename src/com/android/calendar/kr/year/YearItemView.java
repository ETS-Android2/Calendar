package com.android.calendar.kr.year;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

/**
 * 년보기
 * 1개 달의 월수자, 일정개수들을 현시해주는 view
 */
public class YearItemView extends LinearLayout {
    //자식 view들
    TextView mMonthView, mTodayView, mEventCountView;
    LinearLayout mItemLayout;

    public YearItemView(Context context) {
        this(context, null);
    }

    public YearItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YearItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.year_view_item, this);

        //자식 View 들 얻기
        mItemLayout = findViewById(R.id.year_item_layout);
        mMonthView = findViewById(R.id.month_label);
        mTodayView = findViewById(R.id.today_label);
        mEventCountView = findViewById(R.id.event_count_label);
    }

    /**
     * 월문자렬을 해당 TextView 에 설정
     * @param month 월
     */
    public void setMonthText(int month){
        switch (month){
            case 1:
                mMonthView.setText(R.string.january_label);
                break;
            case 2:
                mMonthView.setText(R.string.february_label);
                break;
            case 3:
                mMonthView.setText(R.string.march_label);
                break;
            case 4:
                mMonthView.setText(R.string.april_label);
                break;
            case 5:
                mMonthView.setText(R.string.may_label);
                break;
            case 6:
                mMonthView.setText(R.string.june_label);
                break;
            case 7:
                mMonthView.setText(R.string.july_label);
                break;
            case 8:
                mMonthView.setText(R.string.august_label);
                break;
            case 9:
                mMonthView.setText(R.string.september_label);
                break;
            case 10:
                mMonthView.setText(R.string.october_label);
                break;
            case 11:
                mMonthView.setText(R.string.november_label);
                break;
            case 12:
            default:
                mMonthView.setText(R.string.december_label);
                break;
        }
    }

    /**
     * 일정개수 문자렬을 해당 TextView 에 설정
     * @param eventCount 일정개수
     */
    public void applyInformation(int eventCount){
        final String eventString;
        //일정이 없을때
        if(eventCount == 0) {
            eventString = "";
        }
        //일정이 있을때
        else {
            eventString = getResources().getString(R.string.event_label) + " " + eventCount;
        }
        mEventCountView.setText(eventString);
    }

    /**
     * 현재 달에 대해 오늘날자, 요일을 보여준다.
     * @param isToday 이번달인가?
     */
    public void updateTodayView(boolean isToday) {
        if(!isToday) {
            mTodayView.setVisibility(INVISIBLE);
            mItemLayout.getBackground().mutate().setColorFilter(getResources().getColor(R.color.commonOutlineColor, null), PorterDuff.Mode.SRC_IN);
            return;
        }

        mTodayView.setVisibility(VISIBLE);

        String todayString = "";
        DateTime dateTime = DateTime.now();
        String weekDayString = Utils.getWeekDayString(getContext(), dateTime.getDayOfWeek(), false);
        todayString = getContext().getString(R.string.today_on_year, dateTime.getDayOfMonth(), weekDayString);
        mTodayView.setText(todayString);
        mItemLayout.getBackground().mutate().setColorFilter(getContext().getColor(R.color.year_view_out_line_color), PorterDuff.Mode.SRC_IN);
    }
}
