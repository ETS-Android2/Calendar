package com.android.calendar.kr.year;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;

import com.android.krcalendar.R;

/**
 * 년보기
 * Fragment의 root view
 * 기본성원함수들은 animation속성 함수들이다.
 *
 * @see R.animator#slide_from_left_ym
 * @see R.animator#slide_to_left_ym
 */
public class YearFragmentContainerView extends FrameLayout {
    AllInOneActivity mMainActivity;
    private boolean mAnimationStart = true;

    public YearFragmentContainerView(@NonNull Context context) {
        this(context, null);
    }

    public YearFragmentContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YearFragmentContainerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMainActivity = AllInOneActivity.getMainActivity(context);
    }

    /* Animation 속성함수들 */

    /**
     * 좌우로 움직이는 animation속성함수
     * @param value 0 ~ 1
     */
    public void setSlideX(float value) {
        float width = getResources().getDisplayMetrics().widthPixels;
        setTranslationX(width * value);
    }

    /**
     * 년보기가 왼쪽에서 오른쪽으로 가면서 생겨날때의 animation 속성 함수
     * @param value 0 ~ 1
     */
    public void setActionBarFadeEnter(float value) {
        if(mAnimationStart) {
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.YEAR);
            mAnimationStart = false;
        }

        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);
        if(value == 1) {
            Utils.setYearToMonthTransition(false);
            mAnimationStart = true;
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    /**
     * 년보기가 오른쪽에서 왼쪽으로 가면서 없어질때의 animation 속성 함수
     * @param value 0 ~ 1
     */
    public void setActionBarFadeExit(float value) {
        if(mAnimationStart) {
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.YEAR);
            mAnimationStart = false;
        }

        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);
        if(value == 0) {
            Utils.setYearToMonthTransition(false);
            mAnimationStart = true;
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }
}
