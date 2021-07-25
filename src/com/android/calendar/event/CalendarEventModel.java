/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.calendar.event;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;

import com.android.calendar.settings.GeneralPreferences;
import com.android.calendar.utils.Utils;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.TimeZone;

/**
 * 달력일정에 관한 모든 정보들을 가지고 있는 Model 클라스
 * `리용안됨`으로 표시한 함수 및 변수들은 실지 달력일정을 구성하는데 필요하지만 우리 App에서는 몇개의 마당들만 필요로 하기때문에 리용안하는것들이다.
 * 이것들은 값이 변하지 않고 기정값이 그대로 설정되게 된다.
 */
public class CalendarEventModel implements Serializable {
    /*일정의 Uri와 Id (이 두개는 련관되여있다.)*/
    //Uri(새일정을 창조할때 이것은 null로 들어온다)
    public String mUri = null;
    //Id
    public long mId = -1;

    /*-- 달력 및 일정의 마당값들 --*/
    public long mCalendarId = -1;       //계정의 Id
    public String mCalendarDisplayName = "";    //계정 - 현시될이름
    public String mCalendarAccountName; //계정이름
    public String mCalendarAccountType; //계정형태
    public int mCalendarMaxReminders;   //미리알림 최대개수
    public String mCalendarAllowedReminders;        //허용된 미리알림
    public String mCalendarAllowedAttendeeTypes;    //허용된 참석자형식들 (리용안됨)
    public String mCalendarAllowedAvailability;     //허용된 접근방식들 (리용안됨)
    public String mSyncId = null;   //Google Sync계정일때 Sync계정 Id
    public String mSyncAccountName = null;  //Sync계정 이름
    public String mSyncAccountType = null;  //Sync계정 형태
    public EventColorCache mEventColorCache;    //색갈들을 보관한 Cache자료

    public String mOwnerAccount = null; //일정이 련결된 달력계정이름
    public String mTitle = null;    //제목
    public String mLocation = null; //위치, 리용안됨
    public String mDescription = null;  //설명
    public String mRrule = null;    //반복설정
    public String mOrganizer = null;    //조직자, 리용안됨
    public String mOrganizerDisplayName = null; //조직자이름, 리용안됨

    public boolean mIsOrganizer = true; //조직자인? 리용안됨
    public boolean mIsFirstEventInSeries = true;    //반복일정계렬의 첫일정인가?

    //반복일정계렬의 첫일정의 시작시간, 이 일정에 대해서는 mStart와 mOriginalStart가 같다. (반복일정에서만 리용됨)
    public long mOriginalStart = -1;
    //일정의 시작시간
    public long mStart = -1;    //시작시간
    //반복일정계렬의 첫일정의 마감시간, (반복일정에서만 리용됨)
    public long mOriginalEnd = -1;
    public long mEnd = -1;      //마감시간 (반복일정에서는 마감시간 대신 지속시간 mDuration을 리용한다)
    public String mDuration = null; //지속시간(반복일정에서만 리용됨)
    public String mTimezone; //시간대
    public boolean mAllDay = false; //하루종일
    public boolean mHasAlarm = false;   //미리알림을 가지고 있는가?

    /*-- 아래의 성원변수들은 리용을 안한다. --*/
    public int mAvailability = Events.AVAILABILITY_BUSY;
    public boolean mHasAttendeeData = true;
    public int mSelfAttendeeStatus = -1;
    public int mOwnerAttendeeId = -1;
    public String mOriginalSyncId = null;
    public long mOriginalId = -1;
    public Long mOriginalTime = null;
    public Boolean mOriginalAllDay = null;
    public boolean mGuestsCanModify = false;
    public boolean mGuestsCanInviteOthers = false;
    public boolean mGuestsCanSeeGuests = false;
    public boolean mOrganizerCanRespond = false;
    public int mCalendarAccessLevel = Calendars.CAL_ACCESS_CONTRIBUTOR;
    public int mEventStatus = Events.STATUS_CONFIRMED;

    //Event Cursor에서 자료를 불러들이기 전까지 Model자료는 갱신이 안된다.
    public boolean mModelUpdatedWithEventCursor; //Event Cursor를 통해 Model이 갱신되였는가?
    public int mAccessLevel = 0;    //접근수준(리용안됨)
    public ArrayList<ReminderEntry> mReminders; //미리알림목록
    public ArrayList<ReminderEntry> mDefaultReminders;  //기정의 미리알림목록(일정창조시 이것이 기정으로 설정된다)
    public LinkedHashMap<String, Attendee> mAttendeesList;  //참석자목록(리용안됨)
    private int mCalendarColor = -1;    //달력색갈(리용안됨)
    private boolean mCalendarColorInitialized = false;  //달력색갈이 설정되였는가?(리용안됨)
    private int mEventColor = -1;   //일정색갈(리용안됨)
    private boolean mEventColorInitialized = false; //일정색갈이 설정되였는가?(리용안됨)
    public int mEventType = EventTypeManager.EVENT_TYPE_DEFAULT;    //일정형식

    /**
     * 미리알림목록, 참석자목록, 시간대를 초기화한다.
     */
    public CalendarEventModel() {
        mReminders = new ArrayList<ReminderEntry>();
        mDefaultReminders = new ArrayList<ReminderEntry>();
        mAttendeesList = new LinkedHashMap<String, Attendee>();
        mTimezone = TimeZone.getDefault().getID();
    }

    /**
     * 기정의 미리알림을 얻어서 설정한다.
     * @param context
     */
    public CalendarEventModel(Context context) {
        this();

        mTimezone = Utils.getTimeZone(context, null);   //시간대 얻기
        //기정 미리알림을 Preference 에서 불러온다. (기정값: 10분)
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        String defaultReminder = prefs.getString(GeneralPreferences.KEY_DEFAULT_REMINDER, GeneralPreferences.NO_REMINDER_STRING);
        int defaultReminderMins = Integer.parseInt(defaultReminder);
        if (defaultReminderMins != GeneralPreferences.NO_REMINDER) {
            //적어도 한개의 미리알림은 가지고 있다고 본다.
            mHasAlarm = true;
            mReminders.add(ReminderEntry.valueOf(defaultReminderMins));
            mDefaultReminders.add(ReminderEntry.valueOf(defaultReminderMins));
        }
    }

    /**
     * intent로부터 model자료를 얻는다.
     * @param context Context
     * @param intent Intent
     */
    public CalendarEventModel(Context context, Intent intent) {
        this(context);

        if (intent == null) {
            return;
        }

        /* intent로부터 제목, 설명, 반복 Rule을 받는다. */
        String title = intent.getStringExtra(Events.TITLE);
        if (title != null) {
            mTitle = title;
        }
        String description = intent.getStringExtra(Events.DESCRIPTION);
        if (description != null) {
            mDescription = description;
        }
        String rrule = intent.getStringExtra(Events.RRULE);
        if (!TextUtils.isEmpty(rrule)) {
            mRrule = rrule;
        }
    }

    /**
     * 달력계정이 유효한가를 돌려준다
     */
    public boolean isValid() {
        //계정의 Id를 검사한다.
        if (mCalendarId == -1) {
            return false;
        }

        //계정의 이름을 검사한다.
        if (TextUtils.isEmpty(mOwnerAccount)) {
            return false;
        }
        return true;
    }

    /**
     * 일정이름, 설명이 비였는가를 돌려준다.
     */
    public boolean isEmpty() {
        //이름이 비였는가를 검사
        if (mTitle != null && mTitle.trim().length() > 0) {
            return false;
        }

        //설명이 비였는가를 검사
        if (mDescription != null && mDescription.trim().length() > 0) {
            return false;
        }

        return true;
    }

    /**
     * 일정을 구성하는 마당값들 지우기
     */
    public void clear() {
        mUri = null;
        mId = -1;
        mCalendarId = -1;
        mCalendarColor = -1;
        mCalendarColorInitialized = false;

        mEventColorCache = null;
        mEventColor = -1;
        mEventColorInitialized = false;

        mSyncId = null;
        mSyncAccountName = null;
        mSyncAccountType = null;
        mOwnerAccount = null;

        mTitle = null;
        mLocation = null;
        mDescription = null;
        mRrule = null;
        mOrganizer = null;
        mOrganizerDisplayName = null;
        mIsOrganizer = true;
        mIsFirstEventInSeries = true;

        mOriginalStart = -1;
        mStart = -1;
        mOriginalEnd = -1;
        mEnd = -1;
        mDuration = null;
        mTimezone = null;
        mAllDay = false;
        mHasAlarm = false;

        mHasAttendeeData = true;
        mSelfAttendeeStatus = -1;
        mOwnerAttendeeId = -1;
        mOriginalId = -1;
        mOriginalSyncId = null;
        mOriginalTime = null;
        mOriginalAllDay = null;

        mGuestsCanModify = false;
        mGuestsCanInviteOthers = false;
        mGuestsCanSeeGuests = false;
        mAccessLevel = 0;
        mEventStatus = Events.STATUS_CONFIRMED;
        mOrganizerCanRespond = false;
        mCalendarAccessLevel = Calendars.CAL_ACCESS_CONTRIBUTOR;
        mModelUpdatedWithEventCursor = false;
        mCalendarAllowedReminders = null;
        mCalendarAllowedAttendeeTypes = null;
        mCalendarAllowedAvailability = null;

        mReminders = new ArrayList<ReminderEntry>();
        mAttendeesList.clear();
    }

    /**
     * 참석자목록에 참석자추가
     * @param attendee 참석자
     */
    public void addAttendee(Attendee attendee) {
        mAttendeesList.put(attendee.mEmail, attendee);
    }

    /**
     * 참석자목록을 문자렬로 변환하여 돌려준다.
     */
    public String getAttendeesString() {
        StringBuilder b = new StringBuilder();
        for (Attendee attendee : mAttendeesList.values()) {
            String name = attendee.mName;
            String email = attendee.mEmail;
            String status = Integer.toString(attendee.mStatus);
            b.append("name:").append(name);
            b.append(" email:").append(email);
            b.append(" status:").append(status);
        }
        return b.toString();
    }

    /**
     * HashCode를 생성하여 돌려준다.
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mAllDay ? 1231 : 1237);
        result = prime * result + ((mAttendeesList == null) ? 0 : getAttendeesString().hashCode());
        result = prime * result + (int) (mCalendarId ^ (mCalendarId >>> 32));
        result = prime * result + ((mDescription == null) ? 0 : mDescription.hashCode());
        result = prime * result + ((mDuration == null) ? 0 : mDuration.hashCode());
        result = prime * result + (int) (mEnd ^ (mEnd >>> 32));
        result = prime * result + (mGuestsCanInviteOthers ? 1231 : 1237);
        result = prime * result + (mGuestsCanModify ? 1231 : 1237);
        result = prime * result + (mGuestsCanSeeGuests ? 1231 : 1237);
        result = prime * result + (mOrganizerCanRespond ? 1231 : 1237);
        result = prime * result + (mModelUpdatedWithEventCursor ? 1231 : 1237);
        result = prime * result + mCalendarAccessLevel;
        result = prime * result + (mHasAlarm ? 1231 : 1237);
        result = prime * result + (mHasAttendeeData ? 1231 : 1237);
        result = prime * result + (int) (mId ^ (mId >>> 32));
        result = prime * result + (mIsFirstEventInSeries ? 1231 : 1237);
        result = prime * result + (mIsOrganizer ? 1231 : 1237);
        result = prime * result + ((mLocation == null) ? 0 : mLocation.hashCode());
        result = prime * result + ((mOrganizer == null) ? 0 : mOrganizer.hashCode());
        result = prime * result + ((mOriginalAllDay == null) ? 0 : mOriginalAllDay.hashCode());
        result = prime * result + (int) (mOriginalEnd ^ (mOriginalEnd >>> 32));
        result = prime * result + ((mOriginalSyncId == null) ? 0 : mOriginalSyncId.hashCode());
        result = prime * result + (int) (mOriginalId ^ (mOriginalEnd >>> 32));
        result = prime * result + (int) (mOriginalStart ^ (mOriginalStart >>> 32));
        result = prime * result + ((mOriginalTime == null) ? 0 : mOriginalTime.hashCode());
        result = prime * result + ((mOwnerAccount == null) ? 0 : mOwnerAccount.hashCode());
        result = prime * result + ((mReminders == null) ? 0 : mReminders.hashCode());
        result = prime * result + ((mRrule == null) ? 0 : mRrule.hashCode());
        result = prime * result + mSelfAttendeeStatus;
        result = prime * result + mOwnerAttendeeId;
        result = prime * result + (int) (mStart ^ (mStart >>> 32));
        result = prime * result + ((mSyncAccountName == null) ? 0 : mSyncAccountName.hashCode());
        result = prime * result + ((mSyncAccountType == null) ? 0 : mSyncAccountType.hashCode());
        result = prime * result + ((mSyncId == null) ? 0 : mSyncId.hashCode());
        result = prime * result + ((mTimezone == null) ? 0 : mTimezone.hashCode());
        result = prime * result + ((mTitle == null) ? 0 : mTitle.hashCode());
        result = prime * result + (mAvailability);
        result = prime * result + ((mUri == null) ? 0 : mUri.hashCode());
        result = prime * result + mAccessLevel;
        result = prime * result + mEventStatus;
        return result;
    }

    /**
     * 같은지를 돌려준다.
     * @see Object#equals
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CalendarEventModel)) {
            return false;
        }

        //비교하려는 객체가 CalendarEventModel형일때는 모든 마당값들을 비교한다.
        CalendarEventModel other = (CalendarEventModel) obj;
        if (!checkOriginalModelFields(other)) {
            return false;
        }

        if (mLocation == null) {
            if (other.mLocation != null) {
                return false;
            }
        } else if (!mLocation.equals(other.mLocation)) {
            return false;
        }

        if (mTitle == null) {
            if (other.mTitle != null) {
                return false;
            }
        } else if (!mTitle.equals(other.mTitle)) {
            return false;
        }

        if (mDescription == null) {
            if (other.mDescription != null) {
                return false;
            }
        } else if (!mDescription.equals(other.mDescription)) {
            return false;
        }

        if (mDuration == null) {
            if (other.mDuration != null) {
                return false;
            }
        } else if (!mDuration.equals(other.mDuration)) {
            return false;
        }

        if (mEnd != other.mEnd) {
            return false;
        }
        if (mIsFirstEventInSeries != other.mIsFirstEventInSeries) {
            return false;
        }
        if (mOriginalEnd != other.mOriginalEnd) {
            return false;
        }

        if (mOriginalStart != other.mOriginalStart) {
            return false;
        }
        if (mStart != other.mStart) {
            return false;
        }

        if (mOriginalId != other.mOriginalId) {
            return false;
        }

        if (mOriginalSyncId == null) {
            if (other.mOriginalSyncId != null) {
                return false;
            }
        } else if (!mOriginalSyncId.equals(other.mOriginalSyncId)) {
            return false;
        }

        if (mRrule == null) {
            if (other.mRrule != null) {
                return false;
            }
        } else if (!mRrule.equals(other.mRrule)) {
            return false;
        }
        return true;
    }

    /**
     * 변화된것이 없는가를 돌려준다
     * @param originalModel 초기 model
     * @return true: 변화된것이 없음, false: 변화된것이 있음
     */
    public boolean unchanged(CalendarEventModel originalModel) {
        /* 모든 마당들을 다 비교한다. */
        if (this == originalModel) {
            return true;
        }
        if (originalModel == null) {
            return false;
        }

        if (!checkOriginalModelFields(originalModel)) {
            return false;
        }

        if (TextUtils.isEmpty(mLocation)) {
            if (!TextUtils.isEmpty(originalModel.mLocation)) {
                return false;
            }
        } else if (!mLocation.equals(originalModel.mLocation)) {
            return false;
        }

        if (TextUtils.isEmpty(mTitle)) {
            if (!TextUtils.isEmpty(originalModel.mTitle)) {
                return false;
            }
        } else if (!mTitle.equals(originalModel.mTitle)) {
            return false;
        }

        if(mEventType != originalModel.mEventType)
            return false;

        if (TextUtils.isEmpty(mDescription)) {
            if (!TextUtils.isEmpty(originalModel.mDescription)) {
                return false;
            }
        } else if (!mDescription.equals(originalModel.mDescription)) {
            return false;
        }

        if (TextUtils.isEmpty(mDuration)) {
            if (!TextUtils.isEmpty(originalModel.mDuration)) {
                return false;
            }
        } else if (!mDuration.equals(originalModel.mDuration)) {
            return false;
        }

        if (mEnd != mOriginalEnd) {
            return false;
        }
        if (mStart != mOriginalStart) {
            return false;
        }

        // If this changed the original id and it's not just an exception to the
        // original event
        if (mOriginalId != originalModel.mOriginalId && mOriginalId != originalModel.mId) {
            return false;
        }

        if (TextUtils.isEmpty(mRrule)) {
            // if the rrule is no longer empty check if this is an exception
            if (!TextUtils.isEmpty(originalModel.mRrule)) {
                boolean syncIdNotReferenced = mOriginalSyncId == null
                        || !mOriginalSyncId.equals(originalModel.mSyncId);
                boolean localIdNotReferenced = mOriginalId == -1
                        || !(mOriginalId == originalModel.mId);
                if (syncIdNotReferenced && localIdNotReferenced) {
                    return false;
                }
            }
        } else if (!mRrule.equals(originalModel.mRrule)) {
            return false;
        }
        else if(!mReminders.equals(originalModel.mReminders)){
            return false;
        }

        return true;
    }

    /**
     *
     * 일부 마당값들을 비교하여 다른것이 있는지를 돌려준다.
     * @param originalModel 초기 model
     * @return true: 변화된것이 없음, false: 변화된것이 있음
     */
    protected boolean checkOriginalModelFields(CalendarEventModel originalModel) {
        if (mAllDay != originalModel.mAllDay) {
            return false;
        }

        if (mCalendarId != originalModel.mCalendarId) {
            return false;
        }
        if (mCalendarColor != originalModel.mCalendarColor) {
            return false;
        }
        if (mCalendarColorInitialized != originalModel.mCalendarColorInitialized) {
            return false;
        }
        if (mGuestsCanInviteOthers != originalModel.mGuestsCanInviteOthers) {
            return false;
        }
        if (mGuestsCanModify != originalModel.mGuestsCanModify) {
            return false;
        }
        if (mGuestsCanSeeGuests != originalModel.mGuestsCanSeeGuests) {
            return false;
        }
        if (mOrganizerCanRespond != originalModel.mOrganizerCanRespond) {
            return false;
        }
        if (mCalendarAccessLevel != originalModel.mCalendarAccessLevel) {
            return false;
        }
        if (mModelUpdatedWithEventCursor != originalModel.mModelUpdatedWithEventCursor) {
            return false;
        }
        if (mHasAlarm != originalModel.mHasAlarm) {
            return false;
        }
        if (mHasAttendeeData != originalModel.mHasAttendeeData) {
            return false;
        }
        if (mId != originalModel.mId) {
            return false;
        }
        if (mIsOrganizer != originalModel.mIsOrganizer) {
            return false;
        }

        if (mOrganizer == null) {
            if (originalModel.mOrganizer != null) {
                return false;
            }
        } else if (!mOrganizer.equals(originalModel.mOrganizer)) {
            return false;
        }

        if (mOriginalAllDay == null) {
            if (originalModel.mOriginalAllDay != null) {
                return false;
            }
        } else if (!mOriginalAllDay.equals(originalModel.mOriginalAllDay)) {
            return false;
        }

        if (mOriginalTime == null) {
            if (originalModel.mOriginalTime != null) {
                return false;
            }
        } else if (!mOriginalTime.equals(originalModel.mOriginalTime)) {
            return false;
        }

        if (mOwnerAccount == null) {
            if (originalModel.mOwnerAccount != null) {
                return false;
            }
        } else if (!mOwnerAccount.equals(originalModel.mOwnerAccount)) {
            return false;
        }

        if (mReminders == null) {
            if (originalModel.mReminders != null) {
                return false;
            }
        } else if (!mReminders.equals(originalModel.mReminders)) {
            return false;
        }

        if (mSelfAttendeeStatus != originalModel.mSelfAttendeeStatus) {
            return false;
        }
        if (mOwnerAttendeeId != originalModel.mOwnerAttendeeId) {
            return false;
        }
        if (mSyncAccountName == null) {
            if (originalModel.mSyncAccountName != null) {
                return false;
            }
        } else if (!mSyncAccountName.equals(originalModel.mSyncAccountName)) {
            return false;
        }

        if (mSyncAccountType == null) {
            if (originalModel.mSyncAccountType != null) {
                return false;
            }
        } else if (!mSyncAccountType.equals(originalModel.mSyncAccountType)) {
            return false;
        }

        if (mSyncId == null) {
            if (originalModel.mSyncId != null) {
                return false;
            }
        } else if (!mSyncId.equals(originalModel.mSyncId)) {
            return false;
        }

        if (mTimezone == null) {
            if (originalModel.mTimezone != null) {
                return false;
            }
        } else if (!mTimezone.equals(originalModel.mTimezone)) {
            return false;
        }

        if (mAvailability != originalModel.mAvailability) {
            return false;
        }

        if (mUri == null) {
            if (originalModel.mUri != null) {
                return false;
            }
        } else if (!mUri.equals(originalModel.mUri)) {
            return false;
        }

        if (mAccessLevel != originalModel.mAccessLevel) {
            return false;
        }

        if (mEventStatus != originalModel.mEventStatus) {
            return false;
        }

        if (mEventColor != originalModel.mEventColor) {
            return false;
        }

        if (mEventColorInitialized != originalModel.mEventColorInitialized) {
            return false;
        }

        return true;
    }

    /**
     * 정렬을 진행하고 중복되는것을 없앤다.
     * @return 항상 true 를 돌려준다. 돌림값은 의미가 없고 호출하는쪽에서 편안하게 해주기 위해 리용된다.
     */
    public boolean normalizeReminders() {
        if (mReminders.size() <= 1) {
            return true;
        }

        //정렬
        Collections.sort(mReminders);

        //중복되는것을 없애기
        ReminderEntry prev = mReminders.get(mReminders.size()-1);
        for (int i = mReminders.size()-2; i >= 0; --i) {
            ReminderEntry cur = mReminders.get(i);
            if (prev.equals(cur)) {
                //같은것이 있으면 제거
                mReminders.remove(i+1);
            }
            prev = cur;
        }

        return true;
    }

    /* 달력, 일정의 색갈정보와 관련된 함수들 */
    public boolean isCalendarColorInitialized() {
        return mCalendarColorInitialized;
    }

    public boolean isEventColorInitialized() {
        return mEventColorInitialized;
    }

    public int getCalendarColor() {
        return mCalendarColor;
    }

    public int getEventType(){
        return mEventType;
    }

    public void setCalendarColor(int color) {
        mCalendarColor = color;
        mCalendarColorInitialized = true;
    }

    public int getEventColor() {
        return mEventColor;
    }

    public void setEventColor(int color) {
        mEventColor = color;
        mEventColorInitialized = true;
    }

    public int[] getCalendarEventColors() {
        if (mEventColorCache != null) {
            return mEventColorCache.getColorArray(mCalendarAccountName, mCalendarAccountType);
        }
        return null;
    }

    public String getEventColorKey() {
        if (mEventColorCache != null) {
            return mEventColorCache.getColorKey(mCalendarAccountName, mCalendarAccountType,
                    mEventColor);
        }
        return "";
    }

    /**
     * 참석자클라스(리용안됨)
     */
    public static class Attendee implements Serializable {
        public String mName;    //이름
        public String mEmail;   //전자우편주소
        public int mStatus;     //상태 Attendees.ATTENDEE_STATUS_NONE

        public Attendee(String name, String email) {
            this(name, email, Attendees.ATTENDEE_STATUS_NONE);
        }

        public Attendee(String name, String email, int status) {
            mName = name;
            mEmail = email;
            mStatus = status;
        }

        @Override
        public int hashCode() {
            return (mEmail == null) ? 0 : mEmail.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Attendee)) {
                return false;
            }
            Attendee other = (Attendee) obj;
            if (!TextUtils.equals(mEmail, other.mEmail)) {
                return false;
            }
            return true;
        }

    }

    /**
     * 미리알림을 표현하는 Model 클라스 (분, 방식)
     */
    public static class ReminderEntry implements Comparable<ReminderEntry>, Serializable {
        private final int mMinutes; //분
        private final int mMethod;  //방식

        /**
         * 초기설정
         * @param minutes
         * @param method
         */
        private ReminderEntry(int minutes, int method) {
            // TODO: error-check args
            mMinutes = minutes;
            mMethod = method;
        }

        /**
         * ReminderEntry 객체생성
         */
        public static ReminderEntry valueOf(int minutes, int method) {
            // TODO: cache common instances
            return new ReminderEntry(minutes, method);
        }

        /**
         * ReminderEntry 객체생성
         */
        public static ReminderEntry valueOf(int minutes) {
            return valueOf(minutes, Reminders.METHOD_DEFAULT);
        }

        @Override
        public int hashCode() {
            return mMinutes * 10 + mMethod;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ReminderEntry)) {
                return false;
            }

            ReminderEntry re = (ReminderEntry) obj;

            if (re.mMinutes != mMinutes) {
                return false;
            }

            return re.mMethod == mMethod ||
                    (re.mMethod == Reminders.METHOD_DEFAULT && mMethod == Reminders.METHOD_ALERT) ||
                    (re.mMethod == Reminders.METHOD_ALERT && mMethod == Reminders.METHOD_DEFAULT);
        }

        @NotNull
        @Override
        public String toString() {
            return "ReminderEntry min=" + mMinutes + " meth=" + mMethod;
        }

        @Override
        public int compareTo(ReminderEntry re) {
            if (re.mMinutes != mMinutes) {
                return re.mMinutes - mMinutes;
            }
            if (re.mMethod != mMethod) {
                return mMethod - re.mMethod;
            }
            return 0;
        }

        public int getMinutes() {
            return mMinutes;
        }
        public int getMethod() {
            return mMethod;
        }
    }
}
