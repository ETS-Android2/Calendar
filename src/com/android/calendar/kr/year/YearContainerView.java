package com.android.calendar.kr.year;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;
import com.android.kr_common.Time;

import org.joda.time.DateTime;

/**
 * 년보기
 * 1개 년에 해당한 월들을 현시해주는 Layout
 * 12개의 달 view들을 가지고 있다.
 * @see YearItemView
 */
public class YearContainerView extends ConstraintLayout {
    private int mYear;

    private int mCurYear, mCurMonth;

    public YearContainerView(Context context) {
        this(context, null);
    }

    public YearContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YearContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setup(int year) {
        mYear = year;
        DateTime dateTime = DateTime.now();
        mCurYear = dateTime.getYear();
        mCurMonth = dateTime.getMonthOfYear();

        setMonthTexts();
    }

    /**
     * 월 view들 갱신
     */
    public void setMonthTexts() {
        for (int i = 1; i <= 12; i ++) {
            int id = getDayIdentifier(i, getContext());
            final YearItemView itemView = findViewById(id);
            itemView.setMonthText(i);
            itemView.updateTodayView(mCurYear == mYear && mCurMonth == i);

            final int month = i;
            Context context = getContext();
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DateTime startDay = new DateTime(mYear, month, 1, 0, 0);
                    DateTime endDay = startDay.plusMonths(1).minusDays(1);
                    CalendarController.getInstance(context).sendEvent(CalendarController.EventType.GO_TO,
                            new Time(startDay.getMillis()), new Time(endDay.getMillis()), -1, CalendarController.ViewType.MONTH,
                            0);
                }
            });
            Utils.addCommonTouchListener(itemView);
        }
    }

    /**
     * 일정개수목록을 가지고 월 View들을 갱신한다.
     * @param countArray 일정개수 목록
     */
    public void setEventCountTexts(Integer[] countArray) {
        for (int i = 0; i < 12; i ++) {
            int id = getDayIdentifier(i + 1, getContext());
            YearItemView itemView = findViewById(id);
            itemView.applyInformation(countArray[i]);
        }
    }

    /**
     * 해당 월 view의 Id를 돌려준다.
     * @param monthNumber 월(1-12)
     * @param context Context
     * @return id (3월 -> R.id.month3)
     */
    int getDayIdentifier(int monthNumber, Context context){
        @SuppressLint("DefaultLocale") String idName = String.format("month%1$d", monthNumber);
        Resources resources = context.getResources();
        return resources.getIdentifier(idName, "id", context.getPackageName());
    }
}
