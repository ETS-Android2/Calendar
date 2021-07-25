package com.android.calendar.kr.day;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.utils.Utils;
import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

/**
 * 일보기(양식1)
 * 년별 월현시를 위한 ViewPager에 리용되는 adapter
 */
public class DayPagerAdapter extends RecyclerView.Adapter<DayPagerAdapter.ViewHolder> {
    Context mContext;
    CalendarViewDelegate mDelegate;
    AllInOneActivity mMainActivity;

    public DayPagerAdapter(Context context, CalendarViewDelegate delegate){
        mContext = context;
        mDelegate = delegate;
        mMainActivity = AllInOneActivity.getMainActivity(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        DayViewContainer view = (DayViewContainer) LayoutInflater.from(mContext).inflate(R.layout.day_view_container, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        DayViewContainer view = (DayViewContainer) viewHolder.itemView;

        final int year, month, day;
        DateTime minDate = new DateTime(mDelegate.getMinYear(), mDelegate.getMinYearMonth(), mDelegate.getMinYearDay(), 0, 0);
        DateTime curDate = minDate.plusDays(i);
        year = curDate.getYear();
        month = curDate.getMonthOfYear();
        day = curDate.getDayOfMonth();
        view.setTag(i);
        view.setup(year, month, day, mDelegate);
    }

    @Override
    public int getItemCount() {
        return (int) Utils.daysDiff(mDelegate.getMinYear(), mDelegate.getMinYearMonth(), mDelegate.getMinYearDay(),
                mDelegate.getMaxYear(), mDelegate.getMaxYearMonth(), mDelegate.getMaxYearDay()) + 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
