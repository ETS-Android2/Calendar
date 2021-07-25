package com.android.calendar.kr.vertical;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.calendar.kr.vertical.VerticalCalendarView;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

/**
 * 양식 4
 * 주요일들을 현시하는 view(일-토 2번)
 */
public class VerticalWeekBar extends View implements View.OnClickListener {
    private final float CLICK_OFFSET = getResources().getDimension(R.dimen.day_click_offset);

    VerticalCalendarView.VerticalCalendarViewDelegate mDelegate;
    Paint mPaint = new Paint();
    Paint mWeekendPaint = new Paint();
    float mTextBaseLine;

    //Touch점들을 보관하기 위해 리용된다.
    float mX, mY;
    boolean isClick = false;
    GestureDetector mGestureDetector;

    ExpandWeekBarListener mListener;

    public VerticalWeekBar(Context context) {
        this(context, null);
    }

    public VerticalWeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalWeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnClickListener(this);

        mGestureDetector = new GestureDetector(context, new CustomGestureListener());
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawColor(Utils.getCommonBackgroundColor(getContext()));

        if(mDelegate == null)
            return;

        for (int i = 0; i < 14; i ++) {
            //문자렬얻고 현시
            final int weekDay = i % 7;
            final String weekDayString = Utils.getWeekDayString(getContext(), weekDay, true);
            final float y = mDelegate.getMonthItemHeight() * i + mTextBaseLine;

            canvas.drawText(weekDayString, mDelegate.getMonthItemWidth()/2, y, weekDay == 0? mWeekendPaint : mPaint);
        }
    }

    public void setup(VerticalCalendarView.VerticalCalendarViewDelegate delegate) {
        mDelegate = delegate;

        mPaint.setTextSize(mDelegate.getTextSize());
        mPaint.setColor(mDelegate.getWeekBarColor());
        mPaint.setTextAlign(Paint.Align.CENTER);
        mWeekendPaint.setTextSize(mDelegate.getTextSize());
        mWeekendPaint.setColor(mDelegate.getWeekBarWeekendColor());
        mWeekendPaint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics metrics = mPaint.getFontMetrics();
        mTextBaseLine = mDelegate.getMonthItemHeight() / 2 - metrics.descent + (metrics.bottom - metrics.top) / 2;

        setMeasuredDimension((int) mDelegate.getMonthItemWidth(), getMeasuredHeight());
        invalidate();
    }

    @Override
    public void onClick(View v) {
        if(VerticalCalendarView.VerticalCalendarViewDelegate.isExpanded())
            return;

        if(!isClick)
            return;

        if(mListener != null)
            mListener.onFlingOrClick();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(!VerticalCalendarView.VerticalCalendarViewDelegate.isExpanded())
            mGestureDetector.onTouchEvent(ev);

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

    public void setWeekBarExpandListener(ExpandWeekBarListener listener) {
        mListener = listener;
    }

    public class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 100;
        private static final int SWIPE_THRESHOLD_VELOCITY = 1000;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Fling을 감지한다.
            float xDiff = e2.getX() - e1.getX();
            float yDiff = e2.getY() - e1.getY();

            //Fling값이 턱값을 넘어서면 숨겨진 날자보기를 현시한다.
            if (xDiff > SWIPE_MIN_DISTANCE && Math.abs(xDiff) > Math.abs(yDiff) && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                if(mListener != null)
                    mListener.onFlingOrClick();
                return false;
            }
            return false;
        }
    }

    public interface ExpandWeekBarListener {
        void onFlingOrClick();
    }
}
