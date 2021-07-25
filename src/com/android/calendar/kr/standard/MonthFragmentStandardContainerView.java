package com.android.calendar.kr.standard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

/**
 * 양식 1
 * Fragment의 root view
 * 기본성원함수들은 animation속성 함수들이다.
 *
 * @see R.animator#slide_to_right_ym
 * @see R.animator#slide_from_right_ym
 */
public class MonthFragmentStandardContainerView extends LinearLayout {
    public StandardCalendarView mCalendarView;
    AllInOneActivity mMainActivity;

    boolean mAnimationStart = true;

    public MonthFragmentStandardContainerView(Context context) {
        this(context, null);
    }

    public MonthFragmentStandardContainerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MonthFragmentStandardContainerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMainActivity = AllInOneActivity.getMainActivity(context);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        mCalendarView = findViewById(R.id.calendarView);
    }

    public void setMonthFadeEnter(float value) {
        if(mAnimationStart){
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.MONTH);
        }
        mAnimationStart = false;

        mCalendarView.setFadeAnimatorProgress(value);
        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);

        if(value == 1){
            mAnimationStart = true;
            Utils.setDayToMonthTransition(false);
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    public void setMonthFadeExit(float value) {
        if(mAnimationStart){
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.MONTH);
            mAnimationStart = false;
        }

        setAlpha(value);
        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);

        //Reset flag
        if(value == 0) {
            mAnimationStart = true;
            Utils.setMonthToDayTransition(false);
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    public void setSlideX(float value) {
        float width = getResources().getDisplayMetrics().widthPixels;
        setTranslationX(width * value);
    }

    public void setActionBarFadeEnter(float value) {
        if(mAnimationStart) {
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.MONTH);
            mAnimationStart = false;
        }

        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);
        if(value == 1) {
            Utils.setMonthToYearTransition(false);
            mAnimationStart = true;
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    public void setActionBarFadeExit(float value) {
        if(mAnimationStart) {
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.MONTH);
            mAnimationStart = false;
        }

        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);
        if(value == 0) {
            Utils.setMonthToYearTransition(false);
            mAnimationStart = true;
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    public void setFadeIn(float value) {
        if(mAnimationStart) {
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.MONTH);
            mAnimationStart = false;
        }

        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);
        if(value == 1) {
            mAnimationStart = true;
            mMainActivity.getActionBarHeader().getViewForMonth().setVisibility(VISIBLE);
            Utils.setFromAgendaTransition(false);
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    public void setFadeOut(float value) {
        if(mAnimationStart) {
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.MONTH);
            mAnimationStart = false;
        }

        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);
        if(value == 0) {
            mAnimationStart = true;
            mMainActivity.getActionBarHeader().getViewForMonth().setVisibility(INVISIBLE);
            Utils.setToAgendaTransition(false);
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }
}
