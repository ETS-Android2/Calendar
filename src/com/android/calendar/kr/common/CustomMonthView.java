package com.android.calendar.kr.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.android.calendar.event.EventTypeManager;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.android.krcalendar.R;
import org.joda.time.DateTime;

import static com.android.calendar.utils.Utils.CALENDAR_TYPE1;

/**
 * 양식 1,2
 * 한개 달의 날자들을 현시해주는 view
 * {@link MonthView}, {@link BaseMonthView}, {@link BaseView}들은 이클라스의 기초클라스들이다.
 */
public class CustomMonthView extends MonthView {
    /*
     * 오늘 일정화상교체 Animation 형태
     * 4가지 animation: cube, flip, wave, depth_field
     */
    private static final int ANIMATION_TYPE_CUBE = 0;
    private static final int ANIMATION_TYPE_FLIP = 1;
    private static final int ANIMATION_TYPE_WAVE = 2;
    private static final int ANIMATION_TYPE_DEPTH_FILED = 3;

    //Animation을 진행하는 상수값들
    private static final int ANIMATION_TYPE = ANIMATION_TYPE_FLIP;
    private static final int ANIMATE_DURATION = 400;        //cube, wave, depth_field 의 Animation지속시간
    private static final int ANIMATE_FLIP_DURATION = 300;   //Flip animation은 지속시간이 조금 짧다.
    private static final int ANIMATE_START_DELAY = 600;     //일정화상교체 animation에서 시작 지연시간

    //일정동그라미 반경
    private final float SCHEME_CIRCLE_RADIUS = getContext().getResources().getDimension(R.dimen.scheme_circle_radius);

    //선택된 날자 동그라미의 반경
    private int mRadius;

    /**
     * 일정동그라미를 그리는데 리용되는 Paint
     */
    private final Paint mSchemeBasicPaint = new Paint();

    //오늘의 일정화상목록
    List<Drawable> mTodayEventImages = new ArrayList<>();
    int mTodayImageSize = 0;

    //오늘일정 animation을 진행하는데 리용되는 변수들
    float mTodayX, mTodayY;
    int mAnimateImageIndex = 0;
    boolean mAnimateDirectionForward = true;
    boolean mInitialExpandAnimating = true;
    boolean mAnimating = false;
    float mAnimateProgress = 0;
    Animator mTodayAnimator = null;

    //Animation을 진행하는데 2개의 ImageView가 리용된다.
    ImageView mFirstView, mSecondView;

    public CustomMonthView(Context context) {
        this(context, null);
    }

    public CustomMonthView(Context context, AttributeSet attrs){
        super(context, attrs);

        mSchemeBasicPaint.setAntiAlias(true);
        mSchemeBasicPaint.setStyle(Paint.Style.FILL);
        mSchemeBasicPaint.setTextAlign(Paint.Align.CENTER);
        mSchemeBasicPaint.setFakeBoldText(true);
        mSchemeBasicPaint.setColor(Color.WHITE);

        setLayerType(View.LAYER_TYPE_SOFTWARE, mSelectedPaint);
        setLayerType(View.LAYER_TYPE_SOFTWARE, mSchemeBasicPaint);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    /**
     * 한개 날자의 너비, 동그라미 반경을 계산한다.
     */
    protected void getItemWidth() {
        mItemWidth = (getWidth() - 2 * mDelegate.getCalendarPadding()) / 7;
        mRadius = Math.min(mItemWidth, mItemHeight) / 3;
    }

    @Override
    protected void onDrawToday(Canvas canvas, Calendar calendar, int x, int y) {
        if(mAnimating){
            if(!mInitialExpandAnimating)
                return;
            if(mAnimateProgress == 1)
                return;
        }
        int cx = x + mItemWidth / 2;
        int cy = y + mItemHeight / 2;

        //확대/축소가 있기때문에 canvas를 보관했다가 다시 복귀해준다.
        canvas.save();
        if(mAnimating){
            float animValue = Math.max(0, 1 - 2*mAnimateProgress);
            canvas.scale(animValue, animValue, cx, cy);
            mCurrentDayPaint.setAlpha((int) (animValue * 255));
        }
        else {
            mCurrentDayPaint.setAlpha(255);
        }
        canvas.drawCircle(cx, cy, mRadius, mCurrentDayPaint);
        canvas.restore();
    }

    /**
     * 선택된 날자를 그린다.
     * @param canvas    canvas
     * @param calendar  날자정보
     * @param x         수평좌표
     * @param y         수직좌표
     */
    @Override
    protected boolean onDrawSelected(Canvas canvas, Calendar calendar, int x, int y) {

        int cx = x + mItemWidth / 2;
        int cy = y + mItemHeight / 2;
        canvas.drawCircle(cx, cy, mRadius, mSelectedPaint);
        return true;
    }

    /**
     * 일정동그라미를 그린다.
     * @param canvas   canvas
     * @param calendar 날자정보
     * @param x        수평좌표
     * @param y        수직좌표
     */
    @Override
    protected void onDrawScheme(Canvas canvas, Calendar calendar, int x, int y) {
        mSchemeBasicPaint.setColor(calendar.getSchemeColor());
        int cx = x + mItemWidth / 2;
        int cy = y + mItemHeight / 2;
        canvas.drawCircle(cx, cy + mRadius + SCHEME_CIRCLE_RADIUS + Utils.convertDpToPixel(2, getContext()),
                SCHEME_CIRCLE_RADIUS, mSchemeBasicPaint);
    }

    /**
     * 날자그려주기
     * @param canvas     canvas
     * @param calendar   날자정보
     * @param x          수평좌표
     * @param y          수직좌표
     * @param isSelected 선택되였는가
     */
    @Override
    protected void onDrawText(Canvas canvas, Calendar calendar, int x, int y, boolean isSelected) {

        int cx = x + mItemWidth / 2;
        int cy = y + mItemHeight / 2;
        float baselineY = mTextBaseLine + y;

        boolean isToday = calendar.isCurrentDay();

        if(isToday && calendar.isCurrentMonth()){

            if(mAnimating){
                if(!mInitialExpandAnimating)
                    return;
                if(mAnimateProgress == 1)
                    return;
            }

            //확대/축소가 있기때문에 보관했다가 다시 복귀해준다.
            canvas.save();
            if(mAnimating){
                float animValue = Math.max(0, 1 - 2*mAnimateProgress);
                canvas.scale(animValue, animValue, cx, cy);
                mCurDayTextPaint.setAlpha((int) (animValue * 255));
            }
            else {
                mCurDayTextPaint.setAlpha(255);
            }
            canvas.drawText(String.valueOf(calendar.getDay()), cx, baselineY, mCurDayTextPaint);
            canvas.restore();
        }
        else if (isSelected && calendar.isCurrentMonth()) {
            canvas.drawText(String.valueOf(calendar.getDay()), cx, baselineY,
                    mSelectTextPaint);
        }
        else {
            canvas.drawText(String.valueOf(calendar.getDay()), cx, baselineY,
                    calendar.isCurrentMonth()? mCurMonthTextPaint : mOtherMonthTextPaint);
        }
    }

    /**
     * 오늘 일정화상교체 animation을 보여준다.
     */
    public void startTodayAnimation() {
        getTodayEventData();
        if(mTodayEventImages.isEmpty())
            return;

        if(mFirstView == null){
            mFirstView = new ImageView(getContext());
            mFirstView.layout(0,0, mTodayImageSize, mTodayImageSize);
            addView(mFirstView);
        }
        if(mSecondView == null){
            mSecondView = new ImageView(getContext());
            mSecondView.layout(0,0, mTodayImageSize, mTodayImageSize);
            addView(mSecondView);
        }
        List<Animator> animatorList = new ArrayList<>();
        animatorList.add(getInitialExpandAnimator());
        if(mTodayEventImages.size() > 1) {
            for (int i = 0; i < mTodayEventImages.size(); i++)
                animatorList.addAll(Arrays.asList(getNthImageSwapAnimator(i)));
        }

        if(mTodayAnimator != null)
            mTodayAnimator.end();

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(animatorList);
        animatorSet.start();

        mTodayAnimator = animatorSet;
    }

    /**
     * 오늘의 일정화상 animation없애기
     */
    public void removeTodayAnimation() {
        if(mTodayAnimator != null)
            mTodayAnimator.end();
        removeAllViews();
        mFirstView = null;
        mSecondView = null;
        mAnimating = false;
        invalidate();
    }

    /**
     * 첫 일정화상이 생겨날때에는 확대되면서 생겨난다.
     * @return 이 animation의 animator를 돌려준다.
     */
    public Animator getInitialExpandAnimator(){
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimating = true;
                mInitialExpandAnimating = true;
                mAnimateImageIndex = 0;
                mAnimateProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        animator.setDuration(800);
        animator.setStartDelay(300);
        return animator;
    }

    /**
     * 일정화상들이 교체되는 animation의 animator
     * @param index 화상번호(0 ~ 일정개수-1)
     */
    public Animator[] getNthImageSwapAnimator(int index){
        switch (ANIMATION_TYPE) {
            case ANIMATION_TYPE_CUBE:
                return getCubeAnimator(index);
            case ANIMATION_TYPE_FLIP:
                return getFlipAnimator(index);
            case ANIMATION_TYPE_WAVE:
                return getWaveAnimator(index);
            case ANIMATION_TYPE_DEPTH_FILED:
            default:
                return getDepthFieldAnimator(index);
        }
    }

    /* 4가지 animation 들에 대한 animator 들 */
    public Animator[] getCubeAnimator(int index){
        ValueAnimator animatorForward = ValueAnimator.ofFloat(0, 1);
        animatorForward.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                final Drawable firstDrawable, secondDrawable;
                firstDrawable = mTodayEventImages.get(index);
                if(index == mTodayEventImages.size() - 1){
                    secondDrawable = mTodayEventImages.get(0);
                }
                else {
                    secondDrawable = mTodayEventImages.get(index + 1);
                }

                if(mFirstView != null)
                    mFirstView.setImageDrawable(firstDrawable);
                if(mSecondView != null)
                    mSecondView.setImageDrawable(secondDrawable);
            }
        });
        animatorForward.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimating = true;
                mInitialExpandAnimating = false;
                mAnimateImageIndex = index;
                mAnimateDirectionForward = true;
                mAnimateProgress = (float)animation.getAnimatedValue();

                final float posX = mTodayX - mTodayImageSize/2f;
                final float posY =  mTodayY - mTodayImageSize/2f;
                if (mFirstView != null) {
                    mFirstView.setTranslationX(posX - mAnimateProgress * mTodayImageSize);
                    mFirstView.setTranslationY(posY);
                    mFirstView.setPivotX(mTodayImageSize);
                    mFirstView.setPivotY(mTodayImageSize / 2f);
                    mFirstView.setRotationY(-mAnimateProgress * 90);
                }
                if (mSecondView != null) {
                    mSecondView.setTranslationX(posX + (1 - mAnimateProgress) * mTodayImageSize);
                    mSecondView.setTranslationY(posY);
                    mSecondView.setPivotX(0);
                    mSecondView.setRotationY(mAnimateProgress * 90 - 90);
                }
            }
        });
        animatorForward.setInterpolator(new LinearInterpolator());
        animatorForward.setDuration(ANIMATE_DURATION);
        animatorForward.setStartDelay(ANIMATE_START_DELAY);

        return new Animator[]{animatorForward};
    }
    public Animator[] getFlipAnimator(int index){
        ValueAnimator animatorForward = ValueAnimator.ofFloat(0, 1);
        ValueAnimator animatorBackward = ValueAnimator.ofFloat(0, 1);
        animatorForward.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                final Drawable firstDrawable, secondDrawable;
                firstDrawable = mTodayEventImages.get(index);
                if(index == mTodayEventImages.size() - 1){
                    secondDrawable = mTodayEventImages.get(0);
                }
                else {
                    secondDrawable = mTodayEventImages.get(index + 1);
                }

                if(mFirstView != null)
                    mFirstView.setImageDrawable(firstDrawable);
                if(mSecondView != null)
                    mSecondView.setImageDrawable(secondDrawable);
            }
        });
        animatorForward.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimating = true;
                mInitialExpandAnimating = false;
                mAnimateImageIndex = index;
                mAnimateDirectionForward = true;
                mAnimateProgress = (float)animation.getAnimatedValue();

                final float posX = mTodayX - mTodayImageSize/2f;
                final float posY =  mTodayY - mTodayImageSize/2f;
                if (mFirstView != null) {
                    mFirstView.setVisibility(VISIBLE);
                    mFirstView.setTranslationX(posX);
                    mFirstView.setTranslationY(posY);
                    mFirstView.setRotationY(mAnimateProgress*90);
                }
                if (mSecondView != null) {
                    mSecondView.setVisibility(INVISIBLE);
                }
            }
        });

        animatorBackward.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimating = true;
                mInitialExpandAnimating = false;
                mAnimateImageIndex = index;
                mAnimateDirectionForward = true;
                mAnimateProgress = (float)animation.getAnimatedValue();

                final float posX = mTodayX - mTodayImageSize/2f;
                final float posY =  mTodayY - mTodayImageSize/2f;
                if (mFirstView != null) {
                    mFirstView.setVisibility(INVISIBLE);
                }
                if (mSecondView != null) {
                    mSecondView.setVisibility(VISIBLE);
                    mSecondView.setTranslationX(posX);
                    mSecondView.setTranslationY(posY);
                    mSecondView.setRotationY((1 - mAnimateProgress)*90);
                }
            }
        });

        animatorForward.setInterpolator(new LinearInterpolator());
        animatorBackward.setInterpolator(new LinearInterpolator());
        animatorForward.setDuration(ANIMATE_FLIP_DURATION);
        animatorForward.setStartDelay(ANIMATE_START_DELAY);
        animatorBackward.setDuration(ANIMATE_FLIP_DURATION);

        return new Animator[]{animatorForward, animatorBackward};
    }
    public Animator[] getWaveAnimator(int index){
        ValueAnimator animatorForward = ValueAnimator.ofFloat(0, 1);
        animatorForward.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                final Drawable firstDrawable, secondDrawable;
                firstDrawable = mTodayEventImages.get(index);
                if(index == mTodayEventImages.size() - 1){
                    secondDrawable = mTodayEventImages.get(0);
                }
                else {
                    secondDrawable = mTodayEventImages.get(index + 1);
                }

                if(mFirstView != null)
                    mFirstView.setImageDrawable(firstDrawable);
                if(mSecondView != null)
                    mSecondView.setImageDrawable(secondDrawable);
            }
        });
        animatorForward.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimating = true;
                mInitialExpandAnimating = false;
                mAnimateImageIndex = index;
                mAnimateDirectionForward = true;
                mAnimateProgress = (float)animation.getAnimatedValue();

                final float posX = mTodayX - mTodayImageSize/2f;
                final float posY =  mTodayY - mTodayImageSize/2f;
                if (mFirstView != null) {
                    mFirstView.setTranslationX(posX);
                    mFirstView.setTranslationY(posY);
                    mFirstView.setPivotX(0);
                    mFirstView.setPivotY(mTodayImageSize / 2f);
                    mFirstView.setScaleX(1 - mAnimateProgress);
                    mFirstView.setScaleY(1 - mAnimateProgress);
                }
                if (mSecondView != null) {
                    mSecondView.setTranslationX(posX);
                    mSecondView.setTranslationY(posY);
                    mSecondView.setPivotX(mTodayImageSize);
                    mSecondView.setPivotY(mTodayImageSize / 2f);
                    mSecondView.setScaleX(mAnimateProgress);
                    mSecondView.setScaleY(mAnimateProgress);
                }
            }
        });
        animatorForward.setInterpolator(new LinearInterpolator());
        animatorForward.setDuration(ANIMATE_DURATION);
        animatorForward.setStartDelay(ANIMATE_START_DELAY);

        return new Animator[]{animatorForward};
    }
    public Animator[] getDepthFieldAnimator(int index){
        final float DEPTH_FIELD_SCALE = 0.4f;
        final float DEPTH_FIELD_ALPHA = 0.0f;

        ValueAnimator animatorForward = ValueAnimator.ofFloat(0, 1);
        animatorForward.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                final Drawable firstDrawable, secondDrawable;
                firstDrawable = mTodayEventImages.get(index);
                if(index == mTodayEventImages.size() - 1){
                    secondDrawable = mTodayEventImages.get(0);
                }
                else {
                    secondDrawable = mTodayEventImages.get(index + 1);
                }

                if(mFirstView != null)
                    mFirstView.setImageDrawable(firstDrawable);
                if(mSecondView != null)
                    mSecondView.setImageDrawable(secondDrawable);
            }
        });
        animatorForward.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimating = true;
                mInitialExpandAnimating = false;
                mAnimateImageIndex = index;
                mAnimateDirectionForward = true;
                mAnimateProgress = (float)animation.getAnimatedValue();

                final float posX = mTodayX - mTodayImageSize/2f;
                final float posY =  mTodayY - mTodayImageSize/2f;
                if (mFirstView != null) {
                    mFirstView.setTranslationX(posX - mTodayImageSize * DEPTH_FIELD_SCALE * mAnimateProgress);
                    mFirstView.setTranslationY(posY);
                    mFirstView.setPivotX(0);
                    mFirstView.setPivotY(mTodayImageSize);
                    mFirstView.setScaleX(1 - mAnimateProgress*(1 - DEPTH_FIELD_SCALE));
                    mFirstView.setScaleY(1 - mAnimateProgress*(1 - DEPTH_FIELD_SCALE));
                    mFirstView.setAlpha(1 - mAnimateProgress*(1 - DEPTH_FIELD_ALPHA));
                }
                if (mSecondView != null) {
                    mSecondView.setTranslationX(posX + mTodayImageSize * DEPTH_FIELD_SCALE * (1 - mAnimateProgress));
                    mSecondView.setTranslationY(posY);
                    mSecondView.setPivotX(mTodayImageSize);
                    mSecondView.setPivotY(mTodayImageSize);
                    mSecondView.setScaleX(DEPTH_FIELD_SCALE + (1 - DEPTH_FIELD_SCALE)*mAnimateProgress);
                    mSecondView.setScaleY(DEPTH_FIELD_SCALE + (1 - DEPTH_FIELD_SCALE)*mAnimateProgress);
                    mSecondView.setAlpha(DEPTH_FIELD_ALPHA + (1 - DEPTH_FIELD_ALPHA)*mAnimateProgress);
                }
            }
        });
        animatorForward.setInterpolator(new LinearInterpolator());
        animatorForward.setDuration(ANIMATE_DURATION);
        animatorForward.setStartDelay(ANIMATE_START_DELAY);

        return new Animator[]{animatorForward};
    }

    /**
     * 오늘의 일정정보들을 얻기
     */
    public void getTodayEventData(){
        getItemWidth();
        mTodayImageSize = mRadius * 2 + dipToPx(getContext(), 3);

        mTodayEventImages.clear();
        List<EventManager.OneEvent> eventList = EventManager.getEvents(getContext(), DateTime.now().getMillis(), EventManager.DAY);
        for (EventManager.OneEvent event : eventList) {
            EventTypeManager.OneEventType eventType = EventTypeManager.getEventTypeFromId(event.type);

            final Drawable drawable;
            drawable = ContextCompat.getDrawable(getContext(), eventType.imageResource).mutate();
            Objects.requireNonNull(drawable).setTint(getResources().getColor(eventType.color, null));
            drawable.setBounds(0, 0, mTodayImageSize, mTodayImageSize);
            mTodayEventImages.add(drawable);
        }

        int d = 0;
        for (int i = 0; i < mLineCount; i++) {
            for (int j = 0; j < 7; j++) {
                Calendar calendar = mItems.get(d ++);
                if(calendar.isCurrentMonth() && calendar.isCurrentDay()) {
                    mTodayX = j * mItemWidth + mDelegate.getCalendarPadding() + mItemWidth/2f;
                    mTodayY = i * mItemHeight + mItemHeight/2f;
                    if(mCalendarType == CALENDAR_TYPE1) {
                        mTodayY += mDelegate.getWeekBarHeight();
                    }
                }
            }
        }
    }

    /**
     * 그리기
     * @param canvas 그리기객체
     */
    @Override
    public void dispatchDraw(Canvas canvas){
        getItemWidth();
        DateTime dateTime = DateTime.now();
        boolean isCurrentMonth = dateTime.getYear() == mYear && dateTime.getMonthOfYear() == mMonth;

        //이번달이면 오늘의 일정정보들을 얻는다.
        if(isCurrentMonth)
            getTodayEventData();
        canvas.drawColor(Utils.getCommonBackgroundColor(getContext()));
        super.dispatchDraw(canvas);

        if(isCurrentMonth && mAnimating)
            drawTodayEvents(canvas);
    }

    /**
     * 오늘일정화상 그리기
     * @param canvas
     */
    public void drawTodayEvents(Canvas canvas){
        if(mTodayEventImages.size() == 0)
            return;

        //초기 첫 일정화상이 확대될때
        if(mInitialExpandAnimating){
            final Drawable firstDrawable = mTodayEventImages.get(mAnimateImageIndex);
            final float posX = mTodayX - mTodayImageSize/2f;
            final float posY =  mTodayY - mTodayImageSize/2f;

            Path path = new Path();
            path.addCircle(mTodayX, mTodayY, mTodayImageSize * mAnimateProgress, Path.Direction.CCW);

            //원구역만큼 잘라서 첫 일정화상을 그려준다.
            canvas.save();
            canvas.clipPath(path);
            drawDrawable(canvas, firstDrawable, posX, posY);
            canvas.restore();
        }
    }

    /**
     * dp를 pixel로 변환
     * @param context context
     * @param dpValue dp
     * @return px
     */
    private static int dipToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * (x,y)좌표에 drawable을 그려주기
     * @param canvas 그리기객체
     * @param drawable 그리려는 Drawable
     * @param x 수평좌표
     * @param y 수직좌표
     */
    private static void drawDrawable(Canvas canvas, Drawable drawable, float x, float y) {
        canvas.save();
        canvas.translate(x, y);
        drawable.draw(canvas);
        canvas.restore();
    }
}
