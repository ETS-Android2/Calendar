package com.android.calendar.kr.vertical;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 양식 4
 * Touch사건시 touch사건동작을 enable/disable 시켜주는 RecyclerView.
 */
public class CustomRecyclerView extends RecyclerView {
    private boolean mEnableTouch = true;

    public CustomRecyclerView(@NonNull Context context) {
        super(context);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if(mEnableTouch)
            return super.onTouchEvent(e);
        return true;
    }

    public void enableTouchEvent(boolean enable) {
        mEnableTouch = enable;
    }
}
