package com.android.calendar.kr.vertical;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.kr.common.CalendarUtil;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;
import com.android.calendar.utils.LunarCoreHelper;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import java.util.List;

/**
 * 양식 4
 * 한개 날자에 해당한 일정부분의 view
 * 날자요일 layout, 일정목록 recyclerview를 가지고 있다.
 */
public class VerticalDayViewContainer extends LinearLayout {
    //자식 View들
    private View mDayLabelView;
    private RecyclerView mEventListView;
    private TextView mDayNumber, mWeekDay, mLunarDay;

    public VerticalDayViewContainer(Context context) {
        this(context, null);
    }

    public VerticalDayViewContainer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalDayViewContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        mDayLabelView = findViewById(R.id.day_label_view);
        mEventListView = findViewById(R.id.event_list_view);
        mDayNumber = findViewById(R.id.day_number);
        mWeekDay = findViewById(R.id.week_day);
        mLunarDay = findViewById(R.id.lunar_day);
    }

    public void setup(VerticalCalendarView.VerticalCalendarViewDelegate delegate, int year, int month, int day) {
        //날자, 요일 label 설정
        DateTime dateTime = new DateTime(year, month, day, 0, 0);
        mDayNumber.setText(String.valueOf(day));
        mWeekDay.setText(Utils.getWeekDayString(getContext(), dateTime.getDayOfWeek(), false));

        int []lunar = LunarCoreHelper.convertSolar2Lunar(day, month, year);
        mLunarDay.setText(CalendarUtil.getLunarDayString(lunar, false, getContext()));

        mDayLabelView.getBackground().mutate().setTint(delegate.getDayViewMainBackgroundColor());

        //일정들을 얻고 adapter에 자료로 보낸다.
        List<EventManager.OneEvent> eventList = EventManager.getEvents(getContext(), dateTime.getMillis(), EventManager.DAY);
        VerticalDayEventListAdapter adapter = new VerticalDayEventListAdapter(year, month, day, getContext(), delegate, eventList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        mEventListView.setLayoutManager(layoutManager);
        mEventListView.setAdapter(adapter);
    }

    public RecyclerView getEventListView() {
        return mEventListView;
    }
}
