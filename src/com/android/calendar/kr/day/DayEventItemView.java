package com.android.calendar.kr.day;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.android.calendar.event.EventTypeManager;
import com.android.calendar.event.EventManager;
import com.android.krcalendar.R;

import java.util.Objects;

/**
 * 일보기(양식 1)
 * 한개 일정현시를 위한 view
 * 일정화상, 일정이름, 일정기간을 현시한다.
 */
public class DayEventItemView extends LinearLayout {
    //자식 View들
    ImageView mImage;
    TextView mTitle;
    TextView mWhen;

    //일정정보
    EventManager.OneEvent mEvent;

    //년, 월, 일
    int mYear;
    int mMonth;
    int mDay;

    public DayEventItemView(Context context) {
        this(context, null);
    }

    public DayEventItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DayEventItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mImage = findViewById(R.id.event_image);
        mTitle = findViewById(R.id.event_title);
        mWhen = findViewById(R.id.event_time);
    }

    public void setDate(int year, int month, int day){
        mYear = year;
        mMonth = month;
        mDay = day;
    }

    public void applyFromEventInfo(EventManager.OneEvent event){
        mEvent = event;

        int eventTypeId = event.type;
        EventTypeManager.OneEventType eventType = EventTypeManager.getEventTypeFromId(eventTypeId);

        //일정화상
        Resources resources = getResources();
        Drawable drawable = Objects.requireNonNull(ResourcesCompat.getDrawable(resources, eventType.imageResource, null)).mutate();
        drawable.setTint(resources.getColor(eventType.color, null));
        mImage.setImageDrawable(drawable);

        //일정제목
        if(event.title == null || event.title.isEmpty())
            mTitle.setText(R.string.no_title_label);
        else
            mTitle.setText(event.title);

        //일정기간
        String whenString = event.getHourMinuteString(getContext(), mYear, mMonth, mDay, true);
        mWhen.setText(whenString);
    }
}
