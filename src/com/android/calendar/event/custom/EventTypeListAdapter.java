package com.android.calendar.event.custom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.event.EventTypeManager.OneEventType;
import com.android.krcalendar.R;

import java.util.List;

/**
 * 일정편집화면에서 웃부분에서 일정형식선택을 위하여 모든 일정형식을 다 보여주는데 그때 recyclerview에서 이 adapter를 리용한다.
 */
public class EventTypeListAdapter extends RecyclerView.Adapter<EventTypeListAdapter.ViewHolder> {

    Context mContext;
    LayoutInflater mLayoutInflater;
    List<OneEventType> mEventTypeList;

    //일정형식을 click하는것을 감지하는 listener
    EventTypeItemClickListener mListener;

    //구성자
    public EventTypeListAdapter(Context context, EventTypeItemClickListener listener, List<OneEventType> eventTypes){
        mContext = context;
        mListener = listener;
        mLayoutInflater = LayoutInflater.from(context);
        mEventTypeList = eventTypes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        CustomTextView view = (CustomTextView) mLayoutInflater.inflate(R.layout.event_type_simple, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        CustomTextView textView = (CustomTextView) viewHolder.itemView;

        //화상, 색, label들을 설정하고 click사건동작을 추가한다.
        textView.applyFromEventType(mEventTypeList.get(i));
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null)
                    mListener.onClickEventTypeItem(i);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mEventTypeList.size();
    }

    public void setAdapterData(List<OneEventType> eventTypes){
        mEventTypeList = eventTypes;
    }

    /**
     * 일정형식의 click했을때의 호출되는 함수를 가지고 있는 interface
     */
    public interface EventTypeItemClickListener{
        void onClickEventTypeItem(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
