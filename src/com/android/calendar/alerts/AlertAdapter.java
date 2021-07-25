/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar.alerts;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import com.android.calendar.event.EventTypeManager;
import com.android.kr_common.Time;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.android.calendar.utils.Utils;

import java.util.Locale;
import java.util.TimeZone;

import com.android.krcalendar.R;

import static com.android.calendar.event.EventTypeManager.APP_EVENT_TYPE_COUNT;

/**
 * {@link AlertActivity}의 일정목록 ListView에 적용될 Adapter 클라스
 */
public class AlertAdapter extends ResourceCursorAdapter {

    private final AlertActivity alertActivity;
    private static boolean mFirstTime = true;
    private static int mTitleColor; //제목에 해당한 본문색
    private static int mOtherColor; //제목을 제외한 다른 마당들의 본문색
    private static int mPastEventColor;

    public AlertAdapter(AlertActivity activity, int resource) {
        super(activity, resource, null);
        alertActivity = activity;
    }

    public static void updateView(Context context, View view, int eventType, String eventName, String location,
            long startMillis, long endMillis, boolean allDay) {
        Resources res = context.getResources();

        TextView titleView = view.findViewById(R.id.event_title);
        TextView whenView = view.findViewById(R.id.when);
        TextView whereView = view.findViewById(R.id.where);
        ImageView eventImage = view.findViewById(R.id.event_image);
        if (mFirstTime) {
            mPastEventColor = res.getColor(R.color.alert_past_event, null);
            mTitleColor = res.getColor(R.color.alert_event_title, null);
            mOtherColor = res.getColor(R.color.alert_event_other, null);
            mFirstTime = false;
        }

        //일정화상
        EventTypeManager.OneEventType oneEventType = EventTypeManager.getEventTypeFromId(eventType);
        Drawable drawable = ContextCompat.getDrawable(context, oneEventType.imageResource).mutate();

        drawable.setTint(res.getColor(oneEventType.color, null));
        eventImage.setImageDrawable(drawable);

        if (endMillis < System.currentTimeMillis()) {
            titleView.setTextColor(mPastEventColor);
            whenView.setTextColor(mPastEventColor);
            whereView.setTextColor(mPastEventColor);
        } else {
            titleView.setTextColor(mTitleColor);
            whenView.setTextColor(mOtherColor);
            whereView.setTextColor(mOtherColor);
        }

        //제목
        if (eventName == null || eventName.length() == 0) {
            eventName = res.getString(R.string.no_title_label);
        }
        titleView.setText(eventName);

        //일정기간
        String when;
        int flags;
        String tz = Utils.getTimeZone(context, null);
        if (allDay) {
            flags = DateUtils.FORMAT_UTC | DateUtils.FORMAT_SHOW_WEEKDAY |
                    DateUtils.FORMAT_SHOW_DATE;
            tz = Time.TIMEZONE_UTC;
        } else {
            flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE;
        }
        if (DateFormat.is24HourFormat(context)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }

        Time time = new Time(tz);
        time.set(startMillis);
        boolean isDST = time.isDst != 0;
        StringBuilder sb = new StringBuilder(
                Utils.formatDateRange(context, startMillis, endMillis, flags));
        if (!allDay && !tz.equals(Time.getCurrentTimezone())) {
            sb.append(" ").append(TimeZone.getTimeZone(tz).getDisplayName(
                    isDST, TimeZone.SHORT, Locale.getDefault()));
        }

        when = sb.toString();
        whenView.setText(when);

        //위치
        if (location == null || location.length() == 0) {
            whereView.setVisibility(View.GONE);
        } else {
            whereView.setText(location);
            whereView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        //반복일정이면 반복 아이콘을 보여주고 반복일정이 아니면 반복 아이콘을 숨긴다.
        View repeatContainer = view.findViewById(R.id.repeat_icon);
        String rrule = cursor.getString(AlertActivity.INDEX_RRULE);
        if (!TextUtils.isEmpty(rrule)) {
            repeatContainer.setVisibility(View.VISIBLE);
        } else {
            repeatContainer.setVisibility(View.GONE);
        }

        String eventName = cursor.getString(AlertActivity.INDEX_TITLE);         //일정이름
        int eventColor = cursor.getInt(AlertActivity.INDEX_EVENT_COLOR);        //일정색(이것은 일정형식으로 쓰인다)
        String location = cursor.getString(AlertActivity.INDEX_EVENT_LOCATION); //위치
        long startMillis = cursor.getLong(AlertActivity.INDEX_BEGIN);           //시작시간(미리초)
        long endMillis = cursor.getLong(AlertActivity.INDEX_END);               //마감시간(미리초)
        boolean allDay = cursor.getInt(AlertActivity.INDEX_ALL_DAY) != 0;       //하루종일?
        int eventType = EventTypeManager.EVENT_TYPE_DEFAULT;                    //일정형식
        for (int j = 0; j < APP_EVENT_TYPE_COUNT; j ++){
            if (EventTypeManager.APP_EVENT_TYPES[j].id == eventColor){
                eventType = eventColor;
                break;
            }
        }

        //일정정보에 기초하여 View를 설정한다.
        updateView(context, view, eventType, eventName, location, startMillis, endMillis, allDay);
    }

    @Override
    protected void onContentChanged () {
        super.onContentChanged();

        //Activity를 종료한다.
        alertActivity.closeActivityIfEmpty();
    }
}
