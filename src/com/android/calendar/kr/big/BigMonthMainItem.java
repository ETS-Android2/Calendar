package com.android.calendar.kr.big;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.calendar.event.EventManager;
import com.android.calendar.event.EventTypeManager;
import com.android.calendar.kr.common.Calendar;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import java.util.List;

/**
 * 양식 3
 * 한개 날자에 해당한 view(날자, 요일, 일정동그라미들)
 */
public class BigMonthMainItem extends View {
    private boolean mIsSelected = false;        //선택된 날자인가?
    private int mYear, mMonth, mDay, mWeekDay;  //년, 월, 일, 요일
    List<EventManager.OneEvent> mEventList;     //일정목록

    BigCalendarView.BigCalendarViewDelegate mDelegate;

    //Paint, 위치, 크기 값들
    Paint mDayPaint = new Paint();
    Paint mWeekDayPaint = new Paint();
    Paint mEventPaint = new Paint();
    Paint mEventCirclePaint = new Paint();
    private static final float DAY_WEIGHT = 0.4f;
    private static final float WEEKDAY_WEIGHT = 0.3f;
    private static final float EVENT_WEIGHT = 0.3f;
    private static final int MAX_EVENT_CIRCLE_COUNT = 5;
    private float mDayBaseLine, mWeekDayBaseLine, mEventBaseLine;

    public BigMonthMainItem(Context context) {
        this(context, null);
    }

    public BigMonthMainItem(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigMonthMainItem(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.day_round_rect_outline).mutate();
        setBackground(drawable);
    }

    /**
     * 그리기
     * 날자, 요일, 일정 동그라미 혹은 일정개수들을 그려준다.
     */
    @Override
    public void onDraw(Canvas canvas) {
        if(mDelegate == null)
            return;

        if(mIsSelected) {
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.day_round_rect_outline);
            assert drawable != null;
            drawable.setBounds(0, 0, getWidth(), getHeight());
            drawable.setColorFilter(mDelegate.getTodayColor(), PorterDuff.Mode.SRC_IN);
            drawable.draw(canvas);
        }

        final float cx = getWidth()/2f;
        float textY = 0;
        String text = "";

        //날자 그리기
        textY = getHeight() * DAY_WEIGHT / 2 + mDayBaseLine;
        text = String.valueOf(mDay);
        canvas.drawText(text, cx, textY, mDayPaint);

        //요일 그리기
        textY = getHeight() * DAY_WEIGHT + getHeight() * WEEKDAY_WEIGHT / 2 + mWeekDayBaseLine;
        text = Utils.getWeekDayString(getContext(), mWeekDay, false);
        canvas.drawText(text, cx, textY, mWeekDayPaint);

        int eventCount = mEventList.size();

        //일정이 없으면 여기서 끝낸다.
        if(eventCount == 0)
            return;

        //일정개수가 5이하일때
        if(eventCount <= MAX_EVENT_CIRCLE_COUNT){
            int i = 0;
            float circleY = getHeight() * (DAY_WEIGHT + WEEKDAY_WEIGHT) + getHeight() * EVENT_WEIGHT / 2;
            float distance = (getWidth() - mDelegate.getEventCircleRadius() * MAX_EVENT_CIRCLE_COUNT)/(MAX_EVENT_CIRCLE_COUNT + 1);
            float startPos = (getWidth() - mDelegate.getEventCircleRadius() * eventCount - distance * (eventCount - 1)) / 2;

            //순환하면서 일정동그라미를 그린다.
            for (EventManager.OneEvent event : mEventList) {
                EventTypeManager.OneEventType eventType = EventTypeManager.getEventTypeFromId(event.type);
                int color = getResources().getColor(eventType.color, null);

                mEventCirclePaint.setColor(color);
                float circleX = startPos + i * (mDelegate.getEventCircleRadius() + distance) + mDelegate.getEventCircleRadius() / 2;
                canvas.drawCircle(circleX, circleY, mDelegate.getEventCircleRadius(), mEventCirclePaint);
                i ++;
            }
        }

        //일정개수가 5보다 클때
        else {
            textY = getHeight() * (DAY_WEIGHT + WEEKDAY_WEIGHT) + getHeight() * EVENT_WEIGHT / 2 + mEventBaseLine;
            text = getResources().getString(R.string.event_label) + " " + eventCount;
            canvas.drawText(text, cx, textY, mEventPaint);
        }
    }

    public void setup(BigCalendarView.BigCalendarViewDelegate delegate, int year, int month, int day, List<EventManager.OneEvent> eventList) {
        mDelegate = delegate;
        mYear = year;
        mMonth = month;
        mDay = day;
        mEventList = eventList;

        //요일계산
        DateTime dateTime = new DateTime(year, month, day, 0, 0);
        mWeekDay = dateTime.getDayOfWeek() % 7;

        //Paint설정
        mDayPaint.setTextSize(mDelegate.getDaySize());
        mDayPaint.setTextAlign(Paint.Align.CENTER);
        mWeekDayPaint.setTextSize(mDelegate.getWeekDaySize());
        mWeekDayPaint.setTextAlign(Paint.Align.CENTER);
        mEventPaint.setTextSize(mDelegate.getEventSize());
        mEventPaint.setTextAlign(Paint.Align.CENTER);
        mEventCirclePaint.setStyle(Paint.Style.FILL);

        Paint.FontMetrics metrics = mDayPaint.getFontMetrics();
        mDayBaseLine = (metrics.bottom - metrics.top) / 2 - metrics.descent;
        metrics = mWeekDayPaint.getFontMetrics();
        mWeekDayBaseLine = (metrics.bottom - metrics.top) / 2 - metrics.descent;
        metrics = mEventPaint.getFontMetrics();
        mEventBaseLine = (metrics.bottom - metrics.top) / 2 - metrics.descent;

        redraw();
    }

    /**
     * 선택된 날자가 바뀌였을때 배경색을 바꾸고 재그리기 진행
     */
    public void redraw() {
        final Calendar selected = mDelegate.getSelectedDate();  //오늘날자 얻기
        final Calendar today = mDelegate.getCurrentDate();      //선택된 날자얻기

        //선택되였는가?
        mIsSelected = selected.getYear() == mYear && selected.getMonth() == mMonth && selected.getDay() == mDay;
        //오늘인가?
        boolean isToday = today.getYear() == mYear && today.getMonth() == mMonth && today.getDay() == mDay;

        //오늘일때
        if(isToday) {
            //Background의 색갈을 노란색으로 설정한다.
            getBackground().setColorFilter(mDelegate.getTodayFillDayColor(), PorterDuff.Mode.SRC_OVER);

            //글자색 갱신
            mDayPaint.setColor(mDelegate.getDayColor());
            mWeekDayPaint.setColor(mDelegate.getDayColor());
            mEventPaint.setColor(mDelegate.getDayColor());
        }

        //오늘이 아닐때
        else {
            //Background의 색갈을 없앤다.
            getBackground().setColorFilter(null);

            //글자색 갱신
            mDayPaint.setColor(mDelegate.getDayColor());
            mWeekDayPaint.setColor(mDelegate.getWeekDayColor());
            mEventPaint.setColor(mDelegate.getEventColor());
        }

        //재그리기
        invalidate();
    }
}
