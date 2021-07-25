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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Colors;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.calendar.event.CalendarEventModel.Attendee;
import com.android.calendar.event.CalendarEventModel.ReminderEntry;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.helper.CalendarController.EventHandler;
import com.android.calendar.helper.CalendarController.EventInfo;
import com.android.calendar.persistence.CalendarRepository;
import com.android.calendar.utils.ContextHolder;
import com.android.calendar.utils.HsvColorComparator;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * 일정편집화면 Fragment
 */
public class EditEventFragment extends Fragment implements EventHandler, OnClickListener {
    //기정달력의 Own
    public static final String OFFLINE_ACCOUNT_OWNER_NAME = "KR Calendar";
    public static final String DEFAULT_CALENDAR_ACCOUNT_NAME = "KrCalendar";

    private static final String TAG = "EditEventActivity";
    private static final boolean DEBUG = false;

    /**
     * Activity 재창조시 상태들을 보관하기 위한 key 상수값들
     */
    private static final String BUNDLE_KEY_MODEL = "key_model";
    private static final String BUNDLE_KEY_EDIT_STATE = "key_edit_state";
    private static final String BUNDLE_KEY_EVENT = "key_event";
    private static final String BUNDLE_KEY_EDIT_ON_LAUNCH = "key_edit_on_launch";
    private static final String BUNDLE_KEY_EVENT_TYPES_VIEW_EXPANDED = "event_types_view_expanded";
    private static final String BUNDLE_KEY_DIALOG_OPENED = "dialog_opened";

    //Query Token 값들
    private static final int TOKEN_EVENT = 1;
    private static final int TOKEN_ATTENDEES = 1 << 1;
    private static final int TOKEN_REMINDERS = 1 << 2;
    private static final int TOKEN_CALENDARS = 1 << 3;
    private static final int TOKEN_COLORS = 1 << 4;

    private static final int TOKEN_ALL = TOKEN_EVENT | TOKEN_ATTENDEES | TOKEN_REMINDERS
            | TOKEN_CALENDARS | TOKEN_COLORS;
    private static final int TOKEN_UNITIALIZED = 1 << 31;
    private final EventInfo mEvent;
    private final Done mOnDone = new Done();
    private final Intent mIntent;
    public boolean mShowModifyDialogOnLaunch = false;
    EditEventHelper mHelper;
    CalendarEventModel mModel;
    CalendarEventModel mOriginalModel;
    CalendarEventModel mRestoreModel;
    EditEventView mView;
    QueryHandler mHandler;

    int mModification = Utils.MODIFY_UNINITIALIZED;
    //일정형식화상 RecyclerView가 다 보여진 상태인가, 선택된 일정형식만 보여진 상태인가?
    boolean mEventTypesViewExpanded = true;
    boolean mDialogOpened = false;
    /**
     * A bitfield of TOKEN_* to keep track which query hasn't been completed
     * yet. Once all queries have returned, the model can be applied to the
     * view.
     */
    private int mOutstandingQueries = TOKEN_UNITIALIZED;
    private AlertDialog mModifyDialog;
    private EventBundle mEventBundle;
    private ArrayList<ReminderEntry> mReminders;
    private boolean mEditMode;
    private Uri mUri;
    private long mBegin;
    private long mEnd;
    private long mCalendarId = -1;
    private Context mContext;
    private InputMethodManager mInputMethodManager;

    public EditEventFragment(){
        mEvent = null;
        mIntent = null;
        mEditMode = false;
        mReminders = null;
    }

    public EditEventFragment(EventInfo event, ArrayList<ReminderEntry> reminders,
                             Intent intent, boolean editMode) {
        mEvent = event;
        mIntent = intent;
        mEditMode = editMode;
        mReminders = reminders;
        setHasOptionsMenu(true);
    }

    private void setModelIfDone(int queryType) {
        synchronized (this) {
            mOutstandingQueries &= ~queryType;
            if (mOutstandingQueries == 0) {
                if (mRestoreModel != null) {
                    mModel = mRestoreModel;
                }
                if (mShowModifyDialogOnLaunch && mModification == Utils.MODIFY_UNINITIALIZED) {
                    if (!TextUtils.isEmpty(mModel.mRrule)) {
                        displayEditWhichDialog();
                    } else {
                        mModification = Utils.MODIFY_ALL;
                    }
                }
                mView.setModel(mModel, mEventTypesViewExpanded);
                mView.setModification(mModification);

                if(mDialogOpened) showDialog();
            }
        }
    }

    private void displayEditWhichDialog() {
        if (mModification == Utils.MODIFY_UNINITIALIZED) {
            final boolean notSynced = TextUtils.isEmpty(mModel.mSyncId);
            boolean isFirstEventInSeries = mModel.mIsFirstEventInSeries;
            int itemIndex = 0;
            CharSequence[] items;

            //Ignore synchronize for single event update
            if (isFirstEventInSeries) {
                items = new CharSequence[2];
            } else {
                items = new CharSequence[3];
            }
            items[itemIndex++] = mContext.getText(R.string.modify_event);
            items[itemIndex++] = mContext.getText(R.string.modify_all);

            // Do one more check to make sure this remains at the end of the list
            if (!isFirstEventInSeries) {
                items[itemIndex] = mContext.getText(R.string.modify_all_following);
            }

            // Display the modification dialog.
            if (mModifyDialog != null) {
                mModifyDialog.dismiss();
                mModifyDialog = null;
            }

            mModifyDialog = new AlertDialog.Builder(mContext).setTitle(R.string.edit_event_label)
                    .setItems(items, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                // Update this if we start allowing exceptions
                                // to unsynced events in the app
                                mModification = Utils.MODIFY_SELECTED;
                                mModel.mOriginalSyncId = notSynced ? null : mModel.mSyncId;
                                mModel.mOriginalId = mModel.mId;
                            } else if (which == 1) {
                                mModification = Utils.MODIFY_ALL;
                            } else if (which == 2) {
                                mModification = Utils.MODIFY_ALL_FOLLOWING;
                            }

                            mView.setModification(mModification);
                        }
                    }).show();

            mModifyDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Activity a = EditEventFragment.this.getActivity();
                    if (a != null) {
                        a.finish();
                    }
                }
            });

            Objects.requireNonNull(mModifyDialog.getWindow()).setBackgroundDrawableResource(R.drawable.alert_dialog_bg);
            WindowManager.LayoutParams layoutAttributes = Objects.requireNonNull(mModifyDialog.getWindow()).getAttributes();
            mModifyDialog.getWindow().setGravity(Gravity.BOTTOM);
            layoutAttributes.y = (int) mContext.getResources().getDimension(R.dimen.common_dialog_bottom_margin);
            mModifyDialog.getWindow().setAttributes(layoutAttributes);
        }
    }

    private void startQuery() {
        mUri = null;
        mBegin = -1;
        mEnd = -1;
        if (mEvent != null) {
            if (mEvent.id != -1) {
                mModel.mId = mEvent.id;
                mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEvent.id);
            } else {
                // New event. All day?
                mModel.mAllDay = mEvent.extraLong == CalendarController.EXTRA_CREATE_ALL_DAY;
            }
            if (mEvent.startTime != null) {
                mBegin = mEvent.startTime.toMillis(true);
            }
            if (mEvent.endTime != null) {
                mEnd = mEvent.endTime.toMillis(true);
            }
            if (mEvent.calendarId != -1) {
                mCalendarId = mEvent.calendarId;
            }
        } else if (mEventBundle != null) {
            if (mEventBundle.id != -1) {
                mModel.mId = mEventBundle.id;
                mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventBundle.id);
            }
            mBegin = mEventBundle.start;
            mEnd = mEventBundle.end;
        }

        if (mReminders != null) {
            mModel.mReminders = mReminders;
        }

        if (mBegin <= 0) {
            // use a default value instead
            mBegin = mHelper.constructDefaultStartTime(System.currentTimeMillis());
        }
        if (mEnd < mBegin) {
            // use a default value instead
            mEnd = mHelper.constructDefaultEndTime(mBegin, mContext);
        }

        // Kick off the query for the event
        boolean newEvent = mUri == null;
        if (!newEvent) {
            mModel.mCalendarAccessLevel = Calendars.CAL_ACCESS_NONE;
            mOutstandingQueries = TOKEN_ALL;
            if (DEBUG) {
                Log.d(TAG, "startQuery: uri for event is " + mUri.toString());
            }
            mHandler.startQuery(TOKEN_EVENT, null, mUri, EditEventHelper.EVENT_PROJECTION,
                    null /* selection */, null /* selection args */, null /* sort order */);
        } else {
            mOutstandingQueries = TOKEN_CALENDARS | TOKEN_COLORS;
            if (DEBUG) {
                Log.d(TAG, "startQuery: Editing a new event.");
            }
            mModel.mOriginalStart = mBegin;
            mModel.mOriginalEnd = mEnd;
            mModel.mStart = mBegin;
            mModel.mEnd = mEnd;
            mModel.mCalendarId = mCalendarId;
            mModel.mSelfAttendeeStatus = Attendees.ATTENDEE_STATUS_ACCEPTED;

            // Start a query in the background to read the list of calendars and colors
            mHandler.startQuery(TOKEN_CALENDARS, null, Calendars.CONTENT_URI,
                    EditEventHelper.CALENDARS_PROJECTION,
                    EditEventHelper.CALENDARS_WHERE_WRITEABLE_VISIBLE, null /* selection args */,
                    null /* sort order */);

            mHandler.startQuery(TOKEN_COLORS, null, Colors.CONTENT_URI,
                    EditEventHelper.COLORS_PROJECTION,
                    Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT, null, null);

            mModification = Utils.MODIFY_ALL;
            mView.setModification(mModification);
        }
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        mContext = (AppCompatActivity) context;

        mHelper = new EditEventHelper(context);
        mHandler = new QueryHandler(context.getContentResolver());
        mModel = new CalendarEventModel(context, mIntent);
        mInputMethodManager = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.edit_event, container, false);

        mView = new EditEventView((AppCompatActivity)mContext, this, mEditMode, view, mOnDone);
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            //달력에 대한 읽기/쓰기권한이 부여되지 않았으면 toast를 띄워준다.
            Toast.makeText(mContext, R.string.calendar_permission_not_granted, Toast.LENGTH_LONG).show();
        } else {
            startQuery();
        }

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_KEY_MODEL)) {
                mRestoreModel = (CalendarEventModel) savedInstanceState.getSerializable(
                        BUNDLE_KEY_MODEL);

                if(mRestoreModel != null && mRestoreModel.mAllDay) {
                    DateTime endDate = new DateTime(mRestoreModel.mEnd).plusDays(1);
                    mRestoreModel.mEnd = endDate.getMillis();
                }
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_EDIT_STATE)) {
                mModification = savedInstanceState.getInt(BUNDLE_KEY_EDIT_STATE);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_EDIT_ON_LAUNCH)) {
                mShowModifyDialogOnLaunch = savedInstanceState
                        .getBoolean(BUNDLE_KEY_EDIT_ON_LAUNCH);
            }
            if (savedInstanceState.containsKey(BUNDLE_KEY_EVENT)) {
                mEventBundle = (EventBundle) savedInstanceState.getSerializable(BUNDLE_KEY_EVENT);
            }
            mEventTypesViewExpanded = savedInstanceState.getBoolean(BUNDLE_KEY_EVENT_TYPES_VIEW_EXPANDED, true);
            mDialogOpened = savedInstanceState.getBoolean(BUNDLE_KEY_DIALOG_OPENED, false);
        }
    }

    /**
     * 일정보관
     */
    public void onSave(){
        if (EditEventHelper.canModifyEvent(mModel)) {
            if (mView != null && mView.prepareForSave()) {
                if (mModification == Utils.MODIFY_UNINITIALIZED) {
                    mModification = Utils.MODIFY_ALL;
                }
                mOnDone.setDoneCode(Utils.DONE_SAVE | Utils.DONE_EXIT);
            } else {
                mOnDone.setDoneCode(Utils.DONE_EXIT);
            }
        }
        else {
            mOnDone.setDoneCode(Utils.DONE_EXIT);
        }
        mOnDone.run();
    }

    /**
     * 취소단추를 눌렀을때
     */
    public void onCancel(){
        if(onBackPressed()) {
            mOnDone.setDoneCode(Utils.DONE_EXIT);
            mOnDone.run();
        }
    }

    boolean isEmptyNewEvent() {
        if (mOriginalModel != null) {
            // Not new
            return false;
        }

        if (mModel.mOriginalStart != mModel.mStart || mModel.mOriginalEnd != mModel.mEnd) {
            return false;
        }

        return mModel.isEmpty();
    }

    /**
     * 2개의 문자렬들을 비교한다
     * @param firstString 문자렬1
     * @param secondString 문자렬2
     * @return 둘다 null이 아니고 내용이 같을때 true를 돌려준다.
     */
    public boolean isSame(String firstString, String secondString){
        if(firstString == null)
            return secondString == null || secondString.length() == 0;
        if(secondString == null)
            return firstString.length() == 0;
        return firstString.equals(secondString);
    }

    /**
     * @return 입력마당들중의 하나라도 변했을때 true를 돌려준다
     */
    public boolean viewChanged(){
        if(mModel == null)
            return true;
        if(!isSame(mModel.mTitle, mView.mTitleTextView.getText().toString()))
            return true;
        if (mModel.mAllDay != mView.mAllDayCheckBox.isChecked())
            return true;
        if (mModel.mStart != mView.mStartTime.getMillis())
            return true;
        if (mModel.mEnd != mView.mEndTime.getMillis())
            return true;
        if (mModel.getEventType() != mView.mSelectedEventType)
            return true;
        if(!isSame(mModel.mDescription, mView.mDescriptionTextView.getText().toString()))
            return true;
        if(!isSame(mModel.mRrule, mView.mEventRecurrence.toString()) &&
                !isSame("FREQ=", mView.mEventRecurrence.toString()))
            return true;

        //Compare reminders
        ArrayList<ReminderEntry> reminderEntries = mView.getReminders();
        if(!mModel.mReminders.equals(reminderEntries))
            return true;

        return false;
    }

    /**
     * 변경된 자료가 있으면 AlertDialog를 띄워준다.
     * @return 변경된것이 있으면 false, 변경된것이 없으면 true
     */
    @SuppressLint("ClickableViewAccessibility")
    public boolean onBackPressed(){
        boolean viewChanged = viewChanged();
        if(!viewChanged){
            return true;
        }

        showDialog();

        return false;
    }

    /**
     * `변경된 내용을 저장하시겠습니까?`라는 AlertDialog 를 띄운다.
     */
    private void showDialog() {
        AlertDialog dialog = new AlertDialog.Builder(mContext, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setMessage(R.string.apply_these_changes)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();

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
        dialog.show();

        Window window = dialog.getWindow();
        assert window != null;

        //대화창을 화면아래에 떨구어 준다.
        Utils.makeBottomDialog(dialog);

        mDialogOpened = true;
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDialogOpened = false;
            }
        });
    }

    /**
     * 건반 숨기기
     */
    private void hideKeyboard() {
        final View focusedView = ((AppCompatActivity)mContext).getCurrentFocus();
        if (focusedView != null) {
            mInputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroy() {
        if (mView != null) {
            mView.setModel(null, true);
        }
        if (mModifyDialog != null) {
            mModifyDialog.dismiss();
            mModifyDialog = null;
        }

        super.onDestroy();
    }

    @Override
    public void eventsChanged() {
        // TODO Requery to see if event has changed
    }

    @Override
    public void minuteChanged() {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mView.prepareForSave();
        outState.putSerializable(BUNDLE_KEY_MODEL, mModel);
        outState.putInt(BUNDLE_KEY_EDIT_STATE, mModification);
        if (mEventBundle == null && mEvent != null) {
            mEventBundle = new EventBundle();
            mEventBundle.id = mEvent.id;
            if (mEvent.startTime != null) {
                mEventBundle.start = mEvent.startTime.toMillis(true);
            }
            if (mEvent.endTime != null) {
                mEventBundle.end = mEvent.endTime.toMillis(true);
            }
        }
        outState.putBoolean(BUNDLE_KEY_EDIT_ON_LAUNCH, mShowModifyDialogOnLaunch);
        outState.putSerializable(BUNDLE_KEY_EVENT, mEventBundle);
        outState.putBoolean(BUNDLE_KEY_EVENT_TYPES_VIEW_EXPANDED, mView.mEventTypesViewExpanded);
        outState.putBoolean(BUNDLE_KEY_DIALOG_OPENED, mDialogOpened);
    }

    @Override
    public long getSupportedEventTypes() {
        return 0;
    }

    @Override
    public void handleEvent(EventInfo event) {
    }

    /**
     * AlertDialog에서 확인, 취소 단추의 눌림동작
     * @param dialog
     * @param which {@link DialogInterface#BUTTON_POSITIVE} 혹은 {@link DialogInterface#BUTTON_NEGATIVE}
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE){   //확인단추를 눌렀을때
            onSave();
        }
        else if(which == DialogInterface.BUTTON_NEGATIVE){  //취소단추를 눌렀을때
            mOnDone.setDoneCode(Utils.DONE_EXIT);
            mOnDone.run();
        }
    }

    public EditText getTitleTextView() {
        return mView.mTitleTextView;
    }

    public EditText getDescriptionTextView() {
        return mView.mDescriptionTextView;
    }

    private static class EventBundle implements Serializable {
        private static final long serialVersionUID = 1L;
        long id = -1;
        long start = -1;
        long end = -1;
    }

    // TODO turn this into a helper function in EditEventHelper for building the
    // model
    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            //어떠한 리유로 하여 Query가 cursor를 돌려주지 않으면 아무 동작도 안한다.
            if (cursor == null) {
                return;
            }

            //Activity 가 종료되고 있으면 cursor 를 닫는다.
            final Activity activity = EditEventFragment.this.getActivity();
            if (activity == null || activity.isFinishing()) {
                cursor.close();
                return;
            }

            long eventId;
            switch (token) {
                case TOKEN_EVENT:
                    //Cursor가 비였으면 동작을 끝낸다.
                    if (cursor.getCount() == 0) {
                        //이런 현상은 일정이 삭제될때 발생할수 있다.
                        cursor.close();
                        mOnDone.setDoneCode(Utils.DONE_EXIT);
                        mOnDone.run();
                        return;
                    }

                    mOriginalModel = new CalendarEventModel();
                    EditEventHelper.setModelFromCursor(mOriginalModel, cursor);
                    EditEventHelper.setModelFromCursor(mModel, cursor);
                    cursor.close();

                    mOriginalModel.mUri = mUri.toString();

                    mModel.mUri = mUri.toString();
                    mModel.mOriginalStart = mBegin;
                    mModel.mOriginalEnd = mEnd;
                    mModel.mIsFirstEventInSeries = mBegin == mOriginalModel.mStart;
                    mModel.mStart = mBegin;
                    mModel.mEnd = mEnd;
                    eventId = mModel.mId;

                    // TOKEN_ATTENDEES
                    if (mModel.mHasAttendeeData && eventId != -1) {
                        Uri attUri = Attendees.CONTENT_URI;
                        String[] whereArgs = {
                                Long.toString(eventId)
                        };
                        mHandler.startQuery(TOKEN_ATTENDEES, null, attUri,
                                EditEventHelper.ATTENDEES_PROJECTION,
                                EditEventHelper.ATTENDEES_WHERE /* selection */,
                                whereArgs /* selection args */, null /* sort order */);
                    } else {
                        setModelIfDone(TOKEN_ATTENDEES);
                    }

                    // TOKEN_REMINDERS
                    if (mModel.mHasAlarm && mReminders == null) {
                        Uri rUri = Reminders.CONTENT_URI;
                        String[] remArgs = {
                                Long.toString(eventId)
                        };
                        mHandler.startQuery(TOKEN_REMINDERS, null, rUri,
                                EditEventHelper.REMINDERS_PROJECTION,
                                EditEventHelper.REMINDERS_WHERE /* selection */,
                                remArgs /* selection args */, null /* sort order */);
                    } else {
                        if (mReminders == null) {
                            // mReminders should not be null.
                            mReminders = new ArrayList<ReminderEntry>();
                        } else {
                            Collections.sort(mReminders);
                        }
                        mOriginalModel.mReminders = mReminders;
                        mModel.mReminders =
                                (ArrayList<ReminderEntry>) mReminders.clone();
                        setModelIfDone(TOKEN_REMINDERS);
                    }

                    // TOKEN_CALENDARS
                    String[] selArgs = {
                            Long.toString(mModel.mCalendarId)
                    };
                    mHandler.startQuery(TOKEN_CALENDARS, null, Calendars.CONTENT_URI,
                            EditEventHelper.CALENDARS_PROJECTION, EditEventHelper.CALENDARS_WHERE,
                            selArgs /* selection args */, null /* sort order */);

                    // TOKEN_COLORS
                    mHandler.startQuery(TOKEN_COLORS, null, Colors.CONTENT_URI,
                            EditEventHelper.COLORS_PROJECTION,
                            Colors.COLOR_TYPE + "=" + Colors.TYPE_EVENT, null, null);

                    setModelIfDone(TOKEN_EVENT);
                    break;
                case TOKEN_ATTENDEES:
                    try {
                        while (cursor.moveToNext()) {
                            String name = cursor.getString(EditEventHelper.ATTENDEES_INDEX_NAME);
                            String email = cursor.getString(EditEventHelper.ATTENDEES_INDEX_EMAIL);
                            int status = cursor.getInt(EditEventHelper.ATTENDEES_INDEX_STATUS);
                            int relationship = cursor
                                    .getInt(EditEventHelper.ATTENDEES_INDEX_RELATIONSHIP);
                            if (relationship == Attendees.RELATIONSHIP_ORGANIZER) {
                                if (email != null) {
                                    mModel.mOrganizer = email;
                                    mModel.mIsOrganizer = mModel.mOwnerAccount
                                            .equalsIgnoreCase(email);
                                    mOriginalModel.mOrganizer = email;
                                    mOriginalModel.mIsOrganizer = mOriginalModel.mOwnerAccount
                                            .equalsIgnoreCase(email);
                                }

                                if (TextUtils.isEmpty(name)) {
                                    mModel.mOrganizerDisplayName = mModel.mOrganizer;
                                    mOriginalModel.mOrganizerDisplayName =
                                            mOriginalModel.mOrganizer;
                                } else {
                                    mModel.mOrganizerDisplayName = name;
                                    mOriginalModel.mOrganizerDisplayName = name;
                                }
                            }

                            if (email != null) {
                                if (mModel.mOwnerAccount != null &&
                                        mModel.mOwnerAccount.equalsIgnoreCase(email)) {
                                    int attendeeId =
                                            cursor.getInt(EditEventHelper.ATTENDEES_INDEX_ID);
                                    mModel.mOwnerAttendeeId = attendeeId;
                                    mModel.mSelfAttendeeStatus = status;
                                    mOriginalModel.mOwnerAttendeeId = attendeeId;
                                    mOriginalModel.mSelfAttendeeStatus = status;
                                    continue;
                                }
                            }
                            Attendee attendee = new Attendee(name, email);
                            attendee.mStatus = status;
                            mModel.addAttendee(attendee);
                            mOriginalModel.addAttendee(attendee);
                        }
                    } finally {
                        cursor.close();
                    }

                    setModelIfDone(TOKEN_ATTENDEES);
                    break;
                case TOKEN_REMINDERS:
                    try {
                        // Add all reminders to the models
                        while (cursor.moveToNext()) {
                            int minutes = cursor.getInt(EditEventHelper.REMINDERS_INDEX_MINUTES);
                            int method = cursor.getInt(EditEventHelper.REMINDERS_INDEX_METHOD);
                            ReminderEntry re = ReminderEntry.valueOf(minutes, method);
                            mModel.mReminders.add(re);
                            mOriginalModel.mReminders.add(re);
                        }

                        // Sort appropriately for display
                        Collections.sort(mModel.mReminders);
                        Collections.sort(mOriginalModel.mReminders);
                    } finally {
                        cursor.close();
                    }

                    setModelIfDone(TOKEN_REMINDERS);
                    break;
                case TOKEN_CALENDARS:
                    try {
                        if (mModel.mId == -1) {
                            MatrixCursor matrixCursor = Utils.matrixCursorFromCursor(cursor);
                            if(mView.findDefaultCalendarPosition(matrixCursor) == -1 ) {
                                //달력계정이 하나도 없으면 "KR Calendar"라는 계정을 만든다.
                                CalendarRepository repository = new CalendarRepository(activity.getApplication());
                                repository.addLocalCalendar(OFFLINE_ACCOUNT_OWNER_NAME, DEFAULT_CALENDAR_ACCOUNT_NAME);
                                EditEventFragment.this.startQuery();
                            }
                            else {
                                mView.setCalendarsCursor(matrixCursor, isAdded() && isResumed()
                                );
                            }
                        } else {
                            //Cursor 로부터 Model 정보얻기
                            EditEventHelper.setModelFromCalendarCursor(mModel, cursor);
                            EditEventHelper.setModelFromCalendarCursor(mOriginalModel, cursor);
                        }
                    } finally {
                        cursor.close();
                    }
                    setModelIfDone(TOKEN_CALENDARS);
                    break;
                case TOKEN_COLORS:
                    if (cursor.moveToFirst()) {
                        EventColorCache cache = new EventColorCache();
                        do {
                            String colorKey = cursor.getString(EditEventHelper.COLORS_INDEX_COLOR_KEY);
                            int rawColor = cursor.getInt(EditEventHelper.COLORS_INDEX_COLOR);
                            int displayColor = Utils.getDisplayColorFromColor(rawColor);
                            String accountName = cursor
                                    .getString(EditEventHelper.COLORS_INDEX_ACCOUNT_NAME);
                            String accountType = cursor
                                    .getString(EditEventHelper.COLORS_INDEX_ACCOUNT_TYPE);
                            cache.insertColor(accountName, accountType,
                                    displayColor, colorKey);
                        } while (cursor.moveToNext());
                        cache.sortPalettes(new HsvColorComparator());

                        mModel.mEventColorCache = cache;
                    }
                    cursor.close();

                    setModelIfDone(TOKEN_COLORS);
                    break;
                default:
                    cursor.close();
                    break;
            }
        }
    }

    /**
     * 실행을 위한 flag값({@link #mCode})을 가진 Runnable 클라스
     */
    class Done implements EditEventHelper.EditDoneRunnable {
        /**
         * {@link Utils#DONE_SAVE} 혹은 {@link Utils#DONE_EXIT}
         */
        private int mCode = -1;

        @Override
        public void setDoneCode(int code) {
            mCode = code;
        }

        @Override
        public void run() {
            //Toast를 띄우기 위해 Context 를 얻는다.
            Context context = ContextHolder.Companion.getApplicationContext();

            //일정편집방식이 정해지지 않았을때에는 MODIFY_ALL로 설정한다.
            if (mModification == Utils.MODIFY_UNINITIALIZED) {
                mModification = Utils.MODIFY_ALL;
            }

            //UI 로부터 미리알림목록 얻기
            mModel.mReminders = EventViewUtils.reminderItemsToReminders(mView.mReminderItems,
                    mView.mReminderMinuteValues, mView.mReminderMethodValues);
            Collections.sort(mModel.mReminders);

            if ((mCode & Utils.DONE_SAVE) != 0 && mModel != null
                    && EditEventHelper.canModifyEvent(mModel)
                    && mView.prepareForSave()
                    && !isEmptyNewEvent()
                    && mModel.normalizeReminders()
                    && mHelper.saveEvent(mModel, mOriginalModel, mModification, true)) {
                int stringResource;
                if (mModel.mUri != null) {
                    stringResource = R.string.saving_event;
                } else {
                    stringResource = R.string.creating_event;
                }
                Toast.makeText(context, stringResource, Toast.LENGTH_SHORT).show();
            }
            else if ((mCode & Utils.DONE_SAVE) != 0 && mModel != null && isEmptyNewEvent()) {
                Toast.makeText(context, R.string.empty_event, Toast.LENGTH_SHORT).show();
            }

            //DONE_EXIT 가 코드에 포함되여있으면 Activity를 종료한다.
            if ((mCode & Utils.DONE_EXIT) != 0) {
                Activity a = EditEventFragment.this.getActivity();
                if (a != null) {
                    a.finish();
                }
            }

            hideKeyboard();
        }
    }
}
