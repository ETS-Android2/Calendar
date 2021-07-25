package com.android.calendar.kr.general;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.kr.common.CalendarUtil;
import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

/**
 * 양식 2
 * 날자별 일정현시 viewpager를 위한 adaper
 */
public class GeneralDayPagerAdapter extends RecyclerView.Adapter<GeneralDayPagerAdapter.ViewHolder> {
    Context mContext;
    CalendarViewDelegate mDelegate;

    //최소한계날자(1800.1.1)
    DateTime mMinDate;

    public GeneralDayPagerAdapter(Context context, CalendarViewDelegate delegate) {
        mContext = context;
        mDelegate = delegate;
        mMinDate = new DateTime(mDelegate.getMinYear(), 1, 1, 0, 0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.general_day_view_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        DateTime curDate = mMinDate.plusDays(i);
        GeneralDayViewContainer view = (GeneralDayViewContainer) viewHolder.itemView;
        view.setup(mDelegate, curDate.getYear(), curDate.getMonthOfYear(), curDate.getDayOfMonth());
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
