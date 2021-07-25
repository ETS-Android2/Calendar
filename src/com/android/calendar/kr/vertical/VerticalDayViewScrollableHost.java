package com.android.calendar.kr.vertical;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

/**
 * 양식 4
 * 자식 ViewPager의 Touch사건을 감지하면서 touch사건을 자체로 처리해주는 Layout
 */
public class VerticalDayViewScrollableHost extends FrameLayout {
    private static final float MOVE_PROPORTION = 1.0f;

    //자식 View들
    ViewPager2 mDayViewPager;
    CustomRecyclerView mDayRecyclerView;

    //Fling을 감지하기 위한 Gesture detector
    GestureDetector mGestureDetector;

    float mDayTop, mDayBottom, mDayStart, mDayEnd;
    boolean mDownRecyclerView = false;
    boolean mScrolledHorizontal = false;
    float mInitialX, mInitialY, mMoveY;
    private boolean mMoveStarted = false;
    VerticalWeekBar.ExpandWeekBarListener mListener;

    public VerticalDayViewScrollableHost(@NonNull Context context) {
        this(context, null);
    }

    public VerticalDayViewScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalDayViewScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //Day RecyclerView 의 위치, 크기를 얻는다.
        Resources resources = getResources();
        mDayTop = resources.getDimension(R.dimen.action_bar_height)
                + resources.getDimension(R.dimen.vertical_calendar_padding_top)
                + resources.getDimension(R.dimen.vertical_day_view_label_height);
        mDayBottom = Utils.getDisplayDimensions(context).y
                - resources.getDimension(R.dimen.bottom_bar_height);
        mDayStart = resources.getDimension(R.dimen.vertical_calendar_padding_start);
        mDayEnd = resources.getDisplayMetrics().widthPixels
                - resources.getDimension(R.dimen.vertical_calendar_padding_end);

        mGestureDetector = new GestureDetector(context, new CustomGestureListener());
    }

    public void setWeekBarExpandListener(VerticalWeekBar.ExpandWeekBarListener listener) {
        mListener = listener;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mDayViewPager = findViewById(R.id.day_view_pager);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        float x = ev.getX();
        float y = ev.getY();

        mGestureDetector.onTouchEvent(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if(mDayViewPager.getScrollState() == ViewPager2.SCROLL_STATE_SETTLING)
                    mDownRecyclerView = false;
                else
                    mDownRecyclerView = x >= mDayStart && x <= mDayEnd && y >= mDayTop && y <= mDayBottom;

                mInitialX = x;
                mInitialY = y;
                mMoveY = y;
                mMoveStarted = true;

                //day recyclerview를 얻는다.
                if(mDownRecyclerView) {
                    int page = mDayViewPager.getCurrentItem();
                    View view = mDayViewPager.findViewWithTag(page);
                    if(view instanceof VerticalDayViewContainer) {
                        mDayRecyclerView = (CustomRecyclerView) ((VerticalDayViewContainer)view).getEventListView();
                    }
                    else
                        mDayRecyclerView = null;
                }
                else
                    mDayRecyclerView = null;

                mScrolledHorizontal = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if(mScrolledHorizontal)
                    break;

                if(mMoveStarted) {
                    float diffX = Math.abs(x - mInitialX);
                    float diffY = Math.abs(y - mInitialY);
                    if (diffX > diffY * MOVE_PROPORTION) {
                        mDayViewPager.setUserInputEnabled(false);
                        if (mDayRecyclerView != null)
                            mDayRecyclerView.enableTouchEvent(false);

                        mScrolledHorizontal = true;
                        break;
                    } else {
                        mDayViewPager.setUserInputEnabled(true);
                        if (mDayRecyclerView != null)
                            mDayRecyclerView.enableTouchEvent(true);

                        mScrolledHorizontal = false;
                    }

                    mMoveStarted = false;
                }

                if(mDownRecyclerView && mDayRecyclerView != null) {
                    float dy = y - mMoveY;
                    if(dy == 0)
                        break;

                    boolean canScrollUp = mDayRecyclerView.canScrollVertically(1);
                    boolean canScrollDown = mDayRecyclerView.canScrollVertically(-1);
                    boolean moveDown = dy > 0;

                    if((moveDown && !canScrollDown) || (!moveDown && !canScrollUp)){
                        mDownRecyclerView = false;

                        mDayRecyclerView.enableTouchEvent(false);
                        mDayViewPager.setUserInputEnabled(true);

                        @SuppressLint("Recycle") MotionEvent motionEvent = MotionEvent.obtain(ev);
                        motionEvent.setAction(MotionEvent.ACTION_DOWN);
                        mDayViewPager.dispatchTouchEvent(motionEvent);
                    }
                    else {
                        mDayRecyclerView.enableTouchEvent(true);
                        mDayViewPager.setUserInputEnabled(false);
                    }
                    mMoveY = y;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(mDayRecyclerView != null)
                    mDayRecyclerView.enableTouchEvent(true);

                mDayViewPager.setUserInputEnabled(true);
                mDownRecyclerView = false;
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    public class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 100;
        private static final int SWIPE_THRESHOLD_VELOCITY = 1000;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Fling을 감지한다
            float xDiff = e2.getX() - e1.getX();
            float yDiff = e2.getY() - e1.getY();
            if(xDiff > SWIPE_MIN_DISTANCE && Math.abs(xDiff) > Math.abs(yDiff) && mScrolledHorizontal &&
                    Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                if(mListener != null)
                    mListener.onFlingOrClick();
            }
            return false;
        }
    }
}
