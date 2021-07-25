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
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.event.CalendarEventModel.ReminderEntry;
import com.android.calendar.event.EditEventHelper.EditDoneRunnable;
import com.android.calendar.event.custom.CustomTextView;
import com.android.calendar.event.custom.EventTypeListAdapter;
import com.android.calendar.event.custom.GridSpacingItemDecoration;
import com.android.calendar.kr.dialogs.CustomDatePickerDialog;
import com.android.calendar.kr.dialogs.CustomTimePickerDialog;
import com.android.calendar.recurrencepicker.EventRecurrenceFormatter;
import com.android.calendar.recurrencepicker.RecurrencePickerDialog;
import com.android.calendar.settings.GeneralPreferences;
import com.android.calendar.utils.Utils;
import com.android.calendarcommon2.EventRecurrence;
import com.android.kr_common.Time;
import com.android.krcalendar.R;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.android.calendar.event.EditEventFragment.DEFAULT_CALENDAR_ACCOUNT_NAME;
import static com.android.calendar.event.EditEventFragment.OFFLINE_ACCOUNT_OWNER_NAME;
import static com.android.calendar.event.EventTypeManager.APP_EVENT_TYPE_COUNT;
import static com.android.calendar.event.EventTypeManager.EVENT_TYPES_PER_ROW;
import static com.android.calendar.event.EventTypeManager.EVENT_TYPE_DEFAULT;

/**
 * 일정보기화면 Fragment에서 UI갱신, 일정보관을 관리하는 클라스
 * @see EditEventFragment
 */
public class EditEventView implements View.OnClickListener,
        OnItemSelectedListener,
        RecurrencePickerDialog.OnRecurrenceSetListener,
        EventTypeListAdapter.EventTypeItemClickListener
{
    private static final String TAG = "EditEvent";

    //일정형식을 선택했을때의 Animation의 지속시간
    private static final int EVENT_TYPE_FADE_DURATION = 200;
    private static final String PERIOD_SPACE = ". ";

    private static final String FRAG_TAG_RECUR_PICKER = "recurrencePickerDialogFragment";

    ArrayList<View> mEditOnlyList = new ArrayList<View>();
    TextView mLoadingMessage;
    ScrollView mScrollView;
    LinearLayout mBottomActionBar;

    //`뒤로 가기`, `취소`, `보관`단추들
    View mBackButton;
    View mCancelButton;
    View mSaveButton;

    boolean mDialogOpened = false;

    TextView mEditEventTitle;
    SwitchCompat mAllDayCheckBox;
    Spinner mCalendarsSpinner;
    Button mRruleButton;
    EditText mTitleTextView;
    EditText mDescriptionTextView;
    LinearLayout mRemindersContainer;

    //날자, 시간에 해당한 View들
    View mEndRow, mStartRow, mStartDateColumn, mStartTimeColumn, mEndDateColumn, mEndTimeColumn;
    Button mStartDateButton, mEndDateButton, mStartTimeButton, mEndTimeButton;

    View mCalendarSelectorGroup;
    View mCalendarStaticGroup;
    View mDescriptionGroup;
    View mRemindersGroup;

    private ProgressDialog mLoadingCalendarsDialog;

    private final Activity mActivity;
    private final EditDoneRunnable mDone;
    private final View mView;
    private CalendarEventModel mModel;
    private Cursor mCalendarsCursor;
    private final boolean mEditMode;

    public ArrayList<Integer> mReminderMinuteValues;
    public ArrayList<String> mReminderMinuteLabels;
    public ArrayList<Integer> mReminderMethodValues;
    public ArrayList<String> mReminderMethodLabels;
    private int mDefaultReminderMinutes;
    private boolean mSaveAfterQueryComplete = false;
    public DateTime mStartTime;
    public DateTime mEndTime;
    private String mTimezone;
    private boolean mAllDay = false;
    private int mModification = EditEventHelper.MODIFY_UNINITIALIZED;
    public EventRecurrence mEventRecurrence = new EventRecurrence();
    public ArrayList<LinearLayout> mReminderItems = new ArrayList<LinearLayout>(0);
    private final ArrayList<ReminderEntry> mUnsupportedReminders = new ArrayList<ReminderEntry>();
    private String mRrule;

    //일정형식목록에 해당한 RecyclerView 와 Adapter
    RecyclerView mEventTypeSelectView;
    EventTypeListAdapter mEventTypeAdapter;
    List<EventTypeManager.OneEventType> mEventTypeList = new ArrayList<>();

    int mRunningAnimatorCount;
    boolean mEventTypesViewExpanded = true;
    int mSelectedEventType = EVENT_TYPE_DEFAULT;

    @SuppressLint("ClickableViewAccessibility")
    public EditEventView(Activity activity, EditEventFragment editEventFragment, boolean editMode, View view, EditDoneRunnable done) {
        mActivity = activity;
        mView = view;
        mDone = done;
        mEditMode = editMode;

        mLoadingMessage = (TextView) view.findViewById(R.id.loading_message);
        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        mBottomActionBar = view.findViewById(R.id.bottom_action_bar);
        mEditEventTitle = view.findViewById(R.id.edit_event_title);
        mBackButton = view.findViewById(R.id.back_button);
        mSaveButton = view.findViewById(R.id.go_save);
        mCancelButton = view.findViewById(R.id.go_cancel);

        if(mEditMode)   //화면제목을 `일정편집`으로 설정한다.
            mEditEventTitle.setText(activity.getString(R.string.event_edit));

        mCalendarsSpinner = (Spinner) view.findViewById(R.id.calendars_spinner);
        mTitleTextView = (EditText) view.findViewById(R.id.title);
        mDescriptionTextView = (EditText) view.findViewById(R.id.description);

        mStartRow = view.findViewById(R.id.from_row);
        mStartDateColumn = view.findViewById(R.id.start_date_column);
        mStartTimeColumn = view.findViewById(R.id.start_time_column);
        mStartDateButton = view.findViewById(R.id.start_date);
        mStartTimeButton = view.findViewById(R.id.start_time);

        mEndRow = view.findViewById(R.id.to_row);
        mEndDateColumn = view.findViewById(R.id.end_date_column);
        mEndTimeColumn = view.findViewById(R.id.end_time_column);
        mEndDateButton = view.findViewById(R.id.end_date);
        mEndTimeButton = view.findViewById(R.id.end_time);

        mAllDayCheckBox = (SwitchCompat) view.findViewById(R.id.is_all_day);
        mRruleButton = (Button) view.findViewById(R.id.rrule);

        mCalendarSelectorGroup = view.findViewById(R.id.calendar_selector_group);
        mCalendarStaticGroup = view.findViewById(R.id.calendar_group);
        mRemindersGroup = view.findViewById(R.id.reminders_row);
        mDescriptionGroup = view.findViewById(R.id.description_row);

        mEditOnlyList.add(view.findViewById(R.id.all_day_row));
        mEditOnlyList.add(view.findViewById(R.id.from_row));
        mEditOnlyList.add(view.findViewById(R.id.to_row));

        mRemindersContainer = (LinearLayout) view.findViewById(R.id.reminder_items_container);

        mTimezone = Utils.getTimeZone(activity, null);
        mStartTime = new DateTime();
        mEndTime = new DateTime();

        //`적재중`화면을 보여준다.
        setModel(null, true);

        FragmentManager fm = ((FragmentActivity)activity).getSupportFragmentManager();
        RecurrencePickerDialog rpd = (RecurrencePickerDialog) fm
                .findFragmentByTag(FRAG_TAG_RECUR_PICKER);
        if (rpd != null) {
            rpd.setOnRecurrenceSetListener(this);
        }

        View allDayRow = view.findViewById(R.id.all_day_row);
        allDayRow.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mAllDayCheckBox.setChecked(!mAllDayCheckBox.isChecked());
            }
        });

        DateClickListener startDateAction = new DateClickListener(true);
        DateClickListener endDateAction = new DateClickListener(false);

        mStartRow.setOnClickListener(startDateAction);
        mEndRow.setOnClickListener(endDateAction);
        mStartDateColumn.setOnClickListener(startDateAction);
        mEndDateColumn.setOnClickListener(endDateAction);
        mStartTimeColumn.setOnClickListener(this);
        mEndTimeColumn.setOnClickListener(this);

        View repeatRow = view.findViewById(R.id.repeat_row);
        repeatRow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle b = new Bundle();
                b.putLong(RecurrencePickerDialog.BUNDLE_START_TIME_MILLIS,
                        mStartTime.getMillis());

                b.putString(RecurrencePickerDialog.BUNDLE_RRULE, mRrule);

                FragmentManager fm = ((FragmentActivity)mActivity).getSupportFragmentManager();
                RecurrencePickerDialog dialog = (RecurrencePickerDialog)fm.findFragmentByTag(FRAG_TAG_RECUR_PICKER);
                if(dialog == null) {
                    dialog = new RecurrencePickerDialog();
                    dialog.setArguments(b);
                    dialog.setOnRecurrenceSetListener(EditEventView.this);
                    dialog.show(fm, FRAG_TAG_RECUR_PICKER);
                }
            }
        });

        //`뒤로 가기`단추의 눌림동작
        mBackButton.setOnClickListener(v -> {
            EditEventActivity.getEditEventActivity(mView.getContext()).onBackPressed();
        });

        //보관, 취소단추들의 눌림동작들을 추가한다.
        View.OnClickListener saveOrCancel = v -> {
            if(v == mSaveButton){
                editEventFragment.onSave();
            }
            else if(v == mCancelButton){
                editEventFragment.onCancel();
            }
        };
        mSaveButton.setOnClickListener(saveOrCancel);
        mCancelButton.setOnClickListener(saveOrCancel);

        /*-- 일정형식목록 Recycler view 설정 --*/
        mEventTypeSelectView = view.findViewById(R.id.event_type_select);

        Context context = mView.getContext();
        //일정형식목록 얻기
        mEventTypeList.clear();
        mEventTypeList.addAll(Arrays.asList(EventTypeManager.APP_EVENT_TYPES).subList(0, APP_EVENT_TYPE_COUNT));

        mEventTypeAdapter = new EventTypeListAdapter(view.getContext(), this, mEventTypeList);

        GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), EVENT_TYPES_PER_ROW);
        mEventTypeSelectView.setLayoutManager(layoutManager);

        int spacing = (int) (
                (context.getResources().getDisplayMetrics().widthPixels - mEventTypeSelectView.getPaddingStart() - mEventTypeSelectView.getPaddingEnd() -
                        context.getResources().getDimension(R.dimen.simple_event_type_width) * EVENT_TYPES_PER_ROW)/(EVENT_TYPES_PER_ROW - 1));
        mEventTypeSelectView.addItemDecoration(new GridSpacingItemDecoration(EVENT_TYPES_PER_ROW, spacing, false));
        mEventTypeSelectView.setAdapter(mEventTypeAdapter);

        Utils.addCommonTouchListener(mBackButton);
        Utils.addCommonTouchListener(mSaveButton);
        Utils.addCommonTouchListener(mCancelButton);
        Utils.addCommonTouchListener(mStartTimeColumn);
        Utils.addCommonTouchListener(mEndTimeColumn);
        Utils.addCommonTouchListener(allDayRow);
        Utils.addCommonTouchListener(repeatRow);
    }

    /**
     * Integer Array 자원을 불러들여서 돌려준다.
     * @param r Resources
     * @param resNum ArrayRes
     */
    private static ArrayList<Integer> loadIntegerArray(Resources r, @ArrayRes int resNum) {
        int[] values = r.getIntArray(resNum);
        int size = values.length;
        ArrayList<Integer> list = new ArrayList<>(size);

        for (int val : values) {
            list.add(val);
        }

        return list;
    }

    /**
     * String Array 자원을 불러들여서 돌려준다.
     * @param r Resources
     * @param resNum ArrayRes
     */
    private static ArrayList<String> loadStringArray(Resources r, @ArrayRes int resNum) {
        String[] labels = r.getStringArray(resNum);
        return new ArrayList<>(Arrays.asList(labels));
    }

    /**
     * 시작시간, 마감시간에 해당한 TextView 들에 문자렬 채워넣기한다.
     */
    private void populateWhen() {
        setDate(mStartDateButton, mStartTime, false);
        setDate(mEndDateButton, mEndTime, true);
    }

    /**
     * 반복항목에 해당한 TextView 에 문자렬 채워넣기한다.
     */
    private void populateRepeats() {
        Resources r = mActivity.getResources();
        String repeatString;
        boolean enabled;
        if (!TextUtils.isEmpty(mRrule)) {
            repeatString = Utils.getUpperString(Objects.requireNonNull(EventRecurrenceFormatter.getRepeatString(mActivity, r,
                    mEventRecurrence, true)));

            enabled = RecurrencePickerDialog.canHandleRecurrenceRule(mEventRecurrence);
            if (!enabled) {
                Log.e(TAG, "UI can't handle " + mRrule);
            }
        } else {
            repeatString = r.getString(R.string.does_not_repeat);
            enabled = true;
        }

        mRruleButton.setText(repeatString);

        if (mModel.mOriginalSyncId != null) {
            enabled = false;    //편집불가능하게 한다.
        }
        mRruleButton.setEnabled(enabled);
    }

    /**
     * 일정형식목록 RecyclerView 의 자식 View 를 돌려준다
     * @param position 위치
     */
    CustomTextView getEventTypeItem(int position){
        if(mEventTypeSelectView.findViewHolderForAdapterPosition(position) != null){
            return (CustomTextView)(Objects.requireNonNull(mEventTypeSelectView.findViewHolderForAdapterPosition(position)).itemView);
        }
        return null;
    }

    /**
     * 일정형식 하나를 눌렀을때
     * @param position 위치
     */
    @Override
    public void onClickEventTypeItem(int position) {
        mRunningAnimatorCount = 0;

        int length = mEventTypeAdapter.getItemCount();
        for (int i = 0; i < length; i ++) {
            CustomTextView child = getEventTypeItem(i);
            if(child != null) {
                AlphaAnimation animation = new AlphaAnimation(1, 0);
                mRunningAnimatorCount ++;

                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mRunningAnimatorCount --;
                        if(mRunningAnimatorCount != 0)
                            return;

                        //마감동작
                        mEventTypesViewExpanded = !mEventTypesViewExpanded;
                        if(mEventTypesViewExpanded) {
                            mEventTypeAdapter.setAdapterData(mEventTypeList);
                        }
                        else{
                            EventTypeManager.OneEventType eventType = mEventTypeList.get(position);
                            mEventTypeAdapter.setAdapterData(Collections.singletonList(eventType));
                            mSelectedEventType = eventType.id;

                            //일정형식에 따라 UI를 갱신한다.
                            fillUIFromEventType(eventType);
                        }
                        mEventTypeAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onAnimationStart(Animation animation) { }
                    @Override
                    public void onAnimationRepeat(Animation animation) { }
                });
                animation.setDuration(EVENT_TYPE_FADE_DURATION);
                child.startAnimation(animation);
            }
        }
    }

    /**
     * 보관을 할 준비가 되였는가를 돌려준다.
     */
    public boolean prepareForSave() {
        if (mModel == null || (mCalendarsCursor == null && mModel.mUri == null)) {
            return false;
        }
        return fillModelFromUI();
    }

    /**
     * 눌림동작들
     * @param view View
     */
    @Override
    public void onClick(View view) {
        if(view == mStartTimeColumn) { //시작날자(하루종일일때)
            //대화창이 두번이상 켜지는것을 방지한다.
            if(!mDialogOpened) {
                mDialogOpened = true;
                CustomTimePickerDialog dialog = new CustomTimePickerDialog(mStartTime.getHourOfDay(), mStartTime.getMinuteOfHour(), true, mActivity);
                Utils.makeBottomDialog(dialog); //화면아래에 떨구어준다.
                dialog.show();

                dialog.setOnTimeSelectedListener(new DateTimeListener());
                dialog.setOnDismissListener(dialog1 -> mDialogOpened = false);
            }
        }

        else if(view == mEndTimeColumn) { //마감날자(하루종일일때)
            //대화창이 두번이상 켜지는것을 방지한다.
            if(!mDialogOpened) {
                mDialogOpened = true;
                CustomTimePickerDialog dialog = new CustomTimePickerDialog(mEndTime.getHourOfDay(), mEndTime.getMinuteOfHour(), false, mActivity);
                Utils.makeBottomDialog(dialog); //화면아래에 떨구어준다.
                dialog.show();

                dialog.setOnTimeSelectedListener(new DateTimeListener());
                dialog.setOnDismissListener(dialog1 -> mDialogOpened = false);
            }
        }

        else if(view instanceof AppCompatImageButton) { //미리알림삭제
            LinearLayout reminderItem = (LinearLayout) view.getParent().getParent();
            LinearLayout parent = (LinearLayout) reminderItem.getParent();
            parent.removeView(reminderItem);
            mReminderItems.remove(reminderItem);
            updateRemindersVisibility(mReminderItems.size());
            EventViewUtils.updateAddReminderButton(mView, mReminderItems, EventViewUtils.DEFAULT_MAX_REMINDERS/*mModel.mCalendarMaxReminders*/);
        }
    }

    /**
     * 반복설정이 갱신되였을때
     * @param rrule 반복문자렬
     */
    @Override
    public void onRecurrenceSet(String rrule) {
        Log.d(TAG, "Old rrule:" + mRrule);
        Log.d(TAG, "New rrule:" + rrule);
        mRrule = rrule;
        if (mRrule != null) {
            mEventRecurrence.parse(mRrule);
        }
        populateRepeats();
    }

    /**
     * 일정형식이 변할때 자동적으로 UI 갱신
     * @param eventType 일정형식
     */
    private void fillUIFromEventType(EventTypeManager.OneEventType eventType){
        //일정제목을 일정형식이름으로 설정
        if(eventType.id == EVENT_TYPE_DEFAULT){
            mTitleTextView.setText("");
        }
        else {
            mTitleTextView.setText(eventType.title);
        }

        //하루종일, 반복항목 설정
        if(eventType.id == EventTypeManager.EVENT_TYPE_BIRTHDAY){   //생일
            mAllDayCheckBox.setChecked(true);
            onRecurrenceSet("FREQ=YEARLY;WKST=SU");
        }
        else if(eventType.id == EventTypeManager.EVENT_TYPE_HOLIDAY){   //명절
            mAllDayCheckBox.setChecked(true);
            onRecurrenceSet(null);
        }
        else{   //기타 나머지
            mAllDayCheckBox.setChecked(false);
            onRecurrenceSet(null);
        }
    }

    /**
     * UI로부터 model정보들을 생성한다.
     * @return model자료가 null false, 그렇지 않으면 true를 돌려준다.
     */
    private boolean fillModelFromUI() {
        if (mModel == null) {
            return false;
        }
        mModel.mReminders = EventViewUtils.reminderItemsToReminders(mReminderItems,
                mReminderMinuteValues, mReminderMethodValues);
        mModel.mReminders.addAll(mUnsupportedReminders);
        mModel.normalizeReminders();
        mModel.mHasAlarm = mReminderItems.size() > 0;
        mModel.mTitle = mTitleTextView.getText().toString();
        mModel.mAllDay = mAllDayCheckBox.isChecked();
        mModel.mDescription = mDescriptionTextView.getText().toString();
        mModel.mEventType = mEventTypesViewExpanded? EVENT_TYPE_DEFAULT : mSelectedEventType;
        if (TextUtils.isEmpty(mModel.mLocation)) {
            mModel.mLocation = null;
        }
        if (TextUtils.isEmpty(mModel.mDescription)) {
            mModel.mDescription = null;
        }

        //`일정추가`일때는 Calendar 정보를 불러온다.
        if (mModel.mUri == null) {
            mModel.mCalendarId = mCalendarsSpinner.getSelectedItemId();
            int calendarCursorPosition = mCalendarsSpinner.getSelectedItemPosition();
            if (mCalendarsCursor.moveToPosition(calendarCursorPosition)) {
                String calendarOwner = mCalendarsCursor.getString(
                        EditEventHelper.CALENDARS_INDEX_OWNER_ACCOUNT);
                mModel.mOwnerAccount = calendarOwner;
                mModel.mOrganizer = calendarOwner;
                mModel.mCalendarId = mCalendarsCursor.getLong(EditEventHelper.CALENDARS_INDEX_ID);
            }
        }

        if (mModel.mAllDay) {   //하루종일
            //시작, 마감날자들의 시, 분, 초를 0으로 만든 시간들을 얻는다. (그날자의 0시 0분)
            mTimezone = Time.TIMEZONE_UTC;
            DateTime startTime = new DateTime(DateTimeZone.UTC).withDate(mStartTime.getYear(), mStartTime.getMonthOfYear(), mStartTime.getDayOfMonth()).withMillisOfDay(0);
            DateTime endTime = new DateTime(DateTimeZone.UTC).withDate(mEndTime.getYear(), mEndTime.getMonthOfYear(), mEndTime.getDayOfMonth()).withMillisOfDay(0);

            //Model 의 시작시간 설정
            mModel.mStart = startTime.getMillis();

            //Model 의 마감시간 설정
            final long normalizedEndTimeMillis = endTime.getMillis();

            mModel.mEnd = endTime.getMillis();
            if (normalizedEndTimeMillis < mModel.mStart) { //마감시간이 시작시간보다 전에 있는 경우(이것은 오유로 들어온것이다.)
                //마감시간을 시작시간보다 하루 뒤에 있는 시간으로 설정
                mModel.mEnd = mModel.mStart + DateUtils.DAY_IN_MILLIS;
            } else {
                mModel.mEnd = normalizedEndTimeMillis;
            }
        } else {    //하루종일이 아님
            mModel.mStart = mStartTime.getMillis();
            mModel.mEnd = mEndTime.getMillis();
        }
        mModel.mTimezone = mTimezone;

        /*--
        아래의 comment를 통해 무시해버린 코드는 반복일정편집 - `선택된 일정만 편집`할때 새로 창조되는 일정을 비반복일정으로 만들어주는 동작이다.
        하지만 그것을 반복설정을 강제적으로 무시할 필요는 없을것 같아 반복설정은 그대로 보존하도록 해주었다.
        요구사항에 따라서 그것이 비반복일정이 되여야 될수도 있고 입력된 그대로 설정되여야 할수도 있다.
         --*/
        /*
        if (mModification == EditEventHelper.MODIFY_SELECTED) {
            mModel.mRrule = null;
        } else {
            mModel.mRrule = mRrule;
        }
         */
        mModel.mRrule = mRrule;

        return true;
    }

    /**
     * UI에 보여줄 미리알림목록을 불러들인다.
     */
    private void prepareReminders() {
        CalendarEventModel model = mModel;
        Resources r = mActivity.getResources();

        //Resource로부터 전체 미리알림 분, 방식들을 모두 불러들인다.
        mReminderMinuteValues = loadIntegerArray(r, R.array.reminder_minutes_values);
        mReminderMinuteLabels = loadStringArray(r, R.array.reminder_minutes_labels);
        mReminderMethodValues = loadIntegerArray(r, R.array.reminder_methods_values);
        mReminderMethodLabels = loadStringArray(r, R.array.reminder_methods_labels);

        //허용되지 않은 미리알림방식들은 없앤다.
        if (mModel.mCalendarAllowedReminders != null) {
            EventViewUtils.reduceMethodList(mReminderMethodValues, mReminderMethodLabels,
                    mModel.mCalendarAllowedReminders);
        }

        int numReminders = 0;
        if (model.mHasAlarm) {
            ArrayList<ReminderEntry> reminders = model.mReminders;
            numReminders = reminders.size();

            //일정에 있는 미리알림들도 목록에 추가한다.
            for (ReminderEntry re : reminders) {
                if (mReminderMethodValues.contains(re.getMethod())) {
                    EventViewUtils.addMinutesToList(mActivity, mReminderMinuteValues,
                            mReminderMinuteLabels, re.getMinutes());
                }
            }

            //UI에 미리알림목록을 반영한다.
            mUnsupportedReminders.clear();
            for (ReminderEntry re : reminders) {
                if (mReminderMethodValues.contains(re.getMethod())
                        || re.getMethod() == Reminders.METHOD_DEFAULT) {
                    EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderItems,
                            mReminderMinuteValues, mReminderMinuteLabels, mReminderMethodValues,
                            mReminderMethodLabels, re, EventViewUtils.DEFAULT_MAX_REMINDERS, false, null);
                } else {
                    mUnsupportedReminders.add(re);
                }
            }
        }

        //`미리알림추가`, `미리알림목록` View들의 Visible상태를 갱신한다.
        updateRemindersVisibility(numReminders);
        EventViewUtils.updateAddReminderButton(mView, mReminderItems, EventViewUtils.DEFAULT_MAX_REMINDERS/*mModel.mCalendarMaxReminders*/);
    }

    /**
     * Model 설정
     * @param model Model
     * @param eventTypesViewExpanded 일정형식목록이 펼쳐졌을때
     */
    public void setModel(CalendarEventModel model, boolean eventTypesViewExpanded) {
        mModel = model;

        if (model == null) {
            //Model 이 구축될때까지 Loading icon 을 보여준다.
            mLoadingMessage.setVisibility(View.VISIBLE);
            mScrollView.setVisibility(View.GONE);
            mBottomActionBar.setVisibility(View.GONE);
            return;
        }

        long begin = model.mStart;
        long end = model.mEnd;
        mTimezone = model.mTimezone; // 하루종일일정에 대해서는 이것이 UTC Timezone 으로 된다.

        //시작시간, 마감시간을 DateTime형으로 변환하여 얻는다.
        if (begin > 0) {
            if(model.mAllDay)
                mStartTime = new DateTime(DateTimeZone.UTC).withMillis(begin);
            else
                mStartTime = new DateTime(begin);
        }
        if (end > 0) {
            if(model.mAllDay)
                mEndTime = new DateTime(DateTimeZone.UTC).withMillis(end);
            else
                mEndTime = new DateTime(end);
        }

        mRrule = model.mRrule;
        if (!TextUtils.isEmpty(mRrule)) {
            mEventRecurrence.parse(mRrule);
        }

        if (mEventRecurrence.startDate == null) {
            Time time = new Time();
            time.set(mStartTime.getMillis());
            mEventRecurrence.startDate = time;
        }

        mAllDayCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mEndTime = mEndTime.plusDays(1);
                }
                else{
                    mEndTime = mEndTime.minusDays(1);
                }
                onAllDayVisibilityChanged(isChecked);
            }
        });

        mAllDay = false; // default to false. Let setAllDayViewsVisibility update it as needed
        if (model.mAllDay) {
            mEndTime = mEndTime.minusDays(1);
            mAllDayCheckBox.setChecked(true);
            mTimezone = Utils.getTimeZone(mActivity, null);
        } else {
            mAllDayCheckBox.setChecked(false);
        }

        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(mActivity);
        String defaultReminderString = prefs.getString(
                GeneralPreferences.KEY_DEFAULT_REMINDER, GeneralPreferences.NO_REMINDER_STRING);
        mDefaultReminderMinutes = Integer.parseInt(defaultReminderString);

        prepareReminders();

        View reminderAddButton = mView.findViewById(R.id.reminder_add);
        reminderAddButton.setOnClickListener(v -> addReminder());
        Utils.addCommonTouchListener(reminderAddButton);

        if (model.mTitle != null) {
            mTitleTextView.setTextKeepState(model.mTitle);
        }

        if (model.mDescription != null) {
            mDescriptionTextView.setTextKeepState(model.mDescription);
        }

        if (model.mUri != null) {
            // This is an existing event so hide the calendar spinner
            // since we can't change the calendar.
            View calendarGroup = mView.findViewById(R.id.calendar_selector_group);
            calendarGroup.setVisibility(View.GONE);
            TextView tv = (TextView) mView.findViewById(R.id.calendar_textview);
            tv.setText(model.mCalendarDisplayName);
            tv = (TextView) mView.findViewById(R.id.calendar_textview_secondary);
            if (tv != null) {
                tv.setText(model.mOwnerAccount);
            }
        } else {
            View calendarGroup = mView.findViewById(R.id.calendar_group);
            calendarGroup.setVisibility(View.GONE);
        }

        if(mEditMode) {
            mSelectedEventType = model.mEventType;
            mEventTypesViewExpanded = false;

            mEventTypeAdapter.setAdapterData(Collections.singletonList(EventTypeManager.getEventTypeFromId(mSelectedEventType)));
            mEventTypeAdapter.notifyDataSetChanged();
        } else if(!eventTypesViewExpanded){
            mSelectedEventType = model.mEventType;
            mEventTypesViewExpanded = false;

            mEventTypeAdapter.setAdapterData(Collections.singletonList(EventTypeManager.getEventTypeFromId(mSelectedEventType)));
            mEventTypeAdapter.notifyDataSetChanged();
        }

        populateWhen();
        populateRepeats();

        updateView();
        mScrollView.setVisibility(View.VISIBLE);
        mBottomActionBar.setVisibility(View.VISIBLE);
        mLoadingMessage.setVisibility(View.GONE);
        sendAccessibilityEvent();
    }

    private void sendAccessibilityEvent() {
        AccessibilityManager am =
                (AccessibilityManager) mActivity.getSystemService(Service.ACCESSIBILITY_SERVICE);
        if (!am.isEnabled() || mModel == null) {
            return;
        }
        StringBuilder b = new StringBuilder();
        addFieldsRecursive(b, mView);
        CharSequence msg = b.toString();

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        event.setClassName(getClass().getName());
        event.setPackageName(mActivity.getPackageName());
        event.getText().add(msg);
        event.setAddedCount(msg.length());

        am.sendAccessibilityEvent(event);
    }

    private void addFieldsRecursive(StringBuilder b, View v) {
        if (v == null || v.getVisibility() != View.VISIBLE) {
            return;
        }
        if (v instanceof TextView) {
            CharSequence tv = ((TextView) v).getText();
            if (!TextUtils.isEmpty(tv.toString().trim())) {
                b.append(tv).append(PERIOD_SPACE);
            }
        } else if (v instanceof RadioGroup) {
            RadioGroup rg = (RadioGroup) v;
            int id = rg.getCheckedRadioButtonId();
            if (id != View.NO_ID) {
                b.append(((RadioButton) (v.findViewById(id))).getText()).append(PERIOD_SPACE);
            }
        } else if (v instanceof Spinner) {
            Spinner s = (Spinner) v;
            if (s.getSelectedItem() instanceof String) {
                String str = ((String) (s.getSelectedItem())).trim();
                if (!TextUtils.isEmpty(str)) {
                    b.append(str).append(PERIOD_SPACE);
                }
            }
        } else if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int children = vg.getChildCount();
            for (int i = 0; i < children; i++) {
                addFieldsRecursive(b, vg.getChildAt(i));
            }
        }
    }

    /**
     * Configures the Calendars spinner.  This is only done for new events, because only new
     * events allow you to select a calendar while editing an event.
     * <p>
     * We tuck a reference to a Cursor with calendar database data into the spinner, so that
     * we can easily extract calendar-specific values when the value changes (the spinner's
     * onItemSelected callback is configured).
     */
    public void setCalendarsCursor(Cursor cursor, boolean userVisible) {
        // If there are no syncable calendars, then we cannot allow
        // creating a new event.
        mCalendarsCursor = cursor;
        if (cursor == null || cursor.getCount() == 0) {
            // Cancel the "loading calendars" dialog if it exists
            if (mSaveAfterQueryComplete) {
                mLoadingCalendarsDialog.cancel();
            }

            return;
        }

        int selection;
        selection = findDefaultCalendarPosition(cursor);

        // populate the calendars spinner
        CalendarsAdapter adapter = new CalendarsAdapter(mActivity,
                R.layout.calendars_spinner_item, cursor);
        mCalendarsSpinner.setAdapter(adapter);
        mCalendarsSpinner.setOnItemSelectedListener(this);
        mCalendarsSpinner.setSelection(selection);

        if (mSaveAfterQueryComplete) {
            mLoadingCalendarsDialog.cancel();
            if (prepareForSave()) {
                int exit = userVisible ? Utils.DONE_EXIT : 0;
                mDone.setDoneCode(Utils.DONE_SAVE | exit);
                mDone.run();
            } else if (userVisible) {
                mDone.setDoneCode(Utils.DONE_EXIT);
                mDone.run();
            } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "SetCalendarsCursor:Save failed and unable to exit view");
            }
        }
    }

    /**
     * View 들을 갱신한다.
     */
    public void updateView() {
        if (mModel == null) {
            return;
        }
        if (EditEventHelper.canModifyEvent(mModel)) {
            setViewStates(mModification);
        } else {
            setViewStates(Utils.MODIFY_UNINITIALIZED);
        }
    }

    /**
     * 편집방식에 따라서 View 들을 갱신한다.
     */
    private void setViewStates(int mode) {
        //일정을 편집할수 있는지 먼저 검사한다.
        if (mode == Utils.MODIFY_UNINITIALIZED || !EditEventHelper.canModifyEvent(mModel)) {
            mCalendarSelectorGroup.setVisibility(View.GONE);
            mCalendarStaticGroup.setVisibility(View.GONE);
            mRruleButton.setEnabled(false);
            if (EditEventHelper.canAddReminders(mModel)) {
                mRemindersGroup.setVisibility(View.VISIBLE);
            } else {
                mRemindersGroup.setVisibility(View.GONE);
            }
        } else {
            if (mModel.mOriginalSyncId == null) {
                mRruleButton.setEnabled(true);
            } else {
                mRruleButton.setEnabled(false);
                mRruleButton.setBackground(null);
            }
        }

        onAllDayVisibilityChanged(mAllDayCheckBox.isChecked());
    }

    /**
     * 편집방식 설정
     * @param modifyWhich 편집방식 {@link Utils#MODIFY_ALL}
     */
    public void setModification(int modifyWhich) {
        mModification = modifyWhich;
        updateView();
    }

    /**
     * Cursor 로부터 기정 Calendar 를 찾아서 번호를 돌려준다.
     * @param calendarsCursor Cursor
     */
    public int findDefaultCalendarPosition(Cursor calendarsCursor) {
        if (calendarsCursor.getCount() <= 0) {
            return -1;
        }

        String defaultCalendar = OFFLINE_ACCOUNT_OWNER_NAME + "/" + DEFAULT_CALENDAR_ACCOUNT_NAME;

        int calendarsOwnerIndex = calendarsCursor.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT);
        int calendarNameIndex = calendarsCursor.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
        int position = 0;
        calendarsCursor.moveToPosition(-1);
        while (calendarsCursor.moveToNext()) {
            String calendarOwner = calendarsCursor.getString(calendarsOwnerIndex);
            String calendarName = calendarsCursor.getString(calendarNameIndex);
            String currentCalendar = calendarOwner + "/" + calendarName;
            if (defaultCalendar.equals(currentCalendar)) { //기정 Calendar 와 같은지 비교
                return position;
            }
            position++;
        }
        return -1;
    }

    /**
     * 미리알림개수에 따라서 미리알림을 숨기기 혹은 현시한다.
     * @param numReminders 미리알림개수
     */
    private void updateRemindersVisibility(int numReminders) {
        if (numReminders == 0) {
            mRemindersContainer.setVisibility(View.GONE);
        } else {
            mRemindersContainer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 미리알림을 추가한다.
     */
    private void addReminder() {
        //1개의 미리알림을 추가한다.
        EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderItems,
                mReminderMinuteValues, mReminderMinuteLabels,
                mReminderMethodValues, mReminderMethodLabels,
                ReminderEntry.valueOf(mDefaultReminderMinutes),
                EventViewUtils.DEFAULT_MAX_REMINDERS, false, null);

        //미리알림view, 알림추가단추의 visiblity 갱신
        updateRemindersVisibility(mReminderItems.size());
        EventViewUtils.updateAddReminderButton(mView, mReminderItems, EventViewUtils.DEFAULT_MAX_REMINDERS/*mModel.mCalendarMaxReminders*/);
    }

    @SuppressLint("SetTextI18n")
    private void setDate(TextView view, DateTime dateTime, boolean isEndButton) {
        final DateTime outDate;
        if(mAllDay && isEndButton) {
            outDate = dateTime.minusDays(1);
        }
        else {
            outDate = new DateTime(dateTime);
        }

        @SuppressLint("DefaultLocale") final String stringDate = String.format("%1$d.%2$02d.%3$02d", outDate.getYear(), outDate.getMonthOfYear(), outDate.getDayOfMonth());
        view.setText(stringDate);
    }

    private void setTime(TextView view, DateTime dateTime) {
        String stringTime = DateUtils.formatDateTime(mActivity, dateTime.getMillis(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_12HOUR);
        view.setText(stringTime);
    }

    /**
     * 하루종일 CheckBox 의 check상태가 변하였을때 호출된다.
     * @param isChecked CheckBox 가  check 되였는가?
     */
    protected void onAllDayVisibilityChanged(boolean isChecked) {
        mAllDay = isChecked;

        setDate(mStartDateButton, mStartTime, false);
        setDate(mEndDateButton, mEndTime, true);
        setTime(mStartTimeButton, mStartTime);
        setTime(mEndTimeButton, mEndTime);

        if(isChecked) { //Check된 상태
            //시간에 해당한 View들은 숨긴다.
            mStartTimeColumn.setVisibility(View.GONE);
            mEndTimeColumn.setVisibility(View.GONE);

            //한행 전체가 눌리울수 있게 하고 개별적인 날자 View들은 눌리울수 없도록 한다.
            mStartRow.setClickable(true);
            mEndRow.setClickable(true);
            mStartDateColumn.setClickable(false);
            mEndDateColumn.setClickable(false);
            mStartDateColumn.setBackground(null);
            mEndDateColumn.setBackground(null);
            mStartDateColumn.setOnTouchListener(null);
            mEndDateColumn.setOnTouchListener(null);
            Utils.addCommonTouchListener(mStartRow);
            Utils.addCommonTouchListener(mEndRow);
        }
        else {  //Check가 안된 상태
            //시간에 해당한 View들을 보여준다.
            mStartTimeColumn.setVisibility(View.VISIBLE);
            mEndTimeColumn.setVisibility(View.VISIBLE);

            //한행 전체가 눌리울수 있게 하고 개별적인 날자 View들은 눌리울수 없도록 한다.
            mStartRow.setClickable(false);
            mStartDateColumn.setClickable(true);
            mEndRow.setClickable(false);
            mEndDateColumn.setClickable(true);

            //ripple drawable을 얻어서 background로 설정한다.
            int resourceId = Utils.getThemeDrawableAttribute(mView.getContext(), android.R.attr.selectableItemBackground);
            Drawable drawable1 = ContextCompat.getDrawable(mView.getContext(), resourceId);
            Drawable drawable2 = ContextCompat.getDrawable(mView.getContext(), resourceId);
            mStartDateColumn.setBackground(drawable1);
            mEndDateColumn.setBackground(drawable2);

            Utils.addCommonTouchListener(mStartDateColumn);
            Utils.addCommonTouchListener(mEndDateColumn);
            mStartRow.setOnTouchListener(null);
            mEndRow.setOnTouchListener(null);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // This is only used for the Calendar spinner in new events, and only fires when the
        // calendar selection changes or on screen rotation
        Cursor c = (Cursor) parent.getItemAtPosition(position);
        if (c == null) {
            // TODO: can this happen? should we drop this check?
            Log.w(TAG, "Cursor not set on calendar item");
            return;
        }

        // Do nothing if the selection didn't change so that reminders will not get lost
        int idColumn = c.getColumnIndexOrThrow(Calendars._ID);
        long calendarId = c.getLong(idColumn);
        int colorColumn = c.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR);
        int color = c.getInt(colorColumn);
        int displayColor = Utils.getDisplayColorFromColor(color);

        // Prevents resetting of data (reminders, etc.) on orientation change.
        if (calendarId == mModel.mCalendarId && mModel.isCalendarColorInitialized() &&
                displayColor == mModel.getCalendarColor()) {
            return;
        }

        mModel.mCalendarId = calendarId;
        mModel.setCalendarColor(displayColor);
        mModel.mCalendarAccountName = c.getString(EditEventHelper.CALENDARS_INDEX_ACCOUNT_NAME);
        mModel.mCalendarAccountType = c.getString(EditEventHelper.CALENDARS_INDEX_ACCOUNT_TYPE);
        mModel.setEventColor(mModel.getCalendarColor());

        // Update the max/allowed reminders with the new calendar properties.
        int maxRemindersColumn = c.getColumnIndexOrThrow(Calendars.MAX_REMINDERS);
        mModel.mCalendarMaxReminders = c.getInt(maxRemindersColumn);
        int allowedRemindersColumn = c.getColumnIndexOrThrow(Calendars.ALLOWED_REMINDERS);
        mModel.mCalendarAllowedReminders = c.getString(allowedRemindersColumn);
        int allowedAttendeeTypesColumn = c.getColumnIndexOrThrow(Calendars.ALLOWED_ATTENDEE_TYPES);
        mModel.mCalendarAllowedAttendeeTypes = c.getString(allowedAttendeeTypesColumn);
        int allowedAvailabilityColumn = c.getColumnIndexOrThrow(Calendars.ALLOWED_AVAILABILITY);
        mModel.mCalendarAllowedAvailability = c.getString(allowedAvailabilityColumn);

        // Discard the current reminders and replace them with the model's default reminder set.
        // We could attempt to save & restore the reminders that have been added, but that's
        // probably more trouble than it's worth.
        mModel.mReminders.clear();
        mModel.mReminders.addAll(mModel.mDefaultReminders);
        mModel.mHasAlarm = mModel.mReminders.size() != 0;

        // Update the UI elements.
        mReminderItems.clear();
        LinearLayout reminderLayout =
                (LinearLayout) mScrollView.findViewById(R.id.reminder_items_container);
        reminderLayout.removeAllViews();
        prepareReminders();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    ArrayList<ReminderEntry> getReminders() {
        return EventViewUtils.reminderItemsToReminders(mReminderItems, mReminderMinuteValues, mReminderMethodValues);
    }

    /**
     * 반복항목마당을 설정한다.
     * @param oldDate 본래의 시작시간(편집전)
     * @param newDate 새로운 시작시간(현재)
     */
    public void updateRecurrence(DateTime oldDate, DateTime newDate) {
        int freq = mEventRecurrence.freq;
        if(freq == EventRecurrence.NEVER) {     //주반복
            return;
        }

        if(freq == EventRecurrence.WEEKLY) {    //주반복
            if(mEventRecurrence.byday != null && mEventRecurrence.byday.length > 0) {

                boolean isInRepeatSeries = false;
                int weekDay = oldDate.getDayOfWeek() % 7;
                int size = mEventRecurrence.byday.length;

                for (int i = 0; i < size; i ++) {
                    int tempByDay = mEventRecurrence.byday[i];
                    int tempByWeekDay = EventRecurrence.day2TimeDay(tempByDay);
                    if(weekDay == tempByWeekDay) {
                        isInRepeatSeries = true;
                        break;
                    }
                }

                if(isInRepeatSeries) {
                    final int oldStartDay, newStartDay, dayDiff;
                    oldStartDay = Time.getJulianDay(oldDate);
                    newStartDay = Time.getJulianDay(newDate);
                    dayDiff = newStartDay - oldStartDay;

                    for (int i = 0; i < size; i ++) {
                        int byDay = mEventRecurrence.byday[i];
                        int byWeekDay = EventRecurrence.day2TimeDay(byDay);

                        //The mode result can be negative value, so calculate once more with 7 bigger value
                        int newByWeekDay = (((byWeekDay + dayDiff) % 7) + 7) % 7;
                        mEventRecurrence.byday[i] = EventRecurrence.timeDay2Day(newByWeekDay);
                    }
                }
            }
        }

        else if(freq == EventRecurrence.MONTHLY) {  //월반복
            if(mEventRecurrence.bymonthday != null && mEventRecurrence.bymonthday.length > 0) {
                int byMonthDay = newDate.getDayOfMonth();
                mEventRecurrence.bymonthday[0] = byMonthDay;
            }
            else if(mEventRecurrence.byday != null && mEventRecurrence.byday.length > 0 &&
                    mEventRecurrence.bydayNum != null && mEventRecurrence.bydayNum.length > 0) {
                final DateTime nextWeek = newDate.plusWeeks(1);
                final int weekDay = newDate.getDayOfWeek() % 7;
                final int day = EventRecurrence.timeDay2Day(weekDay);
                final int dayNum;
                if(newDate.getMonthOfYear() != nextWeek.getMonthOfYear())
                    dayNum = -1;
                else
                    dayNum = (newDate.getDayOfMonth() + 6)/7;

                mEventRecurrence.byday[0] = day;
                mEventRecurrence.bydayNum[0] = dayNum;
            }
        }

        mRrule = mEventRecurrence.toString();

        String repeatString;
        Resources resources = mActivity.getResources();
        repeatString = Utils.getUpperString(Objects.requireNonNull(EventRecurrenceFormatter.getRepeatString(mActivity, resources,
                mEventRecurrence, true)));

        mRruleButton.setText(repeatString);
    }

    public static class CalendarsAdapter extends ResourceCursorAdapter {
        public CalendarsAdapter(Context context, int resourceId, Cursor c) {
            super(context, resourceId, c);
            setDropDownViewResource(R.layout.calendars_dropdown_item);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            View colorBar = view.findViewById(R.id.color);
            int colorColumn = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR);
            int nameColumn = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
            int ownerColumn = cursor.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT);
            if (colorBar != null) {
                colorBar.setBackgroundColor(Utils.getDisplayColorFromColor(cursor
                        .getInt(colorColumn)));
            }

            TextView name = (TextView) view.findViewById(R.id.calendar_name);
            if (name != null) {
                String displayName = cursor.getString(nameColumn);
                name.setText(displayName);

                TextView accountName = (TextView) view.findViewById(R.id.account_name);
                if (accountName != null) {
                    accountName.setText(cursor.getString(ownerColumn));
                    accountName.setVisibility(TextView.VISIBLE);
                }
            }
        }
    }

    /**
     * 시작날자, 마감날자, 시작시간(시,분), 마감시간(시,분)을 눌렀을때의 동작을 정의하는 클라스
     */
    private class DateTimeListener implements
            CustomTimePickerDialog.OnTimeSelectedListener, CustomDatePickerDialog.OnDateSelectedListener {

        public DateTimeListener() {
        }

        public void onDateTimeChanged(DateTime dateTime, Boolean isStart) {
            if(isStart) {
                DateTime originalStartTime = mStartTime;
                boolean startDateChanged = originalStartTime.getYear() != dateTime.getYear() ||
                        originalStartTime.getMonthOfYear() != dateTime.getMonthOfYear() ||
                        originalStartTime.getDayOfMonth() != dateTime.getDayOfMonth();
                if(startDateChanged) {
                    updateRecurrence(originalStartTime, dateTime);
                }

                mStartTime = dateTime;
                setDate(mStartDateButton, mStartTime, false);
                setTime(mStartTimeButton, mStartTime);

                //마감시간이 시작시간보다 앞에 놓이는 경우 시작시간을 수정
                if(mAllDay) {   //하루종일일정
                    DateTime tempStartDate = new DateTime(mStartTime.getYear(), mStartTime.getMonthOfYear(), mStartTime.getDayOfMonth(), 0, 0);
                    DateTime tempEndDate = new DateTime(mEndTime.getYear(), mEndTime.getMonthOfYear(), mEndTime.getDayOfMonth(), 0, 0).minusDays(1);
                    long diff = tempEndDate.getMillis() - tempStartDate.getMillis();
                    if(diff < 0) {
                        mEndTime = new DateTime(mStartTime.getYear(), mStartTime.getMonthOfYear(), mStartTime.getDayOfMonth(), mEndTime.getHourOfDay(), mEndTime.getMinuteOfHour())
                                .plusDays(1);
                        setDate(mEndDateButton, mEndTime, true);
                        setTime(mEndTimeButton, mEndTime);
                    }
                }
                else {  //하루종일이 아님
                    long diff = mEndTime.getMillis() - mStartTime.getMillis();
                    if(diff < 0) {
                        mEndTime = mStartTime.plusHours(1);
                        setDate(mEndDateButton, mEndTime, true);
                        setTime(mEndTimeButton, mEndTime);
                    }
                }
            }
            else {
                mEndTime = dateTime;
                if(mAllDay)
                    mEndTime = mEndTime.plusDays(1);
                setDate(mEndDateButton, mEndTime, true);
                setTime(mEndTimeButton, mEndTime);

                DateTime originalStartTime = mStartTime;

                //시작시간이 마감시간보다 뒤에 놓이는 경우 시작시간을 수정
                if(mAllDay) {   //하루종일
                    DateTime tempStartDate = new DateTime(mStartTime.getYear(), mStartTime.getMonthOfYear(), mStartTime.getDayOfMonth(), 0, 0);
                    DateTime tempEndDate = new DateTime(mEndTime.getYear(), mEndTime.getMonthOfYear(), mEndTime.getDayOfMonth(), 0, 0).minusDays(1);
                    long diff = tempEndDate.getMillis() - tempStartDate.getMillis();
                    if(diff < 0) {
                        mStartTime = new DateTime(tempEndDate.getYear(), tempEndDate.getMonthOfYear(), tempEndDate.getDayOfMonth(), mStartTime.getHourOfDay(), mStartTime.getMinuteOfHour());
                        setDate(mStartDateButton, mStartTime, false);
                        setTime(mStartTimeButton, mStartTime);
                    }
                }
                else {  //하루종일이 아님
                    long diff = mEndTime.getMillis() - mStartTime.getMillis();
                    if(diff < 0) {
                        mStartTime = mEndTime.minusHours(1);
                        setDate(mStartDateButton, mStartTime, false);
                        setTime(mStartTimeButton, mStartTime);
                    }
                }

                boolean startDateChanged = originalStartTime.getYear() != dateTime.getYear() ||
                        originalStartTime.getMonthOfYear() != dateTime.getMonthOfYear() ||
                        originalStartTime.getDayOfMonth() != dateTime.getDayOfMonth();
                if(startDateChanged) {
                    updateRecurrence(originalStartTime, mStartTime);
                }
            }
        }

        @Override
        public void onTimeSelected(int hour, int minute, boolean isStart) {
            final DateTime dateTime;
            if(isStart) {
                dateTime = mStartTime.withHourOfDay(hour).withMinuteOfHour(minute);
            }
            else {
                dateTime = mEndTime.withHourOfDay(hour).withMinuteOfHour(minute);
            }

            onDateTimeChanged(dateTime, isStart);
        }

        @Override
        public void onDateSelected(int year, int monthOfYear, int dayOfMonth) {
        }

        @Override
        public void onDateSelected(int year, int monthOfYear, int dayOfMonth, boolean isStart) {
            final DateTime dateTime;
            if(isStart) {
                dateTime = new DateTime(year, monthOfYear + 1, dayOfMonth, mStartTime.getHourOfDay(), mStartTime.getMinuteOfHour());
            }
            else {
                dateTime = new DateTime(year, monthOfYear + 1, dayOfMonth, mEndTime.getHourOfDay(), mEndTime.getMinuteOfHour());
            }

            onDateTimeChanged(dateTime, isStart);
        }
    }

    /**
     * 하루종일일정에 대해서 시작날자, 마감날자 의 행을 눌렀을때의 동작을 정의하는 클라스
     */
    private class DateClickListener implements View.OnClickListener {
        private final Boolean mStart;

        public DateClickListener(boolean isStart) {
            mStart = isStart;
        }

        @Override
        public void onClick(View v) {
            final DateTimeListener listener = new DateTimeListener();

            //대화창이 두번이상 펼쳐지는것을 방지
            if(!mDialogOpened) {
                mDialogOpened = true;

                final CustomDatePickerDialog dialog;
                if(mStart) {
                    dialog = new CustomDatePickerDialog(mStartTime.getYear(), mStartTime.getMonthOfYear(), mStartTime.getDayOfMonth(), mAllDay, true, mActivity);
                }
                else {
                    dialog = new CustomDatePickerDialog(mEndTime.getYear(), mEndTime.getMonthOfYear(), mEndTime.getDayOfMonth(), mAllDay, false, mActivity);
                }
                dialog.setOnDateSelectedListener(listener);

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mDialogOpened = false;
                    }
                });

                Utils.makeBottomDialog(dialog);
                dialog.show();
            }
        }
    }
}
