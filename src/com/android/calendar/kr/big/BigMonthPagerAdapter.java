package com.android.calendar.kr.big;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.krcalendar.R;

/**
 * 양식3
 * 기본 ViewPager에 해당한 Adpater
 * 1개 페지: 월날자, 일정
 */
public class BigMonthPagerAdapter extends RecyclerView.Adapter<BigMonthPagerAdapter.ViewHolder> {
    private final int mViewWidth;   //한개 페지 너비
    private final int mViewHeight;  //한개 페지 높이

    private final Context mContext;
    private final BigCalendarView.BigCalendarViewDelegate mDelegate;

    public BigMonthPagerAdapter(Context context, BigCalendarView.BigCalendarViewDelegate delegate, int viewWidth, int viewHeight) {
        mContext = context;
        mDelegate = delegate;
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.big_month_view_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        //페지위치로부터 년, 월 계산
        final int year = i / 12 + mDelegate.getMinYear();
        final int month = i % 12 + 1;

        BigMonthViewContainer view = (BigMonthViewContainer) viewHolder.itemView;
        view.setup(mDelegate, year, month, mViewWidth, mViewHeight);
        view.setTag(i);
    }

    @Override
    public int getItemCount() {
        return (mDelegate.getMaxYear() - mDelegate.getMinYear() + 1)*12;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
