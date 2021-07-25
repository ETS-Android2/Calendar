package com.android.calendar.kr.big;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import java.util.GregorianCalendar;
import java.util.List;

import static com.android.calendar.kr.big.BigCalendarView.DAYS_ONE_PAGE;
import static com.android.calendar.kr.big.BigCalendarView.DAYS_PER_COLUMN;
import static com.android.calendar.kr.big.BigCalendarView.DAYS_PER_ROW;

/**
 * 양식 3
 * 월날자 현시(4x4)를 위한 RecyclerView에 리용되는 adapter
 */
public class BigMonthMainViewAdapter extends RecyclerView.Adapter<BigMonthMainViewAdapter.ViewHolder> {
    private final BigCalendarView.BigCalendarViewDelegate mDelegate;
    private final Context mContext;

    private final int mViewWidth;   //한개 페지 너비
    private final int mViewHeight;  //한개 페지 높이
    private final int mYear;        //년
    private final int mMonth;       //월
    private final int mPosition;    //페지위치(0 혹은 1)

    //일정목록
    List<EventManager.OneEvent> mEventList;

    public BigMonthMainViewAdapter(Context context, BigCalendarView.BigCalendarViewDelegate delegate, int year, int month, int viewWidth, int viewHeight, int position) {
        mContext = context;
        mDelegate = delegate;
        mYear = year;
        mMonth = month;
        mViewWidth = viewWidth;
        mViewHeight = viewHeight;
        mPosition = position;

        DateTime dateTime = new DateTime(year, month, 1, 0, 0);
        mEventList = EventManager.getEvents(context, dateTime.getMillis(), EventManager.MONTH);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.big_month_item_simple, viewGroup, false);
        GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams) view.getLayoutParams();
        lp.width = mViewWidth / DAYS_PER_ROW;
        lp.height = mViewHeight / DAYS_PER_COLUMN;

        Utils.addCommonTouchListener(view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final int day;
        //우 페지
        if(mPosition == 0)
            day = i + 1;
        //아래 페지
        else
            day = DAYS_ONE_PAGE + i + 1;

        List<EventManager.OneEvent> eventList = EventManager.getEventsFromDate(mEventList, mYear, mMonth, day);
        BigMonthMainItem view = viewHolder.itemView.findViewById(R.id.month_day_content_view);
        view.setup(mDelegate, mYear, mMonth, day, eventList);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BigCalendarSelectListener listener = mDelegate.getCalendarSelectListener();
                if(listener != null) {
                    listener.onCalendarSelect(mYear, mMonth, day, true);
                }
            }
        });

        view.setTag(i);
    }

    @Override
    public int getItemCount() {
        GregorianCalendar curMonth = new GregorianCalendar(mYear, mMonth - 1, 1);
        int dayCount = curMonth.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
        if(mPosition == 0)
            return DAYS_ONE_PAGE;
        return dayCount - DAYS_ONE_PAGE;
    }

    public interface BigCalendarSelectListener {
        void onCalendarSelect(int year, int month, int day, boolean isClick);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
