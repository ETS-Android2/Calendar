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

package com.android.calendar.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.calendar.agenda.AgendaFragment;
import com.android.calendar.alerts.AlertService;
import com.android.calendar.customize.ImageTextView;
import com.android.calendar.event.EventInfoActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.helper.CalendarController.EventHandler;
import com.android.calendar.helper.CalendarController.EventInfo;
import com.android.calendar.helper.CalendarController.EventType;
import com.android.calendar.helper.CalendarController.ViewType;
import com.android.calendar.kr.big.BigCalendarView;
import com.android.calendar.kr.big.MonthViewFragmentBig;
import com.android.calendar.kr.common.CalendarView;
import com.android.calendar.kr.day.DayViewFragment;
import com.android.calendar.kr.dialogs.CustomDatePickerDialog;
import com.android.calendar.kr.general.GeneralCalendarView;
import com.android.calendar.kr.general.MonthViewFragmentGeneral;
import com.android.calendar.kr.standard.MonthViewFragmentStandard;
import com.android.calendar.kr.vertical.MonthViewFragmentVertical;
import com.android.calendar.kr.vertical.VerticalCalendarView;
import com.android.calendar.kr.year.YearViewFragment;
import com.android.calendar.settings.GeneralPreferences;
import com.android.calendar.settings.SettingsActivity;
import com.android.calendar.utils.Utils;
import com.android.calendar.views.ActionBarHeader;
import com.android.kr_common.Time;
import com.android.krcalendar.R;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static com.android.calendar.utils.Utils.CALENDAR_TYPE1;
import static com.android.calendar.utils.Utils.CALENDAR_TYPE2;
import static com.android.calendar.utils.Utils.CALENDAR_TYPE3;
import static com.android.calendar.utils.Utils.CALENDAR_TYPE4;

/**
 * 기본화면 activity
 */
public class AllInOneActivity extends AppCompatActivity implements EventHandler,
        OnSharedPreferenceChangeListener, View.OnClickListener{

    private static final String TAG = "AllInOneActivity";
    private static final boolean DEBUG = false;

    private static final int VIEW_CALENDAR = 101;
    public static final String SELECTED_TIME = "selected_time";

    private static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    private static final String BUNDLE_KEY_RESTORE_VIEW = "key_restore_view";
    private static final String BUNDLE_KEY_RESTORE_PREV_VIEW = "key_restore_prev_view";

    private static final String BUNDLE_KEY_RESTORE_START_TIME = "key_restore_start_time";
    private static final String BUNDLE_KEY_RESTORE_END_TIME = "key_restore_end_time";
    private static final String BUNDLE_KEY_RESTORE_QUERY = "key_restore_query";

    private long mStartTimeMillis = 0;
    private long mEndTimeMillis = 0;
    private String mQuery = "";

    private static final int HANDLER_KEY = 0;
    private static final int PERMISSIONS_REQUEST_WRITE_CALENDAR = 0;

    /**
     * 다른 클라스들에서 MainActivity를 얻기 위하여 리용하는 정적변수이다.
     * {@link #onCreate}에서 this로 설정하고 {@link #onDestroy}에서 null로 설정한다.
     */
    private static AllInOneActivity mMainActivity;

    //달력 읽기/쓰기 권한 상수문자렬
    public String[] PERMISSION_LIST = {
            Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR
    };

    //분이 변하는것을 감지하는 listener
    BroadcastReceiver mTimeTickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            FragmentManager fm = getSupportFragmentManager();
            List<Fragment> fragmentList = fm.getFragments();
            if(!fragmentList.isEmpty()) {
                Fragment fragment = fragmentList.get(0);
                if(fragment instanceof EventHandler) {
                    ((EventHandler)fragment).minuteChanged();
                }
            }
        }
    };

    //Calendar관리 변수
    private CalendarController mController;

    //달력일정이 변하는것을 감지하는 observer
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            eventsChanged();
        }
    };

    private ContentResolver mContentResolver;
    private int mPreviousView;  //이전의 보기(년,월,일,일정목록)
    private int mCurrentView;   //현재의 보기(년,월,일,일정목록)
    private boolean mUpdateOnResume = false;    //OnResume에서 fragment를 다시 창조하겠는가?

    //Event가 변할때 그것을 보관하기 위해 리용되는 변수
    EventInfo mEventInfo;

    private long mViewEventId = -1;
    private long mIntentEventStartMillis = -1;
    private long mIntentEventEndMillis = -1;
    private boolean mIntentAllDay = false;

    //Bottom bar 단추들
    ImageTextView mBtnNewEvent;
    ImageTextView mBtnAgenda;
    ImageTextView mBtnCalculate;
    ImageTextView mBtnGoDate;
    ImageTextView mBtnSettings;

    //같은 대화창이 두번이상 켜지는것을 방지하기 위해 리용되는 변수
    boolean mDialogOpened = false;

    //년, 월, 일, 단추들을 가진 자식 View
    ActionBarHeader mActionBarHeader;

    @Override
    protected void onCreate(Bundle icicle) {
        if(Utils.isDayTheme())
            setTheme(R.style.CalendarAppThemeDay);
        else
            setTheme(R.style.CalendarAppThemeNight);

        super.onCreate(icicle);

        mMainActivity = this;

        //CalendarController객체를 창조
        mController = CalendarController.getInstance(this);

        //Notification Chanel들을 창조
        AlertService.createChannels(this);

        //권한이 부여되였는지 검사
        checkAppPermissions();

        //Intent나 Bundle로부터 time, view type들을 얻는다.
        long timeMillis = -1;
        int viewType = -1;
        int prevView = -1;
        final Intent intent = getIntent();

        if (icicle != null) {
            //Bundle로부터 정보들 얻기
            timeMillis = icicle.getLong(BUNDLE_KEY_RESTORE_TIME);
            viewType = icicle.getInt(BUNDLE_KEY_RESTORE_VIEW, -1);
            prevView = icicle.getInt(BUNDLE_KEY_RESTORE_PREV_VIEW, -1);

            if(viewType == ViewType.AGENDA) {
                mStartTimeMillis = icicle.getLong(BUNDLE_KEY_RESTORE_START_TIME, 0);
                mEndTimeMillis = icicle.getLong(BUNDLE_KEY_RESTORE_END_TIME, 0);
                mQuery = icicle.getString(BUNDLE_KEY_RESTORE_QUERY);
            }
        } else {
            //Intent로부터 정보들 얻기
            String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action)) {
                timeMillis = parseViewAction(intent);
            }

            if (timeMillis == -1) {
                timeMillis = Utils.timeFromIntentInMillis(intent);
            }
        }

        if (viewType == -1 || viewType > ViewType.MAX_VALUE) {
            viewType = ViewType.MONTH;
        }
        Time t = new Time();
        t.set(timeMillis);

        //View 설정
        setContentView(R.layout.all_in_one_material);

        //Bottom bar 단추들얻고 click사건 추가
        mBtnNewEvent = findViewById(R.id.go_new_event);
        mBtnAgenda = findViewById(R.id.go_agenda);
        mBtnCalculate = findViewById(R.id.go_calculate_date);
        mBtnGoDate = findViewById(R.id.go_date);
        mBtnSettings = findViewById(R.id.go_settings);

        mBtnNewEvent.setOnClickListener(this);
        mBtnAgenda.setOnClickListener(this);
        mBtnCalculate.setOnClickListener(this);
        mBtnGoDate.setOnClickListener(this);
        mBtnSettings.setOnClickListener(this);

        Utils.addCommonTouchListener(mBtnNewEvent);
        Utils.addCommonTouchListener(mBtnAgenda);
        Utils.addCommonTouchListener(mBtnCalculate);
        Utils.addCommonTouchListener(mBtnGoDate);
        Utils.addCommonTouchListener(mBtnSettings);

        mActionBarHeader = findViewById(R.id.actionbar_header);
        mActionBarHeader.initialize();

        //기본 Frame에 Fragment추가
        initFragments(timeMillis, viewType, prevView);

        //Preference감지하기 위해 listener추가
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        //권한이 부여되였을때에만 ContentResolver를 창조하기 Observer를 추가한다.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED) {
            mContentResolver = getContentResolver();

            mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI,
                    true, mObserver);
        }
    }

    /**
     * 권한이 부여되지 않았으면 권한요청 dialog를 띄워준다.
     */
    private void checkAppPermissions() {
        if(!Utils.isPermissionGranted(this, PERMISSION_LIST)) {
            ActivityCompat.requestPermissions(this,PERMISSION_LIST,
                    PERMISSIONS_REQUEST_WRITE_CALENDAR);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_CALENDAR) {//사용자가 권한요청을 접수했을때
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                //권한이 요청되였다고 알려준다.
                mController.sendEvent(EventType.CALENDAR_PERMISSION_GRANTED, null, null, -1, ViewType.CURRENT,
                        0);
                onPermissionGranted();
            }

            //사용자가 권한요청을 거절했을때
            else {
                //`다시 묻지 않음`을 check 안했을때
                if (Utils.shouldShowRequestPermissionsRationale(this, PERMISSION_LIST)) {
                    //toast를 띄운다.
                    finishApplicationWithToast();
                }
                //`다시 묻지 않음`을 check 했을때
                else {
                    //App정보화면으로 가면서 toast를 띄운다.
                    finishAndShowAppInformationWithToast();
                }
            }
            return;
        }

        //ics, vcs파일들을 지운다.
        cleanupCachedEventFiles();
    }

    /**
     * Toast를 띄워주고 app을 끝낸다
     */
    public void finishApplicationWithToast() {
        Toast.makeText(getApplicationContext(), R.string.user_rejected_calendar_write_permission, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * (1) Toast를 띄워주고 app을 끝낸다.
     * (2) App정보화면을 펼쳐준다.
     */
    public void finishAndShowAppInformationWithToast() {
        Toast.makeText(getApplicationContext(), R.string.user_must_grant_calendar_write_permission, Toast.LENGTH_LONG).show();
        finish();

        //App정보화면
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 1);
    }

    /**
     * 다른 app으로부터 이 app을 열었을때 intent로부터 자료 얻는다
     * @param intent 입력
     * @return 시간을 돌려준다.(미리초)
     */
    private long parseViewAction(final Intent intent) {
        long timeMillis = -1;
        Uri data = intent.getData();
        if (data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("events")) {
                try {
                    mViewEventId = Long.parseLong(Objects.requireNonNull(data.getLastPathSegment()));
                    if (mViewEventId != -1) {
                        mIntentEventStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
                        mIntentEventEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
                        mIntentAllDay = intent.getBooleanExtra(EXTRA_EVENT_ALL_DAY, false);
                        timeMillis = mIntentEventStartMillis;
                    }
                } catch (NumberFormatException e) {
                    //례외처리는 안한다.
                }
            }
        }
        return timeMillis;
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Event Handler추가
        mController.registerFirstEventHandler(HANDLER_KEY, this);
        //권한이 부여되지 않았으면 아무 동작도 하지 않는다.
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (mUpdateOnResume) {
            if (mController.getViewType() != ViewType.AGENDA) {
                initFragments(mController.getTime(), mController.getViewType(), -1);
            }
            mUpdateOnResume = false;
        }

        if (mViewEventId != -1 && mIntentEventStartMillis != -1 && mIntentEventEndMillis != -1) {
            long currentMillis = System.currentTimeMillis();
            long selectedTime = -1;
            if (currentMillis > mIntentEventStartMillis && currentMillis < mIntentEventEndMillis) {
                selectedTime = currentMillis;
            }
            mController.sendEventRelatedEventWithExtra(EventType.VIEW_EVENT, mViewEventId,
                    mIntentEventStartMillis, mIntentEventEndMillis,
                    EventInfo.buildViewExtraLong(mIntentAllDay),
                    selectedTime);
            mViewEventId = -1;
            mIntentEventStartMillis = -1;
            mIntentEventEndMillis = -1;
            mIntentAllDay = false;
        }

        registerReceiver(mTimeTickReceiver,  new IntentFilter(Intent.ACTION_TIME_TICK));
    }


    @Override
    protected void onPause() {
        super.onPause();

        //Event Handler삭제
        mController.deregisterEventHandler(HANDLER_KEY);

        //권한이 부여되지 않았으면 아무 동작도 하지 않는다.
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Manifest.permission.WRITE_CALENDAR is not granted");
            return;
        }

        unregisterReceiver(mTimeTickReceiver);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(BUNDLE_KEY_RESTORE_TIME, mController.getTime());
        outState.putInt(BUNDLE_KEY_RESTORE_VIEW, mCurrentView);
        outState.putInt(BUNDLE_KEY_RESTORE_PREV_VIEW, mPreviousView);

        if (mCurrentView == ViewType.AGENDA) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment f = fm.findFragmentById(R.id.main_pane);

            if(f instanceof AgendaFragment) {
                AgendaFragment fragment = (AgendaFragment) f;
                outState.putLong(BUNDLE_KEY_RESTORE_START_TIME, fragment.getStartMillis());
                outState.putLong(BUNDLE_KEY_RESTORE_END_TIME, fragment.getEndMillis());
                outState.putString(BUNDLE_KEY_RESTORE_QUERY, fragment.getQuery());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mMainActivity = null;

        if(mContentResolver != null)
            mContentResolver.unregisterContentObserver(mObserver);

        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        mController.deregisterAllEventHandlers();
        CalendarController.removeInstance(this);

        //ics, vcs cache 파일들 지우기
        cleanupCachedEventFiles();
    }

    /**
     * cache등록부에 림시적으로 보관된 ics, vcs파일들을 지운다
     */
    private void cleanupCachedEventFiles() {
        if (!isExternalStorageWritable()) return;
        File cacheDir = getExternalCacheDir();
        if(cacheDir == null) return;

        File[] files = cacheDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String filename = file.getName();
            if (filename.endsWith(".ics") || filename.endsWith(".vcs")) {
                file.delete();
            }
        }
    }

    /**
     * 외장기억기를 읽기/쓰기할수 있는가를 돌려준다.
     * @return true: 읽기/쓰기할수 있음, false: 없음
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void initFragments(long timeMillis, int viewType, int prevViewType) {
        if (DEBUG) {
            Log.d(TAG, "Initializing to " + timeMillis + " for view " + viewType);
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        setMainPane(ft, R.id.main_pane, viewType, prevViewType, timeMillis, null, true);

        Time t = new Time();
        t.set(timeMillis);

        mController.sendEvent(EventType.GO_TO, t, null, -1, viewType);
    }

    @Override
    public void onBackPressed() {
        //일보기에서 뒤로
        if(mCurrentView == ViewType.DAY) {
            mController.sendEvent(EventType.GO_TO, null, null, -1, ViewType.MONTH);
        }

        //년보기에서 뒤로
        else if(mCurrentView == ViewType.YEAR) {
            long timeMillis = -1;

            List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
            if(!fragmentList.isEmpty()){
                Fragment fragment = fragmentList.get(0);
                if(fragment instanceof YearViewFragment) {
                    timeMillis = ((YearViewFragment)fragment).getTimeMillis();
                }
            }

            if(timeMillis < 0) {
                mController.sendEvent(EventType.GO_TO, null, null, -1, ViewType.MONTH);
            }
            else {
                Time time = new Time();
                time.set(timeMillis);
                mController.sendEvent(EventType.GO_TO, time, time, -1, ViewType.MONTH);
            }
        }

        //일정보기에서 뒤로
        else if (mCurrentView == ViewType.AGENDA) {
            if(mPreviousView == ViewType.DAY && Utils.getCalendarTypePreference(this) != CALENDAR_TYPE1){
                mPreviousView = ViewType.MONTH;
            }

            mController.sendEvent(EventType.GO_TO, null, null, -1, mPreviousView);
        }

        //월보기에서 뒤로
        else {
            //Fragment back stack이 비였다면 onBackpressed가 아니라 finishAfterTransition을 호출한다.
            FragmentManager fragmentManager = getSupportFragmentManager();
            if(fragmentManager.getBackStackEntryCount() == 0) {
                //양식4에서 back단추가 눌리웠다면 날자가 펼쳐져있는 상태인가를 검사한다.
                if(mCurrentView == ViewType.MONTH) {
                    List<Fragment> fragments = fragmentManager.getFragments();
                    if(!fragments.isEmpty()) {
                        Fragment fragment = fragments.get(0);
                        if(fragment instanceof MonthViewFragmentVertical &&
                                !VerticalCalendarView.VerticalCalendarViewDelegate.isExpanded()) {
                            //날자가 숨겨진 상태에서 back를 누르면 날자를 다시 보여준다.
                            ((MonthViewFragmentVertical) fragment).getCalendarView().onFlingOrClick();
                            return;
                        }
                    }
                }
                finishAfterTransition();
            }
            else
                super.onBackPressed();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        //달력형식이 변하였을때
        if(key.equals(Utils.CALENDAR_TYPE_PREF)){
            mUpdateOnResume = true;
            if(mCurrentView == ViewType.DAY ) {
                if(Utils.getCalendarTypePreference(this) != CALENDAR_TYPE1) {
                    mController.setViewType(ViewType.MONTH);
                }
                else {
                    mController.setViewType(ViewType.DAY);
                }
            }
        }
    }

    /**
     * Bottom bar의 단추들의 visible상태 갱신
     */
    public void updateVisibility(){
        final int viewType = mCurrentView;
        final boolean toAgenda = viewType == ViewType.AGENDA && mPreviousView != ViewType.AGENDA;
        final boolean fromAgenda = viewType != ViewType.AGENDA && mPreviousView == ViewType.AGENDA;
        if(toAgenda || fromAgenda) {
            if (toAgenda) {
                ((ViewGroup) mBtnAgenda.getParent()).setVisibility(View.GONE);
                ((ViewGroup) mBtnCalculate.getParent()).setVisibility(View.GONE);
                ((ViewGroup) mBtnGoDate.getParent()).setVisibility(View.VISIBLE);
            }
            else{
                ((ViewGroup) mBtnAgenda.getParent()).setVisibility(View.VISIBLE);
                ((ViewGroup) mBtnCalculate.getParent()).setVisibility(View.VISIBLE);
                ((ViewGroup) mBtnGoDate.getParent()).setVisibility(View.GONE);
            }
        }

        else if(viewType == ViewType.YEAR) {
            ((ViewGroup)mBtnAgenda.getParent()).setVisibility(View.GONE);
        }
        else if(viewType == ViewType.MONTH) {
            ((ViewGroup)mBtnAgenda.getParent()).setVisibility(View.VISIBLE);
        }

        mActionBarHeader.updateViews();
    }

    public boolean viewToAgenda(){
        return mCurrentView == ViewType.AGENDA && mPreviousView != ViewType.AGENDA;
    }

    public boolean viewFromAgenda(){
        return mCurrentView != ViewType.AGENDA && mPreviousView == ViewType.AGENDA;
    }

    private void setMainPane(
            FragmentTransaction ft, int viewId, int viewType, int prevViewType, long timeMillis, EventInfo eventInfo, boolean force) {

        FragmentManager fragmentManager = getSupportFragmentManager();

        //일부 경우에는 Fragment를 재창조할 필요가 없으므로 검사를 먼저 하고 그 경우들에는 함수를 끝낸다.
        if (!force && !mUpdateOnResume && mCurrentView == viewType) {
            return;
        }

        //다음의 경우들에 transition을 진행한다.
        /*
            일정목록 -> 월, 일
            월, 일 -> 일정목록
            월 -> 일
            일 -> 월
            년 -> 월
            월 -> 년
         */
        int calendarType = Utils.getCalendarTypePreference(this);
        boolean doTransition =
                (viewType == ViewType.AGENDA && mCurrentView != ViewType.AGENDA) ||
                        (viewType != ViewType.AGENDA && mCurrentView == ViewType.AGENDA) ||
                        (viewType == ViewType.MONTH && mCurrentView == ViewType.DAY) ||
                        (viewType == ViewType.DAY && mCurrentView == ViewType.MONTH) ||
                        (viewType == ViewType.MONTH && mCurrentView == ViewType.YEAR) ||
                        (viewType == ViewType.YEAR && mCurrentView == ViewType.MONTH);

        boolean dayToMonthTransition = viewType == ViewType.MONTH && mCurrentView == ViewType.DAY && calendarType == CALENDAR_TYPE1;
        boolean monthToDayTransition = viewType == ViewType.DAY && mCurrentView == ViewType.MONTH && calendarType == CALENDAR_TYPE1;
        boolean monthToYearTransition = viewType == ViewType.YEAR && mCurrentView == ViewType.MONTH;
        boolean yearToMonthTransition = viewType == ViewType.MONTH && mCurrentView == ViewType.YEAR;
        boolean toAgendaTransition = viewType == ViewType.AGENDA;
        boolean fromAgendaTransition = mCurrentView == ViewType.AGENDA;

        String fragmentTAG = "";

        if (viewType != mCurrentView) {
            //이전의  ViewType 을 보관한다.
            if(prevViewType != -1)
                mPreviousView = prevViewType;

            else if (mCurrentView > 0) {
                mPreviousView = mCurrentView;
            }

            mCurrentView = viewType;
        }

        //Fragment생성
        Fragment frag;

        switch (viewType) {
            case ViewType.AGENDA:
                if(eventInfo == null) {
                    if(mEventInfo != null)
                        frag = new AgendaFragment(timeMillis, mEventInfo.startTime, mEventInfo.endTime, mQuery);
                    else {
                        if(mStartTimeMillis != 0)
                            frag = new AgendaFragment(timeMillis, new Time(mStartTimeMillis), new Time(mEndTimeMillis), mQuery);
                        else
                            frag = new AgendaFragment();
                    }
                }
                else {
                    frag = new AgendaFragment(timeMillis, eventInfo.startTime, eventInfo.endTime, mQuery);
                    mEventInfo = eventInfo;
                }
                fragmentTAG = "AGENDA";
                break;
            case ViewType.YEAR:
                frag = new YearViewFragment(timeMillis);
                fragmentTAG = "YEAR";
                break;
            case ViewType.DAY:
                frag = new DayViewFragment(timeMillis);
                fragmentTAG = "DAY";
                break;
            case ViewType.MONTH:
            default:
                if(calendarType == CALENDAR_TYPE1) {
                    frag = new MonthViewFragmentStandard(timeMillis);
                }
                else if(calendarType == CALENDAR_TYPE2) {
                    frag = new MonthViewFragmentGeneral(timeMillis);
                }
                else if(calendarType == CALENDAR_TYPE3) {
                    frag = new MonthViewFragmentBig(timeMillis);
                }
                else if(calendarType == CALENDAR_TYPE4){
                    frag = new MonthViewFragmentVertical(timeMillis);
                }
                else {
                    frag = new MonthViewFragmentStandard(timeMillis);
                }

                fragmentTAG = "MONTH";
                break;
        }

        //ft가 null일때에만 transition을 진행한다.
        if (ft == null) {
            ft = fragmentManager.beginTransaction();
        }
        else {
            doTransition = false;
        }

        Utils.setDayToMonthTransition(false);
        Utils.setMonthToDayTransition(false);
        Utils.setMonthToYearTransition(false);
        Utils.setYearToMonthTransition(false);
        Utils.setToAgendaTransition(false);
        Utils.setFromAgendaTransition(false);
        Utils.setTodayBothVisible(false);

        //Fragment표준 transition이 아니라 자체정의 transition animation을 준다.
        if (doTransition) {
            if(monthToDayTransition) {
                Utils.setMonthToDayTransition(true);
                ft.setCustomAnimations(R.animator.day_enter, R.animator.month_exit);
            }
            else if(dayToMonthTransition) {
                Utils.setDayToMonthTransition(true);
                ft.setCustomAnimations(R.animator.month_enter, R.animator.day_exit);
            }
            else if(monthToYearTransition) {
                Utils.setMonthToYearTransition(true);
                ft.setCustomAnimations(R.animator.slide_from_left_ym, R.animator.slide_to_right_ym);

                //Controller의 시간이 올해가 아니면 `오늘`단추에 대한 fade animation을 주지 않는다.
                final DateTime nowTime = DateTime.now();
                final DateTime dateTime = new DateTime(mController.getTime());
                if(nowTime.getYear() != dateTime.getYear())
                    Utils.setTodayBothVisible(true);
            }
            else if(yearToMonthTransition) {
                Utils.setYearToMonthTransition(true);
                ft.setCustomAnimations(R.animator.slide_from_right_ym, R.animator.slide_to_left_ym);

                //Controller의 시간이 올해가 아니면 `오늘`단추에 대한 fade animation을 주지 않는다.
                final DateTime nowTime = DateTime.now();
                final DateTime dateTime = new DateTime(mController.getTime());
                if(nowTime.getYear() != dateTime.getYear())
                    Utils.setTodayBothVisible(true);
            }
            else if(toAgendaTransition) {
                Utils.setToAgendaTransition(true);
                ft.setCustomAnimations(R.animator.slide_from_right_a, R.animator.slide_to_left_a);
            }
            else if(fromAgendaTransition) {
                Utils.setFromAgendaTransition(true);
                ft.setCustomAnimations(R.animator.slide_from_left_a, R.animator.slide_to_right_a);
            }
            else {
                ft.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
            }
        }

        ft.replace(viewId, frag, fragmentTAG);
        ft.commit();

        //key와 EventHandler를 등록한다.
        mController.registerEventHandler(viewId, (EventHandler) frag);
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.VIEW_EVENT | EventType.LAUNCH_MONTH_PICKER;
    }

    @Override
    public void handleEvent(EventInfo event) {
        if (event.eventType == EventType.GO_TO) {
            mEventInfo = event;

            if(event.selectedTime != null) {
                setMainPane(
                        null, R.id.main_pane, event.viewType, -1, event.selectedTime.toMillis(false), event, false);
            }
            else {
                setMainPane(
                        null, R.id.main_pane, event.viewType, -1, event.startTime.toMillis(false), event, false);
            }
        }

        else if (event.eventType == EventType.VIEW_EVENT) {
            //일정보기화면 activity를 연다.
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClass(this, EventInfoActivity.class);

            Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, event.id);
            intent.setData(eventUri);       //Event Uri
            intent.putExtra(EXTRA_EVENT_BEGIN_TIME, event.startTime.toMillis(false));   //시작시간
            intent.putExtra(EXTRA_EVENT_END_TIME, event.endTime.toMillis(false));       //마감시간
            startActivity(intent);
        }

        else if(event.eventType == EventType.LAUNCH_MONTH_PICKER) {
            if(event.selectedTime != null) {
                setMainPane(
                        null, R.id.main_pane, event.viewType, -1, event.selectedTime.toMillis(false), event, false);
            }
        }
    }

    //권한요청이 접수되였을때
    public void onPermissionGranted() {
        //content observer를 등록한다.
        mContentResolver = getContentResolver();
        mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI,
                true, mObserver);
    }

    @Override
    public void eventsChanged() {
        mController.sendEvent(EventType.EVENTS_CHANGED, null, null, -1, ViewType.CURRENT);
    }

    @Override
    public void minuteChanged() {
    }

    /**
     * 단추들의 눌림동작들 추가
     */
    @Override
    public void onClick(View v) {
        //Transition animation이 진행될때는 단추사건을 막는다.
        if(Utils.isOnTransition()){
            return;
        }

        if (v == mBtnNewEvent)      //`새 일정`
            onSelectNewEvent();
        else if (v == mBtnAgenda)   //`일정목록`
            onSelectAgenda();
        else if (v == mBtnCalculate)    //`날자계산`
            onSelectCalculate();
        else if (v == mBtnGoDate)       //`날자선택`
            onSelectGoDate();
        else if (v == mBtnSettings)     //`설정`
            onSelectSettings();
    }

    /**
     * `새일정`단추를 눌렀을때
     */
    public void onSelectNewEvent(){
        Time t = new Time();
        t.set(mController.getTime());

        //오늘을 선택했을때는 시간, 분을 현재 시간, 분으로 설정한다.
        DateTime now = DateTime.now();
        if(now.getYear() == t.year && now.getMonthOfYear() == t.month + 1 && now.getDayOfMonth() == t.monthDay) {
            t.hour = now.getHourOfDay();
            t.minute = now.getMinuteOfHour();
        }
        //다른 날을 선택했을때는 0시, 0분으로 설정한다
        else {
            t.hour = 0;
            t.minute = 0;
        }
        t.second = 0;   //0초

        //30분이후는 다음 시간 0분으로, 0 - 30분은 그 시간 30분으로 설정
        //(10시 20분 -> 10시 30분, 10시 40분 -> 11시 0분)
        if (t.minute > 30) {
            t.plusHours(1);
            t.minute = 0;
        } else if (t.minute > 0 && t.minute < 30) {
            t.minute = 30;
        }
        mController.sendEventRelatedEvent(
                EventType.CREATE_EVENT, -1, t.toMillis(true), 0, -1);
    }

    /**
     * `일정목록`딘추를 눌렀을때
     */
    public void onSelectAgenda(){
        Time startTime = new Time();
        Time endTime = new Time();
        Time selectedTime = new Time();

        //현재달의 1일과 마지막날자를 일정목록의 시작/마감시간으로 설정한다.
        DateTime dateTime = new DateTime(mController.getTime());
        DateTime firstDate = new DateTime(dateTime.getYear(), dateTime.getMonthOfYear(), 1, 0, 0);
        DateTime lastDate = firstDate.plusMonths(1).minusDays(1);

        startTime.set(firstDate.getMillis());
        endTime.set(lastDate.getMillis());
        selectedTime.set(dateTime.getMillis());

        mController.sendEvent(EventType.GO_TO, startTime, endTime, selectedTime,-1, ViewType.AGENDA,
                0);
    }

    /**
     * `날자계산`을 눌렀을때
     */
    public void onSelectCalculate(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(getApplicationContext(), DateCalculateActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SELECTED_TIME, mController.getTime());
        startActivityForResult(intent, VIEW_CALENDAR);
    }

    /**
     * `날자가기`를 눌렀을때(일정목록화면에서)
     */
    public void onSelectGoDate(){
        if(mDialogOpened)
            return;

        CustomDatePickerDialog dialog = new CustomDatePickerDialog(new Time(mController.getTime()), this);
        Utils.makeBottomDialog(dialog);

        dialog.setOnDateSelectedListener(new CustomDatePickerDialog.OnDateSelectedListener() {
            @Override
            public void onDateSelected(int year, int monthOfYear, int dayOfMonth) {
                DateTime selectedDate = new DateTime(year, monthOfYear + 1, dayOfMonth, 0, 0);
                mController.setTime(selectedDate.getMillis());

                FragmentManager fm = getSupportFragmentManager();
                Fragment f = fm.findFragmentById(R.id.main_pane);
                if(f instanceof AgendaFragment){
                    ((AgendaFragment)f).onSelectDate(selectedDate);
                }
            }

            @Override
            public void onDateSelected(int year, int monthOfYear, int dayOfMonth, boolean isStart) {
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDialogOpened = false;
            }
        });

        mDialogOpened = true;
        dialog.show();
    }

    /**
     * `설정`딘추를 눌렀을때
     */
    public void onSelectSettings(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(getApplicationContext(), SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == VIEW_CALENDAR) {
            if(resultCode == RESULT_OK) {
                assert data != null;
                long timeMillis = data.getLongExtra(SELECTED_TIME, 0);
                Time time = new Time(timeMillis);
                mController.sendEvent(CalendarController.EventType.GO_TO, time, time, time, -1, CalendarController.ViewType.CURRENT,
                        CalendarController.EXTRA_GOTO_DATE);
            }
        }
    }

    /* 년, 월, 일 변화 감지 listener 들을 추가*/
    public void addYearChangeListeners(YearViewFragment yearViewFragment){
        yearViewFragment.addYearChangeListener(mActionBarHeader);
    }

    public void addMonthChangeListeners(CalendarView calendarView){
        calendarView.addMonthChangeListener(mActionBarHeader);
    }

    public void addMonthChangeListeners(VerticalCalendarView calendarView){
        calendarView.addMonthChangeListener(mActionBarHeader);
    }

    public void addMonthChangeListeners(BigCalendarView calendarView){
        calendarView.addMonthChangeListener(mActionBarHeader);
    }

    public void addDayChangeListeners(DayViewFragment dayViewFragment){
        dayViewFragment.addDayChangeListener(mActionBarHeader);
    }

    public void addDayChangeListeners(VerticalCalendarView calendarView){
        calendarView.addDayChangeListener(mActionBarHeader);
    }

    public void addDayChangeListeners(BigCalendarView calendarView){
        calendarView.addDayChangeListener(mActionBarHeader);
    }

    public void addDayChangeListeners(GeneralCalendarView calendarView){
        calendarView.addDayChangeListener(mActionBarHeader);
    }

    public ActionBarHeader getActionBarHeader(){
        return mActionBarHeader;
    }

    /**
     * @param context
     * @return context로부터 AllInOneActivity를 얻어서 돌려준다
     */
    public static AllInOneActivity getMainActivity(Context context) {
        if (context instanceof AllInOneActivity) {
            return (AllInOneActivity) context;
        }
        return ((AllInOneActivity) ((ContextWrapper) context).getBaseContext());
    }

    public CalendarController getCalendarController() {
        return mController;
    }

    public static AllInOneActivity getMainActivity() {
        return mMainActivity;
    }
}
