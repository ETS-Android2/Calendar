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

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.calendar.activities.AbstractCalendarActivity;
import com.android.calendar.event.CalendarEventModel.ReminderEntry;
import com.android.calendar.helper.AsyncQueryService;
import com.android.calendar.persistence.CalendarRepository;
import com.android.calendar.utils.Utils;
import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.EventRecurrence;
import com.android.calendarcommon2.RecurrenceProcessor;
import com.android.calendarcommon2.RecurrenceSet;
import com.android.kr_common.Time;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.TimeZone;

import static com.android.calendar.event.EventTypeManager.APP_EVENT_TYPE_COUNT;

/**
 * 일정 추가/갱신을 진행하기 위한 Helper클라스
 */
public class EditEventHelper {
    private static final String TAG = "EditEventHelper";
    private static final boolean DEBUG = false;

    public static final int UPDATE_EVENT_TOKEN = 1960;
    private static final String NO_EVENT_COLOR = "";

    //일정을 얻기 위한 column 마당들
    public static final String[] EVENT_PROJECTION = new String[] {
            Events._ID, // 0
            Events.TITLE, // 1
            Events.DESCRIPTION, // 2
            Events.EVENT_LOCATION, // 3
            Events.ALL_DAY, // 4
            Events.HAS_ALARM, // 5
            Events.CALENDAR_ID, // 6
            Events.DTSTART, // 7
            Events.DTEND, // 8
            Events.DURATION, // 9
            Events.EVENT_TIMEZONE, // 10
            Events.RRULE, // 11
            Events._SYNC_ID, // 12
            Events.AVAILABILITY, // 13
            Events.ACCESS_LEVEL, // 14
            Events.OWNER_ACCOUNT, // 15
            Events.HAS_ATTENDEE_DATA, // 16
            Events.ORIGINAL_SYNC_ID, // 17
            Events.ORGANIZER, // 18
            Events.GUESTS_CAN_MODIFY, // 19
            Events.ORIGINAL_ID, // 20
            Events.STATUS, // 21
            Events.CALENDAR_COLOR, // 22
            Events.EVENT_COLOR, // 23
            Events.EVENT_COLOR_KEY, // 24
            Events.ACCOUNT_NAME, // 25
            Events.ACCOUNT_TYPE, // 26
    };
    protected static final int EVENT_INDEX_ID = 0;
    protected static final int EVENT_INDEX_TITLE = 1;
    protected static final int EVENT_INDEX_DESCRIPTION = 2;
    protected static final int EVENT_INDEX_EVENT_LOCATION = 3;
    protected static final int EVENT_INDEX_ALL_DAY = 4;
    protected static final int EVENT_INDEX_HAS_ALARM = 5;
    protected static final int EVENT_INDEX_CALENDAR_ID = 6;
    protected static final int EVENT_INDEX_DTSTART = 7;
    protected static final int EVENT_INDEX_DTEND = 8;
    protected static final int EVENT_INDEX_DURATION = 9;
    protected static final int EVENT_INDEX_TIMEZONE = 10;
    protected static final int EVENT_INDEX_RRULE = 11;
    protected static final int EVENT_INDEX_SYNC_ID = 12;
    protected static final int EVENT_INDEX_AVAILABILITY = 13;
    protected static final int EVENT_INDEX_ACCESS_LEVEL = 14;
    protected static final int EVENT_INDEX_OWNER_ACCOUNT = 15;
    protected static final int EVENT_INDEX_HAS_ATTENDEE_DATA = 16;
    protected static final int EVENT_INDEX_ORIGINAL_SYNC_ID = 17;
    protected static final int EVENT_INDEX_ORGANIZER = 18;
    protected static final int EVENT_INDEX_GUESTS_CAN_MODIFY = 19;
    protected static final int EVENT_INDEX_ORIGINAL_ID = 20;
    protected static final int EVENT_INDEX_EVENT_STATUS = 21;
    protected static final int EVENT_INDEX_CALENDAR_COLOR = 22;
    protected static final int EVENT_INDEX_EVENT_COLOR = 23;
    protected static final int EVENT_INDEX_ACCOUNT_NAME = 25;
    protected static final int EVENT_INDEX_ACCOUNT_TYPE = 26;

    //미리알림을 얻기 위한 column 마당들
    public static final String[] REMINDERS_PROJECTION = new String[] {
            Reminders._ID, // 0
            Reminders.MINUTES, // 1
            Reminders.METHOD, // 2
    };
    public static final int REMINDERS_INDEX_MINUTES = 1;
    public static final int REMINDERS_INDEX_METHOD = 2;
    public static final String REMINDERS_WHERE = Reminders.EVENT_ID + "=?";

    //반복일정 편집방식들
    protected static final int MODIFY_UNINITIALIZED = 0;
    protected static final int MODIFY_SELECTED = 1;
    protected static final int MODIFY_ALL_FOLLOWING = 2;
    protected static final int MODIFY_ALL = 3;

    private final AsyncQueryService mService;
    private final Context mContext;

    //달력을 얻기 위한 column 마당들
    static final String[] CALENDARS_PROJECTION = new String[] {
            Calendars._ID, // 0
            Calendars.CALENDAR_DISPLAY_NAME, // 1
            Calendars.OWNER_ACCOUNT, // 2
            Calendars.CALENDAR_COLOR, // 3
            Calendars.CAN_ORGANIZER_RESPOND, // 4
            Calendars.CALENDAR_ACCESS_LEVEL, // 5
            Calendars.VISIBLE, // 6
            Calendars.MAX_REMINDERS, // 7
            Calendars.ALLOWED_REMINDERS, // 8
            Calendars.ALLOWED_ATTENDEE_TYPES, // 9
            Calendars.ALLOWED_AVAILABILITY, // 10
            Calendars.ACCOUNT_NAME, // 11
            Calendars.ACCOUNT_TYPE, //12
    };
    static final int CALENDARS_INDEX_ID = 0;
    static final int CALENDARS_INDEX_DISPLAY_NAME = 1;
    static final int CALENDARS_INDEX_OWNER_ACCOUNT = 2;
    static final int CALENDARS_INDEX_COLOR = 3;
    static final int CALENDARS_INDEX_CAN_ORGANIZER_RESPOND = 4;
    static final int CALENDARS_INDEX_ACCESS_LEVEL = 5;
    static final int CALENDARS_INDEX_MAX_REMINDERS = 7;
    static final int CALENDARS_INDEX_ALLOWED_REMINDERS = 8;
    static final int CALENDARS_INDEX_ALLOWED_ATTENDEE_TYPES = 9;
    static final int CALENDARS_INDEX_ALLOWED_AVAILABILITY = 10;
    static final int CALENDARS_INDEX_ACCOUNT_NAME = 11;
    static final int CALENDARS_INDEX_ACCOUNT_TYPE = 12;

    static final String CALENDARS_WHERE_WRITEABLE_VISIBLE = Calendars.CALENDAR_ACCESS_LEVEL + ">="
            + Calendars.CAL_ACCESS_CONTRIBUTOR + " AND " + Calendars.VISIBLE + "=1";
    static final String CALENDARS_WHERE = Calendars._ID + "=?";

    //달력색을 얻기 위한 column마당들
    static final String[] COLORS_PROJECTION = new String[] {
        Colors._ID, // 0
        Colors.ACCOUNT_NAME,
        Colors.ACCOUNT_TYPE,
        Colors.COLOR, // 1
        Colors.COLOR_KEY // 2
    };

    static final int COLORS_INDEX_ACCOUNT_NAME = 1;
    static final int COLORS_INDEX_ACCOUNT_TYPE = 2;
    static final int COLORS_INDEX_COLOR = 3;
    static final int COLORS_INDEX_COLOR_KEY = 4;

    //참석자들을 얻기 위한 column마당들
    static final String[] ATTENDEES_PROJECTION = new String[] {
            Attendees._ID, // 0
            Attendees.ATTENDEE_NAME, // 1
            Attendees.ATTENDEE_EMAIL, // 2
            Attendees.ATTENDEE_RELATIONSHIP, // 3
            Attendees.ATTENDEE_STATUS, // 4
    };
    static final int ATTENDEES_INDEX_ID = 0;
    static final int ATTENDEES_INDEX_NAME = 1;
    static final int ATTENDEES_INDEX_EMAIL = 2;
    static final int ATTENDEES_INDEX_RELATIONSHIP = 3;
    static final int ATTENDEES_INDEX_STATUS = 4;
    static final String ATTENDEES_WHERE = Attendees.EVENT_ID + "=? AND attendeeEmail IS NOT NULL";

    public EditEventHelper(Context context) {
        mService = ((AbstractCalendarActivity)context).getAsyncQueryService();
        mContext = context;
    }

    /**
     * 일정을 보관한다.
     *
     * @param model 현재의 일정 model
     * @param originalModel 원래의 일정 model
     * @param modifyWhich 반복일정 편집에 대한 편집방식, {@link #MODIFY_SELECTED}, {@link #MODIFY_ALL_FOLLOWING}, {@link #MODIFY_ALL}중의 하나
     * @return 보관이 성공하면 true를, 그렇지 않으면 fakse를 돌려준다.
     */
    public boolean saveEvent(CalendarEventModel model, CalendarEventModel originalModel,
            int modifyWhich, boolean updateEvent) {
        if (DEBUG) {
            Log.d(TAG, "Saving event model: " + model);
        }

        //Model에 null이 들어올때 false를 돌려준다.
        if (model == null) {
            Log.e(TAG, "Attempted to save null model.");
            return false;
        }

        //Model이 유효한 자료가 아니면 false를 돌려준다.
        if (!model.isValid()) {
            Log.e(TAG, "Attempted to save invalid model.");
            return false;
        }

        //현재의 model자료와 원래의 model자료가 같은 일정을 가리키지 않으면 false를 돌려준다.
        if (originalModel != null && !isSameEvent(model, originalModel)) {
            Log.e(TAG, "Attempted to update existing event but models didn't refer to the same "
                    + "event.");
            return false;
        }

        //일정이 변화된것이 없으면 false를 돌려준다.
        if (originalModel != null && model.unchanged(originalModel)) {
            return false;
        }

        //OriginalModel이 비였고 또 현재 Model의 Uri가 비였으면 false를 돌려준다.
        if (model.mUri != null && originalModel == null) {
            Log.e(TAG, "Existing event but no originalModel provided. Aborting save.");
            return false;
        }

        //연산목록을 만든다.
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        int eventIdIndex = -1;

        boolean updateReminders = true; //미리알림보관을 하겠는가?
        boolean forceSaveReminders = false; //강제보관(변경된것이 없어도 무조건 보관)

        //해당 마당들을 보관하기 위한 ContentValues 창조
        ContentValues values = getContentValuesFromModel(model);

        //Uri얻기
        Uri uri = null;
        if (model.mUri != null) {
            uri = Uri.parse(model.mUri);
        }

        //미리알림이 있는가? -> Events.HAS_ALARM
        ArrayList<ReminderEntry> reminders = model.mReminders;
        int len = reminders.size();
        values.put(Events.HAS_ALARM, (len > 0) ? 1 : 0);

        if (uri == null) {  //일정을 창조할때
            //이 마당들은 기정값을 써넣는다.
            values.put(Events.HAS_ATTENDEE_DATA, 1);
            values.put(Events.STATUS, Events.STATUS_CONFIRMED);

            eventIdIndex = ops.size();
            ContentProviderOperation.Builder b = ContentProviderOperation.newInsert(
                    Events.CONTENT_URI).withValues(values);
            ops.add(b.build());
            forceSaveReminders = true;
        }
        //본래 일정도 비반복일정이고 현재의 일정도 비반복일정일때
        else if (TextUtils.isEmpty(model.mRrule) && TextUtils.isEmpty(originalModel.mRrule)) {
            // Simple update to a non-recurring event
            checkTimeDependentFields(originalModel, model, values, modifyWhich);
            ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
        }
        //본래일정은 비반복일정이고 현재 일정은 반복일정일때
        else if (TextUtils.isEmpty(originalModel.mRrule)) {
            ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
        }
        else if (modifyWhich == MODIFY_SELECTED) {  //`현재 일정`갱신, 3단계로 진행한다.
            //반복회수를 가지고 있는가?
            boolean oldRuleHasCount = false;
            //새 반복 Rule
            String newRule = "";

            /*---------------(1) 이전의 일정의 마감시간(혹은 반복회수) 설정-------------*/
            if (!isFirstEventInSeries(model, originalModel)) { //반복계렬에서 이전 일정을 가지고 있을때 = 현재 일정이 반복계렬의 첫 일정이 아닐때
                EventRecurrence recurrence = new EventRecurrence();
                recurrence.parse(originalModel.mRrule);
                oldRuleHasCount = recurrence.count > 0;

                newRule = updatePastEvents(ops, originalModel, model.mOriginalStart);
            } else {    //반복계렬의 첫 일정일때
                boolean isLocal = model.mSyncAccountType.equals(CalendarContract.ACCOUNT_TYPE_LOCAL);
                Uri deleteContentUri = isLocal ? CalendarRepository.asLocalCalendarSyncAdapter(model.mSyncAccountName, Events.CONTENT_URI) : Events.CONTENT_URI;
                Uri deleteUri = ContentUris.withAppendedId(deleteContentUri, model.mId);

                //일정을 삭제한다.
                mService.startDelete(mService.getNextToken(), null, deleteUri, null, null,
                        Utils.NO_DELAY);
            }

            /*-------------(2) 이후의 일정을 창조------------*/
            eventIdIndex = ops.size();

            boolean hasNextEvent = false;   //반복계렬에서 다음 일정을 가지고 있는가? = 현재의 일정이 반복계렬의 마지막일정인가?
            long startEventTime = 0;        //반복계렬의 다음 일정의 시작시간(미리초)

            //다음 일정의 시작시간 얻기
            Time time = new Time(model.mOriginalStart);
            int startDay = Time.getJulianDay(time);
            ContentResolver contentResolver = mContext.getContentResolver();
            Uri.Builder builder = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon();
            ContentUris.appendId(builder, startDay);
            ContentUris.appendId(builder, Long.MAX_VALUE);
            String selection = "(" + CalendarContract.Instances.EVENT_ID +
                    "==" + model.mId +
                    " AND " + CalendarContract.Instances.BEGIN +
                    ">" + model.mOriginalStart +
                    ")";
            @SuppressLint("Recycle") Cursor cursor = contentResolver.query(builder.build(), new String[]{
                            CalendarContract.Instances.BEGIN
                    }, selection,
                    null, "begin asc limit 1");
            if(cursor != null){
                cursor.moveToFirst();
                if(cursor.getCount() >= 1){
                    long millis = cursor.getLong(0);
                    DateTime startEventDate = new DateTime(millis);
                    startEventDate = new DateTime(startEventDate.getYear(), startEventDate.getMonthOfYear(), startEventDate.getDayOfMonth(),
                            startEventDate.getHourOfDay(), startEventDate.getMinuteOfHour());
                    startEventTime = startEventDate.getMillis();
                    hasNextEvent = true;
                }
            }

            if(hasNextEvent){ //다음 일정을 가지고 있을때
                //일정창조를 위해 새 Model을 만든다.
                CalendarEventModel newModel = originalModel;
                newModel.mStart = startEventTime;
                newModel.mUri = null;

                //우의 코드에서 updatePastEvents()를 통해 얻은 새로운 Rule을 새 model의 Rule로 설정한다.
                if(oldRuleHasCount)
                    newModel.mRrule = newRule;

                EventRecurrence eventRecurrence = new EventRecurrence();
                eventRecurrence.parse(newModel.mRrule);
                if(eventRecurrence.count > 0) {
                    //반복회수를 하나 줄여준다.
                    //이것은 현재의 일정이 새로 창조될 반복일정계렬에 속하지 않기때문이다.
                    eventRecurrence.count --;
                    newModel.mRrule = eventRecurrence.toString();
                }

                //일정을 창조한다.
                saveEvent(newModel, null, Utils.MODIFY_ALL, false);
            }

            /*-------------- 현재의 일정 창조 ---------------------*/
            model.mUri = null;
            saveEvent(model, null, Utils.MODIFY_ALL, true);
            updateReminders = false;
        }
        else if (modifyWhich == MODIFY_ALL_FOLLOWING) { //`현재 및 이후`일정 갱신
            if (TextUtils.isEmpty(model.mRrule)) {  //반복일정이 아닐때
                if (isFirstEventInSeries(model, originalModel)) {   //반복계렬의 첫 일정일때
                    ops.add(ContentProviderOperation.newDelete(uri).build());
                } else {   //반복계렬의 첫 일정이 아닐때 = 다음일정이 있을때
                    updatePastEvents(ops, originalModel, model.mOriginalStart);
                }
                eventIdIndex = ops.size();
                values.put(Events.STATUS, originalModel.mEventStatus);
                ops.add(ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(values)
                        .build());
            } else {    //반복일정일때
                if (isFirstEventInSeries(model, originalModel)) {
                    checkTimeDependentFields(originalModel, model, values, modifyWhich);
                    ContentProviderOperation.Builder b = ContentProviderOperation.newUpdate(uri)
                            .withValues(values);
                    ops.add(b.build());
                } else {
                    String newRrule = updatePastEvents(ops, originalModel, model.mOriginalStart);
                    if (model.mRrule.equals(originalModel.mRrule)) {
                        values.put(Events.RRULE, newRrule);
                    }

                    //이후 일정을 창조한다.
                    eventIdIndex = ops.size();
                    values.put(Events.STATUS, originalModel.mEventStatus);
                    ops.add(ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(
                            values).build());
                }
            }
            forceSaveReminders = true;
        }
        else if (modifyWhich == MODIFY_ALL) {   //`모든 일정`갱신
            if (TextUtils.isEmpty(model.mRrule)) {  //반복일정이 아닐때
                /* 일정갱신이 아니라 삭제, 창조를 진행한다.*/
                //삭제를 먼저 한다.
                ops.add(ContentProviderOperation.newDelete(uri).build());

                //새 일정을 창조한다.
                eventIdIndex = ops.size();
                ops.add(ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(values)
                        .build());
                forceSaveReminders = true;
            } else {    //반복일정일때
                checkTimeDependentFields(originalModel, model, values, modifyWhich);
                ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());    //일정갱신
            }
        }

        if(updateReminders) {   //미리알림갱신
            boolean newEvent = (eventIdIndex != -1);
            ArrayList<ReminderEntry> originalReminders;
            if (originalModel != null) {
                originalReminders = originalModel.mReminders;
            } else {
                originalReminders = new ArrayList<ReminderEntry>();
            }

            if (newEvent) { //창조
                saveRemindersWithBackRef(ops, eventIdIndex, reminders, originalReminders,
                        true);
            } else {    //갱신
                long eventId = ContentUris.parseId(uri);
                saveReminders(ops, eventId, reminders, originalReminders, forceSaveReminders);
            }
        }

        final int token;
        if(updateEvent && modifyWhich != MODIFY_SELECTED) { //반복일정편집의 `현재 및 이후 일정` 혹은 `모든 일정`편집일때
            token = UPDATE_EVENT_TOKEN;

            //`모든 일정`편집일때
            if(modifyWhich == MODIFY_ALL && model.mRrule != null && !model.mRrule.isEmpty()) {
                mService.setShowChangedTime(model.mAllDay, model.mStart, model.mEnd);
            } else {
                mService.unsetShowChangedTime();
            }
        }
        else {  //그외의 경우
            token = mService.getNextToken();
        }

        mService.startBatch(token, null, android.provider.CalendarContract.AUTHORITY, ops,
                    Utils.NO_DELAY);

        return true;
    }

    /**
     * 기정의 시작시간 계산
     * @param now 시간(미리초)
     * @return
     */
    protected long constructDefaultStartTime(long now) {
        Time defaultStart = new Time();
        defaultStart.set(now);
        defaultStart.second = 0;
        defaultStart.minute = 30;
        long defaultStartMillis = defaultStart.toMillis(false);
        if (now < defaultStartMillis) {
            return defaultStartMillis;
        } else {
            return defaultStartMillis + 30 * DateUtils.MINUTE_IN_MILLIS;
        }
    }

    /**
     * 마감시간을 기정의 지속시간에 기초하여 계산
     * @param startTime 시작시간(미리초)
     * @param context
     */
    protected long constructDefaultEndTime(long startTime, Context context) {
        return startTime + Utils.getDefaultEventDurationInMillis(context);
    }

    /**
     * 시간관련 마당들을 설정
     * @param originalModel 원래의 Model
     * @param model 현재의 Model
     * @param values ContentValues
     * @param modifyWhich 편집방식
     */
    void checkTimeDependentFields(CalendarEventModel originalModel, CalendarEventModel model,
            ContentValues values, int modifyWhich) {
        long oldBegin = model.mOriginalStart;
        long oldEnd = model.mOriginalEnd;
        boolean oldAllDay = originalModel.mAllDay;
        String oldRrule = originalModel.mRrule;
        String oldTimezone = originalModel.mTimezone;

        long newBegin = model.mStart;
        long newEnd = model.mEnd;
        boolean newAllDay = model.mAllDay;
        String newRrule = model.mRrule;
        String newTimezone = model.mTimezone;

        //시간관련 마당들중에 아무것도 변경된것이 없으면 ContentValues에서 그 마당들을 없애고 끝낸다.
        if (oldBegin == newBegin && oldEnd == newEnd && oldAllDay == newAllDay
                && TextUtils.equals(oldRrule, newRrule)
                && TextUtils.equals(oldTimezone, newTimezone)) {
            values.remove(Events.DTSTART);
            values.remove(Events.DTEND);
            values.remove(Events.DURATION);
            values.remove(Events.ALL_DAY);
            values.remove(Events.RRULE);
            values.remove(Events.EVENT_TIMEZONE);
            return;
        }

        //둘다 비반복일정이면 끝낸다.
        if (TextUtils.isEmpty(oldRrule) || TextUtils.isEmpty(newRrule)) {
            return;
        }

        if (modifyWhich == MODIFY_ALL) {    //`모든 일정`갱신
            Time temp = new Time();
            temp.setToNow();

            //시작시간 계산
            long newStartMillis = originalModel.mStart;
            if (oldBegin != newBegin) {
                long offset = newBegin - oldBegin;
                newStartMillis += offset;
            }
            if (newAllDay) {
                DateTime dateTime = new DateTime(DateTimeZone.UTC).withMillis(newStartMillis).withMillisOfDay(0);
                newStartMillis = dateTime.getMillis();
            }

            //시작시간 갱신
            values.put(Events.DTSTART, newStartMillis);
        }
    }

    /**
     * 반복일정의 마감시간을 설정해주어 반복일정이 그 시간까지 제한되도록 한다.
     * @param ops 연산목록
     * @param originalModel 반복일정 Model
     * @param endTimeMillis 제한시간(미리초)
     * @return 갱신된 반복 Rule 을 돌려준다.
     */
    public String updatePastEvents(ArrayList<ContentProviderOperation> ops,
            CalendarEventModel originalModel, long endTimeMillis) {
        //하루종일, 반복 Rule을 얻는다.
        boolean origAllDay = originalModel.mAllDay;
        String origRrule = originalModel.mRrule;

        //새로 설정될 반복 Rule
        String newRrule = origRrule;

        //반복 Rule 문자렬로부터 EventRecurrence 자료를 얻는다.
        EventRecurrence origRecurrence = new EventRecurrence();
        origRecurrence.parse(origRrule);

        //반복일정계렬의 첫 일정의 시작시간을 얻는다.
        long startTimeMillis = originalModel.mStart;
        Time dtstart = new Time();
        dtstart.timezone = originalModel.mTimezone;
        dtstart.set(startTimeMillis);

        //마당값들을 갱신해주기 위한 ContentValues 를 창조한다.
        ContentValues updateValues = new ContentValues();

        /* 반복회수가 있으면 마감시간을 설정하는것이 아니라 반복회수를 줄여준다*/
        if (origRecurrence.count > 0) { //반복회수가 있을때
            //제한시간까지의 반복일정계렬을 모두 얻는다.
            RecurrenceSet recurSet = new RecurrenceSet(originalModel.mRrule, null, null, null);
            RecurrenceProcessor recurProc = new RecurrenceProcessor();
            long[] recurrences;
            try {
                recurrences = recurProc.expand(dtstart, recurSet, startTimeMillis, endTimeMillis);
            } catch (DateException de) {
                throw new RuntimeException(de); //례외 발생
            }

            //제한시간까지의 반복일정계렬을 얻을수 없다면 례외를 발생
            if (recurrences.length == 0) {
                throw new RuntimeException("can't use this method on first instance");
            }

            //반복회수를 계산된 반복일정계렬의 길이로 설정한다.
            origRecurrence.count = recurrences.length;

            //새로 창조될 일정의 반복회수도 계산해준다.
            EventRecurrence excepRecurrence = new EventRecurrence();
            excepRecurrence.parse(origRrule);
            excepRecurrence.count -= recurrences.length;
            newRrule = excepRecurrence.toString();
        } else {    //반복회수가 없을때
            //마감시간을 계산한다.
            Time untilTime = new Time();
            untilTime.set(endTimeMillis - 1000); //1초를 덜어준다. (제한시간은 포함시키지 말아야 하기때문이다)
            if (origAllDay) { //`하루종일`일정일떼
                //마감시간의 시, 분, 초를 0으로 설정한다.
                DateTime untilDateTime = new DateTime(DateTimeZone.UTC).withMillis(endTimeMillis - 1000);
                untilTime.hour = 0;
                untilTime.minute = 0;
                untilTime.second = 0;
                untilTime.year = untilDateTime.getYear();
                untilTime.month = untilDateTime.getMonthOfYear() - 1;
                untilTime.monthDay = untilDateTime.getDayOfMonth();
                untilTime.normalize(false);

                //시작시간의 시, 분, 초도 0으로 설정한다.
                dtstart.hour = 0;
                dtstart.minute = 0;
                dtstart.second = 0;
                dtstart.allDay = true;
                dtstart.switchTimezone(Time.TIMEZONE_UTC);
            }
            untilTime.switchTimezone(Time.TIMEZONE_UTC);

            //마감시간을 반복 rule에 추가한다.
            origRecurrence.until = untilTime.format2445();
        }

        //ContentValues의 해당한 마당들에 값들을 설정한다.
        updateValues.put(Events.RRULE, origRecurrence.toString());
        updateValues.put(Events.DTSTART, dtstart.normalize(true));

        //연산목록에 추가
        ContentProviderOperation.Builder b =
                ContentProviderOperation.newUpdate(Uri.parse(originalModel.mUri))
                .withValues(updateValues);
        ops.add(b.build());

        //새로운 반복 Rule을 돌려준다.
        return newRrule;
    }

    /**
     * 두 일정 Model이 같은 일정을 가리키고 있는지를 돌려준다.
     * @param model
     * @param originalModel
     */
    public static boolean isSameEvent(CalendarEventModel model, CalendarEventModel originalModel) {
        if (originalModel == null) {
            return true;
        }

        if (model.mCalendarId != originalModel.mCalendarId) {
            return false;
        }
        return model.mId == originalModel.mId;
    }

    /**
     * 미리알림 보관
     * @param ops 연산목록
     * @param eventId 일정의 Id
     * @param reminders 미리알림목록
     * @param originalReminders 본래의 미리알림목록
     * @param forceSave 강제보관, true로 설정되면 변화가 없더라도 다시 갱신을 시킨다.
     * @return 보관이 성공하면 true, 보관이 실패하면 false를 돌려준다.
     */
    public static boolean saveReminders(ArrayList<ContentProviderOperation> ops, long eventId,
            ArrayList<ReminderEntry> reminders, ArrayList<ReminderEntry> originalReminders,
            boolean forceSave) {
        //본래의 미리알림목록과 현재의 미리알림목록이 같으면 자료기지를 갱신시키지 않는다.
        if (reminders.equals(originalReminders) && !forceSave) {
            return false;
        }

        //이미 있던 미리알림을 먼저 삭제한다.
        String where = Reminders.EVENT_ID + "=?";
        String[] args = new String[] {Long.toString(eventId)};
        ContentProviderOperation.Builder b = ContentProviderOperation
                .newDelete(Reminders.CONTENT_URI);
        b.withSelection(where, args);
        ops.add(b.build());

        //새 미리알림들을 추가한다.
        ContentValues values = new ContentValues();
        int len = reminders.size();
        for (int i = 0; i < len; i++) {
            ReminderEntry re = reminders.get(i);

            values.clear();
            values.put(Reminders.MINUTES, re.getMinutes());
            values.put(Reminders.METHOD, re.getMethod());
            values.put(Reminders.EVENT_ID, eventId);
            b = ContentProviderOperation.newInsert(Reminders.CONTENT_URI).withValues(values);
            ops.add(b.build());
        }
        return true;
    }

    /**
     * 미리알림들을 보관한다.
     * @param ops 연산목록
     * @param eventIdIndex 연산인수 {@link ContentProviderOperation.Builder#withValueBackReference}
     * @param forceSave 강제보관, true로 설정되면 변화가 없더라도 다시 갱신을 시킨다.
     * @return 보관이 성공하면 true, 그렇지 못하면 false를 돌려준다.
     */
    public static boolean saveRemindersWithBackRef(ArrayList<ContentProviderOperation> ops,
            int eventIdIndex, ArrayList<ReminderEntry> reminders,
            ArrayList<ReminderEntry> originalReminders, boolean forceSave) {
        //미리알림이 변하지 않았다면 자료기지를 갱신하지 않는다.
        if (reminders.equals(originalReminders) && !forceSave) {
            return false;
        }

        //이미 있는 미리알림들을 모두 삭제한다.
        ContentProviderOperation.Builder b = ContentProviderOperation
                .newDelete(Reminders.CONTENT_URI);
        b.withSelection(Reminders.EVENT_ID + "=?", new String[1]);
        b.withSelectionBackReference(0, eventIdIndex);
        ops.add(b.build());

        ContentValues values = new ContentValues();
        int len = reminders.size();

        //미리알림들을 추가한다.
        for (int i = 0; i < len; i++) {
            ReminderEntry re = reminders.get(i);

            values.clear();
            values.put(Reminders.MINUTES, re.getMinutes());
            values.put(Reminders.METHOD, re.getMethod());
            b = ContentProviderOperation.newInsert(Reminders.CONTENT_URI).withValues(values);
            b.withValueBackReference(Reminders.EVENT_ID, eventIdIndex);
            ops.add(b.build());
        }
        return true;
    }

    /**
     * 반복일정계렬에서 첫번째 일정인지를 돌려준다
     * @param model 현재의 일정 model
     * @param originalModel 반복일정 model
     */
    static boolean isFirstEventInSeries(CalendarEventModel model,
            CalendarEventModel originalModel) {
        return model.mOriginalStart == originalModel.mStart;
    }

    /**
     * 반복일정에 대한 column마당들 설정
     * 일반일정과 달리 반복일정에서는 마감시간({@link Events#DTEND})이 아니라 지속시간({@link Events#DURATION})을 준다.
     * @param values
     * @param model
     */
    void addRecurrenceRule(ContentValues values, CalendarEventModel model) {
        String rrule = model.mRrule;

        //반복 RULE 마당을 설정한다.
        values.put(Events.RRULE, rrule);

        //Model에 설정된 시작시간, 마감시간, 지속시간을 얻는다.
        long end = model.mEnd;
        long start = model.mStart;
        String duration = model.mDuration;

        //DURATION 마당에 설정할 문자렬을 계산한다.
        boolean isAllDay = model.mAllDay;
        if (end >= start) {
            if (isAllDay) { //`하루종일` 일정
                //날자수로 지속시간 계산
                long days = (end - start + DateUtils.DAY_IN_MILLIS - 1)
                        / DateUtils.DAY_IN_MILLIS;
                duration = "P" + days + "D";
            } else {    //`하루종일`이 아닌 일정
                //초수로 지속시간 계산
                long seconds = (end - start) / DateUtils.SECOND_IN_MILLIS;
                duration = "P" + seconds + "S";
            }
        } else if (TextUtils.isEmpty(duration)) {
            //기정값들로 설정한다.
            if (isAllDay) {
                duration = "P1D";
            } else {
                duration = "P3600S";
            }
        }

        //계산된 문자렬을 DURATION 마당값으로 설정한다.
        values.put(Events.DURATION, duration);
        //DTEND 마당은 null로 설정한다.
        values.put(Events.DTEND, (Long) null);
    }

    /**
     * Cursor로부터 Event Model자료 설정
     * @param model 써넣기 하려는 Model변수
     * @param cursor Query()를 통하여 얻은 Cursor
     */
    public static void setModelFromCursor(CalendarEventModel model, Cursor cursor) {
        //Model, Cursor 가 비였는가 검사
        if (model == null || cursor == null || cursor.getCount() < 1) {
            Log.wtf(TAG, "Attempted to build non-existent model or from an incorrect query.");
            return;
        }

        model.clear();
        cursor.moveToFirst();

        /* 마당값들을 하나씩 얻어서 Model에 설정한다. */
        model.mId = cursor.getInt(EVENT_INDEX_ID);
        model.mTitle = cursor.getString(EVENT_INDEX_TITLE);
        model.mDescription = cursor.getString(EVENT_INDEX_DESCRIPTION);
        model.mLocation = cursor.getString(EVENT_INDEX_EVENT_LOCATION);
        model.mAllDay = cursor.getInt(EVENT_INDEX_ALL_DAY) != 0;
        model.mHasAlarm = cursor.getInt(EVENT_INDEX_HAS_ALARM) != 0;
        model.mCalendarId = cursor.getInt(EVENT_INDEX_CALENDAR_ID);
        model.mStart = cursor.getLong(EVENT_INDEX_DTSTART);
        String tz = cursor.getString(EVENT_INDEX_TIMEZONE);
        if (TextUtils.isEmpty(tz)) {
            Log.w(TAG, "Query did not return a timezone for the event.");
            model.mTimezone = TimeZone.getDefault().getID();
        } else {
            model.mTimezone = tz;
        }
        String rRule = cursor.getString(EVENT_INDEX_RRULE);
        model.mRrule = rRule;
        model.mSyncId = cursor.getString(EVENT_INDEX_SYNC_ID);
        model.mSyncAccountName = cursor.getString(EVENT_INDEX_ACCOUNT_NAME);
        model.mSyncAccountType = cursor.getString(EVENT_INDEX_ACCOUNT_TYPE);
        model.mAvailability = cursor.getInt(EVENT_INDEX_AVAILABILITY);
        int accessLevel = cursor.getInt(EVENT_INDEX_ACCESS_LEVEL);
        model.mOwnerAccount = cursor.getString(EVENT_INDEX_OWNER_ACCOUNT);
        model.mHasAttendeeData = cursor.getInt(EVENT_INDEX_HAS_ATTENDEE_DATA) != 0;
        model.mOriginalSyncId = cursor.getString(EVENT_INDEX_ORIGINAL_SYNC_ID);
        model.mOriginalId = cursor.getLong(EVENT_INDEX_ORIGINAL_ID);
        model.mOrganizer = cursor.getString(EVENT_INDEX_ORGANIZER);
        model.mIsOrganizer = model.mOwnerAccount.equalsIgnoreCase(model.mOrganizer);
        model.mGuestsCanModify = cursor.getInt(EVENT_INDEX_GUESTS_CAN_MODIFY) != 0;

        int rawEventColor;
        if (cursor.isNull(EVENT_INDEX_EVENT_COLOR)) {
            rawEventColor = cursor.getInt(EVENT_INDEX_CALENDAR_COLOR);
        } else {
            rawEventColor = cursor.getInt(EVENT_INDEX_EVENT_COLOR);
        }
        model.setEventColor(Utils.getDisplayColorFromColor(rawEventColor));

        //일정형식 설정
        //이것은 Event 마당중에서 Events.EVENT_COLOR(일정색) 을 통해 얻는다
        model.mEventType = EventTypeManager.EVENT_TYPE_DEFAULT;
        if(!cursor.isNull(EVENT_INDEX_EVENT_COLOR)){
            int eventIndexColor = cursor.getInt(EVENT_INDEX_EVENT_COLOR);
            for (int i = 0; i < APP_EVENT_TYPE_COUNT; i ++)
            {
                if(EventTypeManager.APP_EVENT_TYPES[i].id == eventIndexColor){
                    model.mEventType = eventIndexColor;
                    break;
                }
            }
        }

        model.mAccessLevel = accessLevel;
        model.mEventStatus = cursor.getInt(EVENT_INDEX_EVENT_STATUS);

        boolean hasRRule = !TextUtils.isEmpty(rRule);
        if (hasRRule) {
            //반복일정일때는 DURATION을 얻고
            model.mDuration = cursor.getString(EVENT_INDEX_DURATION);
        } else {
            //반복일정이 아닐때는 END를 얻는다.
            model.mEnd = cursor.getLong(EVENT_INDEX_DTEND);
        }
        model.mModelUpdatedWithEventCursor = true;
    }

    /**
     * Cursor로부터 Calendar Model자료 설정
     *
     * @param model 써넣기 하려는 Model변수
     * @param cursor Query()를 통하여 얻은 Cursor
     */
    public static void setModelFromCalendarCursor(CalendarEventModel model, Cursor cursor) {
        /* Model, Cursor이 유효한가를 먼저 검사한다. */
        if (model == null || cursor == null) {
            Log.wtf(TAG, "Attempted to build non-existent model or from an incorrect query.");
            return;
        }
        if (model.mCalendarId == -1) {
            return;
        }
        if (!model.mModelUpdatedWithEventCursor) {
            Log.wtf(TAG,
                    "Can't update model with a Calendar cursor until it has seen an Event cursor.");
            return;
        }

        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (model.mCalendarId != cursor.getInt(CALENDARS_INDEX_ID)) {
                continue;
            }

            /* 마당들을 하나씩 얻어서 Model에 설정한다. */
            model.mOrganizerCanRespond = cursor.getInt(CALENDARS_INDEX_CAN_ORGANIZER_RESPOND) != 0;
            model.mCalendarAccessLevel = cursor.getInt(CALENDARS_INDEX_ACCESS_LEVEL);
            model.mCalendarDisplayName = cursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
            model.setCalendarColor(Utils.getDisplayColorFromColor(
                    cursor.getInt(CALENDARS_INDEX_COLOR)));
            model.mCalendarAccountName = cursor.getString(CALENDARS_INDEX_ACCOUNT_NAME);
            model.mCalendarAccountType = cursor.getString(CALENDARS_INDEX_ACCOUNT_TYPE);
            model.mCalendarMaxReminders = cursor.getInt(CALENDARS_INDEX_MAX_REMINDERS);
            model.mCalendarAllowedReminders = cursor.getString(CALENDARS_INDEX_ALLOWED_REMINDERS);
            model.mCalendarAllowedAttendeeTypes = cursor
                    .getString(CALENDARS_INDEX_ALLOWED_ATTENDEE_TYPES);
            model.mCalendarAllowedAvailability = cursor
                    .getString(CALENDARS_INDEX_ALLOWED_AVAILABILITY);

            return;
        }
    }

    /**
     * 일정을 편집할수 있는지를 돌려준다
     * @param model {@link CalendarEventModel}
     */
    public static boolean canModifyEvent(CalendarEventModel model) {
        return canModifyCalendar(model)
                && (model.mIsOrganizer || model.mGuestsCanModify);
    }

    /**
     * 달력을 편집할수 있는지를 돌려준다.
     * @param model {@link CalendarEventModel}
     */
    public static boolean canModifyCalendar(CalendarEventModel model) {
        return model.mCalendarAccessLevel >= Calendars.CAL_ACCESS_CONTRIBUTOR
                || model.mCalendarId == -1;
    }

    /**
     * 미리알림을 추가할수 있는지를 돌려준다.
     * @param model {@link CalendarEventModel}
     */
    public static boolean canAddReminders(CalendarEventModel model) {
        return model.mCalendarAccessLevel >= Calendars.CAL_ACCESS_READ;
    }

    /**
     * Model자료로부터 ContentValues 써넣는다.
     * @param model {@link CalendarEventModel}
     * @return ContentValues
     */
    ContentValues getContentValuesFromModel(CalendarEventModel model) {
        //일정제목
        String title = model.mTitle;

        //하루종일
        boolean isAllDay = model.mAllDay;

        //Recurrence Rule
        String rrule = model.mRrule;

        //시간대
        String timezone = model.mTimezone;
        if (timezone == null) {
            timezone = TimeZone.getDefault().getID();
        }

        //시작시간, 마감시간을 계산하고 미리초로 얻는다.
        Time startTime = new Time(timezone);
        Time endTime = new Time(timezone);
        startTime.set(model.mStart);
        endTime.set(model.mEnd);
        long startMillis;
        long endMillis;
        long calendarId = model.mCalendarId;
        if (isAllDay) {
            // Reset start and end time, ensure at least 1 day duration, and set
            // the timezone to UTC, as required for all-day events.
            timezone = Time.TIMEZONE_UTC;
            startMillis = model.mStart;
            endMillis = model.mEnd;

            if (endMillis < startMillis + DateUtils.DAY_IN_MILLIS) {
                // EditEventView#fillModelFromUI() should treat this case, but we want to ensure
                // the condition anyway.
                endMillis = startMillis + DateUtils.DAY_IN_MILLIS;
            }
        } else {
            startMillis = startTime.toMillis(true);
            endMillis = endTime.toMillis(true);
        }

        //ContentValues를 구성하고 마당값들을 하나씩 추가한다.
        ContentValues values = new ContentValues();
        values.put(Events.CALENDAR_ID, calendarId);     //달력계정 Id
        values.put(Events.EVENT_TIMEZONE, timezone);    //시간대
        values.put(Events.TITLE, title);                //제목
        values.put(Events.ALL_DAY, isAllDay ? 1 : 0);   //하루종일, 1: 하루종일, 0: 하루종일이 아님
        values.put(Events.DTSTART, startMillis);        //시작시간
        values.put(Events.RRULE, rrule);                //반복 rule

        //반복일정인가 아닌가에 따라 마감시간/지속시간
        if (!TextUtils.isEmpty(rrule)) {
            addRecurrenceRule(values, model);
        } else {
            values.put(Events.DURATION, (String) null);
            values.put(Events.DTEND, endMillis);
        }

        //설명
        if (model.mDescription != null) {
            values.put(Events.DESCRIPTION, model.mDescription.trim());
        } else {
            values.put(Events.DESCRIPTION, (String) null);
        }

        //위치
        if (model.mLocation != null) {
            values.put(Events.EVENT_LOCATION, model.mLocation.trim());
        } else {
            values.put(Events.EVENT_LOCATION, (String) null);
        }

        //AVAILABILITY, HAS_ATTENDEE_DATA, STATUS, EVENT_COLOR_KEY들 설정
        //이 값들은 사용자에 의해 입력되지 않고 기정값들로 설정된다
        values.put(Events.AVAILABILITY, model.mAvailability);
        values.put(Events.HAS_ATTENDEE_DATA, model.mHasAttendeeData ? 1 : 0);
        int accessLevel = model.mAccessLevel;
        values.put(Events.ACCESS_LEVEL, accessLevel);
        values.put(Events.STATUS, model.mEventStatus);
        if (model.isEventColorInitialized()) {
            if (model.getEventColor() == model.getCalendarColor()) {
                values.put(Events.EVENT_COLOR_KEY, NO_EVENT_COLOR);
            } else {
                values.put(Events.EVENT_COLOR_KEY, model.getEventColorKey());
            }
        }

        //일정형식
        values.put(Events.EVENT_COLOR, model.getEventType());
        return values;
    }

    /**
     * Code값을 가진 Runnable 클라스
     * {@link #setDoneCode}를 통해 Code값 설정
     */
    public interface EditDoneRunnable extends Runnable {
        void setDoneCode(int code);
    }
}
