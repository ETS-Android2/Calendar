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
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.fragment.app.FragmentTransaction;

import com.android.calendar.activities.AbstractCalendarActivity;
import com.android.calendar.event.CalendarEventModel.ReminderEntry;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.helper.CalendarController.EventInfo;
import com.android.calendar.utils.Utils;
import com.android.calendar.widgets.EventViewWidgetProvider;
import com.android.calendar.widgets.MonthViewWidgetProvider;
import com.android.kr_common.Time;
import com.android.krcalendar.R;

import java.util.ArrayList;

import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static com.android.calendar.helper.CalendarController.EVENT_EDIT_ON_LAUNCH;
import static com.android.calendar.widgets.EventViewWidgetProvider.NEXT_NOT_UPDATE;

/**
 * 일정편집화면
 * 일정을 편집/추가할때 펼쳐진다.
 * 기본 동작들은 {@link EditEventFragment}에서 진행된다.
 */
public class EditEventActivity extends AbstractCalendarActivity {
    public static final String EXTRA_EVENT_REMINDERS = "reminders";
    private static final String TAG = "EditEventActivity";
    private static final boolean DEBUG = false;

    //기본화면을 차지하는 Fragment
    private EditEventFragment mEditFragment;

    private ContentResolver mContentResolver;
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        /**
         * 달력일정이 변하였을때 호출된다.
         */
        @Override
        public void onChange(boolean selfChange) {
            //일정변화가 일어났다는것을 Widget들에 알린다.
            AppWidgetManager man = AppWidgetManager.getInstance(EditEventActivity.this);
            int[] ids = man.getAppWidgetIds(
                    new ComponentName(EditEventActivity.this, EventViewWidgetProvider.class));
            Intent updateIntent = new Intent();
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(EventViewWidgetProvider.WIDGET_IDS_KEY, ids);
            updateIntent.putExtra(NEXT_NOT_UPDATE, 0);
            sendBroadcast(updateIntent);

            ids = man.getAppWidgetIds(
                    new ComponentName(EditEventActivity.this, MonthViewWidgetProvider.class));
            updateIntent = new Intent();
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(MonthViewWidgetProvider.WIDGET_IDS_KEY, ids);
            updateIntent.putExtra(NEXT_NOT_UPDATE, 0);
            sendBroadcast(updateIntent);
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if(Utils.isDayTheme())
            setTheme(R.style.CalendarAppThemeDay);
        else
            setTheme(R.style.CalendarAppThemeNight);

        setContentView(R.layout.simple_frame_layout);
        EventInfo eventInfo = getEventInfoFromIntent();
        ArrayList<ReminderEntry> reminders = getReminderEntriesFromIntent();

        //편집/창조인가?
        boolean editMode = getIntent().getBooleanExtra(EVENT_EDIT_ON_LAUNCH, false);

        //Fragment가 이미 존재한다면 그것을 얻는다.
        mEditFragment = (EditEventFragment) getSupportFragmentManager().findFragmentById(R.id.body_frame);

        //존재하지 않는다면 Fragment를 창조한다.
        if (mEditFragment == null) {
            Intent intent = null;
            if (eventInfo.id == -1) {
                intent = getIntent();
            }

            mEditFragment = new EditEventFragment(eventInfo, reminders,
                    intent, editMode);

            mEditFragment.mShowModifyDialogOnLaunch = getIntent().getBooleanExtra(
                    CalendarController.EVENT_EDIT_ON_LAUNCH, false);

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.body_frame, mEditFragment);
            ft.show(mEditFragment);
            ft.commit();
        }

        mContentResolver = getContentResolver();
        mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, mObserver);
    }

    /**
     * @return Intent로부터 보내온 ReminderEntry배렬을 돌려준다.
     */
    @SuppressWarnings("unchecked")
    private ArrayList<ReminderEntry> getReminderEntriesFromIntent() {
        Intent intent = getIntent();
        return (ArrayList<ReminderEntry>) intent.getSerializableExtra(EXTRA_EVENT_REMINDERS);
    }

    /**
     * Intent 로부터 EventInfo 자료를 얻어서 돌려준다
     */
    private EventInfo getEventInfoFromIntent() {
        EventInfo info = new EventInfo();
        Intent intent = getIntent();

        //Event Id얻기
        long eventId = -1;
        Uri data = intent.getData();
        if (data != null) {
            try {
                eventId = Long.parseLong(data.getLastPathSegment());
            } catch (NumberFormatException e) {
                if (DEBUG) {
                    Log.d(TAG, "Create new event");
                }
            }
        }

        //하루종일
        boolean allDay = intent.getBooleanExtra(EXTRA_EVENT_ALL_DAY, false);

        //시작시간, 마감시간
        long begin = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
        long end = intent.getLongExtra(EXTRA_EVENT_END_TIME, -1);
        if (end != -1) {
            info.endTime = new Time();
            if (allDay) {
                info.endTime.timezone = Time.TIMEZONE_UTC;
            }
            info.endTime.set(end);
        }
        if (begin != -1) {
            info.startTime = new Time();
            if (allDay) {
                info.startTime.timezone = Time.TIMEZONE_UTC;
            }
            info.startTime.set(begin);
        }
        info.id = eventId;

        //제목
        info.eventTitle = intent.getStringExtra(Events.TITLE);

        //달력 id
        info.calendarId = intent.getLongExtra(Events.CALENDAR_ID, -1);

        if (allDay) {
            info.extraLong = CalendarController.EXTRA_CREATE_ALL_DAY;
        } else {
            info.extraLong = 0;
        }

        return info;
    }

    //입력칸의 바깥을 touch했을때 focus를 해제해준다
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {

            EditText titleView = mEditFragment.getTitleTextView();
            EditText descriptionView = mEditFragment.getDescriptionTextView();

            View v = getCurrentFocus();
            if(v != titleView && v != descriptionView)
                return super.dispatchTouchEvent(ev);

            //Rect령역 계산
            Rect titleRect = new Rect();
            titleView.getGlobalVisibleRect(titleRect);
            Rect descriptionRect = new Rect();
            descriptionView.getGlobalVisibleRect(descriptionRect);

            int x = (int) ev.getRawX();
            int y = (int) ev.getRawY();
            if(!titleRect.contains(x, y) && !descriptionRect.contains(x, y)) {
                v.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Fragment를 통해 변경된것이 있는가를 검사하고 변경된것이 없을때에만 back동작을 진행한다.
     */
    @Override
    public void onBackPressed(){
        if(mEditFragment != null) {
            if(mEditFragment.onBackPressed())
                super.onBackPressed();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mContentResolver.unregisterContentObserver(mObserver);
    }

    /**
     * @param context
     * @return context로부터 EditEventActivity를 돌려준다.
     */
    public static EditEventActivity getEditEventActivity(Context context) {
        if (context instanceof EditEventActivity) {
            return (EditEventActivity) context;
        }
        return ((EditEventActivity) ((ContextWrapper) context).getBaseContext());
    }
}
