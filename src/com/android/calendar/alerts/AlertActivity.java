/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarAlerts;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import androidx.core.content.ContextCompat;

import com.android.calendar.helper.AsyncQueryService;
import com.android.calendar.event.EventInfoActivity;
import com.android.calendar.utils.Utils;
import com.android.calendar.alerts.GlobalDismissManager.AlarmId;

import java.util.LinkedList;
import java.util.List;

import com.android.krcalendar.R;

/**
 * Notification이 한개 혹은 여러개 떠있는 상태에서 기재를 잠금했다가 해제할때 이 Activity가 뜬다.
 * @see R.layout#alert_activity
 */
public class AlertActivity extends Activity implements OnClickListener {
    private static final String TAG = "AlertActivity";

    //일정을 얻기 위한 column 마당들
    private static final String[] PROJECTION = new String[] {
        CalendarAlerts._ID,              // 0
        CalendarAlerts.EVENT_COLOR,      // 1
        CalendarAlerts.TITLE,            // 2
        CalendarAlerts.EVENT_LOCATION,   // 3
        CalendarAlerts.ALL_DAY,          // 4
        CalendarAlerts.BEGIN,            // 5
        CalendarAlerts.END,              // 6
        CalendarAlerts.EVENT_ID,         // 7
        CalendarAlerts.RRULE,            // 8
        CalendarAlerts.HAS_ALARM,        // 9
        CalendarAlerts.STATE,            // 10
        CalendarAlerts.ALARM_TIME,       // 11
    };

    //실지 리용되는 마당들
    public static final int INDEX_ROW_ID = 0;
    public static final int INDEX_EVENT_COLOR = 1;
    public static final int INDEX_TITLE = 2;
    public static final int INDEX_EVENT_LOCATION = 3;
    public static final int INDEX_ALL_DAY = 4;
    public static final int INDEX_BEGIN = 5;
    public static final int INDEX_END = 6;
    public static final int INDEX_EVENT_ID = 7;
    public static final int INDEX_RRULE = 8;
    public static final int INDEX_STATE = 10;

    //Query()함수에 리용되는 Selection, Selection_args
    private static final String SELECTION = CalendarAlerts.STATE + "=?";
    private static final String[] SELECTION_ARGS = new String[] {
        Integer.toString(CalendarAlerts.STATE_FIRED)
    };

    //일정목록을 현시하기 위한 ListView와 Adapter
    private AlertAdapter mAdapter;
    private ListView mListView;

    //Query를 진행하는 Handler, Cursor변수들
    private QueryHandler mQueryHandler;
    private Cursor mCursor;

    //일정항목을 선택하였을때의 동작을 정의하는 listener
    private final OnItemClickListener mViewListener = new OnItemClickListener() {
        @SuppressLint("NewApi")
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long i) {
            AlertActivity alertActivity = AlertActivity.this;
            Cursor cursor = alertActivity.getItemForView(view);

            long alarmId = cursor.getLong(INDEX_ROW_ID);    //Alert Id
            long eventId = cursor.getLong(AlertActivity.INDEX_EVENT_ID);    //Event Id
            long startMillis = cursor.getLong(AlertActivity.INDEX_BEGIN);   //시작시간

            // Mark this alarm as DISMISSED
            dismissAlarm(alarmId, eventId, startMillis);

            // build an intent and task stack to start EventInfoActivity with AllInOneActivity
            // as the parent activity rooted to home.
            long endMillis = cursor.getLong(AlertActivity.INDEX_END);
            Intent eventIntent = AlertUtils.buildEventViewIntent(AlertActivity.this, eventId,
                    startMillis, endMillis);

            TaskStackBuilder.create(AlertActivity.this).addParentStack(EventInfoActivity.class)
                    .addNextIntent(eventIntent).startActivities();

            //Activity를 종료한다.
            alertActivity.finish();
        }
    };

    //`모두 해제`단추
    private Button mDismissAllButton;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.alert_activity);

        //Title bar설정
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_bar);

        //Title bar divider의 색갈을 파란색으로 설정한다.
        int dividerId = getResources().getIdentifier("android:id/titleDivider", null, null);
        View dividerView = findViewById(dividerId);
        if(dividerView != null)
            dividerView.setBackgroundColor(getColor(R.color.common_selected_color));

        //Background는 둥근4각형으로, 화면아래에 떨구어준다.
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.alert_view_bg);
        getWindow().getDecorView().setBackground(drawable);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.gravity = Gravity.BOTTOM;
        lp.y = (int) Utils.convertDpToPixel(5, this);
        getWindow().setAttributes(lp);

        //일정목록 ListView에 적용할 Adpater 객체 생성
        mAdapter = new AlertAdapter(this, R.layout.alert_item);

        //ListView를 얻고 Adapter를 설정하고 일정항목눌림을 처리하기 위한 Click Listener를 추가한다.
        mListView = (ListView) findViewById(R.id.alert_container);
        mListView.setItemsCanFocus(true);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mViewListener);

        //`모두 해제`단추를 얻고 Click사건동작을 추가한다.
        mDismissAllButton = (Button) findViewById(R.id.dismiss_all);
        mDismissAllButton.setOnClickListener(this);

        //처음에는 단추를 disable시켰다가 query가 끝나면 다시 enable 시킨다.
        mDismissAllButton.setEnabled(false);

        //Query Handler 창조
        mQueryHandler = new QueryHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCursor == null) {  //Cursor가 null이면 Query를 시작한다
            Uri uri = CalendarAlerts.CONTENT_URI_BY_INSTANCE;
            mQueryHandler.startQuery(0, null, uri, PROJECTION, SELECTION, SELECTION_ARGS,
                    CalendarContract.CalendarAlerts.DEFAULT_SORT_ORDER);
        } else {    //Cursor 가 null 이 아니면 requery()를 호출한다.
            if (!mCursor.requery()) {
                Log.w(TAG, "Cursor#requery() failed.");
                mCursor.close();
                mCursor = null;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        AlertService.updateAlertNotification(this);

        //Cursor를 비능동시킨다.
        if (mCursor != null) {
            mCursor.deactivate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Cursor를 닫는다.
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    public void onClick(View v) {
        //`모두 해제` 단추의 눌림동작
        if (v == mDismissAllButton) {
            //Notification들을 모두 닫는다.
            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancelAll();

            //Alarm들을 없앤다.
            dismissFiredAlarms();

            //Activity를 종료한다.
            finish();
        }
    }

    private void dismissFiredAlarms() {
        ContentValues values = new ContentValues(1 /* size */);
        values.put(PROJECTION[INDEX_STATE], CalendarAlerts.STATE_DISMISSED);
        String selection = CalendarAlerts.STATE + "=" + CalendarAlerts.STATE_FIRED;
        mQueryHandler.startUpdate(0, null, CalendarAlerts.CONTENT_URI, values,
                selection, null /* selectionArgs */, Utils.NO_DELAY);

        if (mCursor == null) {
            Log.e(TAG, "Unable to globally dismiss all notifications because cursor was null.");
            return;
        }
        if (mCursor.isClosed()) {
            Log.e(TAG, "Unable to globally dismiss all notifications because cursor was closed.");
            return;
        }
        if (!mCursor.moveToFirst()) {
            Log.e(TAG, "Unable to globally dismiss all notifications because cursor was empty.");
            return;
        }

        List<AlarmId> alarmIds = new LinkedList<AlarmId>();
        do {
            long eventId = mCursor.getLong(INDEX_EVENT_ID);
            long eventStart = mCursor.getLong(INDEX_BEGIN);
            alarmIds.add(new AlarmId(eventId, eventStart));
        } while (mCursor.moveToNext());
        initiateGlobalDismiss(alarmIds);
    }

    /**
     * Alarm 을 없앤다.
     * @param id
     * @param eventId 일정 Id
     * @param startTime 시작시간(미리초)
     */
    private void dismissAlarm(long id, long eventId, long startTime) {
        ContentValues values = new ContentValues(1 /* size */);
        values.put(PROJECTION[INDEX_STATE], CalendarAlerts.STATE_DISMISSED);
        String selection = CalendarAlerts._ID + "=" + id;
        mQueryHandler.startUpdate(0, null, CalendarAlerts.CONTENT_URI, values,
                selection, null /* selectionArgs */, Utils.NO_DELAY);

        List<AlarmId> alarmIds = new LinkedList<AlarmId>();
        alarmIds.add(new AlarmId(eventId, startTime));
        initiateGlobalDismiss(alarmIds);
    }

    /**
     * Alarm 들을 없앤다.
     * @param alarmIds Alarm Id 목록
     */
    @SuppressWarnings("unchecked")
    private void initiateGlobalDismiss(List<AlarmId> alarmIds) {
        new AsyncTask<List<AlarmId>, Void, Void>() {
            @Override
            protected Void doInBackground(List<AlarmId>... params) {
                GlobalDismissManager.dismissGlobally(getApplicationContext(), params[0]);
                return null;
            }
        }.execute(alarmIds);
    }

    /**
     * Cursor가 비였을때 Activity를 종료한다.
     */
    void closeActivityIfEmpty() {
        if (mCursor != null && !mCursor.isClosed() && mCursor.getCount() == 0) {
            AlertActivity.this.finish();
        }
    }

    /**
     * Cursor가 Null이거나 비였으면 true를 돌려준다, 그외에는 false를 돌려준다.
     */
    public boolean isEmpty() {
        return mCursor == null || (mCursor.getCount() == 0);
    }

    public Cursor getItemForView(View view) {
        final int index = mListView.getPositionForView(view);
        if (index < 0) {
            return null;
        }
        return (Cursor) mListView.getAdapter().getItem(index);
    }

    private class QueryHandler extends AsyncQueryService {
        public QueryHandler(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            //Query 실행이 끝났을때
            if (isFinishing()) {    //Activity가 종료되고 있으면 Cursor를 닫는다.
                cursor.close();
            } else {    //일정목록들을 Cursor로부터 얻고 `모두 해제`단추를 enable 시킨다.
                mCursor = cursor;
                mAdapter.changeCursor(cursor);
                mListView.setSelection(cursor.getCount() - 1);

                mDismissAllButton.setEnabled(true); //단추를 enable 시킨다.
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
        }
    }
}
