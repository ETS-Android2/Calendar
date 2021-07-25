/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.calendar.event;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.calendar.activities.AbstractCalendarActivity;
import com.android.calendar.event.CalendarEventModel.ReminderEntry;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;
import com.android.calendar.widgets.EventViewWidgetProvider;
import com.android.calendar.widgets.MonthViewWidgetProvider;
import com.android.krcalendar.R;

import java.util.ArrayList;
import java.util.List;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static com.android.calendar.helper.CalendarController.EXTRA_RECURRENCE_MODIFY_ALL;
import static com.android.calendar.widgets.EventViewWidgetProvider.NEXT_NOT_UPDATE;

/**
 * 일정정보화면
 * 일정정보들을 보여준다.(시간, 반복, 알림, 내용)
 * 기본동작들은 {@link EventInfoFragment} 에서 진행한다.
 */
public class EventInfoActivity extends AbstractCalendarActivity implements CalendarController.EventHandler {
    private static final String TAG = "EventInfoActivity";
    private EventInfoFragment mInfoFragment;
    private static final int HANDLER_KEY = 0;
    CalendarController mController;

    //Activity가 창조될때 그것을 정적변수로 보관해둔다.
    static EventInfoActivity mEventInfoActivity;

    //달력일정이 변하는것을 감지하는 observer
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange) {
            //일정변화가 일어났다는것을 Widget들에 알린다.
            AppWidgetManager man = AppWidgetManager.getInstance(EventInfoActivity.this);
            int[] ids = man.getAppWidgetIds(
                    new ComponentName(EventInfoActivity.this, EventViewWidgetProvider.class));
            Intent updateIntent = new Intent();
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(EventViewWidgetProvider.WIDGET_IDS_KEY, ids);
            updateIntent.putExtra(NEXT_NOT_UPDATE, 0);
            sendBroadcast(updateIntent);

            ids = man.getAppWidgetIds(
                    new ComponentName(EventInfoActivity.this, MonthViewWidgetProvider.class));
            updateIntent = new Intent();
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(MonthViewWidgetProvider.WIDGET_IDS_KEY, ids);
            updateIntent.putExtra(NEXT_NOT_UPDATE, 0);
            sendBroadcast(updateIntent);

            CalendarController.SendEventChangedToMainActivity();
        }
    };
    private long mStartMillis, mEndMillis;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mEventInfoActivity = this;

        /*-- Theme 설정(현재는 Night Theme 만 지원) --*/
        if(Utils.isDayTheme())
            setTheme(R.style.CalendarAppThemeDay);
        else
            setTheme(R.style.CalendarAppThemeNight);

        //Intent 혹은 Bundle로부터 정보들 얻기
        Intent intent = getIntent();
        long eventId = -1;
        ArrayList<ReminderEntry> reminders = null;

        if (icicle != null) {
            eventId = icicle.getLong(EventInfoFragment.BUNDLE_KEY_EVENT_ID);
            mStartMillis = icicle.getLong(EventInfoFragment.BUNDLE_KEY_START_MILLIS);
            mEndMillis = icicle.getLong(EventInfoFragment.BUNDLE_KEY_END_MILLIS);
        } else if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            mStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
            mEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
            Uri data = intent.getData();
            if (data != null) {
                try {
                    List<String> pathSegments = data.getPathSegments();
                    int size = pathSegments.size();
                    if (size > 2 && "EventTime".equals(pathSegments.get(2))) {
                        eventId = Long.parseLong(pathSegments.get(1));
                        if (size > 4) {
                            mStartMillis = Long.parseLong(pathSegments.get(3));
                            mEndMillis = Long.parseLong(pathSegments.get(4));
                        }
                    } else {
                        eventId = Long.parseLong(data.getLastPathSegment());
                    }
                } catch (NumberFormatException e) {
                    if (eventId != -1) {
                        if (mStartMillis == 0 || mEndMillis == 0) {
                            mStartMillis = 0;
                            mEndMillis = 0;
                        }
                    }
                }
            }
        }

        if (eventId == -1) {
            Log.w(TAG, "No event id");
            Toast.makeText(this, R.string.event_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }

        mController = CalendarController.getInstance(this);
        mController.registerFirstEventHandler(HANDLER_KEY, this);

        setContentView(R.layout.simple_frame_layout);

        //Fragment가 이미 존재한다면 그것을 얻는다.
        mInfoFragment = (EventInfoFragment)
                getSupportFragmentManager().findFragmentById(R.id.body_frame);

        //존재하지 않는다면 새로 창조한다.
        if (mInfoFragment == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            mInfoFragment = new EventInfoFragment(eventId, mStartMillis, mEndMillis,
                    reminders);
            ft.replace(R.id.body_frame, mInfoFragment);
            ft.commit();
        }

        getContentResolver().registerContentObserver(CalendarContract.Events.CONTENT_URI,
                true, mObserver);
    }

    public static EventInfoActivity getEventInfoActivity(Context context) {
        if (context instanceof EventInfoActivity) {
            return (EventInfoActivity) context;
        }
        return ((EventInfoActivity) ((ContextWrapper) context).getBaseContext());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // From the Android Dev Guide: "It's important to note that when
        // onNewIntent(Intent) is called, the Activity has not been restarted,
        // so the getIntent() method will still return the Intent that was first
        // received with onCreate(). This is why setIntent(Intent) is called
        // inside onNewIntent(Intent) (just in case you call getIntent() at a
        // later time)."
        setIntent(intent);
    }

    @Override
    protected void onDestroy() {
        getContentResolver().unregisterContentObserver(mObserver);
        mController.deregisterAllEventHandlers();
        CalendarController.removeInstance(this);

        mEventInfoActivity = null;
        super.onDestroy();
    }

    @Override
    public long getSupportedEventTypes() {
        return CalendarController.EventType.EVENT_UPDATED;
    }

    @Override
    public void handleEvent(CalendarController.EventInfo event) {
        if(event.eventType == CalendarController.EventType.EVENT_UPDATED){
            long event_id = event.id;
            //반복일정갱신이 끝났을때 새로창조된 일정의 Id, 시작, 마감시간를 보내주어 UI를 갱신하도록 한다.
            if((event.extraLong & EXTRA_RECURRENCE_MODIFY_ALL) != 0)
                mInfoFragment.onEventsChangedWithId(event_id, event.startTime.toMillis(true),
                        event.endTime.toMillis(true));
            else
                mInfoFragment.onEventsChangedWithId(event_id);
        }
    }

    @Override
    public void eventsChanged() {
    }

    @Override
    public void minuteChanged() {
    }

    public static EventInfoActivity getEventInfoActivity() {
        return mEventInfoActivity;
    }
}
