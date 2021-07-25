package com.android.calendar.kr.vertical;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.kr.common.CalendarUtil;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

/**
 * 양식 4
 * 날자별 일정현시 viewpager를 위한 adaper
 */
public class VerticalDayPagerAdapter extends RecyclerView.Adapter<VerticalDayPagerAdapter.ViewHolder> {

    Context mContext;
    VerticalCalendarView.VerticalCalendarViewDelegate mDelegate;

    //최소날자(이것이 첫 페지로 된다.)
    DateTime mMinDate;

    public VerticalDayPagerAdapter(Context context, VerticalCalendarView.VerticalCalendarViewDelegate delegate) {
        mContext = context;
        mDelegate = delegate;
        mMinDate = new DateTime(mDelegate.getMinYear(), 1, 1, 0, 0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.vertical_day_view_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        DateTime date = mMinDate.plusDays(i);
        VerticalDayViewContainer view = (VerticalDayViewContainer) viewHolder.itemView;
        view.setup(mDelegate, date.getYear(), date.getMonthOfYear(), date.getDayOfMonth());
        view.setTag(i);
    }

    @Override
    public int getItemCount() {
        final DateTime minDate, maxDate;
        minDate = new DateTime(mDelegate.getMinYear(), 1, 1, 0, 0);
        maxDate = new DateTime(mDelegate.getMaxYear(), 12, 31, 0, 0);
        return (int) ((maxDate.getMillis() - minDate.getMillis())/ CalendarUtil.ONE_DAY + 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
