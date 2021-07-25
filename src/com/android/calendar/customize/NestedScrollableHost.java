package com.android.calendar.customize;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

/**
 * 끌기방향과 Viewpager2의 페지방향이 다를때 scroll을 막아주기 위한 layout이다.
 * xml에서 Viewpager2를 NestedScrollableHost Tag로 감싸준다.
 * 이때 Viewpager2은 NestedScrollableHost의 유일한 자식이여야 한다.
 */
public class NestedScrollableHost extends FrameLayout {
    private static final float MOVE_PROPORTION = 1.0f;
    private float mInitialX = 0.0f;
    private float mInitialY = 0.0f;
    private boolean mMoveStarted = false;

    private View child() { return (this.getChildCount() > 0 ? this.getChildAt(0) : null); }

    private void init() {
    }

    public NestedScrollableHost(@NonNull Context context) {
        super(context);
        this.init();
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //Touch사건을 감지한다.
        float x = ev.getX();
        float y = ev.getY();

        if(!(child() instanceof ViewPager2))
            return super.onInterceptTouchEvent(ev);

        ViewPager2 child = (ViewPager2) child();
        switch (ev.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                mInitialX = x;
                mInitialY = y;
                mMoveStarted = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if(!mMoveStarted)
                    break;

                float dx = Math.abs(x - mInitialX);
                float dy = Math.abs(y - mInitialY);

                if(dx >= 0 && dx <= 1 && dy >= 0 && dy <= 1)
                    break;

                mMoveStarted = false;

                assert child != null;

                //끌기방향과 페지방향이 다를때에는 scroll을 막는다.
                if(child.getOrientation() == ViewPager2.ORIENTATION_HORIZONTAL) {
                    child.setUserInputEnabled(!(dy > dx * MOVE_PROPORTION));
                }
                else {
                    child.setUserInputEnabled(!(dx > dy * MOVE_PROPORTION));
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                assert child != null;
                child.setUserInputEnabled(true);
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }
}