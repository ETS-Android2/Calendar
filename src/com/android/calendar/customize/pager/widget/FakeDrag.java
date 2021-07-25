//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.calendar.customize.pager.widget;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

final class FakeDrag {
    private final CustomViewPager2 mViewPager;
    private final ScrollEventAdapter mScrollEventAdapter;
    private final RecyclerView mRecyclerView;
    private VelocityTracker mVelocityTracker;
    private int mMaximumVelocity;
    private float mRequestedDragDistance;
    private int mActualDraggedDistance;
    private long mFakeDragBeginTime;

    FakeDrag(CustomViewPager2 viewPager, ScrollEventAdapter scrollEventAdapter, RecyclerView recyclerView) {
        this.mViewPager = viewPager;
        this.mScrollEventAdapter = scrollEventAdapter;
        this.mRecyclerView = recyclerView;
    }

    boolean isFakeDragging() {
        return this.mScrollEventAdapter.isFakeDragging();
    }

    @UiThread
    boolean beginFakeDrag() {
        if (this.mScrollEventAdapter.isDragging()) {
            return false;
        } else {
            this.mRequestedDragDistance = (float)(this.mActualDraggedDistance = 0);
            this.mFakeDragBeginTime = SystemClock.uptimeMillis();
            this.beginFakeVelocityTracker();
            this.mScrollEventAdapter.notifyBeginFakeDrag();
            if (!this.mScrollEventAdapter.isIdle()) {
                this.mRecyclerView.stopScroll();
            }

            this.addFakeMotionEvent(this.mFakeDragBeginTime, 0, 0.0F, 0.0F);
            return true;
        }
    }

    @UiThread
    boolean fakeDragBy(float offsetPxFloat) {
        if (!this.mScrollEventAdapter.isFakeDragging()) {
            return false;
        } else {
            this.mRequestedDragDistance -= offsetPxFloat;
            int offsetPx = Math.round(this.mRequestedDragDistance - (float)this.mActualDraggedDistance);
            this.mActualDraggedDistance += offsetPx;
            long time = SystemClock.uptimeMillis();
            boolean isHorizontal = this.mViewPager.getOrientation() == 0;
            int offsetX = isHorizontal ? offsetPx : 0;
            int offsetY = isHorizontal ? 0 : offsetPx;
            float x = isHorizontal ? this.mRequestedDragDistance : 0.0F;
            float y = isHorizontal ? 0.0F : this.mRequestedDragDistance;
            this.mRecyclerView.scrollBy(offsetX, offsetY);
            this.addFakeMotionEvent(time, 2, x, y);
            return true;
        }
    }

    @UiThread
    boolean endFakeDrag() {
        if (!this.mScrollEventAdapter.isFakeDragging()) {
            return false;
        } else {
            this.mScrollEventAdapter.notifyEndFakeDrag();
            boolean pixelsPerSecond = true;
            VelocityTracker velocityTracker = this.mVelocityTracker;
            velocityTracker.computeCurrentVelocity(1000, (float)this.mMaximumVelocity);
            int xVelocity = (int)velocityTracker.getXVelocity();
            int yVelocity = (int)velocityTracker.getYVelocity();
            if (!this.mRecyclerView.fling(xVelocity, yVelocity)) {
                this.mViewPager.snapToPage();
            }

            return true;
        }
    }

    private void beginFakeVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
            ViewConfiguration configuration = ViewConfiguration.get(this.mViewPager.getContext());
            this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        } else {
            this.mVelocityTracker.clear();
        }

    }

    private void addFakeMotionEvent(long time, int action, float x, float y) {
        MotionEvent ev = MotionEvent.obtain(this.mFakeDragBeginTime, time, action, x, y, 0);
        this.mVelocityTracker.addMovement(ev);
        ev.recycle();
    }
}
