package com.android.calendar.kr.general;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.event.EventManager;
import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

import java.util.List;

import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator;
import me.everything.android.ui.overscroll.adapters.RecyclerViewOverScrollDecorAdapter;

import static com.android.calendar.utils.Utils.CUSTOM_TOUCH_DRAG_MOVE_RATIO_FWD;
import static me.everything.android.ui.overscroll.OverScrollBounceEffectDecoratorBase.DEFAULT_DECELERATE_FACTOR;
import static me.everything.android.ui.overscroll.OverScrollBounceEffectDecoratorBase.DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK;

/**
 * 양식 2
 * 한개 날자에 해당한 일정부분의 view
 * 일정목록 recyclerview를 가지고 있다.
 */
public class GeneralDayViewContainer extends LinearLayout {
    CalendarViewDelegate mDelegate;

    //자식 view들
    TextView mNoEventView;
    RecyclerView mEventListView;
    GeneralDayEventListAdapter mAdapter;

    public GeneralDayViewContainer(Context context) {
        this(context, null);
    }

    public GeneralDayViewContainer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GeneralDayViewContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mNoEventView = findViewById(R.id.no_event_view);
        mEventListView = findViewById(R.id.event_list_view);
    }

    public void setup(CalendarViewDelegate delegate, int year, int month, int day) {
        mDelegate = delegate;

        DateTime dateTime = new DateTime(year, month, day, 0, 0);
        //일정목록 얻기
        List<EventManager.OneEvent> eventList = EventManager.getEvents(getContext(), dateTime.getMillis(), EventManager.DAY);

        //일정이 없을때
        if(eventList.isEmpty()) {
            mNoEventView.setVisibility(VISIBLE);
        }

        //일정이 있을때
        else {
            mNoEventView.setVisibility(GONE);
        }

        //일정목록 RecyclerView 에 Adapter, LayoutManager 설정
        mAdapter = new GeneralDayEventListAdapter(year, month, day, getContext(), mDelegate, eventList);
        mEventListView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        mEventListView.setAdapter(mAdapter);

        //Over Scroll bounce 효과주기
        new VerticalOverScrollBounceEffectDecorator(new RecyclerViewOverScrollDecorAdapter(mEventListView),
                CUSTOM_TOUCH_DRAG_MOVE_RATIO_FWD, DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK, DEFAULT_DECELERATE_FACTOR);
    }
}
