package com.android.calendar.kr.day;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.event.EventManager;
import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator;
import me.everything.android.ui.overscroll.adapters.RecyclerViewOverScrollDecorAdapter;

import static com.android.calendar.utils.Utils.CUSTOM_TOUCH_DRAG_MOVE_RATIO_FWD;
import static me.everything.android.ui.overscroll.OverScrollBounceEffectDecoratorBase.DEFAULT_DECELERATE_FACTOR;
import static me.everything.android.ui.overscroll.OverScrollBounceEffectDecoratorBase.DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK;

/**
 * 일보기(양식 1)
 * 1개 날자패지에 해당한 view
 * 월배경화상, 날자, 읍력날자, 일정목록들을 현시한다.
 */
public class DayViewContainer extends FrameLayout {
    //년, 월, 일
    int mYear = -1;
    int mMonth = -1;
    int mDay = -1;

    CalendarViewDelegate mDelegate;
    AllInOneActivity mMainActivity;

    //일정목록에 해당한 RecyclerView와 Adapter
    private RecyclerView mDayEventRecyclerView;
    private DayEventsRecyclerViewAdapter mDayEventsAdapter;

    TextView mNoEventView;  //`일정없음` TextView

    public DayViewContainer(Context context) {
        this(context, null);
    }

    public DayViewContainer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DayViewContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mMainActivity = AllInOneActivity.getMainActivity(context);
    }

    public void setup(int year, int month, int day, CalendarViewDelegate delegate){
        mYear = year;
        mMonth = month;
        mDay = day;
        mDelegate = delegate;
        mDayEventsAdapter.setDate(year, month, day);

        //월보기 - 일보기 전환할때 alpha 값을 0으로 주어 투명하게 만든다.
        if(Utils.isMonthToDayTransition()) {
            DateTime selectedTime = new DateTime(mMainActivity.getCalendarController().getTime());
            if(selectedTime.getYear() == mYear && selectedTime.getMonthOfYear() == mMonth && selectedTime.getDayOfMonth() == mDay)
                setAlpha(0);
        }

        long timeMillis = (new DateTime(mYear, mMonth, mDay, 0, 0)).getMillis();

        //일정목록을 얻는다.
        final List<EventManager.OneEvent> eventList;
        eventList = EventManager.getEvents(getContext(), timeMillis, EventManager.DAY);

        //일정이 없을때
        if(eventList.isEmpty()){
            //`일정없음`을 보여준다
            mDayEventRecyclerView.setVisibility(GONE);
            mNoEventView.setVisibility(VISIBLE);
        }

        //일정이 있을때
        else {
            //일정목록을 보여준다.
            mNoEventView.setVisibility(GONE);
            mDayEventsAdapter.setAdapterData(eventList);
            mDayEventsAdapter.notifyDataSetChanged();
            mDayEventRecyclerView.setVisibility(VISIBLE);
        }
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mDayEventRecyclerView = findViewById(R.id.day_events_view);
        mDayEventsAdapter = new DayEventsRecyclerViewAdapter(getContext(), Collections.emptyList());
        mDayEventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        mDayEventRecyclerView.setAdapter(mDayEventsAdapter);

        mNoEventView = findViewById(R.id.no_event_view);

        //Over scroll bounce 효과를 준다.
        new VerticalOverScrollBounceEffectDecorator(new RecyclerViewOverScrollDecorAdapter(mDayEventRecyclerView),
                CUSTOM_TOUCH_DRAG_MOVE_RATIO_FWD, DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK, DEFAULT_DECELERATE_FACTOR);
    }
}
