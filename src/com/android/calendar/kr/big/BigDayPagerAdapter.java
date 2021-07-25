package com.android.calendar.kr.big;

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
 * 양식 3
 * 일별 날자, 일정현시를 위한 recyclerview에 해당한 adapter
 */
public class BigDayPagerAdapter extends RecyclerView.Adapter<BigDayPagerAdapter.ViewHolder> {

    Context mContext;
    BigCalendarView.BigCalendarViewDelegate mDelegate;

    DateTime mMinDate;
    public BigDayPagerAdapter(Context context, BigCalendarView.BigCalendarViewDelegate delegate) {
        mContext = context;
        mDelegate = delegate;
        mMinDate = new DateTime(mDelegate.getMinYear(), 1, 1, 0, 0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.big_day_view_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        DateTime curDate = mMinDate.plusDays(i);
        BigDayViewContainer view = (BigDayViewContainer) viewHolder.itemView;
        view.setup(mDelegate, curDate.getYear(), curDate.getMonthOfYear(), curDate.getDayOfMonth());
    }

    /**
     * 페지개수
     * @return 최소날자 ~ 최대날자의 날자수차이를 돌려준다.
     */
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
