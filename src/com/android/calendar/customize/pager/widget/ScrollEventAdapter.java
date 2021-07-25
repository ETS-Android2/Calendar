//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.calendar.customize.pager.widget;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import com.android.calendar.customize.pager.widget.CustomViewPager2.OnPageChangeCallback;

import java.util.Locale;

final class ScrollEventAdapter extends OnScrollListener {
    private static final int STATE_IDLE = 0;
    private static final int STATE_IN_PROGRESS_MANUAL_DRAG = 1;
    private static final int STATE_IN_PROGRESS_SMOOTH_SCROLL = 2;
    private static final int STATE_IN_PROGRESS_IMMEDIATE_SCROLL = 3;
    private static final int STATE_IN_PROGRESS_FAKE_DRAG = 4;
    private static final int NO_POSITION = -1;
    private OnPageChangeCallback mCallback;
    @NonNull
    private final CustomViewPager2 mViewPager;
    @NonNull
    private final RecyclerView mRecyclerView;
    @NonNull
    private final LinearLayoutManager mLayoutManager;
    private int mAdapterState;
    private int mScrollState;
    private ScrollEventAdapter.ScrollEventValues mScrollValues;
    private int mDragStartPosition;
    private int mTarget;
    private boolean mDispatchSelected;
    private boolean mScrollHappened;
    private boolean mDataSetChangeHappened;
    private boolean mFakeDragging;

    ScrollEventAdapter(@NonNull CustomViewPager2 viewPager) {
        this.mViewPager = viewPager;
        this.mRecyclerView = this.mViewPager.mRecyclerView;
        this.mLayoutManager = (LinearLayoutManager)this.mRecyclerView.getLayoutManager();
        this.mScrollValues = new ScrollEventAdapter.ScrollEventValues();
        this.resetState();
    }

    private void resetState() {
        this.mAdapterState = 0;
        this.mScrollState = 0;
        this.mScrollValues.reset();
        this.mDragStartPosition = -1;
        this.mTarget = -1;
        this.mDispatchSelected = false;
        this.mScrollHappened = false;
        this.mFakeDragging = false;
        this.mDataSetChangeHappened = false;
    }

    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if ((this.mAdapterState != 1 || this.mScrollState != 1) && newState == 1) {
            this.startDrag(false);
        } else if (this.isInAnyDraggingState() && newState == 2) {
            if (this.mScrollHappened) {
                this.dispatchStateChanged(2);
                this.mDispatchSelected = true;
            }

        } else {
            if (this.isInAnyDraggingState() && newState == 0) {
                boolean dispatchIdle = false;
                this.updateScrollEventValues();
                if (!this.mScrollHappened) {
                    if (this.mScrollValues.mPosition != -1) {
                        this.dispatchScrolled(this.mScrollValues.mPosition, 0.0F, 0);
                    }

                    dispatchIdle = true;
                } else if (this.mScrollValues.mOffsetPx == 0) {
                    dispatchIdle = true;
                    if (this.mDragStartPosition != this.mScrollValues.mPosition) {
                        this.dispatchSelected(this.mScrollValues.mPosition);
                    }
                }

                if (dispatchIdle) {
                    this.dispatchStateChanged(0);
                    this.resetState();
                }
            }

            if (this.mAdapterState == 2 && newState == 0 && this.mDataSetChangeHappened) {
                this.updateScrollEventValues();
                if (this.mScrollValues.mOffsetPx == 0) {
                    if (this.mTarget != this.mScrollValues.mPosition) {
                        this.dispatchSelected(this.mScrollValues.mPosition == -1 ? 0 : this.mScrollValues.mPosition);
                    }

                    this.dispatchStateChanged(0);
                    this.resetState();
                }
            }

        }
    }

    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        this.mScrollHappened = true;
        this.updateScrollEventValues();
        if (this.mDispatchSelected) {
            this.mDispatchSelected = false;
            boolean scrollingForward = dy > 0 || dy == 0 && dx < 0 == this.mViewPager.isRtl();
            this.mTarget = scrollingForward && this.mScrollValues.mOffsetPx != 0 ? this.mScrollValues.mPosition + 1 : this.mScrollValues.mPosition;
            if (this.mDragStartPosition != this.mTarget) {
                this.dispatchSelected(this.mTarget);
            }
        } else if (this.mAdapterState == 0) {
            int position = this.mScrollValues.mPosition;
            this.dispatchSelected(position == -1 ? 0 : position);
        }

        this.dispatchScrolled(this.mScrollValues.mPosition == -1 ? 0 : this.mScrollValues.mPosition, this.mScrollValues.mOffset, this.mScrollValues.mOffsetPx);
        if ((this.mScrollValues.mPosition == this.mTarget || this.mTarget == -1) && this.mScrollValues.mOffsetPx == 0 && this.mScrollState != 1) {
            this.dispatchStateChanged(0);
            this.resetState();
        }

    }

    private void updateScrollEventValues() {
        ScrollEventAdapter.ScrollEventValues values = this.mScrollValues;
        values.mPosition = this.mLayoutManager.findFirstVisibleItemPosition();
        if (values.mPosition == -1) {
            values.reset();
        } else {
            View firstVisibleView = this.mLayoutManager.findViewByPosition(values.mPosition);
            if (firstVisibleView == null) {
                values.reset();
            } else {
                int leftDecorations = this.mLayoutManager.getLeftDecorationWidth(firstVisibleView);
                int rightDecorations = this.mLayoutManager.getRightDecorationWidth(firstVisibleView);
                int topDecorations = this.mLayoutManager.getTopDecorationHeight(firstVisibleView);
                int bottomDecorations = this.mLayoutManager.getBottomDecorationHeight(firstVisibleView);
                LayoutParams params = firstVisibleView.getLayoutParams();
                if (params instanceof MarginLayoutParams) {
                    MarginLayoutParams margin = (MarginLayoutParams)params;
                    leftDecorations += margin.leftMargin;
                    rightDecorations += margin.rightMargin;
                    topDecorations += margin.topMargin;
                    bottomDecorations += margin.bottomMargin;
                }

                int decoratedHeight = firstVisibleView.getHeight() + topDecorations + bottomDecorations;
                int decoratedWidth = firstVisibleView.getWidth() + leftDecorations + rightDecorations;
                boolean isHorizontal = this.mLayoutManager.getOrientation() == 0;
                int start;
                int sizePx;
                if (isHorizontal) {
                    sizePx = decoratedWidth;
                    start = firstVisibleView.getLeft() - leftDecorations - this.mRecyclerView.getPaddingLeft();
                    if (this.mViewPager.isRtl()) {
                        start = -start;
                    }
                } else {
                    sizePx = decoratedHeight;
                    start = firstVisibleView.getTop() - topDecorations - this.mRecyclerView.getPaddingTop();
                }

                values.mOffsetPx = -start;
                if (values.mOffsetPx < 0) {
                    if ((new AnimateLayoutChangeDetector(this.mLayoutManager)).mayHaveInterferingAnimations()) {
                        throw new IllegalStateException("Page(s) contain a ViewGroup with a LayoutTransition (or animateLayoutChanges=\"true\"), which interferes with the scrolling animation. Make sure to call getLayoutTransition().setAnimateParentHierarchy(false) on all ViewGroups with a LayoutTransition before an animation is started.");
                    } else {
                        throw new IllegalStateException(String.format(Locale.US, "Page can only be offset by a positive amount, not by %d", values.mOffsetPx));
                    }
                } else {
                    values.mOffset = sizePx == 0 ? 0.0F : (float)values.mOffsetPx / (float)sizePx;
                }
            }
        }
    }

    private void startDrag(boolean isFakeDrag) {
        this.mFakeDragging = isFakeDrag;
        this.mAdapterState = isFakeDrag ? 4 : 1;
        if (this.mTarget != -1) {
            this.mDragStartPosition = this.mTarget;
            this.mTarget = -1;
        } else if (this.mDragStartPosition == -1) {
            this.mDragStartPosition = this.getPosition();
        }

        this.dispatchStateChanged(1);
    }

    void notifyDataSetChangeHappened() {
        this.mDataSetChangeHappened = true;
    }

    void notifyProgrammaticScroll(int target, boolean smooth) {
        this.mAdapterState = smooth ? 2 : 3;
        this.mFakeDragging = false;
        boolean hasNewTarget = this.mTarget != target;
        this.mTarget = target;
        this.dispatchStateChanged(2);
        if (hasNewTarget) {
            this.dispatchSelected(target);
        }

    }

    void notifyBeginFakeDrag() {
        this.mAdapterState = 4;
        this.startDrag(true);
    }

    void notifyEndFakeDrag() {
        if (!this.isDragging() || this.mFakeDragging) {
            this.mFakeDragging = false;
            this.updateScrollEventValues();
            if (this.mScrollValues.mOffsetPx == 0) {
                if (this.mScrollValues.mPosition != this.mDragStartPosition) {
                    this.dispatchSelected(this.mScrollValues.mPosition);
                }

                this.dispatchStateChanged(0);
                this.resetState();
            } else {
                this.dispatchStateChanged(2);
            }

        }
    }

    void setOnPageChangeCallback(OnPageChangeCallback callback) {
        this.mCallback = callback;
    }

    int getScrollState() {
        return this.mScrollState;
    }

    boolean isIdle() {
        return this.mScrollState == 0;
    }

    boolean isDragging() {
        return this.mScrollState == 1;
    }

    boolean isFakeDragging() {
        return this.mFakeDragging;
    }

    private boolean isInAnyDraggingState() {
        return this.mAdapterState == 1 || this.mAdapterState == 4;
    }

    double getRelativeScrollPosition() {
        this.updateScrollEventValues();
        return (double)this.mScrollValues.mPosition + (double)this.mScrollValues.mOffset;
    }

    private void dispatchStateChanged(int state) {
        if (this.mAdapterState != 3 || this.mScrollState != 0) {
            if (this.mScrollState != state) {
                this.mScrollState = state;
                if (this.mCallback != null) {
                    this.mCallback.onPageScrollStateChanged(state);
                }

            }
        }
    }

    private void dispatchSelected(int target) {
        if (this.mCallback != null) {
            this.mCallback.onPageSelected(target);
        }

    }

    private void dispatchScrolled(int position, float offset, int offsetPx) {
        if (this.mCallback != null) {
            this.mCallback.onPageScrolled(position, offset, offsetPx);
        }

    }

    private int getPosition() {
        return this.mLayoutManager.findFirstVisibleItemPosition();
    }

    private static final class ScrollEventValues {
        int mPosition;
        float mOffset;
        int mOffsetPx;

        ScrollEventValues() {
        }

        void reset() {
            this.mPosition = -1;
            this.mOffset = 0.0F;
            this.mOffsetPx = 0;
        }
    }
}
