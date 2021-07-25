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

package com.android.calendar.helper;

import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract.Events;
import android.util.Log;
import android.util.Pair;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.event.EditEventActivity;
import com.android.calendar.event.EventInfoActivity;
import com.android.calendar.utils.Utils;
import com.android.kr_common.Time;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.WeakHashMap;

import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static com.android.calendar.event.EditEventHelper.UPDATE_EVENT_TOKEN;

/**
 * Activity, Fragment 간에 서로 통신을 진행하기 위해 공통적으로 리용하는 관리클라스이다.
 */
public class CalendarController {
    /**
     * @see com.android.calendar.event.EditEventActivity
     * `일정편집/추가` 화면에서 편집인가, 추가인가(Boolean)를 extra자료로 보내기 위한 문자렬상수.
     */
    public static final String EVENT_EDIT_ON_LAUNCH = "editMode";

    /**
     * @see EventInfo#extraLong
     * @see #sendEvent
     * Event를 보낼때 extra 파라메터로 보내기 위한 상수들이다
     */
    public static final long EXTRA_NONE = 0;                            //Extra자료가 없음
    public static final long EXTRA_CREATE_ALL_DAY = 1;                  //`하루종일`일정인가?
    public static final long EXTRA_GOTO_DATE = 1 << 1;                  //`날자로 가기`인가?
    public static final long EXTRA_GOTO_TODAY = 1 << 2;                 //`오늘로 가기`인가?
    public static final long EXTRA_RECURRENCE_MODIFY_ALL = 1 << 3;      //`반복일정 '모두변경'`후에 보내오는 사건인가?

    //Static 성원변수들
    private static final boolean DEBUG = false;                         //Debug Log
    private static final String TAG = "CalendarController";             //Log Tag
    private static final WeakHashMap<Context, WeakReference<CalendarController>> instances =
            new WeakHashMap<>();                                        //CalendarController 객체들을 관리하는 map자료

    //Activity로부터 CalendarController객체를 얻기 위한 식별자 형태로 쓰인다.
    private final Context mContext;

    /**
     * {@link #sendEvent}를 통해 사건을 보냈을때 사건을 받을 handler들을 이 map자료에 보관한다.
     */
    private final LinkedHashMap<Integer,EventHandler> eventHandlers =
            new LinkedHashMap<>(5);

    /**
     * {@link #eventHandlers}를 설정하기 위해 리용되는 변수들
     */
    private final LinkedList<Integer> mToBeRemovedEventHandlers = new LinkedList<Integer>();
    private final LinkedHashMap<Integer, EventHandler> mToBeAddedEventHandlers = new LinkedHashMap<
            Integer, EventHandler>();
    private Pair<Integer, EventHandler> mFirstEventHandler;
    private Pair<Integer, EventHandler> mToBeAddedFirstEventHandler;

    /**
     * {@link #sendEvent}를 통해 받은 시간을 보관해둔다.
     */
    private final Time mTime = new Time();

    //시간대가 변할때 호출되는 runnable
    private final Runnable mUpdateTimezone = new Runnable() {
        @Override
        public void run() {
            mTime.switchTimezone(Utils.getTimeZone(mContext, this));
        }
    };

    /**
     * {@link #sendEvent}가 동시에 여러개 호춝되였을때 순차적으로 실행해주기 위한 counter변수
     */
    private volatile int mDispatchInProgressCounter = 0;

    private int mViewType = -1; //현재의 보기형태
    private long mEventId = -1; //Event Id

    private CalendarController(Context context) {
        //초기화
        mContext = context;
        mUpdateTimezone.run();
        mTime.setToNow();
    }

    /**
     * @param context Activity
     * @return Activity 에 해당한 CalendarController 객체를 돌려준다.
     */
    public static CalendarController getInstance(Context context) {
        synchronized (instances) {
            //Map 자료에서 context에 matching되는 항목을 찾는다.
            CalendarController controller = null;
            WeakReference<CalendarController> weakController = instances.get(context);
            if (weakController != null) {
                controller = weakController.get();
            }

            //찾지 못했다면 새로 창조하고 map에 추가한다.
            if (controller == null) {
                controller = new CalendarController(context);
                instances.put(context, new WeakReference(controller));
            }

            //새로 창조되거나 map자료에 들어있는 CalendarController객체를 돌려준다.
            return controller;
        }
    }

    /**
     * map자료에서 Activity에 해당한 항목을 찾아서 없앤다.
     * @param context Activity
     */
    public static void removeInstance(Context context) {
        instances.remove(context);
    }

    public void sendEventRelatedEvent(long eventType, long eventId, long startMillis,
                                      long endMillis, long selectedMillis) {
        // TODO: pass the real allDay status or at least a status that says we don't know the
        // status and have the receiver query the data.
        // The current use of this method for VIEW_EVENT is by the day view to show an EventInfo
        // so currently the missing allDay status has no effect.
        sendEventRelatedEventWithExtra(eventType, eventId, startMillis, endMillis,
                EXTRA_NONE,
                selectedMillis);
    }

    /**
     * 사건전달
     *
     * @param eventType {@link EventType}중의 하나
     * @param eventId Event Id
     * @param startMillis 시작시간
     * @param endMillis 마감시간
     * @param extraLong All Day, Response와 같은 추가적인 정보들을 담고있는 Flag변수
     * @param selectedMillis 선택된 시간(미리초)
     */
    public void sendEventRelatedEventWithExtra(long eventType, long eventId,
                                               long startMillis, long endMillis, long extraLong, long selectedMillis) {
        sendEventRelatedEventWithExtraWithTitleWithCalendarId(eventType, eventId,
                startMillis, endMillis, extraLong, selectedMillis, null, -1);
    }

    /**
     * 사건전달
     *
     * @param eventType {@link EventType}중의 하나
     * @param eventId Event Id
     * @param startMillis 시작시간
     * @param endMillis 마감시간
     * @param extraLong All Day, Response와 같은 추가적인 정보들을 담고있는 Flag변수
     * @param selectedMillis 선택된 시간(미리초)
     * @param title 일정제목
     * @param calendarId Calendar Id
     */
    public void sendEventRelatedEventWithExtraWithTitleWithCalendarId(long eventType,
                                                                      long eventId, long startMillis, long endMillis, long extraLong,
                                                                      long selectedMillis, String title, long calendarId) {
        EventInfo info = new EventInfo();
        info.eventType = eventType;
        info.id = eventId;
        info.startTime = new Time(Utils.getTimeZone(mContext, mUpdateTimezone));
        info.startTime.set(startMillis);
        if (selectedMillis != -1) {
            info.selectedTime = new Time(Utils.getTimeZone(mContext, mUpdateTimezone));
            info.selectedTime.set(selectedMillis);
        } else {
            info.selectedTime = info.startTime;
        }
        info.endTime = new Time(Utils.getTimeZone(mContext, mUpdateTimezone));
        info.endTime.set(endMillis);
        info.extraLong = extraLong;
        info.eventTitle = title;
        info.calendarId = calendarId;
        this.sendEvent(info);
    }

    /**
     * 사건전달
     *
     * @param eventType {@link EventType}중의 하나
     * @param start     시작시간
     * @param end       마감시간
     * @param eventId   event id
     * @param viewType  {@link ViewType}
     */
    public void sendEvent(long eventType, Time start, Time end, long eventId,
                          int viewType) {
        sendEvent(eventType, start, end, start, eventId, viewType, EXTRA_NONE);
    }

    /**
     * 사건전달
     *
     * @param eventType {@link EventType}중의 하나
     * @param start     시작시간
     * @param end       마감시간
     * @param eventId   event id
     * @param viewType  {@link ViewType}
     * @param extraLong All Day, Response와 같은 추가적인 정보들을 담고있는 Flag변수
     */
    public void sendEvent(long eventType, Time start, Time end, long eventId,
                          int viewType, long extraLong) {
        sendEvent(eventType, start, end, start, eventId, viewType, extraLong);
    }

    public void sendEvent(long eventType, Time start, Time end, Time selected,
                          long eventId, int viewType, long extraLong) {
        EventInfo info = new EventInfo();
        info.eventType = eventType;
        info.startTime = start;
        info.selectedTime = selected;
        info.endTime = end;
        info.id = eventId;
        info.viewType = viewType;
        info.extraLong = extraLong;
        this.sendEvent(info);
    }

    /**
     * 사건전달
     * @param event 일정정보
     */
    public void sendEvent(final EventInfo event) {
        if (event.viewType == ViewType.CURRENT) {
            event.viewType = mViewType;
        } else {
            mViewType = event.viewType;
        }

        if (DEBUG) {
            Log.d(TAG, "vvvvvvvvvvvvvvv");
            Log.d(TAG, "Start  " + (event.startTime == null ? "null" : event.startTime.toString()));
            Log.d(TAG, "End    " + (event.endTime == null ? "null" : event.endTime.toString()));
            Log.d(TAG, "Select " + (event.selectedTime == null ? "null" : event.selectedTime.toString()));
            Log.d(TAG, "mTime  " + (mTime == null ? "null" : mTime.toString()));
        }

        //시작시간 얻기
        long startMillis = 0;
        if (event.startTime != null) {
            startMillis = event.startTime.toMillis(false);
        }

        //시간정보얻기
        if (event.selectedTime != null && event.selectedTime.toMillis(false) > 0) {
            mTime.set(event.selectedTime);
        } else {
            if (startMillis != 0) {
                // selectedTime is not set so set mTime to startTime iff it is not
                // within start and end times
                long mtimeMillis = mTime.toMillis(false);
                if (mtimeMillis < startMillis
                        || (event.endTime != null && mtimeMillis > event.endTime.toMillis(false))) {
                    mTime.set(event.startTime);
                }
            }
            event.selectedTime = mTime;
        }

        //시작시간이 정의 안됬으면 mTime과 같게 준다.
        if (startMillis == 0) {
            event.startTime = mTime;
        }
        if (DEBUG) {
            Log.d(TAG, "Start  " + (event.startTime == null ? "null" : event.startTime.toString()));
            Log.d(TAG, "End    " + (event.endTime == null ? "null" : event.endTime.toString()));
            Log.d(TAG, "Select " + (event.selectedTime == null ? "null" : event.selectedTime.toString()));
            Log.d(TAG, "mTime  " + (mTime == null ? "null" : mTime.toString()));
            Log.d(TAG, "^^^^^^^^^^^^^^^");
        }

        //일정의 id를 얻는다.
        if ((event.eventType & EventType.CREATE_EVENT) != 0) {
            if (event.id > 0) {
                mEventId = event.id;
            } else {
                mEventId = -1;
            }
        }

        boolean handled = false;
        synchronized (this) {
            mDispatchInProgressCounter++;

            if (DEBUG) {
                Log.d(TAG, "sendEvent: Dispatching to " + eventHandlers.size() + " handlers");
            }

            //Event Handler들에 event를 보낸다.
            if (mFirstEventHandler != null) {
                EventHandler handler = mFirstEventHandler.second;
                if (handler != null && (handler.getSupportedEventTypes() & event.eventType) != 0
                        && !mToBeRemovedEventHandlers.contains(mFirstEventHandler.first)) {
                    handler.handleEvent(event);
                    handled = true;
                }
            }
            for (Entry<Integer, EventHandler> entry : eventHandlers.entrySet()) {
                int key = entry.getKey();
                if (mFirstEventHandler != null && key == mFirstEventHandler.first) {
                    continue;
                }
                EventHandler eventHandler = entry.getValue();
                if (eventHandler != null
                        && (eventHandler.getSupportedEventTypes() & event.eventType) != 0) {
                    if (mToBeRemovedEventHandlers.contains(key)) {
                        continue;
                    }
                    eventHandler.handleEvent(event);
                    handled = true;
                }
            }

            mDispatchInProgressCounter--;

            if (mDispatchInProgressCounter == 0) {
                //삭제할 hanlder들을 하나씩 삭제한다.
                if (mToBeRemovedEventHandlers.size() > 0) {
                    for (Integer zombie : mToBeRemovedEventHandlers) {
                        eventHandlers.remove(zombie);
                        if (mFirstEventHandler != null && zombie.equals(mFirstEventHandler.first)) {
                            mFirstEventHandler = null;
                        }
                    }
                    mToBeRemovedEventHandlers.clear();
                }

                //새로운 handler들을 추가
                if (mToBeAddedFirstEventHandler != null) {
                    mFirstEventHandler = mToBeAddedFirstEventHandler;
                    mToBeAddedFirstEventHandler = null;
                }
                if (mToBeAddedEventHandlers.size() > 0) {
                    for (Entry<Integer, EventHandler> food : mToBeAddedEventHandlers.entrySet()) {
                        eventHandlers.put(food.getKey(), food.getValue());
                    }
                }
            }
        }

        if (!handled) { //Event handler가 없어서 event를 보내지 못했을때
            //일정 보기/창조/편집/삭제를 진행한다.
            long endTime = (event.endTime == null) ? -1 : event.endTime.toMillis(false);
            if (event.eventType == EventType.CREATE_EVENT) {
                launchCreateEvent(event.startTime.toMillis(false), endTime,
                        event.extraLong == EXTRA_CREATE_ALL_DAY, event.eventTitle,
                        event.calendarId);
            } else if (event.eventType == EventType.VIEW_EVENT) {
                launchViewEvent(event.id, event.startTime.toMillis(false), endTime);
            }
        }
    }

    /**
     * Event Handler를 추가하거나 이미 있던것을 갱신한다.
     *
     * @param key Event Handler를 식별하는 key (Layout Resource가 될수 있다)
     * @param eventHandler Fragment 혹은 Activity
     */
    public void registerEventHandler(int key, EventHandler eventHandler) {
        synchronized (this) {
            mToBeAddedEventHandlers.clear();
            if (mDispatchInProgressCounter > 0) {
                mToBeAddedEventHandlers.put(key, eventHandler);
            } else {
                eventHandlers.put(key, eventHandler);
            }
        }
    }

    /**
     * 사건을 처음에 받는 EventHandler 등록
     * @param key Event Handler에 해당한 key
     * @param eventHandler EventHandler
     */
    public void registerFirstEventHandler(int key, EventHandler eventHandler) {
        synchronized (this) {
            registerEventHandler(key, eventHandler);
            if (mDispatchInProgressCounter > 0) {
                mToBeAddedFirstEventHandler = new Pair<Integer, EventHandler>(key, eventHandler);
            } else {
                mFirstEventHandler = new Pair<Integer, EventHandler>(key, eventHandler);
            }
        }
    }

    /**
     * Event Handler없애기
     * @param key Event Handler에 해당한 key
     */
    public void deregisterEventHandler(Integer key) {
        synchronized (this) {
            if (mDispatchInProgressCounter > 0) {
                mToBeRemovedEventHandlers.add(key);
            } else {
                eventHandlers.remove(key);
                if (mFirstEventHandler != null && mFirstEventHandler.first.equals(key)) {
                    mFirstEventHandler = null;
                }
            }
        }
    }

    /**
     * 등록되였던 Event Handler들을 모두 없애기
     */
    public void deregisterAllEventHandlers() {
        synchronized (this) {
            if (mDispatchInProgressCounter > 0) {
                mToBeRemovedEventHandlers.addAll(eventHandlers.keySet());
            } else {
                eventHandlers.clear();
                mFirstEventHandler = null;
            }
        }
    }

    /**
     * @return the time that this controller is currently pointed at
     */
    public long getTime() {
        return mTime.toMillis(false);
    }

    /**
     * mTime을 설정한다.
     *
     * @param millisTime 미리초시간
     */
    public void setTime(long millisTime) {
        mTime.set(millisTime);
    }

    /**
     * @return Event Id
     */
    public long getEventId() {
        return mEventId;
    }

    /**
     * Event Id 설정
     * @param eventId Evnet Id
     */
    public void setEventId(long eventId) {
        mEventId = eventId;
    }

    public int getViewType() {
        return mViewType;
    }
    public void setViewType(int viewType) {
        mViewType = viewType;
    }

    /**
     * 일정창조화면을 펼친다
     * @param startMillis 시작시간
     * @param endMillis 마감시간
     * @param allDayEvent 하루종일?
     * @param title 제목
     * @param calendarId 달력계정의 Id
     */
    private void launchCreateEvent(long startMillis, long endMillis, boolean allDayEvent,
                                   String title, long calendarId) {
        Intent intent = generateCreateEventIntent(startMillis, endMillis, allDayEvent, title,
                calendarId);
        mEventId = -1;
        mContext.startActivity(intent);
    }

    /**
     * 일정 창조/편집을 위한 intent 를 만들어서 돌려준다.
     * @param startMillis 시작시간
     * @param endMillis 마감시간
     * @param allDayEvent 하루종일?
     * @param title 제목
     * @param calendarId 달력계정의 Id
     * @return Intent
     */
    public Intent generateCreateEventIntent(long startMillis, long endMillis,
                                            boolean allDayEvent, String title, long calendarId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(mContext, EditEventActivity.class);
        intent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
        intent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
        intent.putExtra(EXTRA_EVENT_ALL_DAY, allDayEvent);
        intent.putExtra(Events.CALENDAR_ID, calendarId);
        intent.putExtra(Events.TITLE, title);
        return intent;
    }

    /**
     * 일정보기화면을 펼친다.
     * @param eventId Event Id
     * @param startMillis 시작시간
     * @param endMillis 마감시간
     */
    public void launchViewEvent(long eventId, long startMillis, long endMillis) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
        intent.setData(eventUri);
        intent.setClass(mContext, AllInOneActivity.class);
        intent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
        intent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
        mContext.startActivity(intent);
    }

    /**
     * 기본화면에 `일정변화`가 일어났다고 알려주기
     */
    public static void SendEventChangedToMainActivity(){
        AllInOneActivity mainActivity = AllInOneActivity.getMainActivity();
        if(mainActivity != null) {
            CalendarController controller = CalendarController.getInstance(mainActivity);
            controller.sendEvent(CalendarController.EventType.EVENTS_CHANGED, null, null, -1, CalendarController.ViewType.CURRENT);
        }
    }

    /**
     * 일정보기화면에 `일정갱신`이 일어났다고 알려주기
     * @param results 결과
     * @param token
     */
    public static void SendEventUpdatedToEventInfoActivity(ContentProviderResult[] results, int token){
        EventInfoActivity activity = EventInfoActivity.getEventInfoActivity();
        if(activity != null && token == UPDATE_EVENT_TOKEN) {
            CalendarController controller = CalendarController.getInstance(activity);
            long event_id;
            if (results.length >= 1 && results[0].uri != null && results[0].uri.getLastPathSegment() != null) {
                event_id = Long.parseLong(Objects.requireNonNull(results[0].uri.getLastPathSegment()));
            } else if (results.length >= 2 && results[1].uri != null && results[1].uri.getLastPathSegment() != null) {
                event_id = Long.parseLong(Objects.requireNonNull(results[1].uri.getLastPathSegment()));
            } else {
                event_id = -1;
            }
            controller.sendEvent(EventType.EVENT_UPDATED, null, null,
                    event_id, CalendarController.ViewType.CURRENT);
        }
    }

    /**
     * 일정보기화면에 `일정갱신`이 일어났다고 알려주기
     * @param results 결과
     * @param token
     * @param allDay 하루종일?
     * @param start 시작시간
     * @param end 마감시간
     */
    public static void SendEventUpdatedToEventInfoActivity(ContentProviderResult[] results, int token,
                                                           boolean allDay, long start, long end) {
        EventInfoActivity activity = EventInfoActivity.getEventInfoActivity();
        if(activity != null) {
            CalendarController controller = CalendarController.getInstance(activity);
            long event_id = 0;

            if(token == UPDATE_EVENT_TOKEN) {
                if (results.length >= 1 && results[0].uri != null && results[0].uri.getLastPathSegment() != null) {
                    event_id = Long.parseLong(Objects.requireNonNull(results[0].uri.getLastPathSegment()));
                } else if (results.length >= 2 && results[1].uri != null && results[1].uri.getLastPathSegment() != null) {
                    event_id = Long.parseLong(Objects.requireNonNull(results[1].uri.getLastPathSegment()));
                } else {
                    event_id = -1;
                }

                DateTime dateTime;
                final Time tStart = new Time();
                final Time tEnd = new Time();

                if(allDay) {
                    dateTime = new DateTime(DateTimeZone.UTC).withMillis(start);
                    tStart.set(dateTime.getDayOfMonth(), dateTime.getMonthOfYear() - 1, dateTime.getYear());

                    dateTime = new DateTime(DateTimeZone.UTC).withMillis(end);
                    tEnd.set(dateTime.getDayOfMonth(), dateTime.getMonthOfYear() - 1, dateTime.getYear());
                }

                else {
                    dateTime = new DateTime(start);
                    tStart.set(0, dateTime.getMinuteOfHour(), dateTime.getHourOfDay(),
                            dateTime.getDayOfMonth(), dateTime.getMonthOfYear() - 1, dateTime.getYear());

                    dateTime = new DateTime(end);
                    tEnd.set(0, dateTime.getMinuteOfHour(), dateTime.getHourOfDay(),
                            dateTime.getDayOfMonth(), dateTime.getMonthOfYear() - 1, dateTime.getYear());
                }

                controller.sendEvent(EventType.EVENT_UPDATED, tStart, tEnd,
                        event_id, CalendarController.ViewType.CURRENT, EXTRA_RECURRENCE_MODIFY_ALL);
            }
        }
    }

    /**
     * 사건을 전달할때의 사건 형태
     */
    public interface EventType {
        //일정창조/편집
        long CREATE_EVENT = 1L;

        //일정보기
        long VIEW_EVENT = 1L << 1;

        //오늘 혹은 어떤 날자로 이행
        long GO_TO = 1L << 2;

        //`일정변화`가 일어났을때
        long EVENTS_CHANGED = 1L << 3;

        //년.월 label을 눌렀을때 년보기로 넘어가기 위한 Event Type
        long LAUNCH_MONTH_PICKER = 1L << 4;

        //Calendar 읽기/쓰기 권한요청이 접수되였을때 UI를 갱신시키기 위한 Event Type
        long CALENDAR_PERMISSION_GRANTED = 1L << 5;

        //일정보기 - 편집후 일정이 갱신되였다는것을 알려주기 위한 Event Type
        long EVENT_UPDATED = 1L << 6;
    }

    /**
     * 보기형식
     * 년/월/일/일정
     */
    public interface ViewType {
        int CURRENT = 0;
        int AGENDA = 1;
        int DAY = 2;
        int MONTH = 3;
        int YEAR = 4;
        int MAX_VALUE = 4;
    }

    /**
     * 모든 Activity/Fragment 에 사건을 전달해주기 위한 interface로 쓰인다.
     */
    public interface EventHandler {
        /**
         * @see EventType
         * @see #handleEvent
         * @return 들어오는 사건들중에서 처리할 event들을 filter해주기 위한 함수
         */
        long getSupportedEventTypes();

        /**
         * {@link #sendEvent}를 통해 어디선가 사건을 전달하면 이 함수에서 그 사건을 받는다.
         * @param event 보내오는 자료
         */
        void handleEvent(EventInfo event);

        /**
         * 달력일정변경이 일어났을때 호출된다.
         */
        void eventsChanged();

        /**
         * 분이 바뀌는 경우에 호출된다.(App이 켜져있을동안은 매 분마다 발생)
         */
        void minuteChanged();
    }

    /**
     * 사건을 전달할때의 사건정보
     */
    public static class EventInfo {
        private static final long ALL_DAY_MASK = 0x100;
        private static final int ATTENDEE_STATUS_NONE_MASK = 0x01;

        public long eventType;  // 사건의 형태
        public int viewType;    // 보기형태
        public long id;         // Event id
        public Time selectedTime;   // 선택된 날자정보

        public Time startTime;  //시작시간
        public Time endTime;    //마감시간

        public String eventTitle;   //제목
        public long calendarId;     //달력계정의 Id

        /**
         * For EventType.VIEW_EVENT:
         * It is the default attendee response and an all day event indicator.
         * Set to Attendees.ATTENDEE_STATUS_NONE, Attendees.ATTENDEE_STATUS_ACCEPTED,
         * Attendees.ATTENDEE_STATUS_DECLINED, or Attendees.ATTENDEE_STATUS_TENTATIVE.
         * To signal the event is an all-day event, "or" ALL_DAY_MASK with the response.
         * Alternatively, use buildViewExtraLong(), getResponse(), and isAllDay().
         * <p/>
         * For EventType.CREATE_EVENT:
         * Set to {@link #EXTRA_CREATE_ALL_DAY} for creating an all-day event.
         * <p/>
         * For EventType.GO_TO:
         * Set to {@link #EXTRA_GOTO_DATE} to consider the date but ignore the time.
         * Set to {@link #EXTRA_GOTO_TODAY} if this is a user request to go to the current time.
         * <p/>
         * For EventType.UPDATE_TITLE:
         * Set formatting flags for Utils.formatDateRange
         */
        public long extraLong;

        /**
         * Extra파라메터정의
         * @param allDay 하루종일?
         * @return Long
         */
        public static long buildViewExtraLong(boolean allDay) {
            long extra = allDay ? ALL_DAY_MASK : 0;
            extra |= ATTENDEE_STATUS_NONE_MASK;

            return extra;
        }

        /**
         * 하루종일 일정에 대한 처리를 진행하는가?
         */
        public boolean isAllDay() {
            if (eventType != EventType.VIEW_EVENT) {
                Log.wtf(TAG, "illegal call to isAllDay , wrong event type " + eventType);
                return false;
            }
            return (extraLong & ALL_DAY_MASK) != 0;
        }
    }
}