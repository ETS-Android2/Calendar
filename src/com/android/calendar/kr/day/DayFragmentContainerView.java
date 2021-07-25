package com.android.calendar.kr.day;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.helper.CalendarController;
import com.android.calendar.utils.Utils;
import com.android.calendar.kr.common.CalendarUtil;
import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.calendar.utils.LunarCoreHelper;
import com.android.krcalendar.R;

/**
 * 일보기(양식 1)
 * Fragment의 root view
 * 기본성원함수들은 animation속성 함수들이다.
 */
public class DayFragmentContainerView extends RelativeLayout implements DayViewFragment.OnDayChangeListener {

    private static final int DAY_TEXT_FILL_COLOR = Color.WHITE;
    private static final int DAY_TEXT_STROKE_COLOR = 0xFF00A000;
    private final float DAY_TEXT_STROKE_WIDTH = getResources().getDimension(R.dimen.day_text_stroke_width);
    private final float MONTH_TEXT_SIZE_ON_DAY = getResources().getDimension(R.dimen.month_text_size_on_day);
    private final float DAY_TEXT_SIZE_ON_DAY = getResources().getDimension(R.dimen.day_text_size_on_day);
    private final float LUNAR_DAY_TEXT_SIZE = getResources().getDimension(R.dimen.lunar_day_text_size_on_day);
    private static final int LUNAR_DAY_TEXT_STROKE_WIDTH = 2;
    private static final int LUNAR_DAY_TEXT_FILL_COLOR = 0xffffffff;
    private static final int LUNAR_DAY_TEXT_STROKE_COLOR = 0xFF00A000;

    //현재 페지의 년, 월, 일
    int mYear = -1;
    int mMonth = -1;
    int mDay = -1;

    //월배경화상을 보여주는 ImageView, 그 높이
    ImageView mBackground;
    int mBackgroundHeight;

    //일정목록을 보여주는 ViewPager
    ViewPager2 mDayViewPager;

    //오늘 단추
    View mTodayButton;

    //Calendar Delegate
    CalendarViewDelegate mDelegate;

    //기본 Activity
    AllInOneActivity mMainActivity;

    //그리기를 위한 Paint 객체들
    Paint mMonthTextPaint = new Paint();
    Paint mDayTextPaint = new Paint();
    Paint mLunarTextPaint = new Paint();

    float mFadeAlpha = 1;   //Animation 을 진행할때의 fade alpha 값
    boolean mAnimationStart = true; //Animation 이 진행중인가?

    public DayFragmentContainerView(Context context) {
        this(context, null);
    }

    public DayFragmentContainerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DayFragmentContainerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mMainActivity = AllInOneActivity.getMainActivity(context);

        mMonthTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mMonthTextPaint.setTextSize(MONTH_TEXT_SIZE_ON_DAY);
        mMonthTextPaint.setTextAlign(Paint.Align.CENTER);

        mDayTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mDayTextPaint.setTextSize(DAY_TEXT_SIZE_ON_DAY);
        mDayTextPaint.setTextAlign(Paint.Align.CENTER);

        mLunarTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mLunarTextPaint.setTextSize(LUNAR_DAY_TEXT_SIZE);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        //자식 View 들 얻기
        mBackground = findViewById(R.id.bg_image);
        mDayViewPager = findViewById(R.id.day_view_pager);
        mTodayButton = findViewById(R.id.go_to_today);

        if(Utils.isMonthToDayTransition()){
            setDayFade(0);
        }

        mMainActivity.getActionBarHeader().setTodayButtonForDay(mTodayButton);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);
        if(mYear == -1)
            return;

        //음력날자 현시
        int[] lunarDay = LunarCoreHelper.convertSolar2Lunar(mDay, mMonth, mYear);
        String lunarDayString = CalendarUtil.getLunarDayString(lunarDay, true, getContext());

        float lunarShowX = getResources().getDimension(R.dimen.month_text_left_padding);
        float lunarShowY = mBackgroundHeight - getResources().getDimension(R.dimen.lunar_text_bottom_padding);

        Utils.drawStrokedText(lunarDayString, canvas, mLunarTextPaint, LUNAR_DAY_TEXT_STROKE_WIDTH, LUNAR_DAY_TEXT_FILL_COLOR,
                LUNAR_DAY_TEXT_STROKE_COLOR, mFadeAlpha, lunarShowX, lunarShowY);

        /*-- 월, 일 문자렬 현시 --*/
        final float centerX = getWidth() * 0.5f;
        final float dayY, monthY;
        final float topY, bottomY;

        String dayString = String.format("%1$d", mDay);
        String monthString = Utils.getMonthString(mMonth, getContext());

        Rect bounds = new Rect();

        topY = Utils.convertDpToPixel(45, getContext());
        mLunarTextPaint.getTextBounds(lunarDayString, 0, lunarDayString.length(), bounds);
        bottomY = mBackgroundHeight - getResources().getDimension(R.dimen.lunar_text_bottom_padding) - bounds.height();

        //일 현시
        monthY = topY + (bottomY - topY) * 0.35f;
        dayY = topY + (bottomY - topY) * 0.8f;

        Utils.drawStrokedText(dayString, canvas, mDayTextPaint, DAY_TEXT_STROKE_WIDTH,
                DAY_TEXT_FILL_COLOR, DAY_TEXT_STROKE_COLOR, mFadeAlpha, centerX, dayY);

        mMonthTextPaint.setColor(Color.WHITE);
        mMonthTextPaint.setAlpha((int) (mFadeAlpha * 255));
        mMonthTextPaint.setStyle(Paint.Style.FILL);

        //월 현시
        canvas.drawText(monthString, 0, monthString.length(), centerX, monthY, mMonthTextPaint);
    }

    public void setFadeAnimatorProgress(float progress) {
        mFadeAlpha = progress;
        invalidate();
    }

    /**
     * Slide Animation 속성함수
     * @param value Float(0~1)
     */
    public void setSlideX(float value) {
        float width = getResources().getDisplayMetrics().widthPixels;
        setTranslationX(width * value);
    }

    public void setCalendarViewDelegate(CalendarViewDelegate delegate){
        mDelegate = delegate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mBackgroundHeight = MeasureSpec.getSize(heightMeasureSpec) * mDelegate.getMonthBackgroundHeightPercent()/100;
        int dayViewHeight = MeasureSpec.getSize(heightMeasureSpec) - mBackgroundHeight;
        int bgHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mBackgroundHeight, MeasureSpec.EXACTLY);
        int dayViewHeightMeasureSpec = MeasureSpec.makeMeasureSpec(dayViewHeight, MeasureSpec.EXACTLY);
        mBackground.measure(widthMeasureSpec, bgHeightMeasureSpec);
        mDayViewPager.measure(widthMeasureSpec, dayViewHeightMeasureSpec);

        ViewGroup.LayoutParams lpBackground = mBackground.getLayoutParams();
        lpBackground.height = mBackgroundHeight;

        ViewGroup.LayoutParams lpDayPager = mDayViewPager.getLayoutParams();
        lpDayPager.height = dayViewHeight;
    }

    /**
     * 날자선택이 변할때 호출된다.
     * @param year 년
     * @param month 월
     * @param day 일
     */
    @Override
    public void onDayChange(int year, int month, int day) {
        if(month != mMonth) {
            if(mMonth == -1){
                mBackground.setImageResource(Utils.getMonthImageResource(month));
            }
            else {
                Drawable oldDrawable = ContextCompat.getDrawable(getContext(), Utils.getMonthImageResource(mMonth));
                Drawable newDrawable = ContextCompat.getDrawable(getContext(), Utils.getMonthImageResource(month));

                TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                        oldDrawable, newDrawable
                });

                mBackground.setImageDrawable(td);
                td.startTransition(getResources().getInteger(R.integer.image_change_duration));
            }
        }
        mYear = year;
        mMonth = month;
        mDay = day;
        invalidate();
    }

    public void setDayFade(float value) {
        int currentItem = mDayViewPager.getCurrentItem();
        if(mDayViewPager.findViewWithTag(currentItem) != null){
            DayViewContainer dayViewContainer = (DayViewContainer)mDayViewPager.findViewWithTag(currentItem);
            dayViewContainer.setAlpha(value);
        }

        setFadeAnimatorProgress(value);
        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);
    }

    public void setDayFadeEnter(float value) {
        if(mAnimationStart)
        {
            mMainActivity.getActionBarHeader().updateViews();
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.DAY);
        }

        mAnimationStart = false;
        setDayFade(value);

        if(value == 1){
            mAnimationStart = true;
            Utils.setMonthToDayTransition(false);
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    public void setDayFadeExit(float value) {
        if(value == 1){
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.DAY);
        }

        mAnimationStart = false;

        setDayFade(value);

        mBackground.setVisibility(INVISIBLE);

        //Reset flag
        if(value == 0) {
            mAnimationStart = true;
            Utils.setDayToMonthTransition(false);
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    public void setFadeIn(float value) {
        if(mAnimationStart) {
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.DAY);
            mAnimationStart = false;
        }

        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);
        if(value == 1) {
            mAnimationStart = true;
            Utils.setFromAgendaTransition(false);
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    public void setFadeOut(float value) {
        if(mAnimationStart) {
            mMainActivity.getActionBarHeader().getAnimateViews(CalendarController.ViewType.DAY);
            mAnimationStart = false;
        }

        mMainActivity.getActionBarHeader().setFadeAnimationProgress(value);
        if(value == 0) {
            mAnimationStart = true;
            Utils.setToAgendaTransition(false);
            mMainActivity.getActionBarHeader().clearAnimateViews();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mMainActivity.getActionBarHeader().setTodayButtonForDay(null);
    }
}
