package com.android.calendar.kr.big;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.android.calendar.kr.common.Calendar;
import com.android.krcalendar.R;

import static com.android.calendar.kr.big.BigCalendarView.DAYS_ONE_PAGE;

/**
 * 양식 3
 * 한개 월 - 월표시부분 - 한개 페지의 content view
 * 4 x 4날자현시를 위한 recyclerview를 가지고 있다.
 */
public class BigMonthViewContainer extends RelativeLayout {
    BigCalendarView.BigCalendarViewDelegate mDelegate;

    private ViewPager2 mOneMonthViewPager;
    private OneMonthPagerAdapter mAdapter;

    public BigMonthViewContainer(Context context) {
        this(context, null);
    }

    public BigMonthViewContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigMonthViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        mOneMonthViewPager = findViewById(R.id.one_month_pager);

        //Overscroll효과 없애기
        View child = mOneMonthViewPager.getChildAt(0);
        if(child instanceof RecyclerView) {
            child.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
    }

    public void setup(BigCalendarView.BigCalendarViewDelegate delegate, int year, int month, int viewWidth, int viewHeight) {
        mDelegate = delegate;

        mAdapter = new OneMonthPagerAdapter(getContext(), delegate, year, month, viewWidth, viewHeight);
        mOneMonthViewPager.setAdapter(mAdapter);

        //선택된 날자로 이행
        Calendar selected = delegate.getSelectedDate();
        if(year == selected.getYear() && month == selected.getMonth()) {
            gotoPosition(selected.getDay(), false);
        }
    }

    /**
     * ViewPager 재그리기
     */
    public void invalidateViewPager() {
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 날자가 있는 페지로 이행한다.
     * @param position 날자
     * @param smoothScroll 유연한 Scroll?
     */
    public void gotoPosition(int position, boolean smoothScroll) {
        if(position < DAYS_ONE_PAGE)
            mOneMonthViewPager.setCurrentItem(0, smoothScroll);
        else
            mOneMonthViewPager.setCurrentItem(1, smoothScroll);
        mAdapter.notifyDataSetChanged();
    }
}
