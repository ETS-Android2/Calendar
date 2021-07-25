package com.android.calendar.kr.standard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.calendar.activities.AllInOneActivity;
import com.android.calendar.kr.common.CalendarViewDelegate;
import com.android.calendar.kr.common.CustomMonthView;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import org.joda.time.DateTime;

/**
 * 양식 1
 * 한개 월 - 월표시부분 - 한개 페지의 content view
 * 한달 날자들을 현시
 */
public class StandardMonthViewContainer extends LinearLayout {
    private static final int MONTH_TEXT_FILL_COLOR = Color.WHITE;
    private static final int MONTH_TEXT_STROKE_COLOR = 0xFF00A000;
    private static final int MONTH_TEXT_STROKE_WIDTH = 5;
    private final float MONTH_TEXT_SIZE_ON_MONTH = getResources().getDimension(R.dimen.month_text_size_on_month);
    private static final float TRANSFORM_START_DELAY = 0.1f;

    //날자들을 그려주는 View
    private CustomMonthView mMonthView;
    //배경화상 ImageView
    private MaskImageView mBackground;

    CalendarViewDelegate mDelegate;
    AllInOneActivity mMainActivity;

    //년, 월
    int mYear = -1, mMonth = -1;

    int mBackgroundHeight;
    Paint mPaint = new Paint();
    float mBackgroundOffset = 0;
    float mMonthViewOffset = 0;
    float mBgAlphaPos;
    Boolean mUseFadeAlpha = false;
    float mFadeAlpha = 1;

    public StandardMonthViewContainer(Context context) {
        this(context, null);
    }

    public StandardMonthViewContainer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StandardMonthViewContainer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMainActivity = AllInOneActivity.getMainActivity(context);

        mPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        mPaint.setTextSize(MONTH_TEXT_SIZE_ON_MONTH);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();
        mMonthView = findViewById(R.id.month_view);
        mBackground = findViewById(R.id.bg_image);
    }

    public void setup(CalendarViewDelegate delegate, int year, int month, int position){
        mMonthView.setup(delegate);
        mMonthView.initMonthWithDate(year, month);
        mMonthView.setCalendarType(Utils.CALENDAR_TYPE1);
        mMonthView.setSelectedCalendar(delegate.mSelectedCalendar);
        mBackground.setImageResource(Utils.getMonthImageResource(month));

        mDelegate = delegate;

        mYear = year;
        mMonth = month;

        if(Utils.isDayToMonthTransition()) {
            DateTime selectedTime = new DateTime(mMainActivity.getCalendarController().getTime());
            if(selectedTime.getYear() == mYear && selectedTime.getMonthOfYear() == mMonth) {
                mUseFadeAlpha = true;
                mFadeAlpha = 0;
            }
        }

        //Set tag to find this view
        setTag(position);
    }

    @Override
    public void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);

        //월 수자를 그려준다.
        if(mYear != -1 && mMonth != -1){
            canvas.translate(0, mMonthViewOffset);
            canvas.save();
            @SuppressLint("DefaultLocale") String monthString = String.format("%1$d", mMonth);
            Rect bounds = new Rect();
            mPaint.getTextBounds(monthString, 0, monthString.length(), bounds);

            final float x;
            final float y;

            x = getResources().getDimension(R.dimen.month_text_left_padding);
            y = mBackgroundHeight - getResources().getDimension(R.dimen.month_text_bottom_padding);

            //Alpha값을 계산
            int alpha = (int) (100 + (255-100)*mBgAlphaPos);

            //월 수자의 안쪽 그려주기
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Utils.applyAlpha(MONTH_TEXT_FILL_COLOR, alpha));
            if(mUseFadeAlpha) {
                //Apply alpha to paint
                mPaint.setAlpha((int) (mFadeAlpha * 255));
            }
            canvas.drawText(monthString, x, y, mPaint);

            //월 수자의 테두리 그려주기
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(MONTH_TEXT_STROKE_WIDTH);
            mPaint.setColor(Utils.applyAlpha(MONTH_TEXT_STROKE_COLOR, alpha));
            if(mUseFadeAlpha) {
                mPaint.setAlpha((int) (mFadeAlpha * 255));
            }
            canvas.drawText(monthString, x, y, mPaint);

            canvas.restore();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mBackgroundHeight = MeasureSpec.getSize(heightMeasureSpec) * mDelegate.getMonthBackgroundHeightPercent()/100;
        int BgHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mBackgroundHeight, MeasureSpec.EXACTLY);
        mBackground.measure(widthMeasureSpec, BgHeightMeasureSpec);
    }

    public void setFadeAnimatorProgress(float progress) {
        mUseFadeAlpha = true;
        mFadeAlpha = progress;
        invalidate();
    }

    /**
     * 페지전환할때 호출된다
     * @param position 이동위치 -1 ~ 1
     */
    public void setTransformAnimatorProgress(float position){
        //2개의 View가 이동한다.
        //position이 부수일때는 첫번째 View에 해당되고 0,정수일때는 두번째 View에 해당된다.
        if(position < 0) {
            float abovePos = Math.abs(position);
            mBackgroundOffset = (getHeight() - mBackgroundHeight) * abovePos;
            mMonthViewOffset = 0;
        }
        else {
            float belowPos = 1 - position;
            mBackgroundOffset = 0;
            if (belowPos < TRANSFORM_START_DELAY) {
                mMonthViewOffset = -mBackgroundHeight;
            } else {
                mMonthViewOffset = -mBackgroundHeight + mBackgroundHeight * (belowPos - TRANSFORM_START_DELAY) / (1 - TRANSFORM_START_DELAY);
            }
        }

        mBackground.setTranslationY(mBackgroundOffset);
        mMonthView.setTranslationY(mMonthViewOffset);
        mBackground.setAlphaProgress(Math.abs(position));
        mBgAlphaPos = 1 - Math.abs(position);
        invalidate();
    }

    public CustomMonthView getMonthView(){
        return mMonthView;
    }
}
