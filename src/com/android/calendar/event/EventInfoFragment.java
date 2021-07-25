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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;

import com.android.calendar.event.CalendarEventModel.ReminderEntry;
import com.android.calendar.helper.AsyncQueryService;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.helper.CalendarController.EventInfo;
import com.android.calendar.helper.CalendarController.EventType;
import com.android.calendar.recurrencepicker.EventRecurrenceFormatter;
import com.android.calendar.utils.Utils;
import com.android.calendarcommon2.DateException;
import com.android.calendarcommon2.Duration;
import com.android.calendarcommon2.EventRecurrence;
import com.android.kr_common.Time;
import com.android.krcalendar.R;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static com.android.calendar.helper.CalendarController.EVENT_EDIT_ON_LAUNCH;

/**
 * 일정보기화면 Fragment
 */
public class EventInfoFragment extends DialogFragment implements
        CalendarController.EventHandler {

    public static final boolean DEBUG = false;
    public static final String TAG = "EventInfoFragment";

    //Activity가 재창조될때 상태들을 복귀해주기 위한 Key상수값들
    public static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    public static final String BUNDLE_KEY_START_MILLIS = "key_start_millis";
    public static final String BUNDLE_KEY_END_MILLIS = "key_end_millis";
    public static final String BUNDLE_KEY_DELETE_DIALOG_VISIBLE = "key_delete_dialog_visible";

    //달력계정정보를 얻기 위한 Column 마당들
    static final String[] CALENDARS_PROJECTION = new String[]{
            Calendars._ID,                      // 0
            Calendars.CALENDAR_DISPLAY_NAME,    // 1
            Calendars.OWNER_ACCOUNT,            // 2
    };
    //우의 마당들에서 필요한 column들
    static final int CALENDARS_INDEX_DISPLAY_NAME = 1;
    static final int CALENDARS_INDEX_OWNER_ACCOUNT = 2;

    //Selection 파라메터로 설정할
    static final String CALENDARS_WHERE = Calendars._ID + "=?";
    static final String CALENDARS_DUPLICATE_NAME_WHERE = Calendars.CALENDAR_DISPLAY_NAME + "=?";
    static final String CALENDARS_VISIBLE_WHERE = Calendars.VISIBLE + "=?";

    //QueryHandler 에서 리용할 Token 값들
    private static final int TOKEN_QUERY_EVENT = 1;
    private static final int TOKEN_QUERY_CALENDARS = 1 << 1;
    private static final int TOKEN_QUERY_DUPLICATE_CALENDARS = 1 << 2;
    private static final int TOKEN_QUERY_REMINDERS = 1 << 3;
    private static final int TOKEN_QUERY_VISIBLE_CALENDARS = 1 << 4;
    private static final int TOKEN_QUERY_COLORS = 1 << 5;
    private static final int TOKEN_QUERY_ALL = TOKEN_QUERY_DUPLICATE_CALENDARS
            | TOKEN_QUERY_CALENDARS | TOKEN_QUERY_EVENT
            | TOKEN_QUERY_REMINDERS | TOKEN_QUERY_VISIBLE_CALENDARS | TOKEN_QUERY_COLORS;

    //달력일정들을 얻기 위한 column마당들
    private static final String[] EVENT_PROJECTION = new String[] {
        Events._ID,                  // 0  DeleteEventHelper 에서 리용됨
        Events.TITLE,                // 1  DeleteEventHelper 에서 리용됨
        Events.RRULE,                // 2  DeleteEventHelper 에서 리용됨
        Events.ALL_DAY,              // 3  DeleteEventHelper 에서 리용됨
        Events.CALENDAR_ID,          // 4  DeleteEventHelper 에서 리용됨
        Events.DTSTART,              // 5  DeleteEventHelper 에서 리용됨
        Events._SYNC_ID,             // 6  DeleteEventHelper 에서 리용됨
        Events.EVENT_TIMEZONE,       // 7  DeleteEventHelper 에서 리용됨
        Events.DESCRIPTION,          // 8
        Events.EVENT_LOCATION,       // 9
        Calendars.CALENDAR_ACCESS_LEVEL, // 10
        Events.CALENDAR_COLOR,       // 11
        Events.EVENT_COLOR,          // 12
        Events.HAS_ATTENDEE_DATA,    // 13
        Events.ORGANIZER,            // 14
        Events.HAS_ALARM,            // 15
        Calendars.MAX_REMINDERS,     // 16
        Calendars.ALLOWED_REMINDERS, // 17
        Events.CUSTOM_APP_PACKAGE,   // 18
        Events.CUSTOM_APP_URI,       // 19
        Events.DTEND,                // 20
        Events.DURATION,             // 21
        Events.ORIGINAL_SYNC_ID      // 22 DeleteEventHelper 에서 리용됨
    };
    //우의 마당들에서 필요한 column들
    private static final int EVENT_INDEX_ID = 0;
    private static final int EVENT_INDEX_TITLE = 1;
    private static final int EVENT_INDEX_RRULE = 2;
    private static final int EVENT_INDEX_ALL_DAY = 3;
    private static final int EVENT_INDEX_CALENDAR_ID = 4;
    private static final int EVENT_INDEX_DTSTART = 5;
    private static final int EVENT_INDEX_DESCRIPTION = 8;
    private static final int EVENT_INDEX_EVENT_COLOR = 12;
    private static final int EVENT_INDEX_HAS_ALARM = 15;
    private static final int EVENT_INDEX_ALLOWED_REMINDERS = 17;
    private static final int EVENT_INDEX_DTEND = 20;
    private static final int EVENT_INDEX_DURATION = 21;

    //미리알림을 얻기 위한 column마당들
    private static final String[] REMINDERS_PROJECTION = new String[] {
        Reminders._ID,      // 0
        Reminders.MINUTES,  // 1
        Reminders.METHOD    // 2
    };
    private static final String REMINDERS_WHERE = Reminders.EVENT_ID + "=?";

    //Fade Animation의 지속시간
    private static final int FADE_IN_TIME = 300;

    //Loading View를 보여주기 전에 기다리는 시간
    //그시간동안에 일정정보들이 다 현시되면 LoadingView를 보여주지 않는다.
    private static final int LOADING_MSG_DELAY = 600;
    private static final int LOADING_MSG_MIN_DISPLAY_TIME = 600;

    public ArrayList<ReminderEntry> mReminders = new ArrayList<>();
    public ArrayList<ReminderEntry> mOriginalReminders = new ArrayList<ReminderEntry>();
    public ArrayList<ReminderEntry> mUnsupportedReminders = new ArrayList<ReminderEntry>();

    private int mCurrentQuery = 0;
    private View mView;
    private Uri mUri;
    private long mEventId;

    //달력, 일정, 미리알림들을 얻기 위한 Cursor들
    private Cursor mCalendarsCursor;
    private Cursor mEventCursor;
    private Cursor mRemindersCursor;

    //시작시간, 마감시간, 하루종일
    private long mStartMillis;
    private long mEndMillis;
    private boolean mAllDay;

    //일정삭제대화창이 떠있는가?
    private boolean mDeleteDialogVisible = false;

    //일정삭제를 위해 리용하는 DeleteEventHelper 변수
    private DeleteEventHelper mDeleteHelper;

    //미리알림을 가지고 있는가?
    private boolean mHasAlarm;

    //미리알림 최대개수
    private String mCalendarAllowedReminders;

    //자식 View들
    private TextView mReminderTextView;
    private ScrollView mScrollView;
    private View mLoadingMsgView;
    private ObjectAnimator mAnimateAlpha;
    private long mLoadingMsgStartTime;
    private final Runnable mLoadingMsgAlphaUpdater = new Runnable() {
        @Override
        public void run() {
            //일정한 delay 시간이 흐른후 일정자료가 현시되지 않았을 경우 Loading View 를 현시한다.
            if (!mAnimateAlpha.isRunning() && mScrollView.getAlpha() == 0) {
                mLoadingMsgStartTime = System.currentTimeMillis();
                mLoadingMsgView.setAlpha(1);
            }
        }
    };
    private boolean mNoCrossFade = false;  // Used to prevent repeated cross-fade

    /*
     * 미리알림 분들의 문자렬(10분, 15분, 20분,...), 수값(10, 15, 20, ...)
     */
    private ArrayList<Integer> mReminderMinuteValues;
    private ArrayList<String> mReminderMinuteLabels;

    /*
     * 미리알림 방식들의 문자렬(Notification, Email, SMS, Alarm), 수값(1, 2, 3, 4)
     */
    private ArrayList<Integer> mReminderMethodValues;
    private ArrayList<String> mReminderMethodLabels;

    private QueryHandler mHandler;
    private boolean mIsPaused = true;
    private boolean mDismissOnResume = false;
    private final Runnable onDeleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (EventInfoFragment.this.mIsPaused) {
                mDismissOnResume = true;
                return;
            }
            if (EventInfoFragment.this.isVisible()) {
                EventInfoFragment.this.dismiss();
            }
        }
    };
    private Activity mActivity;
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            updateEvent(mView);
        }
    };
    private CalendarController mController;

    public EventInfoFragment(Uri uri, long startMillis, long endMillis,
                             ArrayList<ReminderEntry> reminders) {
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        mUri = uri;
        mStartMillis = startMillis;
        mEndMillis = endMillis;

        if(reminders != null)
            mReminders = reminders;
    }

    //파라메터없는 기정의 구성자
    public EventInfoFragment() {
    }

    public EventInfoFragment(long eventId, long startMillis, long endMillis,
                             ArrayList<ReminderEntry> reminders) {
        this(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), startMillis,
                endMillis, reminders);
        mEventId = eventId;
    }

    /**
     * 응근수배렬자료를 얻어서 돌려준다.
     * @param r
     * @param resNum Resource Id {@link R.array#reminder_minutes_values}
     * @return ArrayList
     */
    private static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
        int[] values = r.getIntArray(resNum);
        int size = values.length;
        ArrayList<Integer> list = new ArrayList<Integer>(size);

        for (int val : values) {
            list.add(val);
        }

        return list;
    }

    /**
     * 문자렬배렬자료를 얻어서 돌려준다.
     * @param r
     * @param resNum Resource Id {@link R.array#reminder_minutes_labels}
     * @return ArrayList
     */
    private static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        return new ArrayList<>(Arrays.asList(labels));
    }

    private void sendAccessibilityEventIfQueryDone(int token) {
        mCurrentQuery |= token;
    }

    /**
     * Activity와의 련결이 파괴되였을때
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mController.deregisterEventHandler(R.layout.event_info);
        mController = null;
    }

    /**
     * Activity가 련결되였을때
     * @param context
     */
    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        mActivity = (Activity)context;
        mController = CalendarController.getInstance(mActivity);
        mController.registerEventHandler(R.layout.event_info, this);

        mHandler = new QueryHandler(context);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Activity 가 재창조될때 상태들을 복귀한다.
        if (savedInstanceState != null) {
            mDeleteDialogVisible =
                savedInstanceState.getBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE,false);
            mEventId = savedInstanceState.getLong(BUNDLE_KEY_EVENT_ID);
            mStartMillis = savedInstanceState.getLong(BUNDLE_KEY_START_MILLIS);
            mEndMillis = savedInstanceState.getLong(BUNDLE_KEY_END_MILLIS);
        }

        mView = inflater.inflate(R.layout.event_info, container, false);

        //자식 view들
        View backButton = mView.findViewById(R.id.back_button);
        mScrollView = mView.findViewById(R.id.event_info_scroll_view);
        mLoadingMsgView = mView.findViewById(R.id.event_info_loading_msg);
        mReminderTextView = mView.findViewById(R.id.reminder_text);

        if (mUri == null) {
            //Event ID로부터 Uri를 얻는다.
            mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
        }

        //Fade Animation 정의
        mAnimateAlpha = ObjectAnimator.ofFloat(mScrollView, "Alpha", 0, 1);
        mAnimateAlpha.setDuration(FADE_IN_TIME);
        mAnimateAlpha.addListener(new AnimatorListenerAdapter() {
            int defLayerType;

            @Override
            public void onAnimationStart(Animator animation) {
                // Use hardware layer for better performance during animation
                defLayerType = mScrollView.getLayerType();
                mScrollView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                // Ensure that the loading message is gone before showing the
                // event info
                mLoadingMsgView.removeCallbacks(mLoadingMsgAlphaUpdater);
                mLoadingMsgView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mScrollView.setLayerType(defLayerType, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mScrollView.setLayerType(defLayerType, null);
                // Do not cross fade after the first time
                mNoCrossFade = true;
            }
        });

        //시작에는 일정정보, 적재화면에 해당한 View들의 alpha값을 0으로 주어 안보이도록 한다.
        mLoadingMsgView.setAlpha(0);
        mScrollView.setAlpha(0);

        //delay시간이 지나면 적재화면을 보여준다.(일정정보들이 채 적재되지 못하여 현시가 안되였을 경우)
        mLoadingMsgView.postDelayed(mLoadingMsgAlphaUpdater, LOADING_MSG_DELAY);

        //Query를 통해 일정 불러들이기
        mHandler.startQuery(TOKEN_QUERY_EVENT, null, mUri, EVENT_PROJECTION,
                null, null, null);

        //미리알림변수들을 설정한다.
        prepareReminders();

        //편집, 삭제 단추들의 눌림동작들 정의
        View editButton = mView.findViewById(R.id.action_edit);
        View deleteButton = mView.findViewById(R.id.action_delete);
        editButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doEdit();
            }
        });
        deleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDeleteDialogVisible)
                    return;

                mDeleteHelper =
                        new DeleteEventHelper(mActivity, mReminders, true);
                mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
                mDeleteDialogVisible = true;
                mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
            }
        });

        //`뒤로 가기`단추를 눌렀을때 parent activity의 onBackPressed를 호출한다.
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        Utils.addCommonTouchListener(backButton);
        Utils.addCommonTouchListener(editButton);
        Utils.addCommonTouchListener(deleteButton);

        return mView;
    }

    /**
     * 일정을 얻는 Cursor를 초기화한다.
     * @return Cursor가 null이거나 비였으면 true를, 그렇지 않으면 false를 돌려준다.
     */
    private boolean initEventCursor() {
        if ((mEventCursor == null) || (mEventCursor.getCount() == 0)) {
            return true;
        }
        mEventCursor.moveToFirst();
        mEventId = mEventCursor.getInt(EVENT_INDEX_ID); //Id
        mHasAlarm = mEventCursor.getInt(EVENT_INDEX_HAS_ALARM) == 1;    //미리알림을 가지고 있는가?
        mCalendarAllowedReminders = mEventCursor.getString(EVENT_INDEX_ALLOWED_REMINDERS); //미리알림 최대개수
        return false;
    }

    /**
     * 상태들 보관
     * @param outState Bundle
     */
    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //일정의 Id, 시작/마감시간, 삭제대화창이 떠있었는가를 보관한다.
        outState.putLong(BUNDLE_KEY_EVENT_ID, mEventId);
        outState.putLong(BUNDLE_KEY_START_MILLIS, mStartMillis);
        outState.putLong(BUNDLE_KEY_END_MILLIS, mEndMillis);
        outState.putBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE, mDeleteDialogVisible);
    }

    @Override
    public void onDestroy() {
        //Cursor들 닫아주기
        if (mEventCursor != null) {
            mEventCursor.close();
        }
        if (mCalendarsCursor != null) {
            mCalendarsCursor.close();
        }
        super.onDestroy();
    }

    /**
     * 편집단추를 눌렀을때
     */
    private void doEdit() {
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
        Intent intent = new Intent(Intent.ACTION_EDIT, uri);
        intent.setClass(mActivity, EditEventActivity.class);

        //일정정보들을 extra값으로서 넣어준다.
        intent.putExtra(EXTRA_EVENT_BEGIN_TIME, mStartMillis);
        intent.putExtra(EXTRA_EVENT_END_TIME, mEndMillis);
        intent.putExtra(EXTRA_EVENT_ALL_DAY, mAllDay);
        intent.putExtra(EditEventActivity.EXTRA_EVENT_REMINDERS, mReminders);
        intent.putExtra(EVENT_EDIT_ON_LAUNCH, true);

        //EditEventActivity 를 띄운다.
        startActivity(intent);
    }

    /**
     * 일정을 UI에 반영
     * @param view
     */
    @SuppressLint("ClickableViewAccessibility")
    private void updateEvent(View view) {
        if (mEventCursor == null || view == null) {
            return;
        }

        //일정화상을 얻기
        int eventTypeId = mEventCursor.getInt(EVENT_INDEX_EVENT_COLOR);
        EventTypeManager.OneEventType oneEventType = EventTypeManager.getEventTypeFromId(eventTypeId);
        ImageView imageView = mView.findViewById(R.id.image);
        int imageSize = (int) getResources().getDimension(R.dimen.event_item_image_size);

        //일정화상을 ImageView에 설정
        Resources resources = getResources();
        Drawable drawable = ResourcesCompat.getDrawable(resources, oneEventType.imageResource, null).mutate();
        drawable.setBounds(0,0, imageSize, imageSize);
        drawable.setTint(resources.getColor(oneEventType.color, null));
        imageView.setImageDrawable(drawable);

        //일정제목 얻기
        String eventName = mEventCursor.getString(EVENT_INDEX_TITLE);
        if (eventName == null || eventName.length() == 0) {
            eventName = requireActivity().getString(R.string.no_title_label);
        }

        //하루종일인가 얻기
        mAllDay = mEventCursor.getInt(EVENT_INDEX_ALL_DAY) != 0;
        //설명 얻기
        String description = mEventCursor.getString(EVENT_INDEX_DESCRIPTION);

        //반복설정 얻기
        String rRule = mEventCursor.getString(EVENT_INDEX_RRULE);

        //mStartMillis, mEndMillis가 설정되지 않았으면 db로부터 읽어들인다.
        if (mStartMillis == 0 && mEndMillis == 0) {
            mStartMillis = mEventCursor.getLong(EVENT_INDEX_DTSTART);
            mEndMillis = mEventCursor.getLong(EVENT_INDEX_DTEND);
            if (mEndMillis == 0) {
                String duration = mEventCursor.getString(EVENT_INDEX_DURATION);
                if (!TextUtils.isEmpty(duration)) {
                    try {
                        Duration d = new Duration();
                        d.parse(duration);
                        long endMillis = mStartMillis + d.getMillis();
                        if (endMillis >= mStartMillis) {
                            mEndMillis = endMillis;
                        } else {
                            Log.d(TAG, "Invalid duration string: " + duration);
                        }
                    } catch (DateException e) {
                        Log.d(TAG, "Error parsing duration string " + duration, e);
                    }
                }
                if (mEndMillis == 0) {
                    mEndMillis = mStartMillis;
                }
            }
        }

        //현재의 시간대 얻기
        String localTimezone = Utils.getTimeZone(getContext(), mTZUpdater);

        //일정기간에 표시될 문자렬 얻기
        final DateTime startDateTime;
        if(mAllDay)
            startDateTime = new DateTime(DateTimeZone.UTC).withMillis(mStartMillis);
        else
            startDateTime = new DateTime(mStartMillis);
        String displayedDateTime = "";

        if(mAllDay){
            DateTime dateTime = new DateTime(startDateTime.getYear(), startDateTime.getMonthOfYear(), startDateTime.getDayOfMonth(), 0, 0);
            displayedDateTime = DateUtils.formatDateTime(getContext(), dateTime.getMillis(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);
        }
        else {
            displayedDateTime = DateUtils.formatDateTime(getContext(), startDateTime.getMillis(),
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR);
        }

        //제목 설정
        setTextCommon(view, R.id.title, eventName);

        //일정시간 설정
        setTextCommon(view, R.id.when_datetime, displayedDateTime);

        //반복문자렬 얻기
        String repeatString = null;
        if (!TextUtils.isEmpty(rRule)) {
            EventRecurrence eventRecurrence = new EventRecurrence();
            eventRecurrence.parse(rRule);
            Time date = new Time(localTimezone);
            date.set(mStartMillis);
            if (mAllDay) {
                date.timezone = Time.TIMEZONE_UTC;
            }
            eventRecurrence.setStartDate(date);
            repeatString = Utils.getUpperString(Objects.requireNonNull(EventRecurrenceFormatter.getRepeatString(
                    getContext(), resources,
                    eventRecurrence, true)));
        }
        //반복문자렬을 설정
        if (repeatString == null || repeatString.isEmpty()) {
            setTextCommon(view, R.id.when_repeat, getString(R.string.does_not_repeat));
        } else {
            setTextCommon(view, R.id.when_repeat, repeatString);
        }

        //`설명` 설정
        if (description != null && description.length() != 0) {
            setTextCommon(view, R.id.description, description);
        }
    }

    private void updateCalendar() {
        if (mCalendarsCursor != null && mEventCursor != null) {
            mCalendarsCursor.moveToFirst();

            //보이는 달력들을 얻기 위한 query를 시작한다.
            mHandler.startQuery(TOKEN_QUERY_VISIBLE_CALENDARS, null, Calendars.CONTENT_URI,
                    CALENDARS_PROJECTION, CALENDARS_VISIBLE_WHERE, new String[] {"1"}, null);
        } else {
            sendAccessibilityEventIfQueryDone(TOKEN_QUERY_DUPLICATE_CALENDARS);
        }
    }

    /**
     * Cursor로부터 미리알림정보들을 불러들인다.
     * @param cursor
     */
    public void initReminders(Cursor cursor) {
        //미리알림목록들을 초기화
        mOriginalReminders.clear();
        mUnsupportedReminders.clear();

        String reminderText = "";

        while (cursor.moveToNext()) {
            int minutes = cursor.getInt(EditEventHelper.REMINDERS_INDEX_MINUTES);
            int method = cursor.getInt(EditEventHelper.REMINDERS_INDEX_METHOD);

            if (method != Reminders.METHOD_DEFAULT && !mReminderMethodValues.contains(method)) {
                //지원하지 않는 미리알림방식일때
                mUnsupportedReminders.add(ReminderEntry.valueOf(minutes, method));
            } else {
                //지원하는 미리알림방식일때
                mOriginalReminders.add(ReminderEntry.valueOf(minutes, method));
            }

            int reminderIndex = mReminderMinuteValues.indexOf(minutes);
            if(reminderIndex >= 0)
                reminderText = mReminderMinuteLabels.get(reminderIndex);
        }

        //미리알림목록을 정렬한다.
        Collections.sort(mOriginalReminders);

        //`미리알림`label 설정
        if (mHasAlarm) {
            mReminders = mOriginalReminders;

            if(!reminderText.equals(""))
                mReminderTextView.setText(reminderText);
        }
    }

    /**
     * Id에 해당한 TextView의 Text설정
     * @param view 부모 View
     * @param id Id
     * @param text
     */
    private void setTextCommon(View view, int id, CharSequence text) {
        TextView textView = view.findViewById(id);
        if (textView == null)
            return;
        textView.setText(text);
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsPaused = false;
        if (mDismissOnResume) {
            mHandler.post(onDeleteRunnable);
        }
    }

    @Override
    public void onPause() {
        mIsPaused = true;
        mHandler.removeCallbacks(onDeleteRunnable);
        super.onPause();
    }

    @Override
    public void eventsChanged() {
    }

    @Override
    public void minuteChanged() {
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.EVENTS_CHANGED;
    }

    @Override
    public void handleEvent(EventInfo event) {
        Log.d("HandleEvent", "EnterInfoFragment");
    }

    /**
     * 편집 - `현재 및 이후 일정` 하여 일정이 갱신되였을때
     * @param eventId
     */
    public void onEventsChangedWithId(long eventId){
        if(eventId != -1) {
            mEventId = eventId;
            mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
        }
        mStartMillis = mEndMillis = 0;
        reloadEvents();
    }

    /**
     * 편집 - `선택된 일정` 하여 일정이 갱신되였을때
     * @param eventId
     * @param start 시작시간(미리초)
     * @param end 마감시간(미리초)
     */
    public void onEventsChangedWithId(long eventId, long start, long end){
        if(eventId != -1) {
            mEventId = eventId;
            mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
        }
        mStartMillis = start;
        mEndMillis = end;
        reloadEvents();
    }

    /**
     * 일정을 다시 적재
     */
    public void reloadEvents() {
        if (mHandler != null) {
            mHandler.startQuery(TOKEN_QUERY_EVENT, null, mUri, EVENT_PROJECTION,
                    null, null, null);
        }
    }

    /**
     * 미리알림 Minute, Method Label/Value 들을 얻는다.
     * 한번만 불러오면 된다.
     */
    synchronized private void prepareReminders() {
        //한번 호출이 되여 변수들이 설정되여있으면 여기서 끝낸다.
        if (mReminderMinuteValues != null && mReminderMinuteLabels != null
                && mReminderMethodValues != null && mReminderMethodLabels != null
                && mCalendarAllowedReminders == null) {
            return;
        }

        Resources r = getResources();
        mReminderMinuteValues = loadIntegerArray(r, R.array.reminder_minutes_values);   //분 수값들 불러오기
        mReminderMinuteLabels = loadStringArray(r, R.array.reminder_minutes_labels);    //분 문자렬들 불러오기
        mReminderMethodValues = loadIntegerArray(r, R.array.reminder_methods_values);   //알림방식 수값들 불러오기
        mReminderMethodLabels = loadStringArray(r, R.array.reminder_methods_labels);    //알림방식 문자렬들 불러오기

        //허용되지 않은 미리알림방식들은 없앤다.
        if (mCalendarAllowedReminders != null) {
            EventViewUtils.reduceMethodList(mReminderMethodValues, mReminderMethodLabels,
                    mCalendarAllowedReminders);
        }
        if (mView != null) {
            mView.invalidate();
        }
    }

    /**
     * 삭제대화창이 꺼졌을때의 동작
     * @return Dialog.OnDismissListener
     */
    private Dialog.OnDismissListener createDeleteOnDismissListener() {
        return new Dialog.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // Since OnPause will force the dialog to dismiss , do
                        // not change the dialog status
                        if (!mIsPaused) {
                            mDeleteDialogVisible = false;
                        }
                    }
                };
    }

    /**
     * 일정 Id
     * @return
     */
    public long getEventId() {
        return mEventId;
    }

    /**
     * 시작시간(미리초)
     * @return
     */
    public long getStartMillis() {
        return mStartMillis;
    }

    /**
     * 마감시간(미리초)
     * @return
     */
    public long getEndMillis() {
        return mEndMillis;
    }

    /**
     * Calendar, Event, Reminder들을 얻기 위한 Query실행클라스
     */
    private class QueryHandler extends AsyncQueryService {
        public QueryHandler(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            //Cursor 에 null이 들어오거나 activity가 종료되였을때는 동작을 끝낸다.
            final Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) {
                if (cursor != null) {
                    cursor.close();
                }
                return;
            }

            switch (token) {
                case TOKEN_QUERY_EVENT:
                    mEventCursor = Utils.matrixCursorFromCursor(cursor);
                    if (initEventCursor()) {
                        //Cursor가 비였을때
                        //이것은 일정이 삭제 되였다는것을 의미하므로 activity를 종료한다.
                        activity.finish();
                        return;
                    }

                    updateEvent(mView);
                    prepareReminders();

                    //Calendar들을 얻기 위한 query실행
                    Uri uri = Calendars.CONTENT_URI;
                    String[] args = new String[]{
                            Long.toString(mEventCursor.getLong(EVENT_INDEX_CALENDAR_ID))};
                    startQuery(TOKEN_QUERY_CALENDARS, null, uri, CALENDARS_PROJECTION,
                            CALENDARS_WHERE, args, null);
                    break;
                case TOKEN_QUERY_CALENDARS:
                    mCalendarsCursor = Utils.matrixCursorFromCursor(cursor);
                    updateCalendar();
                    uri = Colors.CONTENT_URI;
                    startQuery(TOKEN_QUERY_COLORS, null, uri, null, null, null,
                            null);

                    if (mHasAlarm) {
                        //미리알림들을 얻기 위한 query실행
                        args = new String[]{Long.toString(mEventId)};
                        uri = Reminders.CONTENT_URI;
                        startQuery(TOKEN_QUERY_REMINDERS, null, uri,
                                REMINDERS_PROJECTION, REMINDERS_WHERE, args, null);
                    } else {
                        sendAccessibilityEventIfQueryDone(TOKEN_QUERY_REMINDERS);
                    }
                    break;
                case TOKEN_QUERY_COLORS:
                    cursor.close();
                    break;
                case TOKEN_QUERY_REMINDERS:
                    mRemindersCursor = Utils.matrixCursorFromCursor(cursor);
                    initReminders(mRemindersCursor);
                    break;
                case TOKEN_QUERY_VISIBLE_CALENDARS:
                    if (cursor.getCount() > 1) {
                        //Calendar중복을 확인하기 위한 query실행
                        String displayName = mCalendarsCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
                        mHandler.startQuery(TOKEN_QUERY_DUPLICATE_CALENDARS, null,
                                Calendars.CONTENT_URI, CALENDARS_PROJECTION,
                                CALENDARS_DUPLICATE_NAME_WHERE, new String[]{displayName}, null);
                    } else {
                        mCurrentQuery |= TOKEN_QUERY_DUPLICATE_CALENDARS;
                    }
                    break;
                case TOKEN_QUERY_DUPLICATE_CALENDARS:
                    SpannableStringBuilder sb = new SpannableStringBuilder();

                    //달력 이름을 얻는다.
                    String calendarName = mCalendarsCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
                    sb.append(calendarName);

                    //달력계정의 email주소를 얻는다.
                    String email = mCalendarsCursor.getString(CALENDARS_INDEX_OWNER_ACCOUNT);
                    if (cursor.getCount() > 1 && !calendarName.equalsIgnoreCase(email) &&
                            Utils.isValidEmail(email)) {
                        sb.append(" (").append(email).append(")");
                    }
                    break;
            }
            cursor.close();
            sendAccessibilityEventIfQueryDone(token);

            //모든 Query들의 실행이 끝났으면 일정정보들을 현시한다.
            if (mCurrentQuery == TOKEN_QUERY_ALL) {
                //적재화면이 보여지고 있을때
                if (mLoadingMsgView.getAlpha() == 1) {
                    //적재화면과 animation을 없앤다.
                    long timeDiff = LOADING_MSG_MIN_DISPLAY_TIME - (System.currentTimeMillis() -
                            mLoadingMsgStartTime);
                    if (timeDiff > 0) {
                        mAnimateAlpha.setStartDelay(timeDiff);
                    }
                }

                //Fade Animation이 시작안되였을때(Query실행이 너무 빨리 진행됨)
                if (!mAnimateAlpha.isRunning() && !mAnimateAlpha.isStarted() && !mNoCrossFade) {
                    mAnimateAlpha.start();
                }

                //Fade Animation이 진행될때
                else {
                    //적재화면은 숨기고 실지 일정정보화면을 보여준다.
                    mScrollView.setAlpha(1);
                    mLoadingMsgView.setVisibility(View.GONE);
                }

                if (mDeleteDialogVisible) {
                    //삭제대화창 띄우기
                    mDeleteHelper = new DeleteEventHelper(mActivity, true);
                    mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
                    mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
                }
            }
        }
    }
}
