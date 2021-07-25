package com.android.calendar.agenda;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

/**
 * 일정목록화면
 * 일정목록 RecyclerView에 설정할 Adapter클라스
 */
public class AgendaListViewAdapter extends ListAdapter<EventManager.OneEvent, AgendaListViewAdapter.ViewHolder> {
    private LayoutInflater mLayoutInflater;
    private CalendarController mController;

    AllInOneActivity mMainActivity;
    RecyclerView mRecyclerView;

    //검색어
    private String mQuery = "";

    //오늘날자(년, 월, 일)
    private int mCurYear, mCurMonth, mCurDay;

    //ListAdapter에 적용할 DiffCallback
    private static final DiffUtil.ItemCallback<EventManager.OneEvent> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<EventManager.OneEvent>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull EventManager.OneEvent oldEvent, @NonNull EventManager.OneEvent newEvent) {
                    return oldEvent.id == newEvent.id;
                }
                @Override
                public boolean areContentsTheSame(
                        @NonNull EventManager.OneEvent oldEvent, @NonNull EventManager.OneEvent newEvent) {
                    return oldEvent.startTime.getMillis() == newEvent.startTime.getMillis() &&
                            oldEvent.title.equals(newEvent.title) &&
                            oldEvent.type == newEvent.type;
                }
            };

    public AgendaListViewAdapter() {
        super(DIFF_CALLBACK);
    }

    public AgendaListViewAdapter(Context context) {
        super(DIFF_CALLBACK);

        mController = CalendarController.getInstance(context);
        mLayoutInflater = LayoutInflater.from(context);
        mMainActivity = AllInOneActivity.getMainActivity(context);

        //오늘 날자 얻기
        DateTime dateTime = DateTime.now();
        mCurYear = dateTime.getYear();
        mCurMonth = dateTime.getMonthOfYear();
        mCurDay = dateTime.getDayOfMonth();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        AgendaItemView view = (AgendaItemView) mLayoutInflater.inflate(R.layout.agenda_event_item, viewGroup, false);

        //여러개의 항목이 동시에 눌리우는것을 방지하기 위해
        //Touch사건을 처리해주었다.
        Utils.addCommonTouchListener(view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        AgendaItemView view = (AgendaItemView) viewHolder.itemView;
        EventManager.OneEvent event = getItem(position);
        view.applyFromEventInfo(event, event.startDateEquals(mCurYear, mCurMonth, mCurDay));

        //일정을 하나 눌렀을때
        view.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //일정정보화면으로 이행한다.
                mController.sendEventRelatedEvent(CalendarController.EventType.VIEW_EVENT, event.id,
                        event.startTime.getMillis(), event.endTime.getMillis(),
                        -1);
            }
        });
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        AgendaItemView itemView = (AgendaItemView) holder.itemView;
        if(itemView.containsQuery(mQuery))
            itemView.highLightQueryText(mQuery);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView){
        mRecyclerView = recyclerView;
    }

    /**
     * 검색어 설정
     * @param query 검색어
     */
    public void setQuery(String query){
        mQuery = query;
    }

    /**
     * 매 자식들을 순환하면서 제목들을 갱신한다.
     */
    void redrawChildren() {
        int childCount = mRecyclerView.getChildCount();
        for (int i = 0; i < childCount; i ++) {
            AgendaItemView itemView = (AgendaItemView) mRecyclerView.getChildAt(i);
            if(itemView.containsQuery(mQuery))
                itemView.highLightQueryText(mQuery);
        }
    }
    /**
     * @param dateTime 날자
     * @return 입력날자가 들어있는 첫 일정항목의 위치를 돌려준다
     */
    int getDayPosition(DateTime dateTime){
        long curDayMillis = dateTime.getMillis();
        long nextDayMillis = dateTime.plusDays(1).getMillis();
        int  nItemCount = getItemCount();

        for (int i = 0; i < nItemCount; i ++){
            EventManager.OneEvent event = getItem(i);
            final long startMillis, endMillis;
            if(event.allDay){
                startMillis = new DateTime(event.startTime.getYear(), event.startTime.getMonthOfYear(), event.startTime.getDayOfMonth(), 0, 0).getMillis();
                endMillis = new DateTime(event.endTime.getYear(), event.endTime.getMonthOfYear(), event.endTime.getDayOfMonth(), 0, 0).getMillis() - 1;
            }
            else{
                startMillis = event.startTime.getMillis();
                endMillis = event.endTime.getMillis();
            }

            if((startMillis >= curDayMillis && startMillis < nextDayMillis) ||
                    (endMillis > curDayMillis && endMillis < nextDayMillis)){
                return i;
            }

            if(nextDayMillis <= startMillis)
                return i;
        }

        return nItemCount - 1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
