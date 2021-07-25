package com.android.calendar.kr.vertical;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.krcalendar.R;

/**
 * 양식 4
 * 월별 날자현시 viewpager를 위한 adapter
 */
public class VerticalMonthPagerAdapter extends RecyclerView.Adapter<VerticalMonthPagerAdapter.ViewHolder> {

    private final LayoutInflater mLayoutInflater;
    private final VerticalCalendarView.VerticalCalendarViewDelegate mDelegate;

    public VerticalMonthPagerAdapter(Context context, VerticalCalendarView.VerticalCalendarViewDelegate delegate){
        mDelegate = delegate;
        mLayoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = mLayoutInflater.inflate(R.layout.vertical_month_view_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        //년, 월을 계산한다.
        final int year = i / 12 + mDelegate.getMinYear();
        final int month = i % 12 + 1;

        VerticalMonthViewContainer view = (VerticalMonthViewContainer) viewHolder.itemView;
        view.setup(mDelegate, year, month);
        view.setTag(i);

        if(!VerticalCalendarView.VerticalCalendarViewDelegate.isExpanded())
            view.moveWeekBar(mDelegate.getMonthItemWidth()*3);
        else
            view.moveWeekBar(0);
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
