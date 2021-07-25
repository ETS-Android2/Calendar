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

package com.android.calendar.event;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.text.TextUtils;

import com.android.calendar.helper.AsyncQueryService;
import com.android.calendar.utils.Utils;
import com.android.kr_common.Time;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.android.calendar.persistence.CalendarRepository;
import com.android.calendarcommon2.EventRecurrence;

import java.util.ArrayList;
import java.util.Objects;

import com.android.krcalendar.R;

import org.joda.time.DateTime;

/**
 * 일정삭제를 위한 Helper 클라스
 */
public class DeleteEventHelper {
    /**
     * 반복일정의 삭제방식
     * @see R.array#delete_repeating_labels
     */
    public static final int DELETE_SELECTED = 0;        //이 일정만
    public static final int DELETE_ALL_FOLLOWING = 1;   //현재 및 향후 일정
    public static final int DELETE_ALL = 2;             //모든 일정

    private final Activity mActivity;
    private long mStartMillis;
    private long mEndMillis;
    private CalendarEventModel mModel;
    /**
     * true로 설정되면 동작이 완료될때 parent activity의 finish를 호출한다.
     */
    private final boolean mExitWhenDone;

    //삭제-확인을 눌렀을때의 동작을 처리하는 callback
    private Runnable mCallback;
    private int mWhichDelete;
    private AlertDialog mAlertDialog;
    private Dialog.OnDismissListener mDismissListener;
    private final EditEventHelper mHelper;

    private final AsyncQueryService mService;

    //미리알림(1개 혹은 없음)
    public ArrayList<CalendarEventModel.ReminderEntry> mReminders;

    /**
     * 일반일정을 삭제할때의 삭제동작을 위한 listener이다.
     */
    private final DialogInterface.OnClickListener mDeleteNormalDialogListener =
            new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int button) {
            long id = mModel.mId; // mCursor.getInt(mEventIndexId);

            // If this event is part of a local calendar, really remove it from the database
            //
            // "There are two versions of delete: as an app and as a sync adapter.
            // An app delete will set the deleted column on an event and remove all instances of that event.
            // A sync adapter delete will remove the event from the database and all associated data."
            // from https://developer.android.com/reference/android/provider/CalendarContract.Events
            boolean isLocal = mModel.mSyncAccountType.equals(CalendarContract.ACCOUNT_TYPE_LOCAL);
            Uri deleteContentUri = isLocal ? CalendarRepository.asLocalCalendarSyncAdapter(mModel.mSyncAccountName, Events.CONTENT_URI) : Events.CONTENT_URI;

            Uri uri = ContentUris.withAppendedId(deleteContentUri, id);
            mService.startDelete(mService.getNextToken(), null, uri, null, null, Utils.NO_DELAY);
            if (mCallback != null) {
                mCallback.run();
            }
            if (mExitWhenDone) {
                mActivity.finish();
            }
        }
    };

    /**
     * 례외일정을 삭제할때의 삭제동작을 위한 listener이다
     */
    private final DialogInterface.OnClickListener mDeleteExceptionDialogListener =
        new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int button) {
            deleteExceptionEvent();
            if (mCallback != null) {
                mCallback.run();
            }
            if (mExitWhenDone) {
                mActivity.finish();
            }
        }
    };

    /**
     * This callback is used when a repeating event is deleted.
     */
    private final DeletionDialog.DeleteClickedListener mDeleteClickedListener =
            new DeletionDialog.DeleteClickedListener() {
                @Override
                public void onClicked(int which) {
                    mWhichDelete = which;
                    deleteRepeatingEvent(mWhichDelete);
                }
            };

    public DeleteEventHelper(Activity parentActivity, ArrayList<CalendarEventModel.ReminderEntry> reminders, boolean exitWhenDone) {
        this(parentActivity, exitWhenDone);
        if(mReminders != null)
            mReminders.clear();
        mReminders = reminders;
    }

    public DeleteEventHelper(Activity parentActivity, boolean exitWhenDone) {
        if (exitWhenDone && parentActivity == null) {
            throw new IllegalArgumentException("parentActivity is required to exit when done");
        }

        mActivity = parentActivity;
        mHelper = new EditEventHelper(mActivity);
        // TODO move the creation of this service out into the activity.
        mService = new AsyncQueryService(mActivity) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor == null) {
                    return;
                }
                cursor.moveToFirst();
                CalendarEventModel mModel = new CalendarEventModel();
                EditEventHelper.setModelFromCursor(mModel, cursor);
                cursor.close();
                DeleteEventHelper.this.delete(mStartMillis, mEndMillis, mModel, mWhichDelete);
            }
        };
        mExitWhenDone = exitWhenDone;
    }

    /**
     * Does the required processing for deleting an event, which includes
     * first popping up a dialog asking for confirmation (if the event is
     * a normal event) or a dialog asking which events to delete (if the
     * event is a repeating event).  The "which" parameter is used to check
     * the initial selection and is only used for repeating events.  Set
     * "which" to -1 to have nothing selected initially.
     *
     * @param begin the begin time of the event, in UTC milliseconds
     * @param end the end time of the event, in UTC milliseconds
     * @param eventId the event id
     * @param which one of the values {@link #DELETE_SELECTED},
     *  {@link #DELETE_ALL_FOLLOWING}, {@link #DELETE_ALL}, or -1
     */
    public void delete(long begin, long end, long eventId, int which) {
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
        mService.startQuery(mService.getNextToken(), null, uri, EditEventHelper.EVENT_PROJECTION,
                null, null, null);
        mStartMillis = begin;
        mEndMillis = end;
        mWhichDelete = which;
    }

    public void delete(long begin, long end, long eventId, int which, Runnable callback) {
        delete(begin, end, eventId, which);
        mCallback = callback;
    }

    /**
     * Does the required processing for deleting an event.  This method
     * takes a {@link CalendarEventModel} object, which must have a valid
     * uri for referencing the event in the database and have the required
     * fields listed below.
     * The required fields for a normal event are:
     *
     * <ul>
     *   <li> Events._ID </li>
     *   <li> Events.TITLE </li>
     *   <li> Events.RRULE </li>
     * </ul>
     *
     * The required fields for a repeating event include the above plus the
     * following fields:
     *
     * <ul>
     *   <li> Events.ALL_DAY </li>
     *   <li> Events.CALENDAR_ID </li>
     *   <li> Events.DTSTART </li>
     *   <li> Events._SYNC_ID </li>
     *   <li> Events.EVENT_TIMEZONE </li>
     * </ul>
     *
     * If the event no longer exists in the db this will still prompt
     * the user but will return without modifying the db after the query
     * returns.
     *
     * @param begin the begin time of the event, in UTC milliseconds
     * @param end the end time of the event, in UTC milliseconds
     * @param which one of the values {@link #DELETE_SELECTED},
     *  {@link #DELETE_ALL_FOLLOWING}, {@link #DELETE_ALL}, or -1
     */
    @SuppressLint("ClickableViewAccessibility")
    public void delete(long begin, long end, CalendarEventModel model, int which) {
        mWhichDelete = which;
        mStartMillis = begin;
        mEndMillis = end;
        mModel = model;
        mModel.mReminders = mReminders;

        String rRule = model.mRrule;
        String originalEvent = model.mOriginalSyncId;
        if (TextUtils.isEmpty(rRule)) { //반복일정이 아닌 경우
            //확인, 취소를 가진 Alert대화창을 띄운다.
            AlertDialog dialog = new AlertDialog.Builder(mActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setMessage(R.string.delete_this_event_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setNegativeButton(android.R.string.cancel, null).create();

            if (originalEvent == null) {
                //일반일정
                dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                        mActivity.getText(android.R.string.ok),
                        mDeleteNormalDialogListener);
            } else {
                //례외일정
                dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                        mActivity.getText(android.R.string.ok),
                        mDeleteExceptionDialogListener);
            }

            dialog.setOnShowListener(arg0 -> {
                Button positiveButton, negativeButton;
                positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                positiveButton.setAllCaps(false);
                negativeButton.setAllCaps(false);

                Utils.addCommonTouchListener(positiveButton);
                Utils.addCommonTouchListener(negativeButton);
            });
            dialog.setCanceledOnTouchOutside(false);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.alert_dialog_bg);
            dialog.setOnDismissListener(mDismissListener);
            dialog.show();

            //대화창의 위치는 화면아래에, bottom margin을 10dp로 준다.
            WindowManager.LayoutParams windowLayoutParams = dialog.getWindow().getAttributes();
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            windowLayoutParams.y = (int) mActivity.getResources().getDimension(R.dimen.common_dialog_bottom_margin);
            dialog.getWindow().setAttributes(windowLayoutParams);

            mAlertDialog = dialog;
        } else {    //반복일정인 경우
            //`이 일정만`, `현재 및 향후일정`, `모든 일정`
            //이중에서 삭제방식을 선택하는 대화창을 띄운다.
            boolean showFollowing = model.mStart != begin;  //반복의 첫일정을 선택했을때는 `현재 및 향후일정`을 없앤다.
            DeletionDialog dialog = new DeletionDialog(mActivity, R.style.myDialog, showFollowing);

            //Window배경은 투명한것으로 설정한다.
            Window window = dialog.getWindow();
            Objects.requireNonNull(window).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

            //삭제단추
            dialog.setDeleteClickedListener(mDeleteClickedListener);
            dialog.setOnDismissListener(mDismissListener);
            dialog.show();
        }
    }

    /**
     * 례외일정삭제
     */
    private void deleteExceptionEvent() {
        /*-- 간단히 STATUS 마당값을 STATUS_CANCELED 로 갱신한다. --*/
        long id = mModel.mId;
        ContentValues values = new ContentValues();
        values.put(Events.STATUS, Events.STATUS_CANCELED);
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);

        //갱신 시작
        mService.startUpdate(mService.getNextToken(), null, uri, values, null, null,
                Utils.NO_DELAY);
    }

    /**
     * 반복일정 삭제
     * @param which {@link #DELETE_SELECTED}, {@link #DELETE_ALL_FOLLOWING}, {@link #DELETE_ALL}중의 하나
     */
    @SuppressLint("Recycle")
    private void deleteRepeatingEvent(int which) {
        long dtStart = mModel.mStart;
        long id = mModel.mId;

        boolean isLocal = mModel.mSyncAccountType.equals(CalendarContract.ACCOUNT_TYPE_LOCAL);
        Uri deleteContentUri = isLocal ? CalendarRepository.asLocalCalendarSyncAdapter(mModel.mSyncAccountName, Events.CONTENT_URI) : Events.CONTENT_URI;
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        switch (which) {
            //`선택된 일정`삭제
            case DELETE_SELECTED: {
                //만약 지우려는 일정의 반복의 첫 일정일때는 단순히 반복일정의 시작시간만 변경시킨다.
                //그렇지 않은 경우에는 이전일정의 마감시간을 새로 설정하고 이후일정에 대한 창조를 진행한다.
                boolean hasBeforeEvent = dtStart < mStartMillis;

                //지우려는 일정에 반복회수정보가 있으면
                //새로 창조될 일정의 반복회수는 본래의 반복회수보다 작은 값으로 설정되게 된다.
                boolean oldRuleHasCount = false;
                String newRule = "";

                Uri uri = ContentUris.withAppendedId(deleteContentUri, id);
                if(hasBeforeEvent) {    //이전일정이 있을때
                    //이전반복일정이 현재 일정의 시작시간보다 전에 끝나도록 반복설정을 갱신한다.
                    EventRecurrence recurrence = new EventRecurrence();
                    recurrence.parse(mModel.mRrule);
                    oldRuleHasCount = recurrence.count > 0;

                    mModel.mUri = uri.toString();

                    //마감시간을 변경한다.
                    newRule = mHelper.updatePastEvents(ops, mModel, mStartMillis);
                }
                else{   //이전일정이 없을때(지우려는 일정이  반복의 첫일정)
                    mService.startDelete(mService.getNextToken(), null, uri, null, null,
                            Utils.NO_DELAY);
                }

                //반복의 다음일정의 시작날자를 계산한다.
                Time time = new Time(mStartMillis);
                int startDay = Time.getJulianDay(time);

                ContentResolver contentResolver = mActivity.getContentResolver();
                Uri.Builder builder = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon();
                ContentUris.appendId(builder, startDay);
                ContentUris.appendId(builder, Long.MAX_VALUE);

                String selection = "(" + CalendarContract.Instances.EVENT_ID +
                        "==" + mModel.mId +
                        " AND " + CalendarContract.Instances.BEGIN +
                        ">" + mStartMillis +
                        ")";

                @SuppressLint("Recycle") Cursor cursor = contentResolver.query(builder.build(), new String[]{
                        CalendarContract.Instances.BEGIN
                    }, selection, null, "begin asc limit 1");

                DateTime dateTime = new DateTime(mStartMillis);
                boolean hasNextEvent = false;
                long startEventTime = 0;

                if(cursor != null){
                    cursor.moveToFirst();
                    if(cursor.getCount() >= 1){
                        long millis = cursor.getLong(0);
                        DateTime startEventDate = new DateTime(millis);
                        startEventDate = new DateTime(startEventDate.getYear(), startEventDate.getMonthOfYear(), startEventDate.getDayOfMonth(),
                                dateTime.getHourOfDay(), dateTime.getMinuteOfHour());
                        startEventTime = startEventDate.getMillis();
                        hasNextEvent = true;
                    }
                }

                mService.startBatch(mService.getNextToken(), null, android.provider.CalendarContract.AUTHORITY, ops,
                        Utils.NO_DELAY);

                if(hasNextEvent){
                    mModel.mStart = startEventTime;
                    mModel.mUri = null;

                    if(oldRuleHasCount)
                        mModel.mRrule = newRule; //반복회수가 줄어든 새로운 반복설정을 창조할 일정에 설정한다.

                    EventRecurrence eventRecurrence = new EventRecurrence();
                    eventRecurrence.parse(mModel.mRrule);
                    if(eventRecurrence.count > 0) {
                        //반복회수를 하나 감소한다. 그것은
                        //현재의 일정이 새로 창조될 일정에 포함되지 않기때문이다.
                        eventRecurrence.count --;
                        mModel.mRrule = eventRecurrence.toString();
                    }

                    mHelper.saveEvent(mModel, null, Utils.MODIFY_ALL, false);
                }
                break;
            }

            //`모든 일정` 삭제
            case DELETE_ALL: {
                Uri uri = ContentUris.withAppendedId(deleteContentUri, id);
                mService.startDelete(mService.getNextToken(), null, uri, null, null,
                        Utils.NO_DELAY);
                break;
            }

            //현재 및 이후 일정삭제
            case DELETE_ALL_FOLLOWING: {
                //시작시간이 반복 첫일정의 시작시간과 같으면 `모든 일정`삭제와 같은 동작이다.
                if (dtStart == mStartMillis) {
                    Uri uri = ContentUris.withAppendedId(deleteContentUri, id);
                    mService.startDelete(mService.getNextToken(), null, uri, null, null,
                            Utils.NO_DELAY);
                    break;
                }

                //이전반복일정이 현재 일정의 시작시간보다 전에 끝나도록 반복설정을 갱신한다.
                Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
                mModel.mUri = uri.toString();
                mHelper.updatePastEvents(ops, mModel, mStartMillis);

                mService.startBatch(mService.getNextToken(), null, android.provider.CalendarContract.AUTHORITY, ops,
                        Utils.NO_DELAY);
                break;
            }
        }
        if (mCallback != null) {
            mCallback.run();
        }
        if (mExitWhenDone) {
            mActivity.finish();
        }
    }

    public void setOnDismissListener(Dialog.OnDismissListener listener) {
        if (mAlertDialog != null) {
            mAlertDialog.setOnDismissListener(listener);
        }
        mDismissListener = listener;
    }

    public void dismissAlertDialog() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }
}
