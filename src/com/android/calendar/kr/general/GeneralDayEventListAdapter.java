package com.android.calendar.kr.general;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.calendar.kr.common.CommonDayEventItemView;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;
import com.android.krcalendar.R;

import java.util.List;

/**
 * 양식 2
 * 한개 날자의 일정목록 reyclerview를 위한 adapter
 */
public class GeneralDayEventListAdapter extends RecyclerView.Adapter<GeneralDayEventListAdapter.ViewHolder> {
    //년, 월, 일
    public int mYear, mMonth, mDay;

    Context mContext;
    CalendarController mController;
    CalendarViewDelegate mDelegate;
    AllInOneActivity mMainActivity;

    //일정목록
    List<EventManager.OneEvent> mEventList;

    GeneralDayEventListAdapter(
            int year, int month, int day,
            Context context,
            CalendarViewDelegate delegate,
            List<EventManager.OneEvent> eventList) {
        mYear = year;
        mMonth = month;
        mDay = day;

        mContext = context;
        mController = CalendarController.getInstance(context);
        mMainActivity = AllInOneActivity.getMainActivity(context);
        mDelegate = delegate;
        mEventList = eventList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        CommonDayEventItemView view = (CommonDayEventItemView) LayoutInflater.from(mContext).inflate(R.layout.common_day_event_item, viewGroup, false);
        view.setEventItemBackgroundColor(Utils.getThemeAttribute(mContext, R.attr.common_light_background_color));

        Utils.addCommonTouchListener(view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        EventManager.OneEvent event = mEventList.get(i);
        CommonDayEventItemView view = (CommonDayEventItemView) viewHolder.itemView;
        view.setDate(mYear, mMonth, mDay);
        view.applyFromEventInfo(event);

        view.setMargin(8, 8, 0, 15);
        view.setPaddingStart(10);

        view.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mController.sendEventRelatedEvent(CalendarController.EventType.VIEW_EVENT, event.id,
                        event.startTime.getMillis(), event.endTime.getMillis(),
                        -1);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mEventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
