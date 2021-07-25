package com.android.calendar.kr.day;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;
import com.android.krcalendar.R;

import java.util.List;

/**
 * 일보기(양식 1)
 * 일정목록 recyclerview를 위한 adapter
 */
public class DayEventsRecyclerViewAdapter extends RecyclerView.Adapter<DayEventsRecyclerViewAdapter.ViewHolder> {
    LayoutInflater mLayoutInflater;

    CalendarController mController;
    AllInOneActivity mMainActivity;

    //일정목록
    List<EventManager.OneEvent> mEventList;

    //년, 월, 일
    int mYear;
    int mMonth;
    int mDay;

    DayEventsRecyclerViewAdapter(Context context, List<EventManager.OneEvent> eventList){
        mLayoutInflater = LayoutInflater.from(context);
        mEventList = eventList;
        mController = CalendarController.getInstance(context);
        mMainActivity = AllInOneActivity.getMainActivity(context);
    }

    public void setAdapterData(List<EventManager.OneEvent> eventList){
        mEventList = eventList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        DayEventItemView view = (DayEventItemView) mLayoutInflater.inflate(R.layout.day_event_item, viewGroup, false);
        Utils.addCommonTouchListener(view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        DayEventItemView view = (DayEventItemView) viewHolder.itemView;
        view.setDate(mYear, mMonth, mDay);

        EventManager.OneEvent event = mEventList.get(i);
        view.applyFromEventInfo(event);

        view.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mController.sendEventRelatedEvent(CalendarController.EventType.VIEW_EVENT, event.id,
                        event.startTime.getMillis(), event.endTime.getMillis(),
                        -1);
            }
        });
    }

    public void setDate(int year, int month, int day){
        mYear = year;
        mMonth = month;
        mDay = day;
    }

    @Override
    public int getItemCount() {
        return mEventList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
