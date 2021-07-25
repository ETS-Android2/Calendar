package com.android.calendar.kr.vertical;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.calendar.kr.common.Calendar;
import com.android.calendar.kr.common.CalendarUtil;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * 양식 4
 * 한개 월 - 월날자들을 현시해주는 view
 */
public class VerticalMonthView extends View implements View.OnClickListener {
    //년, 월
    int mYear, mMonth;

    VerticalCalendarView.VerticalCalendarViewDelegate mDelegate;

    //Paint변수들
    Paint mOtherMonthPaint = new Paint();
    Paint mCurrentMonthPaint = new Paint();
    Paint mWeekendPaint = new Paint();
    Paint mShapePaint = new Paint();
    float mTextBaseLine;
    float mRadius;
    private final float SCHEME_CIRCLE_RADIUS = getContext().getResources().getDimension(R.dimen.scheme_circle_radius);
    private final float SCHEME_END_PADDING = Utils.convertDpToPixel(2, getContext());

    //한달의 날자들
    List<Calendar> mItems;

    //일정목록
    List<EventManager.OneEvent> mEventList = new ArrayList<>();

    //Touch 할때 위치보관하기 위해 리용된다.
    float mX, mY;
    boolean isClick = false;

    CalendarSelectListener mListener;

    private final float CLICK_OFFSET = getResources().getDimension(R.dimen.day_click_offset);

    public VerticalMonthView(Context context) {
        this(context, null);
    }

    public VerticalMonthView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalMonthView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOnClickListener(this);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if(mDelegate == null)
            return;

        for (int i = 0; i < mItems.size(); i ++) {
            int x = i / 14;
            int y = i % 14;
            drawItem(canvas, x, y, mItems.get(i));
        }
    }

    public void drawItem(Canvas canvas, int x, int y, Calendar calendar) {
        final float cx = mDelegate.getMonthItemWidth() * x + mDelegate.getMonthItemWidth()/2;
        final float textY = mDelegate.getMonthItemHeight() * y + mTextBaseLine;
        final float cy = mDelegate.getMonthItemHeight() * y + mDelegate.getMonthItemHeight()/2;

        //날자문자렬 얻기
        String dayString = String.valueOf(calendar.getDay());

        //Text의 크기를 얻는다.
        Rect rect = new Rect();
        mCurrentMonthPaint.getTextBounds(dayString, 0, dayString.length(), rect);

        //날자, 오늘날자에 해당한 밑선을 그린다.
        if(calendar.isCurrentMonth()) {
            if(!calendar.isCurrentDay() && mDelegate.getSelectedDate().equals(calendar)){
                mShapePaint.setColor(mDelegate.getSelectedDayCircleColor());
                canvas.drawCircle(cx, cy, mRadius, mShapePaint);
            }
            canvas.drawText(dayString, cx, textY, mCurrentMonthPaint);

            //오늘날자 밑선(납작한 4각형)을 그린다.
            if(calendar.isCurrentDay()) {
                mShapePaint.setColor(mDelegate.getTodayUnderlineColor());

                float left, top, right, bottom;
                left = mDelegate.getMonthItemWidth() * x + (mDelegate.getMonthItemWidth() - rect.width()) / 2 - Utils.convertDpToPixel(1, getContext());
                right = mDelegate.getMonthItemWidth() * (x + 1);
                top = textY + rect.height() / 2f;
                bottom = top + mDelegate.getTodayUnderlineHeight();
                canvas.drawRect(left, top, right, bottom, mShapePaint);
            }
        }
        else {
            canvas.drawText(dayString, cx, textY, mOtherMonthPaint);
        }

        if(!calendar.isCurrentMonth())
            return;

        ArrayList<EventManager.OneEvent> ev = EventManager.getEventsFromDate(mEventList, calendar.getYear(), calendar.getMonth(), calendar.getDay());
        if(ev.isEmpty())
            return;

        boolean pastEvents = true;
        for (EventManager.OneEvent event:ev){
            //현재 혹은 미래의 일정을 가지고 있으면 푸른색, 그렇지 않으면 회색
            if(!event.pastOrFutureCurrent()){
                pastEvents = false;
                break;
            }
        }

        final float schemeX = mDelegate.getMonthItemWidth() * (x + 1) - SCHEME_CIRCLE_RADIUS - SCHEME_END_PADDING;
        final float schemeY = textY - rect.height()/2f;

        //일정동그라미색갈을 얻는다.
        if(pastEvents) {
            mShapePaint.setColor(getResources().getColor(R.color.colorPastEvent, null));
        }
        else {
            mShapePaint.setColor(getResources().getColor(R.color.colorFutureEvent, null));
        }

        //일정동그라미를 그린다.
        canvas.drawCircle(schemeX, schemeY, SCHEME_CIRCLE_RADIUS, mShapePaint);
    }

    public void setup(VerticalCalendarView.VerticalCalendarViewDelegate delegate, int year, int month) {
        mYear = year;
        mMonth = month;

        mDelegate = delegate;
        mOtherMonthPaint.setTextSize(mDelegate.getTextSize());
        mCurrentMonthPaint.setTextSize(mDelegate.getTextSize());
        mWeekendPaint.setTextSize(mDelegate.getTextSize());
        mOtherMonthPaint.setColor(mDelegate.getOtherMonthColor());
        mCurrentMonthPaint.setColor(mDelegate.getCurrentMonthColor());
        mWeekendPaint.setColor(mDelegate.getMonthWeekendColor());
        mOtherMonthPaint.setTextAlign(Paint.Align.CENTER);
        mCurrentMonthPaint.setTextAlign(Paint.Align.CENTER);
        mWeekendPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics metrics = mCurrentMonthPaint.getFontMetrics();
        mTextBaseLine = mDelegate.getMonthItemHeight() / 2 - metrics.descent + (metrics.bottom - metrics.top) / 2;

        mRadius = mDelegate.getMonthItemHeight()*0.4f;

        //날자들 얻기
        mItems = CalendarUtil.initCalendarForMonthView(mYear, mMonth, mDelegate.getCurrentDate());
        DateTime dateTime = new DateTime(year, month, 1, 0, 0);
        mEventList = EventManager.getEvents(getContext(), dateTime.getMillis(), EventManager.MONTH);

        invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        float x = ev.getX();
        float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isClick = true;
                mX = x;
                mY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if(isClick) {
                    final float dx = x - mX;
                    final float dy = y - mY;
                    isClick = Math.abs(dx) <= CLICK_OFFSET && Math.abs(dy) <= CLICK_OFFSET;
                }
                break;
            case MotionEvent.ACTION_UP:
                mX = x;
                mY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
                isClick = false;
                break;
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public void onClick(View v) {
        if(!isClick)
            return;

        if(mListener == null)
            return;

        Calendar calendar = getIndex();
        if(calendar == null)
            return;

        mListener.onCalendarSelect(calendar.getYear(), calendar.getMonth(), calendar.getDay());
    }

    private Calendar getIndex() {
        int indexX, indexY, index;
        indexX = (int) (mX / mDelegate.getMonthItemWidth());
        indexY = (int) (mY / mDelegate.getMonthItemHeight());
        index = indexX * 14 + indexY;

        if(index >= mItems.size() || index < 0)
            return null;

        return mItems.get(index);
    }

    public void setCalendarSelectListener(CalendarSelectListener listener) {
        mListener = listener;
    }

    public interface CalendarSelectListener {
        void onCalendarSelect(int year, int month, int day);
    }
}
