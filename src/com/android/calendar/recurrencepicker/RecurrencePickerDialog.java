package com.android.calendar.recurrencepicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TimeFormatException;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.fragment.app.DialogFragment;

import com.android.calendar.utils.Utils;
import com.android.calendar.expandablelayout.ExpandableLayout;
import com.android.calendar.kr.dialogs.CustomDatePickerDialog;
import com.android.calendarcommon2.EventRecurrence;
import com.android.kr_common.Time;
import com.android.krcalendar.R;
import com.lany.numberpicker.NumberPicker;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * 반복설정대화창
 */
public class RecurrencePickerDialog extends DialogFragment
        implements View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        NumberPicker.OnValueChangeListener, RadioGroup.OnCheckedChangeListener, CustomDatePickerDialog.OnDateSelectedListener {

    //Activity가 재창조될때 상태들을 복귀하기 위한 Key상수들
    public static final String BUNDLE_START_TIME_MILLIS = "bundle_event_start_time";
    public static final String BUNDLE_RRULE = "bundle_event_rrule";
    private static final String BUNDLE_MODEL = "bundle_model";
    private static final String BUNDLE_DEFAULT_RECURRENCE_TYPE = "bundle_default_recurrence";
    private static final String BUNDLE_CUSTOM_RECURRENCE_TYPE = "bundle_custom_recurrence";

    private static final int ANIMATE_VISIBLE_DURATION = 200;

    //반복마감(계속/특정일까지/여러번)
    private static final int END_NEVER = 0;
    private static final int END_BY_DATE = 1;
    private static final int END_BY_COUNT = 2;

    //반복 형태
    private static final int RECURRENCE_NO_REPEAT = 0;      //반복안함
    private static final int RECURRENCE_EVERY_DAY = 1;      //매일 반복
    private static final int RECURRENCE_EVERY_WEEK = 2;     //매주 반복
    private static final int RECURRENCE_EVERY_MONTH = 3;    //매월 반복
    private static final int RECURRENCE_EVERY_YEAR = 4;     //매해 반복
    private static final int RECURRENCE_CUSTOMIZE = 5;      //맞춤설정 반복

    //맞춤설정 반복
    private static final int CUSTOM_RECURRENCE_DAILY = 0;   //일마다 반복
    private static final int CUSTOM_RECURRENCE_WEEKLY = 1;  //주마다 반복
    private static final int CUSTOM_RECURRENCE_MONTHLY = 2; //월마다 반복
    private static final int CUSTOM_RECURRENCE_YEARLY = 3;  //년마다 반복

    private static final String TAG = "RecurrencePickerDialog";

    //반복간격의 최대/최소값 (`2주간격으로`에서 2을 의미함)
    private static final int INTERVAL_MAX = 99;
    private static final int INTERVAL_DEFAULT = 1;

    //반복회수의 최대/최소값
    private static final int COUNT_MAX = 999;
    private static final int COUNT_DEFAULT = 5;

    //자식 view 들
    private View mBackButton;
    private RadioButton mRadioNoRepeat, mRadioEveryDay, mRadioEveryWeek, mRadioEveryMonth, mRadioEveryYear, mRadioCustom;

    private ExpandableLayout mViewForCustomize;
    private NumberPicker mViewCustomInterval, mViewCustomRecurrenceType;
    ExpandableLayout mRepeatContent;
    private View mRepeatContentWeekly;
    private RadioGroup mRepeatContentMonthly;

    private final ToggleButton[] mWeekByDayButtons = new ToggleButton[7];

    private Spinner mEndSpinner;
    private View mRecurrenceDuration;
    private TextView mRecurrenceResultLabel;

    private TextView mEndDateTextView;
    private EditText mEndCount;
    private TextView mPostEndCount;

    private boolean mHidePostEndCount;

    //반복설정을 저장하는데 리용되는 변수들
    private RecurrenceModel mModel = new RecurrenceModel();
    private final EventRecurrence mRecurrence = new EventRecurrence();
    private final Time mTime = new Time();

    //시작날자가 2021.1.20 일때 "20일", "세번째 수요일"로 된다.
    private String mMonthRepeatByWeekDayStr = "";
    private String mMonthRepeatByMonthDayStr = "";

    private String mEndDateLabel;   //"특정일까지"
    private String mEndCountLabel;  //"여러번"

    //반복형태상수들을 묶어서 보관한다.
    private static final int[] mFreqModelToEventRecurrence = {
            EventRecurrence.DAILY,
            EventRecurrence.WEEKLY,
            EventRecurrence.MONTHLY,
            EventRecurrence.YEARLY
    };

    /**
     * 아래 상수들중의 하나
     * {@link #RECURRENCE_NO_REPEAT}, {@link #RECURRENCE_EVERY_DAY}, {@link #RECURRENCE_EVERY_WEEK},
     * {@link #RECURRENCE_EVERY_MONTH}, {@link #RECURRENCE_EVERY_YEAR}, {@link #RECURRENCE_CUSTOMIZE}
     */
    private int mDefaultRecurrenceType;

    /**
     * 아래 상수들중의 하나
     * {@link #CUSTOM_RECURRENCE_DAILY}, {@link #CUSTOM_RECURRENCE_WEEKLY},
     * {@link #CUSTOM_RECURRENCE_MONTHLY}, {@link #CUSTOM_RECURRENCE_YEARLY}
     */
    private int mCustomRecurrenceType;

    private Context mContext;
    private Resources mResources;

    //반복설정이 변하였다는것을 알려주기 위한 listener
    private OnRecurrenceSetListener mRecurrenceSetListener;

    //true: Visible갱신함수를 즉시 실행, false: Visible갱신함수를 post를 통해 실행
    private boolean mUpdateVisibilityInstantly = true;

    //Duration, Result View들의 animation을 위한 Animator변수
    private Animator mBottomViewAnimator;
    private boolean mBottomViewAnimateForward = true;

    //Toast가 여러개 창조되는것을 방지하기 위해 리용
    Toast mToast;

    public RecurrencePickerDialog() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.recurrenceDialog);

        mRecurrence.wkst = EventRecurrence.timeDay2Day(Utils.getFirstDayOfWeek());

        Bundle bundle = getArguments();
        String rRule;
        mUpdateVisibilityInstantly = true;

        if(savedInstanceState != null) {
            mTime.set(savedInstanceState.getLong(BUNDLE_START_TIME_MILLIS));

            RecurrenceModel m = (RecurrenceModel) savedInstanceState.get(BUNDLE_MODEL);
            if (m != null) {
                mModel = m;
            }

            mUpdateVisibilityInstantly = false;
        }
        else if(bundle != null) {
            mTime.set(bundle.getLong(BUNDLE_START_TIME_MILLIS));
            rRule = bundle.getString(BUNDLE_RRULE);
            if(rRule != null && !rRule.isEmpty()) {
                mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
                mRecurrence.parse(rRule);
                copyEventRecurrenceToModel(mRecurrence, mModel);
            }
        }
        mTime.normalize(false);

        //주-요일반복
        if (mRecurrence.bydayCount == 0) {
            //현재날자의 요일이 반복요일로 기정적으로 설정되도록 한다.
            mModel.weeklyByDayOfWeek[mTime.weekDay] = true;
        }

        //월반복
        boolean isLastWeek; //마지막주인가?
        if (mModel.monthlyByNthDayOfWeek == 0) {
            mModel.monthlyByDayOfWeek = mTime.weekDay;

            //마지막주인지를 검사
            isLastWeek = Time.isLastWeek(mTime);

            if(isLastWeek)
                mModel.monthlyByNthDayOfWeek = -1;
            else
                mModel.monthlyByNthDayOfWeek = (mTime.monthDay + 6) / 7;
        }
        else
            isLastWeek = mModel.monthlyByNthDayOfWeek == -1;

        //요일반복문자렬들을 얻는다.
        String [][]monthRepeatByDayOfWeekStringArr = new String[7][];
        monthRepeatByDayOfWeekStringArr[0] = mResources.getStringArray(R.array.repeat_by_nth_sun);
        monthRepeatByDayOfWeekStringArr[1] = mResources.getStringArray(R.array.repeat_by_nth_mon);
        monthRepeatByDayOfWeekStringArr[2] = mResources.getStringArray(R.array.repeat_by_nth_tues);
        monthRepeatByDayOfWeekStringArr[3] = mResources.getStringArray(R.array.repeat_by_nth_wed);
        monthRepeatByDayOfWeekStringArr[4] = mResources.getStringArray(R.array.repeat_by_nth_thurs);
        monthRepeatByDayOfWeekStringArr[5] = mResources.getStringArray(R.array.repeat_by_nth_fri);
        monthRepeatByDayOfWeekStringArr[6] = mResources.getStringArray(R.array.repeat_by_nth_sat);

        String[] monthlyByNthDayOfWeekStrings =
                monthRepeatByDayOfWeekStringArr[mModel.monthlyByDayOfWeek];

        if(isLastWeek)
            mMonthRepeatByWeekDayStr =
                    monthlyByNthDayOfWeekStrings[4];
        else
            mMonthRepeatByWeekDayStr =
                    monthlyByNthDayOfWeekStrings[mModel.monthlyByNthDayOfWeek - 1];

        mMonthRepeatByMonthDayStr = EventRecurrenceFormatter.getMonthlyRepeatByDayString(mResources, mTime.monthDay, false);

        //어느 Radio Button이 선택되여야 하는지 따져본다.
        if(mModel.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
            mDefaultRecurrenceType = RECURRENCE_NO_REPEAT;
        }
        else {
            switch (mModel.freq) {
                case RecurrenceModel.FREQ_DAILY:
                    if(mModel.interval == 1)
                        mDefaultRecurrenceType = RECURRENCE_EVERY_DAY;
                    else {
                        mDefaultRecurrenceType = RECURRENCE_CUSTOMIZE;
                        mCustomRecurrenceType = CUSTOM_RECURRENCE_DAILY;
                    }
                    break;

                case RecurrenceModel.FREQ_WEEKLY:
                    boolean isDefaultRecurrence = false;
                    if(mModel.interval == 1) {
                        int startWeekDay = mTime.weekDay;
                        int selectedDayCount = 0;
                        boolean containStartWeekDay = false;

                        for (int i = 0; i < 7; i ++) {
                            if(mModel.weeklyByDayOfWeek[i]) {
                                selectedDayCount ++;
                                if(startWeekDay == i) {
                                    containStartWeekDay = true;
                                }
                            }
                        }

                        isDefaultRecurrence = selectedDayCount == 1 && containStartWeekDay;
                    }

                    if(isDefaultRecurrence) {
                        mDefaultRecurrenceType = RECURRENCE_EVERY_WEEK;
                    }
                    else {
                        mDefaultRecurrenceType = RECURRENCE_CUSTOMIZE;
                        mCustomRecurrenceType = CUSTOM_RECURRENCE_WEEKLY;
                    }
                    break;

                case RecurrenceModel.FREQ_MONTHLY:
                    if(mModel.interval == 1 && mModel.monthlyRepeat == RecurrenceModel.MONTHLY_BY_DATE)
                        mDefaultRecurrenceType = RECURRENCE_EVERY_MONTH;
                    else {
                        mDefaultRecurrenceType = RECURRENCE_CUSTOMIZE;
                        mCustomRecurrenceType = CUSTOM_RECURRENCE_MONTHLY;
                    }
                    break;

                case RecurrenceModel.FREQ_YEARLY:
                    if(mModel.interval == 1)
                        mDefaultRecurrenceType = RECURRENCE_EVERY_YEAR;
                    else {
                        mDefaultRecurrenceType = RECURRENCE_CUSTOMIZE;
                        mCustomRecurrenceType = CUSTOM_RECURRENCE_YEARLY;
                    }
                    break;
            }
        }

        //Activity가 재창조되였을때 보관된 상태들을 복귀한다.
        int savedDefaultRecurrenceType = -1, savedCustomRecurrenceType = -1;
        if(savedInstanceState != null) {
            savedDefaultRecurrenceType = savedInstanceState.getInt(BUNDLE_DEFAULT_RECURRENCE_TYPE, -1);
            savedCustomRecurrenceType = savedInstanceState.getInt(BUNDLE_CUSTOM_RECURRENCE_TYPE, -1);
        }

        if(savedDefaultRecurrenceType != -1) {
            mDefaultRecurrenceType = savedDefaultRecurrenceType;
        }
        if(savedCustomRecurrenceType != -1) {
            mCustomRecurrenceType = savedCustomRecurrenceType;
        }

        if (mModel.endDate == null) {
            mModel.endDate = new Time(mTime);
            mModel.endDate.allDay = true;
            switch (mModel.freq) {
                case RecurrenceModel.FREQ_DAILY:
                case RecurrenceModel.FREQ_WEEKLY:
                    mModel.endDate.plusMonths(1);
                    break;
                case RecurrenceModel.FREQ_MONTHLY:
                    mModel.endDate.plusMonths(3);
                    break;
                case RecurrenceModel.FREQ_YEARLY:
                    mModel.endDate.year += 3;
                    break;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //배경색은 투명한것으로 준다.
        Objects.requireNonNull(Objects.requireNonNull(getDialog()).getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View contentView = inflater.inflate(R.layout.recurrence_picker, container, true);

        //자식 view들 얻기
        mBackButton = contentView.findViewById(R.id.back_button);
        mRadioNoRepeat = contentView.findViewById(R.id.radio_no_repeat);
        mRadioEveryDay = contentView.findViewById(R.id.radio_every_day);
        mRadioEveryWeek = contentView.findViewById(R.id.radio_every_week);
        mRadioEveryMonth = contentView.findViewById(R.id.radio_every_month);
        mRadioEveryYear = contentView.findViewById(R.id.radio_every_year);
        mRadioCustom = contentView.findViewById(R.id.radio_customize);
        mViewForCustomize = contentView.findViewById(R.id.view_for_customize);
        mViewCustomInterval = contentView.findViewById(R.id.interval);
        mViewCustomRecurrenceType = contentView.findViewById(R.id.recurrence_type);
        mRepeatContent = contentView.findViewById(R.id.repeat_content);
        mRepeatContentWeekly = contentView.findViewById(R.id.custom_repeat_weekly);
        mRepeatContentMonthly = contentView.findViewById(R.id.custom_repeat_monthly);
        AppCompatRadioButton repeatMonthSameDate = contentView.findViewById(R.id.repeat_month_same_date);
        AppCompatRadioButton repeatMonthSameWeekDay = contentView.findViewById(R.id.repeat_month_same_weekday);
        mEndSpinner = contentView.findViewById(R.id.endSpinner);
        mPostEndCount = (TextView) contentView.findViewById(R.id.postEndCount);
        mEndCount = (EditText) contentView.findViewById(R.id.endCount);
        mEndDateTextView = (TextView) contentView.findViewById(R.id.endDate);

        mRecurrenceDuration = contentView.findViewById(R.id.recurrence_duration);
        mRecurrenceResultLabel = contentView.findViewById(R.id.recurrence_result);
        mWeekByDayButtons[0] = contentView.findViewById(R.id.week_sunday);
        mWeekByDayButtons[1] = contentView.findViewById(R.id.week_monday);
        mWeekByDayButtons[2] = contentView.findViewById(R.id.week_tuesday);
        mWeekByDayButtons[3] = contentView.findViewById(R.id.week_wednesday);
        mWeekByDayButtons[4] = contentView.findViewById(R.id.week_thursday);
        mWeekByDayButtons[5] = contentView.findViewById(R.id.week_friday);
        mWeekByDayButtons[6] = contentView.findViewById(R.id.week_saturday);

        mEndSpinner.setOnItemSelectedListener(this);

        mBackButton.setOnClickListener(this);
        mRadioNoRepeat.setOnClickListener(this);
        mRadioEveryDay.setOnClickListener(this);
        mRadioEveryWeek.setOnClickListener(this);
        mRadioEveryMonth.setOnClickListener(this);
        mRadioEveryYear.setOnClickListener(this);
        mRadioCustom.setOnClickListener(this);
        mEndDateTextView.setOnClickListener(this);
        mRepeatContentMonthly.setOnCheckedChangeListener(this);

        //수자 선택기들의 최대, 최소 한계를 설정한다.
        mViewCustomInterval.setMinValue(INTERVAL_DEFAULT);
        mViewCustomInterval.setMaxValue(INTERVAL_MAX);
        mViewCustomRecurrenceType.setMinValue(0);
        mViewCustomRecurrenceType.setMaxValue(3);

        String []recurrenceStrings = new String[]{
                mResources.getString(R.string.day_view),
                mResources.getString(R.string.week_view),
                mResources.getString(R.string.month_view),
                mResources.getString(R.string.year_view),
        };
        mViewCustomRecurrenceType.setDisplayedValues(recurrenceStrings);

        mViewCustomInterval.setOnValueChangedListener(this);
        mViewCustomRecurrenceType.setOnValueChangedListener(this);

        //요일단추들의 check상태 갱신
        for (int i = 0; i < 7; i ++) {
            mWeekByDayButtons[i].setChecked(mModel.weeklyByDayOfWeek[i]);
            mWeekByDayButtons[i].setOnClickListener(this);
        }

        //월반복을 위한 View설정
        repeatMonthSameDate.setText(mMonthRepeatByMonthDayStr);
        repeatMonthSameWeekDay.setText(mMonthRepeatByWeekDayStr);
        if (mModel.monthlyRepeat == RecurrenceModel.MONTHLY_BY_DATE) {
            mRepeatContentMonthly.check(R.id.repeat_month_same_date);
        } else if (mModel.monthlyRepeat == RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK) {
            mRepeatContentMonthly.check(R.id.repeat_month_same_weekday);
        }

        //마감 정보들 설정
        Resources resources = getResources();
        ArrayList<CharSequence> endSpinnerLabelList = new ArrayList<>();

        String endForeverLabel = resources.getString(R.string.recurrence_end_continously);
        mEndDateLabel = resources.getString(R.string.recurrence_end_date_label);
        mEndCountLabel = resources.getString(R.string.recurrence_end_count_label);
        endSpinnerLabelList.add(endForeverLabel);
        endSpinnerLabelList.add(mEndDateLabel);
        endSpinnerLabelList.add(mEndCountLabel);

        EndSpinnerAdapter endSpinnerAdapter = new EndSpinnerAdapter(getActivity(), endSpinnerLabelList,
                R.layout.recurrencepicker_freq_item, R.layout.recurrencepicker_end_text);
        endSpinnerAdapter.setDropDownViewResource(R.layout.recurrencepicker_freq_item);
        mEndSpinner.setAdapter(endSpinnerAdapter);
        mEndCount.addTextChangedListener(new MinMaxTextWatcher(1, COUNT_DEFAULT, 300) {
            @Override
            void onChange(int v) {
                if (mModel.endCount != v) {
                    mModel.endCount = v;
                    updateEndCountText();
                    mEndCount.requestLayout();

                    updateRecurrenceResult();
                }
            }
        });

        //반복마감 view들의 visible상태를 설정한다.
        int selection = mModel.end;
        if(selection == END_NEVER) {
            mEndCount.setVisibility(View.GONE);
            mPostEndCount.setVisibility(View.GONE);
            mEndDateTextView.setVisibility(View.GONE);
        }
        else if(selection == END_BY_DATE) {
            mEndCount.setVisibility(View.GONE);
            mPostEndCount.setVisibility(View.GONE);
            mEndDateTextView.setVisibility(View.VISIBLE);
        }
        else if(selection == END_BY_COUNT) {
            mEndCount.setVisibility(View.VISIBLE);
            mPostEndCount.setVisibility(View.VISIBLE);
            mEndDateTextView.setVisibility(View.GONE);
        }
        mEndSpinner.setSelection(selection);

        final String dateStr = DateUtils.formatDateTime(getActivity(),
                mModel.endDate.toMillis(false), DateUtils.FORMAT_NUMERIC_DATE);
        mEndDateTextView.setText(dateStr);

        final String countStr = Integer.toString(mModel.endCount);
        mEndCount.setText(countStr);

        //해당 Radio단추들을 선택한다.
        switch (mDefaultRecurrenceType) {
            case RECURRENCE_NO_REPEAT:
                mRadioNoRepeat.setChecked(true);
                updateDefaultRecurrenceType(RECURRENCE_NO_REPEAT, false);
                break;
            case RECURRENCE_EVERY_DAY:
                mRadioEveryDay.setChecked(true);
                updateDefaultRecurrenceType(RECURRENCE_EVERY_DAY, false);
                break;
            case RECURRENCE_EVERY_WEEK:
                mRadioEveryWeek.setChecked(true);
                updateDefaultRecurrenceType(RECURRENCE_EVERY_WEEK, false);
                break;
            case RECURRENCE_EVERY_MONTH:
                mRadioEveryMonth.setChecked(true);
                updateDefaultRecurrenceType(RECURRENCE_EVERY_MONTH, false);
                break;
            case RECURRENCE_EVERY_YEAR:
                mRadioEveryYear.setChecked(true);
                updateDefaultRecurrenceType(RECURRENCE_EVERY_YEAR, false);
                break;
            case RECURRENCE_CUSTOMIZE:
                mRadioCustom.setChecked(true);
                updateDefaultRecurrenceType(RECURRENCE_CUSTOMIZE, false);

                //아래의 코드에 의하여 interval값이 바뀌기때문에 interval값을 림시로 보관해둔다.
                int backupInterval = mModel.interval;

                switch (mCustomRecurrenceType) {
                    case CUSTOM_RECURRENCE_DAILY:
                        mViewCustomRecurrenceType.setValue(0);
                        updateCustomRecurrenceType(CUSTOM_RECURRENCE_DAILY, false);
                        break;
                    case CUSTOM_RECURRENCE_WEEKLY:
                        mViewCustomRecurrenceType.setValue(1);
                        updateCustomRecurrenceType(CUSTOM_RECURRENCE_WEEKLY, false);
                        break;
                    case CUSTOM_RECURRENCE_MONTHLY:
                        mViewCustomRecurrenceType.setValue(2);
                        updateCustomRecurrenceType(CUSTOM_RECURRENCE_MONTHLY, false);
                        break;
                    case CUSTOM_RECURRENCE_YEARLY:
                        mViewCustomRecurrenceType.setValue(3);
                        updateCustomRecurrenceType(CUSTOM_RECURRENCE_YEARLY, false);
                        break;
                }

                //보관됬던 interval값을 다시 설정한다.
                mModel.interval = backupInterval;
                mViewCustomInterval.setValue(mModel.interval);
                break;
        }

        return contentView;
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(RecurrencePickerDialog.BUNDLE_START_TIME_MILLIS,
                mTime.toMillis(false));

        outState.putParcelable(BUNDLE_MODEL, mModel);
        outState.putInt(BUNDLE_DEFAULT_RECURRENCE_TYPE, mDefaultRecurrenceType);
        outState.putInt(BUNDLE_CUSTOM_RECURRENCE_TYPE, mCustomRecurrenceType);
    }

    // TODO don't lose data when getting data that our UI can't handle

    /**
     * {@link EventRecurrence}의 자료들을 {@link RecurrenceModel}에 복사한다.
     * @param er EventRecurrence 변수
     * @param model RecurrenceModel 변수
     */
    static private void copyEventRecurrenceToModel(final EventRecurrence er,
                                                   RecurrenceModel model) {
        //반복형태
        switch (er.freq) {
            case EventRecurrence.DAILY: //일반복
                model.freq = RecurrenceModel.FREQ_DAILY;
                break;
            case EventRecurrence.MONTHLY:   //월반복
                model.freq = RecurrenceModel.FREQ_MONTHLY;
                break;
            case EventRecurrence.YEARLY:    //년반복
                model.freq = RecurrenceModel.FREQ_YEARLY;
                break;
            case EventRecurrence.WEEKLY:    //주반복
                model.freq = RecurrenceModel.FREQ_WEEKLY;
                break;
            default:
                throw new IllegalStateException("freq=" + er.freq);
        }

        //한개 반복주기의 일/주/달/해 수
        if (er.interval > 0) {
            model.interval = er.interval;
        }

        //반복회수
        model.endCount = er.count;
        if (model.endCount > 0) {
            model.end = RecurrenceModel.END_BY_COUNT;
        }

        //마감날자
        if (!TextUtils.isEmpty(er.until)) {
            if (model.endDate == null) {
                model.endDate = new Time();
            }

            try {
                model.endDate.parse(er.until);
            } catch (TimeFormatException e) {
                model.endDate = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            //END_BY_DATE 와 END_BY_COUNT 가 둘다 설정되여서는 안된다.
            if (model.end == RecurrenceModel.END_BY_COUNT && model.endDate != null) {
                throw new IllegalStateException("freq=" + er.freq);
            }

            model.end = RecurrenceModel.END_BY_DATE;
        }

        // Weekly: repeat by day of week or Monthly: repeat by nth day of week
        // in the month
        Arrays.fill(model.weeklyByDayOfWeek, false);
        if (er.bydayCount > 0) {
            int count = 0;
            for (int i = 0; i < er.bydayCount; i++) {
                int dayOfWeek = EventRecurrence.day2TimeDay(er.byday[i]);
                model.weeklyByDayOfWeek[dayOfWeek] = true;

                if (model.freq == RecurrenceModel.FREQ_MONTHLY/* && er.bydayNum[i] > 0*/) {
                    // LIMITATION: Can handle only (one) weekDayNum and only
                    // when
                    // monthly
                    model.monthlyByDayOfWeek = dayOfWeek;
                    model.monthlyByNthDayOfWeek = er.bydayNum[i];
                    model.monthlyRepeat = RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK;
                    count++;
                }
            }

            if (model.freq == RecurrenceModel.FREQ_MONTHLY) {
                if (er.bydayCount != 1) {
                    // Can't handle 1st Monday and 2nd Wed
                    throw new IllegalStateException("Can handle only 1 byDayOfWeek in monthly");
                }
                if (count != 1) {
                    throw new IllegalStateException(
                            "Didn't specify which nth day of week to repeat for a monthly");
                }
            }
        }

        //월반복 - 날자
        if (model.freq == RecurrenceModel.FREQ_MONTHLY) {
            if (er.bymonthdayCount == 1) {
                if (model.monthlyRepeat == RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK) {
                    throw new IllegalStateException(
                            "Can handle only by monthday or by nth day of week, not both");
                }
                model.monthlyByMonthDay = er.bymonthday[0];
                model.monthlyRepeat = RecurrenceModel.MONTHLY_BY_DATE;
            } else if (er.bymonthCount > 1) {
                // LIMITATION: Can handle only one month day
                throw new IllegalStateException("Can handle only one bymonthday");
            }
        }
    }

    static private void copyModelToEventRecurrence(final RecurrenceModel model,
                                                   EventRecurrence er, Time time) {
        if (model.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
            throw new IllegalStateException("There's no recurrence");
        }

        //시작날자
        if(er.startDate == null)
            er.startDate = time;

        //반복형태
        er.freq = mFreqModelToEventRecurrence[model.freq];

        //간격
        if (model.interval <= 1) {
            er.interval = 0;
        } else {
            er.interval = model.interval;
        }

        //기간
        switch (model.end) {
            case RecurrenceModel.END_BY_DATE:
                if (model.endDate != null) {
                    model.endDate.switchTimezone(Time.TIMEZONE_UTC);
                    model.endDate.normalize(false);
                    er.until = model.endDate.format2445();
                    er.count = 0;
                } else {
                    throw new IllegalStateException("end = END_BY_DATE but endDate is null");
                }
                break;
            case RecurrenceModel.END_BY_COUNT:
                er.count = model.endCount;
                er.until = null;
                if (er.count <= 0) {
                    throw new IllegalStateException("count is " + er.count);
                }
                break;
            default:
                er.count = 0;
                er.until = null;
                break;
        }

        //주, 월반복
        er.bydayCount = 0;
        er.bymonthdayCount = 0;

        switch (model.freq) {
            case RecurrenceModel.FREQ_MONTHLY:
                if (model.monthlyRepeat == RecurrenceModel.MONTHLY_BY_DATE) {
                    if (model.monthlyByMonthDay > 0) {
                        er.bymonthday = new int[1];
                        er.bymonthday[0] = model.monthlyByMonthDay;
                        er.bymonthdayCount = 1;
                    }
                    else if(time != null && time.monthDay > 0) {
                        er.bymonthday = new int[1];
                        er.bymonthday[0] = time.monthDay;
                        er.bymonthdayCount = 1;
                    }
                } else if (model.monthlyRepeat == RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK) {
                    int count = 1;
                    er.byday = new int[count];
                    er.bydayNum = new int[count];
                    er.bydayCount = count;
                    er.byday[0] = EventRecurrence.timeDay2Day(model.monthlyByDayOfWeek);
                    er.bydayNum[0] = model.monthlyByNthDayOfWeek;
                }
                break;
            case RecurrenceModel.FREQ_WEEKLY:
                int count = 0;
                for (int i = 0; i < 7; i++) {
                    if (model.weeklyByDayOfWeek[i]) {
                        count++;
                    }
                }

                if (er.bydayCount < count || er.byday == null || er.bydayNum == null) {
                    er.byday = new int[count];
                    er.bydayNum = new int[count];
                }
                er.bydayCount = count;

                for (int i = 6; i >= 0; i--) {
                    if (model.weeklyByDayOfWeek[i]) {
                        er.bydayNum[--count] = 0;
                        er.byday[count] = EventRecurrence.timeDay2Day(i);
                    }
                }
                break;
        }

        if (!canHandleRecurrenceRule(er)) {
            throw new IllegalStateException("UI generated recurrence that it can't handle. ER:"
                    + er.toString() + " Model: " + model.toString());
        }
    }

    static public boolean canHandleRecurrenceRule(EventRecurrence er) {
        switch (er.freq) {
            case EventRecurrence.DAILY:
            case EventRecurrence.MONTHLY:
            case EventRecurrence.YEARLY:
            case EventRecurrence.WEEKLY:
                break;
            default:
                return false;
        }

        if (er.count > 0 && !TextUtils.isEmpty(er.until)) {
            return false;
        }

        // Weekly: For "repeat by day of week", the day of week to repeat is in
        // er.byday[]

        /*
         * Monthly: For "repeat by nth day of week" the day of week to repeat is
         * in er.byday[] and the "nth" is stored in er.bydayNum[]. Currently we
         * can handle only one and only in monthly
         */
        int numOfByDayNum = 0;
        for (int i = 0; i < er.bydayCount; i++) {
            if (er.bydayNum[i] > 0) {
                ++numOfByDayNum;
            }
        }

        if (numOfByDayNum > 1) {
            return false;
        }

        if (numOfByDayNum > 0 && er.freq != EventRecurrence.MONTHLY) {
            return false;
        }

        // The UI only handle repeat by one day of month i.e. not 9th and 10th
        // of every month
        if (er.bymonthdayCount > 1) {
            return false;
        }

        if (er.freq == EventRecurrence.MONTHLY) {
            if (er.bydayCount > 1) {
                return false;
            }
            if (er.bydayCount > 0 && er.bymonthdayCount > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Update the "Repeat for N events" end option with the proper string values
     * based on the value that has been entered for N.
     */
    private void updateEndCountText() {
        final String END_COUNT_MARKER = "%d";
        String endString = mResources.getQuantityString(R.plurals.recurrence_end_count,
                mModel.endCount);
        int markerStart = endString.indexOf(END_COUNT_MARKER);

        if (markerStart != -1) {
            if (markerStart == 0) {
                Log.e(TAG, "No text to put in to recurrence's end spinner.");
            } else {
                int postTextStart = markerStart + END_COUNT_MARKER.length();
                mPostEndCount.setText(endString.substring(postTextStart,
                        endString.length()).trim());
            }
        }
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);

        mContext = context;
        mResources = getResources();
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //전화면 대화창
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Objects.requireNonNull(dialog.getWindow()).setLayout(width, height);

            getDialog().setOnKeyListener(new DialogInterface.OnKeyListener()
            {
                @Override
                public boolean onKey(android.content.DialogInterface dialog, int keyCode,
                                     android.view.KeyEvent event) {
                    if ((keyCode == android.view.KeyEvent.KEYCODE_BACK)) {
                        if (event.getAction() == KeyEvent.ACTION_UP) {
                            if(saveRecurrence())
                                return false;

                            showFailedToast();
                            return true;
                        }
                    }

                    return false;
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if(v == mBackButton) {
            if(saveRecurrence())
                dismiss();
            else
                showFailedToast();
        }
        else if(v == mRadioNoRepeat) {
            updateDefaultRecurrenceType(RECURRENCE_NO_REPEAT, true);
        }
        else if(v == mRadioEveryDay) {
            updateDefaultRecurrenceType(RECURRENCE_EVERY_DAY, true);
            updateRecurrenceResult();
        }
        else if(v == mRadioEveryWeek) {
            updateDefaultRecurrenceType(RECURRENCE_EVERY_WEEK, true);
            updateRecurrenceResult();
        }
        else if(v == mRadioEveryMonth) {
            updateDefaultRecurrenceType(RECURRENCE_EVERY_MONTH, true);
            updateRecurrenceResult();
        }
        else if(v == mRadioEveryYear) {
            updateDefaultRecurrenceType(RECURRENCE_EVERY_YEAR, true);
            updateRecurrenceResult();
        }
        else if(v == mRadioCustom) {
            updateDefaultRecurrenceType(RECURRENCE_CUSTOMIZE, true);

            switch (mCustomRecurrenceType) {
                case CUSTOM_RECURRENCE_DAILY:
                    mModel.freq = RecurrenceModel.FREQ_DAILY;
                    break;
                case CUSTOM_RECURRENCE_WEEKLY:
                    mModel.freq = RecurrenceModel.FREQ_WEEKLY;
                    break;
                case CUSTOM_RECURRENCE_MONTHLY:
                    mModel.freq = RecurrenceModel.FREQ_MONTHLY;
                    break;
                case CUSTOM_RECURRENCE_YEARLY:
                    mModel.freq = RecurrenceModel.FREQ_YEARLY;
                    break;
            }

            updateRecurrenceResult();
        }
        else if(v instanceof ToggleButton) {
            ToggleButton toggleButton = (ToggleButton)v;

            if(toggleButton == mWeekByDayButtons[0]) {
                mModel.weeklyByDayOfWeek[0] = toggleButton.isChecked();
            }
            else if(toggleButton == mWeekByDayButtons[1]) {
                mModel.weeklyByDayOfWeek[1] = toggleButton.isChecked();
            }
            else if(toggleButton == mWeekByDayButtons[2]) {
                mModel.weeklyByDayOfWeek[2] = toggleButton.isChecked();
            }
            else if(toggleButton == mWeekByDayButtons[3]) {
                mModel.weeklyByDayOfWeek[3] = toggleButton.isChecked();
            }
            else if(toggleButton == mWeekByDayButtons[4]) {
                mModel.weeklyByDayOfWeek[4] = toggleButton.isChecked();
            }
            else if(toggleButton == mWeekByDayButtons[5]) {
                mModel.weeklyByDayOfWeek[5] = toggleButton.isChecked();
            }
            else if(toggleButton == mWeekByDayButtons[6]) {
                mModel.weeklyByDayOfWeek[6] = toggleButton.isChecked();
            }
            updateRecurrenceResult();
        }
        else if(v == mEndDateTextView) {
            //Opens date picker dialog
            CustomDatePickerDialog dialog = new CustomDatePickerDialog(mModel.endDate, getActivity());
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

            //Dialog Layout bottom
            Window window = dialog.getWindow();
            assert window != null;
            WindowManager.LayoutParams windowLayoutParams = window.getAttributes();
            windowLayoutParams.gravity = Gravity.BOTTOM;
            window.setAttributes(windowLayoutParams);

            dialog.setOnDateSelectedListener(this);
        }
    }

    public void setOnRecurrenceSetListener(OnRecurrenceSetListener l) {
        mRecurrenceSetListener = l;
    }

    //If this save is successful, return true, otherwise return false

    /**
     * 반복설정보관
     * @return true: 성공, false: 실패
     */
    private boolean saveRecurrence() {
        if(mDefaultRecurrenceType != RECURRENCE_NO_REPEAT && mDefaultRecurrenceType != RECURRENCE_CUSTOMIZE) {
            mModel.interval = 1;

            if(mDefaultRecurrenceType == RECURRENCE_EVERY_WEEK) {
                for (int i = 0; i < 7; i ++)
                    mModel.weeklyByDayOfWeek[i] = false;
            }
            else if(mDefaultRecurrenceType == RECURRENCE_EVERY_MONTH) {
                mModel.monthlyRepeat = RecurrenceModel.MONTHLY_BY_DATE;
            }
        }

        String rrule;
        if (mModel.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE) {
            rrule = null;
        } else {
            copyModelToEventRecurrence(mModel, mRecurrence, mTime);
            rrule = mRecurrence.toString();
        }

        //If the rule is Customize->Week, and no week day is selected, do not close dialog, and call recurrence listener
        boolean failed = false;
        if(mDefaultRecurrenceType == RECURRENCE_CUSTOMIZE && mCustomRecurrenceType == CUSTOM_RECURRENCE_WEEKLY) {
            boolean noWeekDay = true;
            for (int i = 0; i < 7; i ++) {
                if (mModel.weeklyByDayOfWeek[i]) {
                    noWeekDay = false;
                    break;
                }
            }

            if(noWeekDay)
                failed = true;
        }

        if(failed)
            return false;

        //Toast 가 이미 띄워져 있으면 그것을 없앤다.
        if(mToast != null)
            mToast.cancel();

        mRecurrenceSetListener.onRecurrenceSet(rrule);
        return true;
    }

    /**
     * 일정보관이 실패하였을때 toast를 띄워준다.
     */
    private void showFailedToast() {
        mToast = Toast.makeText(getContext(), R.string.at_least_one_weekday_select, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void updateDefaultRecurrenceType(int recurrenceType, boolean isClick) {
        mDefaultRecurrenceType = recurrenceType;
        Handler handler = new Handler();
        Runnable runnable;

        switch (recurrenceType) {
            case RECURRENCE_NO_REPEAT:
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateVisibilityBottomViews(false, isClick);
                        updateVisibilityCustomizeView(false, isClick);
                    }
                };
                if(mUpdateVisibilityInstantly) {
                    runnable.run();
                }
                else {
                    handler.post(runnable);
                }

                mModel.recurrenceState = RecurrenceModel.STATE_NO_RECURRENCE;
                break;
            case RECURRENCE_EVERY_DAY:
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateVisibilityBottomViews(true, isClick);
                        updateVisibilityCustomizeView(false, isClick);
                    }
                };
                if(mUpdateVisibilityInstantly) {
                    runnable.run();
                }
                else {
                    handler.post(runnable);
                }

                mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
                mModel.freq = RecurrenceModel.FREQ_DAILY;
                break;
            case RECURRENCE_EVERY_WEEK:
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateVisibilityBottomViews(true, isClick);
                        updateVisibilityCustomizeView(false, isClick);
                    }
                };
                if(mUpdateVisibilityInstantly) {
                    runnable.run();
                }
                else {
                    handler.post(runnable);
                }

                mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
                mModel.freq = RecurrenceModel.FREQ_WEEKLY;
                break;
            case RECURRENCE_EVERY_MONTH:
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateVisibilityBottomViews(true, isClick);
                        updateVisibilityCustomizeView(false, isClick);
                    }
                };
                if(mUpdateVisibilityInstantly) {
                    runnable.run();
                }
                else {
                    handler.post(runnable);
                }

                mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
                mModel.freq = RecurrenceModel.FREQ_MONTHLY;
                break;
            case RECURRENCE_EVERY_YEAR:
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateVisibilityBottomViews(true, isClick);
                        updateVisibilityCustomizeView(false, isClick);
                    }
                };
                if(mUpdateVisibilityInstantly) {
                    runnable.run();
                }
                else {
                    handler.post(runnable);
                }

                mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
                mModel.freq = RecurrenceModel.FREQ_YEARLY;
                break;
            case RECURRENCE_CUSTOMIZE:
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        updateVisibilityBottomViews(true, isClick);
                        updateVisibilityCustomizeView(true, isClick);
                    }
                };
                if(mUpdateVisibilityInstantly) {
                    runnable.run();
                }
                else {
                    handler.post(runnable);
                }

                mModel.recurrenceState = RecurrenceModel.STATE_RECURRENCE;
                if(mCustomRecurrenceType == CUSTOM_RECURRENCE_WEEKLY)
                    mModel.freq = RecurrenceModel.FREQ_YEARLY;
                else if(mCustomRecurrenceType == CUSTOM_RECURRENCE_MONTHLY)
                    mModel.freq = RecurrenceModel.FREQ_YEARLY;
                else if(mCustomRecurrenceType == CUSTOM_RECURRENCE_YEARLY)
                    mModel.freq = RecurrenceModel.FREQ_YEARLY;
                else
                    mModel.freq = RecurrenceModel.FREQ_DAILY;
                break;
        }
        mUpdateVisibilityInstantly = true;
    }

    private Animator createBottomViewAnimator() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                float alpha = mBottomViewAnimateForward ? value : 1 - value;
                mRecurrenceResultLabel.setAlpha(alpha);
                mRecurrenceDuration.setAlpha(alpha);
            }
        });
        return animator;
    }

    /**
     * Duration View와 Result View의 Visible상태를 animation과 함께 갱신한다.
     * @see R.id#recurrence_duration
     * @see R.id#recurrence_result
     * @param isShow 보이기/숨기기
     * @param isAnimate Animation을 주겠는가?
     */
    private void updateVisibilityBottomViews(boolean isShow, boolean isAnimate) {
        //Animation을 줄때
        if(isAnimate) {
            //보여주는 animation
            if (isShow) {
                if(mBottomViewAnimator != null && mBottomViewAnimator.isRunning()) {
                    if(mBottomViewAnimateForward)
                        return;
                    else
                        mBottomViewAnimator.cancel();
                }
                else if(mRecurrenceResultLabel.getVisibility() == View.VISIBLE) {
                    return;
                }

                //Fade animation 효과를 준다.
                Animator animator = createBottomViewAnimator();
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mRecurrenceResultLabel.setVisibility(View.VISIBLE);
                        mRecurrenceDuration.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mBottomViewAnimator.removeAllListeners();
                        mBottomViewAnimator = null;
                    }
                });
                animator.setDuration(ANIMATE_VISIBLE_DURATION);
                animator.start();

                mBottomViewAnimator = animator;
                mBottomViewAnimateForward = true;
            }

            //숨기기하는 animation
            else {
                if(mBottomViewAnimator != null && mBottomViewAnimator.isRunning()) {
                    if(!mBottomViewAnimateForward)
                        return;
                    else
                        mBottomViewAnimator.cancel();
                }
                else if(mRecurrenceResultLabel.getVisibility() == View.GONE) {
                    return;
                }

                //Fade animation 효과를 준다.
                Animator animator = createBottomViewAnimator();
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mRecurrenceResultLabel.setVisibility(View.GONE);
                        mRecurrenceDuration.setVisibility(View.GONE);

                        mBottomViewAnimator.removeAllListeners();
                        mBottomViewAnimator = null;
                    }
                });
                animator.setDuration(ANIMATE_VISIBLE_DURATION);
                animator.start();

                mBottomViewAnimator = animator;
                mBottomViewAnimateForward = false;
            }
        }

        //Animation을 주지 않을때
        else {
            if (isShow) {
                mRecurrenceResultLabel.setVisibility(View.VISIBLE);
                mRecurrenceDuration.setVisibility(View.VISIBLE);
            } else {
                mRecurrenceResultLabel.setVisibility(View.GONE);
                mRecurrenceDuration.setVisibility(View.GONE);
            }
        }
    }

    /**
     * View의 visible 상태를 animation과 함께 갱신한다.
     * @param isShow 보이기/숨기기
     * @param isAnimate Animation을 주겠는가?
     */
    private void updateVisibilityCustomizeView(boolean isShow, boolean isAnimate){
        if(isShow)
            mViewForCustomize.expand(isAnimate);
        else
            mViewForCustomize.collapse(isAnimate);
    }

    private void updateVisibilityRepeatContentView(boolean isShow, boolean isAnimate){
        mRepeatContent.setVisibility(View.VISIBLE);
        if(isShow)
            mRepeatContent.expand(isAnimate);
        else
            mRepeatContent.collapse(isAnimate);
    }

    // Implements OnItemSelectedListener interface
    // Freq spinner
    // End spinner

    /**
     * Spinner 조종체의 선택이 바뀌였을때
     * @param parent AdapterView
     * @param view View
     * @param position 위치
     * @param id Id
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mEndSpinner) {
            switch (position) {
                case RecurrenceModel.END_NEVER:
                    mModel.end = RecurrenceModel.END_NEVER;
                    break;
                case RecurrenceModel.END_BY_DATE:
                    mModel.end = RecurrenceModel.END_BY_DATE;
                    break;
                case RecurrenceModel.END_BY_COUNT:
                    mModel.end = RecurrenceModel.END_BY_COUNT;

                    if (mModel.endCount <= 1) {
                        mModel.endCount = 1;
                    } else if (mModel.endCount > COUNT_MAX) {
                        mModel.endCount = COUNT_MAX;
                    }
                    updateEndCountText();
                    break;
            }
            mEndCount.setVisibility(mModel.end == RecurrenceModel.END_BY_COUNT ? View.VISIBLE
                    : View.GONE);
            mEndDateTextView.setVisibility(mModel.end == RecurrenceModel.END_BY_DATE ? View.VISIBLE
                    : View.GONE);
            mPostEndCount.setVisibility(
                    mModel.end == RecurrenceModel.END_BY_COUNT && !mHidePostEndCount?
                            View.VISIBLE : View.GONE);

        }

        updateRecurrenceResult();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        if(picker == mViewCustomInterval) {
            playSoundEffect();
            updateInterval(newVal);
        }
        else if(picker == mViewCustomRecurrenceType) {
            playSoundEffect();
            updateCustomRecurrenceType(newVal, true);
        }

        updateRecurrenceResult();
    }

    private void updateInterval(int value) {
        mModel.interval = value;
    }

    /**
     * 맞춤설정에서 반복방식이 변하였을때 호출된다
     * @param recurrenceType 반복방식
     * @param isClick 눌러서 변하였는가?
     */
    private void updateCustomRecurrenceType(int recurrenceType, boolean isClick) {
        mCustomRecurrenceType = recurrenceType;

        //View 들의 Visible 상태를 갱신한다.
        switch (recurrenceType) {
            case CUSTOM_RECURRENCE_DAILY:   //일반복
                mViewCustomInterval.setMaxValue(100);
                updateVisibilityRepeatContentView(false, isClick);

                mModel.freq = RecurrenceModel.FREQ_DAILY;
                break;

            case CUSTOM_RECURRENCE_WEEKLY:  //주반복
                mViewCustomInterval.setMaxValue(52);
                mRepeatContentWeekly.setVisibility(View.VISIBLE);
                mRepeatContentMonthly.setVisibility(View.GONE);
                updateVisibilityRepeatContentView(true, isClick);

                mModel.freq = RecurrenceModel.FREQ_WEEKLY;
                break;

            case CUSTOM_RECURRENCE_MONTHLY: //월반복
                mViewCustomInterval.setMaxValue(12);
                mRepeatContentWeekly.setVisibility(View.GONE);
                mRepeatContentMonthly.setVisibility(View.VISIBLE);
                updateVisibilityRepeatContentView(true, isClick);

                mModel.freq = RecurrenceModel.FREQ_MONTHLY;
                break;

            case CUSTOM_RECURRENCE_YEARLY:  //년반복
                mViewCustomInterval.setMaxValue(10);
                updateVisibilityRepeatContentView(false, isClick);

                mModel.freq = RecurrenceModel.FREQ_YEARLY;
                break;
        }

        updateInterval(mViewCustomInterval.getValue());
    }

    /**
     * 효과음 재생
     */
    private void playSoundEffect() {
        MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), com.lany.picker.R.raw.click2);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }
        });
    }

    private void updateRecurrenceResult() {
        if(mModel.recurrenceState == RecurrenceModel.STATE_NO_RECURRENCE)
            return;

        //Interval 값을 림시 보관했다가 다시 복귀한다.
        int tempInterval = mModel.interval;
        if(mDefaultRecurrenceType != RECURRENCE_CUSTOMIZE)
            updateInterval(1);
        copyModelToEventRecurrence(mModel, mRecurrence, mTime);
        String resultString = EventRecurrenceFormatter.getFullRepeatString(mContext, mResources, mRecurrence, true);
        mRecurrenceResultLabel.setText(resultString);
        updateInterval(tempInterval);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if(group == mRepeatContentMonthly) {
            if (checkedId == R.id.repeat_month_same_date) {
                mModel.monthlyRepeat = RecurrenceModel.MONTHLY_BY_DATE;
            } else if (checkedId == R.id.repeat_month_same_weekday) {
                mModel.monthlyRepeat = RecurrenceModel.MONTHLY_BY_NTH_DAY_OF_WEEK;
            }
        }

        updateRecurrenceResult();
    }

    @Override
    public void onDateSelected(int year, int monthOfYear, int dayOfMonth) {
        if (mModel.endDate == null) {
            mModel.endDate = new Time(mTime.timezone);
            mModel.endDate.allDay = true;
        }
        mModel.endDate.year = year;
        mModel.endDate.month = monthOfYear;
        mModel.endDate.monthDay = dayOfMonth;
        mModel.endDate.normalize(false);

        //`특정일까지`에서 날자 TextView 에 본문 설정
        final String dateStr = DateUtils.formatDateTime(getActivity(),
                mModel.endDate.toMillis(false), DateUtils.FORMAT_NUMERIC_DATE);
        mEndDateTextView.setText(dateStr);

        updateRecurrenceResult();
    }

    @Override
    public void onDateSelected(int year, int monthOfYear, int dayOfMonth, boolean isStart) {}

    public interface OnRecurrenceSetListener {
        void onRecurrenceSet(String rrule);
    }

    /**
     * 반복설정을 구성하는 Model 클라스
     */
    private static class RecurrenceModel implements Parcelable {
        /*-- 반복 방식들 --*/
        static final int FREQ_DAILY = 0;
        static final int FREQ_WEEKLY = 1;
        static final int FREQ_MONTHLY = 2;
        static final int FREQ_YEARLY = 3;

        static final int END_NEVER = 0;
        static final int END_BY_DATE = 1;
        static final int END_BY_COUNT = 2;

        /**
         * 월반복 - 날자반복
         * 실례: 매달 25일 반복
         */
        static final int MONTHLY_BY_DATE = 0;
        /**
         * 월반복 - 요일반복
         * 실례: 매달 2번째 수요일 반복
         */
        static final int MONTHLY_BY_NTH_DAY_OF_WEEK = 1;

        static final int STATE_NO_RECURRENCE = 0;
        static final int STATE_RECURRENCE = 1;
        /*--  --*/

        int recurrenceState;

        /**
         * 기본 반복방식
         * {@link #FREQ_DAILY}, {@link #FREQ_WEEKLY}, {@link #FREQ_MONTHLY}, {@link #FREQ_YEARLY} 중의 하나
         */
        int freq = FREQ_WEEKLY;

        /**
         * 간격
         * `3주마다`에서 3을 의미함
         */
        int interval = INTERVAL_DEFAULT;

        /**
         * UNTIL and COUNT: How does the the event end?
         *
         */
        int end;

        /**
         * UNTIL: Date of the last recurrence. Used when until == END_BY_DATE
         * 기간 -
         */
        Time endDate;

        /**
         * `마감`-`여러번`에서 반복회수
         */
        int endCount = COUNT_DEFAULT;

        /**
         * `주반복`에서 반복할 요일들 (일 : 0, 월 : 1, ...)
         */
        boolean[] weeklyByDayOfWeek = new boolean[7];

        /**
         * 월반복 방식
         * {@link #MONTHLY_BY_DATE}, {@link #MONTHLY_BY_NTH_DAY_OF_WEEK} 중의 하나
         */
        int monthlyRepeat;

        /**
         * `월반복`-`날자반복`(매달 25일)에서 날자
         * @see #MONTHLY_BY_DATE
         */
        int monthlyByMonthDay;

        /**
         * `월반복`-`요일반복`(매달 2번째 수요일)에서 요일
         * @see #MONTHLY_BY_NTH_DAY_OF_WEEK
         */
        int monthlyByDayOfWeek;

        /**
         * `월반복`-`요일반복`(매달 2번째 수요일)에서 주순서
         * @see #MONTHLY_BY_NTH_DAY_OF_WEEK
         */
        int monthlyByNthDayOfWeek;

        public RecurrenceModel() {
        }

        protected RecurrenceModel(Parcel in) {
            recurrenceState = in.readInt();
            freq = in.readInt();
            interval = in.readInt();
            end = in.readInt();
            endCount = in.readInt();
            weeklyByDayOfWeek = in.createBooleanArray();
            monthlyRepeat = in.readInt();
            monthlyByMonthDay = in.readInt();
            monthlyByDayOfWeek = in.readInt();
            monthlyByNthDayOfWeek = in.readInt();
        }

        public final Creator<RecurrenceModel> CREATOR = new Creator<RecurrenceModel>() {
            @Override
            public RecurrenceModel createFromParcel(Parcel in) {
                return new RecurrenceModel(in);
            }

            @Override
            public RecurrenceModel[] newArray(int size) {
                return new RecurrenceModel[size];
            }
        };

        /*
         * (generated method)
         */
        @Override
        public String toString() {
            return "Model [freq=" + freq + ", interval=" + interval + ", end=" + end + ", endDate="
                    + endDate + ", endCount=" + endCount + ", weeklyByDayOfWeek="
                    + Arrays.toString(weeklyByDayOfWeek) + ", monthlyRepeat=" + monthlyRepeat
                    + ", monthlyByMonthDay=" + monthlyByMonthDay + ", monthlyByDayOfWeek="
                    + monthlyByDayOfWeek + ", monthlyByNthDayOfWeek=" + monthlyByNthDayOfWeek + "]";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(freq);
            dest.writeInt(interval);
            dest.writeInt(end);
            dest.writeInt(endDate.year);
            dest.writeInt(endDate.month);
            dest.writeInt(endDate.monthDay);
            dest.writeInt(endCount);
            dest.writeBooleanArray(weeklyByDayOfWeek);
            dest.writeInt(monthlyRepeat);
            dest.writeInt(monthlyByMonthDay);
            dest.writeInt(monthlyByDayOfWeek);
            dest.writeInt(monthlyByNthDayOfWeek);
            dest.writeInt(recurrenceState);
        }
    }

    /**
     * 최대, 최소한계를 가진 옹근수입력을 위한 TextWatcher 클라스
     */
    static class MinMaxTextWatcher implements TextWatcher {
        private final int mMin;
        private final int mMax;
        private final int mDefault;

        public MinMaxTextWatcher(int min, int defaultInt, int max) {
            mMin = min;
            mMax = max;
            mDefault = defaultInt;
        }

        @Override
        public void afterTextChanged(Editable s) {

            boolean updated = false;
            int value;
            try {
                value = Integer.parseInt(s.toString());
            } catch (NumberFormatException e) {
                value = mDefault;
            }

            //한계값을 넘어섰는가 검사
            if (value < mMin) {
                value = mMin;
                updated = true;
            } else if (value > mMax) {
                updated = true;
                value = mMax;
            }

            //한계값을 넘어섰으면 그 한계값으로 설정
            if (updated) {
                s.clear();
                s.append(Integer.toString(value));
            }

            onChange(value);
        }

        /**
         * 이것을 override 하여 text변화를 감지한다.
         */
        void onChange(int value) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    /**
     * `기간`에 해당한 Spinner 조종체에 적용하는 Adapter
     */
    private class EndSpinnerAdapter extends ArrayAdapter<CharSequence> {
        final String END_DATE_MARKER = "%s";
        final String END_COUNT_MARKER = "%d";

        private final LayoutInflater mInflater;
        private final int mItemResourceId;
        private final int mTextResourceId;
        private final ArrayList<CharSequence> mStrings;
        private final String mEndDateString;
        private boolean mUseFormStrings;

        /**
         * @param context Context
         * @param textResourceId Resource Id
         */
        public EndSpinnerAdapter(Context context, ArrayList<CharSequence> strings,
                                 int itemResourceId, int textResourceId) {
            super(context, itemResourceId, strings);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mItemResourceId = itemResourceId;
            mTextResourceId = textResourceId;
            mStrings = strings;
            mEndDateString = getResources().getString(R.string.recurrence_end_date);

            // If either date or count strings don't translate well, such that we aren't assured
            // to have some text available to be placed in the spinner, then we'll have to use
            // the more form-like versions of both strings instead.
            int markerStart = mEndDateString.indexOf(END_DATE_MARKER);
            if (markerStart <= 0) {
                // The date string does not have any text before the "%s" so we'll have to use the
                // more form-like strings instead.
                mUseFormStrings = true;
            } else {
                String countEndStr = getResources().getQuantityString(
                        R.plurals.recurrence_end_count, 1);
                markerStart = countEndStr.indexOf(END_COUNT_MARKER);
                if (markerStart <= 0) {
                    // The count string does not have any text before the "%d" so we'll have to use
                    // the more form-like strings instead.
                    mUseFormStrings = true;
                }
            }

            if (mUseFormStrings) {
                // We'll have to set the layout for the spinner to be weight=0 so it doesn't
                // take up too much space.
                mEndSpinner.setLayoutParams(
                        new TableLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            // Check if we can recycle the view
            if (convertView == null) {
                v = mInflater.inflate(mTextResourceId, parent, false);
            } else {
                v = convertView;
            }

            TextView item = (TextView) v.findViewById(R.id.spinner_item);
            int markerStart;
            switch (position) {
                case END_NEVER:
                    item.setText(mStrings.get(END_NEVER));
                    break;
                case END_BY_DATE:
                    markerStart = mEndDateString.indexOf(END_DATE_MARKER);

                    if (markerStart != -1) {
                        if (mUseFormStrings || markerStart == 0) {
                            // If we get here, the translation of "Until" doesn't work correctly,
                            // so we'll just set the whole "Until a date" string.
                            item.setText(mEndDateLabel);
                        } else {
                            item.setText(mEndDateString.substring(0, markerStart).trim());
                        }
                    }
                    break;
                case END_BY_COUNT:
                    String endString = mResources.getQuantityString(R.plurals.recurrence_end_count,
                            mModel.endCount);
                    markerStart = endString.indexOf(END_COUNT_MARKER);

                    if (markerStart != -1) {
                        if (mUseFormStrings || markerStart == 0) {
                            // If we get here, the translation of "For" doesn't work correctly,
                            // so we'll just set the whole "For a number of events" string.
                            item.setText(mEndCountLabel);
                            // Also, we'll hide the " events" that would have been at the end.
                            mPostEndCount.setVisibility(View.GONE);
                            // Use this flag so the onItemSelected knows whether to show it later.
                            mHidePostEndCount = true;
                        } else {
                            int postTextStart = markerStart + END_COUNT_MARKER.length();
                            mPostEndCount.setText(endString.substring(postTextStart,
                                    endString.length()).trim());
                            // In case it's a recycled view that wasn't visible.
                            if (mModel.end == END_BY_COUNT) {
                                mPostEndCount.setVisibility(View.VISIBLE);
                            }
                            if (endString.charAt(markerStart - 1) == ' ') {
                                markerStart--;
                            }
                            item.setText(endString.substring(0, markerStart).trim());
                        }
                    }
                    break;
                default:
                    v = null;
                    break;
            }

            return v;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View v;
            // Check if we can recycle the view
            if (convertView == null) {
                v = mInflater.inflate(mItemResourceId, parent, false);
            } else {
                v = convertView;
            }

            TextView item = (TextView) v.findViewById(R.id.spinner_item);
            item.setText(mStrings.get(position));

            return v;
        }
    }
}
