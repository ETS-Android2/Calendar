package com.android.calendar.kr.year;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.event.EventManager;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;

/**
 * 년보기
 * 년별 월현시를 위한 ViewPager에 리용되는 adapter
 */
public class YearPagerAdapter extends RecyclerView.Adapter<YearPagerAdapter.ViewHolder> {
    Context mContext;

    //최대, 최소 년도
    private final int mMinYear;
    private final int mMaxYear;

    public YearPagerAdapter(Context context, int minYear, int maxYear) {
        mContext = context;
        mMinYear = minYear;
        mMaxYear = maxYear;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        final View v = LayoutInflater.from(mContext).inflate(R.layout.year_view_layout, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        final int year = mMinYear + position;
        YearContainerView v = (YearContainerView) viewHolder.itemView;
        v.setup(year);

        //년의 월별 일정개수들을 얻는다.
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                DateTime startDay = new DateTime(year, 1, 1, 0, 0);
                Integer []integerArr = new Integer[12];
                Arrays.fill(integerArr, 0);

                //해당 년의 일정들을 얻고 월별일정개수들을 계산한다.
                List<EventManager.OneEvent> eventList = EventManager.getEvents(mContext, startDay.getMillis(), EventManager.YEAR);
                int size = eventList.size();
                for (int k = 0; k < size; k ++) {
                    EventManager.OneEvent event = eventList.get(k);
                    final DateTime endDate;

                    if(event.allDay) {
                        endDate = event.endTime.minusDays(1);
                    }
                    else {
                        endDate = event.endTime;
                    }

                    int startMonth, endMonth;
                    if(event.startTime.getYear() < year)
                        startMonth = 0;
                    else {
                        startMonth = event.startTime.getMonthOfYear() - 1;
                    }

                    if(endDate.getYear() > year)
                        endMonth = 11;
                    else {
                        endMonth = endDate.getMonthOfYear() - 1;
                    }

                    for (int l = startMonth; l <= endMonth; l ++) {
                        integerArr[l] ++;
                    }
                }

                v.setEventCountTexts(integerArr);
            }
        };

        //일정개수를 thread를 통해 얻는다.
        Thread thread = new Thread(runnable);
        thread.start();
    }

    @Override
    public int getItemCount() {
        return mMaxYear - mMinYear + 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
