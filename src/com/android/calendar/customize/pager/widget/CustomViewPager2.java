//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.calendar.customize.pager.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener;
import androidx.recyclerview.widget.RecyclerView.Recycler;
import androidx.recyclerview.widget.RecyclerView.State;
import androidx.viewpager2.R.styleable;
import androidx.viewpager2.adapter.StatefulAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

/**
 * Scroll속도를 늦추어준 ViewPager
 * 모든 동작들은 ViewPager2과 꼭 같다.
 * @see androidx.viewpager2.widget.ViewPager2
 */
public final class CustomViewPager2 extends ViewGroup {
    public static final int ORIENTATION_HORIZONTAL = 0;
    public static final int ORIENTATION_VERTICAL = 1;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;
    public static final int OFFSCREEN_PAGE_LIMIT_DEFAULT = -1;
    static boolean sFeatureEnhancedA11yEnabled = true;
    private final Rect mTmpContainerRect = new Rect();
    private final Rect mTmpChildRect = new Rect();
    private CompositeOnPageChangeCallback mExternalPageChangeCallbacks = new CompositeOnPageChangeCallback(3);
    int mCurrentItem;
    boolean mCurrentItemDirty = false;
    private AdapterDataObserver mCurrentItemDataSetChangeObserver;
    private LinearLayoutManager mLayoutManager;
    private int mPendingCurrentItem;
    private Parcelable mPendingAdapterState;
    RecyclerView mRecyclerView;
    private PagerSnapHelper mPagerSnapHelper;
    ScrollEventAdapter mScrollEventAdapter;
    private CompositeOnPageChangeCallback mPageChangeEventDispatcher;
    private FakeDrag mFakeDragger;
    private PageTransformerAdapter mPageTransformerAdapter;
    private ItemAnimator mSavedItemAnimator;
    private boolean mSavedItemAnimatorPresent;
    private boolean mUserInputEnabled;
    private int mOffscreenPageLimit;
    CustomViewPager2.AccessibilityProvider mAccessibilityProvider;

    public CustomViewPager2(@NonNull Context context) {
        super(context);
        this.mCurrentItemDataSetChangeObserver = new NamelessClass_1();
        this.mPendingCurrentItem = -1;
        this.mSavedItemAnimator = null;
        this.mSavedItemAnimatorPresent = false;
        this.mUserInputEnabled = true;
        this.mOffscreenPageLimit = -1;
        this.initialize(context, (AttributeSet)null);
    }

    public CustomViewPager2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mCurrentItemDataSetChangeObserver = new NamelessClass_1();
        this.mPendingCurrentItem = -1;
        this.mSavedItemAnimator = null;
        this.mSavedItemAnimatorPresent = false;
        this.mUserInputEnabled = true;
        this.mOffscreenPageLimit = -1;
        this.initialize(context, attrs);
    }

    public CustomViewPager2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mCurrentItemDataSetChangeObserver = new NamelessClass_1();
        this.mPendingCurrentItem = -1;
        this.mSavedItemAnimator = null;
        this.mSavedItemAnimatorPresent = false;
        this.mUserInputEnabled = true;
        this.mOffscreenPageLimit = -1;
        this.initialize(context, attrs);
    }

    @RequiresApi(21)
    public CustomViewPager2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.mCurrentItemDataSetChangeObserver = new NamelessClass_1();
        this.mPendingCurrentItem = -1;
        this.mSavedItemAnimator = null;
        this.mSavedItemAnimatorPresent = false;
        this.mUserInputEnabled = true;
        this.mOffscreenPageLimit = -1;
        this.initialize(context, attrs);
    }

    class NamelessClass_1 extends CustomViewPager2.DataSetChangeObserver {
        NamelessClass_1() {
            super(/*(NamelessClass_1)null*/);
        }

        public void onChanged() {
            CustomViewPager2.this.mCurrentItemDirty = true;
            CustomViewPager2.this.mScrollEventAdapter.notifyDataSetChangeHappened();
        }
    }

    private void initialize(Context context, AttributeSet attrs) {
        this.mAccessibilityProvider = (CustomViewPager2.AccessibilityProvider)(sFeatureEnhancedA11yEnabled ? new CustomViewPager2.PageAwareAccessibilityProvider() : new CustomViewPager2.BasicAccessibilityProvider());
        this.mRecyclerView = new CustomViewPager2.RecyclerViewImpl(context);
        this.mRecyclerView.setId(ViewCompat.generateViewId());
        this.mRecyclerView.setDescendantFocusability(131072);
        this.mLayoutManager = new CustomViewPager2.LinearLayoutManagerImpl(context);
        this.mRecyclerView.setLayoutManager(this.mLayoutManager);
        this.mRecyclerView.setScrollingTouchSlop(1);

        try {
            Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
            touchSlopField.setAccessible(true);

            int touchSlop = (int) touchSlopField.get(this.mRecyclerView);
            touchSlopField.set(this.mRecyclerView, touchSlop * 5);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        this.setOrientation(context, attrs);
        this.mRecyclerView.setLayoutParams(new LayoutParams(-1, -1));
        this.mRecyclerView.addOnChildAttachStateChangeListener(this.enforceChildFillListener());
        this.mScrollEventAdapter = new ScrollEventAdapter(this);
        this.mFakeDragger = new FakeDrag(this, this.mScrollEventAdapter, this.mRecyclerView);
        this.mPagerSnapHelper = new CustomViewPager2.PagerSnapHelperImpl();
        this.mPagerSnapHelper.attachToRecyclerView(this.mRecyclerView);
        this.mRecyclerView.addOnScrollListener(this.mScrollEventAdapter);
        this.mPageChangeEventDispatcher = new CompositeOnPageChangeCallback(3);
        this.mScrollEventAdapter.setOnPageChangeCallback(this.mPageChangeEventDispatcher);
        CustomViewPager2.OnPageChangeCallback currentItemUpdater = new CustomViewPager2.OnPageChangeCallback() {
            public void onPageSelected(int position) {
                if (CustomViewPager2.this.mCurrentItem != position) {
                    CustomViewPager2.this.mCurrentItem = position;
                    CustomViewPager2.this.mAccessibilityProvider.onSetNewCurrentItem();
                }

            }

            public void onPageScrollStateChanged(int newState) {
                if (newState == 0) {
                    CustomViewPager2.this.updateCurrentItem();
                }

            }
        };
        CustomViewPager2.OnPageChangeCallback focusClearer = new CustomViewPager2.OnPageChangeCallback() {
            public void onPageSelected(int position) {
                CustomViewPager2.this.clearFocus();
                if (CustomViewPager2.this.hasFocus()) {
                    CustomViewPager2.this.mRecyclerView.requestFocus(2);
                }

            }
        };
        this.mPageChangeEventDispatcher.addOnPageChangeCallback(currentItemUpdater);
        this.mPageChangeEventDispatcher.addOnPageChangeCallback(focusClearer);
        this.mAccessibilityProvider.onInitialize(this.mPageChangeEventDispatcher, this.mRecyclerView);
        this.mPageChangeEventDispatcher.addOnPageChangeCallback(this.mExternalPageChangeCallbacks);
        this.mPageTransformerAdapter = new PageTransformerAdapter(this.mLayoutManager);
        this.mPageChangeEventDispatcher.addOnPageChangeCallback(this.mPageTransformerAdapter);
        this.attachViewToParent(this.mRecyclerView, 0, this.mRecyclerView.getLayoutParams());
    }

    private OnChildAttachStateChangeListener enforceChildFillListener() {
        return new OnChildAttachStateChangeListener() {
            public void onChildViewAttachedToWindow(@NonNull View view) {
                androidx.recyclerview.widget.RecyclerView.LayoutParams layoutParams = (androidx.recyclerview.widget.RecyclerView.LayoutParams)view.getLayoutParams();
                if (layoutParams.width != -1 || layoutParams.height != -1) {
                    throw new IllegalStateException("Pages must fill the whole ViewPager2 (use match_parent)");
                }
            }

            public void onChildViewDetachedFromWindow(@NonNull View view) {
            }
        };
    }

    @RequiresApi(23)
    public CharSequence getAccessibilityClassName() {
        return (CharSequence)(this.mAccessibilityProvider.handlesGetAccessibilityClassName() ? this.mAccessibilityProvider.onGetAccessibilityClassName() : super.getAccessibilityClassName());
    }

    private void setOrientation(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, styleable.ViewPager2);
//        if (VERSION.SDK_INT >= 29) {
//            this.saveAttributeDataForStyleable(context, styleable.ViewPager2, attrs, a, 0, 0);
//        }

        try {
            this.setOrientation(a.getInt(styleable.ViewPager2_android_orientation, 0));
        } finally {
            a.recycle();
        }

    }

    @Nullable
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        CustomViewPager2.SavedState ss = new CustomViewPager2.SavedState(superState);
        ss.mRecyclerViewId = this.mRecyclerView.getId();
        ss.mCurrentItem = this.mPendingCurrentItem == -1 ? this.mCurrentItem : this.mPendingCurrentItem;
        if (this.mPendingAdapterState != null) {
            ss.mAdapterState = this.mPendingAdapterState;
        } else {
            Adapter<?> adapter = this.mRecyclerView.getAdapter();
            if (adapter instanceof StatefulAdapter) {
                ss.mAdapterState = ((StatefulAdapter)adapter).saveState();
            }
        }

        return ss;
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof CustomViewPager2.SavedState)) {
            super.onRestoreInstanceState(state);
        } else {
            CustomViewPager2.SavedState ss = (CustomViewPager2.SavedState)state;
            super.onRestoreInstanceState(ss.getSuperState());
            this.mPendingCurrentItem = ss.mCurrentItem;
            this.mPendingAdapterState = ss.mAdapterState;
        }
    }

    private void restorePendingState() {
        if (this.mPendingCurrentItem != -1) {
            Adapter<?> adapter = this.getAdapter();
            if (adapter != null) {
                if (this.mPendingAdapterState != null) {
                    if (adapter instanceof StatefulAdapter) {
                        ((StatefulAdapter)adapter).restoreState(this.mPendingAdapterState);
                    }

                    this.mPendingAdapterState = null;
                }

                this.mCurrentItem = Math.max(0, Math.min(this.mPendingCurrentItem, adapter.getItemCount() - 1));
                this.mPendingCurrentItem = -1;
                this.mRecyclerView.scrollToPosition(this.mCurrentItem);
                this.mAccessibilityProvider.onRestorePendingState();
            }
        }
    }

    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        Parcelable state = (Parcelable)container.get(this.getId());
        if (state instanceof CustomViewPager2.SavedState) {
            int previousRvId = ((CustomViewPager2.SavedState)state).mRecyclerViewId;
            int currentRvId = this.mRecyclerView.getId();
            container.put(currentRvId, container.get(previousRvId));
            container.remove(previousRvId);
        }

        super.dispatchRestoreInstanceState(container);
        this.restorePendingState();
    }

    public void setAdapter(@Nullable Adapter adapter) {
        Adapter<?> currentAdapter = this.mRecyclerView.getAdapter();
        this.mAccessibilityProvider.onDetachAdapter(currentAdapter);
        this.unregisterCurrentItemDataSetTracker(currentAdapter);
        this.mRecyclerView.setAdapter(adapter);
        this.mCurrentItem = 0;
        this.restorePendingState();
        this.mAccessibilityProvider.onAttachAdapter(adapter);
        this.registerCurrentItemDataSetTracker(adapter);
    }

    private void registerCurrentItemDataSetTracker(@Nullable Adapter<?> adapter) {
        if (adapter != null) {
            adapter.registerAdapterDataObserver(this.mCurrentItemDataSetChangeObserver);
        }

    }

    private void unregisterCurrentItemDataSetTracker(@Nullable Adapter<?> adapter) {
        if (adapter != null) {
            adapter.unregisterAdapterDataObserver(this.mCurrentItemDataSetChangeObserver);
        }

    }

    @Nullable
    public Adapter getAdapter() {
        return this.mRecyclerView.getAdapter();
    }

    public void onViewAdded(View child) {
        throw new IllegalStateException(this.getClass().getSimpleName() + " does not support direct child views");
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.measureChild(this.mRecyclerView, widthMeasureSpec, heightMeasureSpec);
        int width = this.mRecyclerView.getMeasuredWidth();
        int height = this.mRecyclerView.getMeasuredHeight();
        int childState = this.mRecyclerView.getMeasuredState();
        width += this.getPaddingLeft() + this.getPaddingRight();
        height += this.getPaddingTop() + this.getPaddingBottom();
        width = Math.max(width, this.getSuggestedMinimumWidth());
        height = Math.max(height, this.getSuggestedMinimumHeight());
        this.setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, childState), resolveSizeAndState(height, heightMeasureSpec, childState << 16));
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = this.mRecyclerView.getMeasuredWidth();
        int height = this.mRecyclerView.getMeasuredHeight();
        this.mTmpContainerRect.left = this.getPaddingLeft();
        this.mTmpContainerRect.right = r - l - this.getPaddingRight();
        this.mTmpContainerRect.top = this.getPaddingTop();
        this.mTmpContainerRect.bottom = b - t - this.getPaddingBottom();
        Gravity.apply(8388659, width, height, this.mTmpContainerRect, this.mTmpChildRect);
        this.mRecyclerView.layout(this.mTmpChildRect.left, this.mTmpChildRect.top, this.mTmpChildRect.right, this.mTmpChildRect.bottom);
        if (this.mCurrentItemDirty) {
            this.updateCurrentItem();
        }

    }

    void updateCurrentItem() {
        if (this.mPagerSnapHelper == null) {
            throw new IllegalStateException("Design assumption violated.");
        } else {
            View snapView = this.mPagerSnapHelper.findSnapView(this.mLayoutManager);
            if (snapView != null) {
                int snapPosition = this.mLayoutManager.getPosition(snapView);
                if (snapPosition != this.mCurrentItem && this.getScrollState() == 0) {
                    this.mPageChangeEventDispatcher.onPageSelected(snapPosition);
                }

                this.mCurrentItemDirty = false;
            }
        }
    }

    int getPageSize() {
        RecyclerView rv = this.mRecyclerView;
        return this.getOrientation() == 0 ? rv.getWidth() - rv.getPaddingLeft() - rv.getPaddingRight() : rv.getHeight() - rv.getPaddingTop() - rv.getPaddingBottom();
    }

    public void setOrientation(int orientation) {
        this.mLayoutManager.setOrientation(orientation);
        this.mAccessibilityProvider.onSetOrientation();
    }

    public int getOrientation() {
        return this.mLayoutManager.getOrientation();
    }

    boolean isRtl() {
        return this.mLayoutManager.getLayoutDirection() == 1;
    }

    public void setCurrentItem(int item) {
        this.setCurrentItem(item, true);
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        if (this.isFakeDragging()) {
            throw new IllegalStateException("Cannot change current item when ViewPager2 is fake dragging");
        } else {
            this.setCurrentItemInternal(item, smoothScroll);
        }
    }

    void setCurrentItemInternal(int item, boolean smoothScroll) {
        Adapter<?> adapter = this.getAdapter();
        if (adapter == null) {
            if (this.mPendingCurrentItem != -1) {
                this.mPendingCurrentItem = Math.max(item, 0);
            }

        } else if (adapter.getItemCount() > 0) {
            item = Math.max(item, 0);
            item = Math.min(item, adapter.getItemCount() - 1);
            if (item != this.mCurrentItem || !this.mScrollEventAdapter.isIdle()) {
                if (item != this.mCurrentItem || !smoothScroll) {
                    double previousItem = (double)this.mCurrentItem;
                    this.mCurrentItem = item;
                    this.mAccessibilityProvider.onSetNewCurrentItem();
                    if (!this.mScrollEventAdapter.isIdle()) {
                        previousItem = this.mScrollEventAdapter.getRelativeScrollPosition();
                    }

                    this.mScrollEventAdapter.notifyProgrammaticScroll(item, smoothScroll);
                    if (!smoothScroll) {
                        this.mRecyclerView.scrollToPosition(item);
                    } else {
                        if (Math.abs((double)item - previousItem) > 3.0D) {
                            this.mRecyclerView.scrollToPosition((double)item > previousItem ? item - 3 : item + 3);
                            this.mRecyclerView.post(new CustomViewPager2.SmoothScrollToPosition(item, this.mRecyclerView));
                        } else {
                            this.mRecyclerView.smoothScrollToPosition(item);
                        }

                    }
                }
            }
        }
    }

    public int getCurrentItem() {
        return this.mCurrentItem;
    }

    public int getScrollState() {
        return this.mScrollEventAdapter.getScrollState();
    }

    public boolean beginFakeDrag() {
        return this.mFakeDragger.beginFakeDrag();
    }

    public boolean fakeDragBy(@SuppressLint({"SupportAnnotationUsage"}) @Px float offsetPxFloat) {
        return this.mFakeDragger.fakeDragBy(offsetPxFloat);
    }

    public boolean endFakeDrag() {
        return this.mFakeDragger.endFakeDrag();
    }

    public boolean isFakeDragging() {
        return this.mFakeDragger.isFakeDragging();
    }

    void snapToPage() {
        View view = this.mPagerSnapHelper.findSnapView(this.mLayoutManager);
        if (view != null) {
            int[] snapDistance = this.mPagerSnapHelper.calculateDistanceToFinalSnap(this.mLayoutManager, view);
            if (snapDistance[0] != 0 || snapDistance[1] != 0) {
                this.mRecyclerView.smoothScrollBy(snapDistance[0], snapDistance[1]);
            }

        }
    }

    public void setUserInputEnabled(boolean enabled) {
        this.mUserInputEnabled = enabled;
        this.mAccessibilityProvider.onSetUserInputEnabled();
    }

    public boolean isUserInputEnabled() {
        return this.mUserInputEnabled;
    }

    public void setOffscreenPageLimit(int limit) {
        if (limit < 1 && limit != -1) {
            throw new IllegalArgumentException("Offscreen page limit must be OFFSCREEN_PAGE_LIMIT_DEFAULT or a number > 0");
        } else {
            this.mOffscreenPageLimit = limit;
            this.mRecyclerView.requestLayout();
        }
    }

    public int getOffscreenPageLimit() {
        return this.mOffscreenPageLimit;
    }

    public boolean canScrollHorizontally(int direction) {
        return this.mRecyclerView.canScrollHorizontally(direction);
    }

    public boolean canScrollVertically(int direction) {
        return this.mRecyclerView.canScrollVertically(direction);
    }

    public void registerOnPageChangeCallback(@NonNull CustomViewPager2.OnPageChangeCallback callback) {
        this.mExternalPageChangeCallbacks.addOnPageChangeCallback(callback);
    }

    public void unregisterOnPageChangeCallback(@NonNull CustomViewPager2.OnPageChangeCallback callback) {
        this.mExternalPageChangeCallbacks.removeOnPageChangeCallback(callback);
    }

    public void setPageTransformer(@Nullable CustomViewPager2.PageTransformer transformer) {
        if (transformer != null) {
            if (!this.mSavedItemAnimatorPresent) {
                this.mSavedItemAnimator = this.mRecyclerView.getItemAnimator();
                this.mSavedItemAnimatorPresent = true;
            }

            this.mRecyclerView.setItemAnimator((ItemAnimator)null);
        } else if (this.mSavedItemAnimatorPresent) {
            this.mRecyclerView.setItemAnimator(this.mSavedItemAnimator);
            this.mSavedItemAnimator = null;
            this.mSavedItemAnimatorPresent = false;
        }

        if (transformer != this.mPageTransformerAdapter.getPageTransformer()) {
            this.mPageTransformerAdapter.setPageTransformer(transformer);
            this.requestTransform();
        }
    }

    public void requestTransform() {
        if (this.mPageTransformerAdapter.getPageTransformer() != null) {
            double relativePosition = this.mScrollEventAdapter.getRelativeScrollPosition();
            int position = (int)relativePosition;
            float positionOffset = (float)(relativePosition - (double)position);
            int positionOffsetPx = Math.round((float)this.getPageSize() * positionOffset);
            this.mPageTransformerAdapter.onPageScrolled(position, positionOffset, positionOffsetPx);
        }
    }

    @RequiresApi(17)
    public void setLayoutDirection(int layoutDirection) {
        super.setLayoutDirection(layoutDirection);
        this.mAccessibilityProvider.onSetLayoutDirection();
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        this.mAccessibilityProvider.onInitializeAccessibilityNodeInfo(info);
    }

    @RequiresApi(16)
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        return this.mAccessibilityProvider.handlesPerformAccessibilityAction(action, arguments) ? this.mAccessibilityProvider.onPerformAccessibilityAction(action, arguments) : super.performAccessibilityAction(action, arguments);
    }

    public void addItemDecoration(@NonNull ItemDecoration decor) {
        this.mRecyclerView.addItemDecoration(decor);
    }

    public void addItemDecoration(@NonNull ItemDecoration decor, int index) {
        this.mRecyclerView.addItemDecoration(decor, index);
    }

    @NonNull
    public ItemDecoration getItemDecorationAt(int index) {
        return this.mRecyclerView.getItemDecorationAt(index);
    }

    public int getItemDecorationCount() {
        return this.mRecyclerView.getItemDecorationCount();
    }

    public void invalidateItemDecorations() {
        this.mRecyclerView.invalidateItemDecorations();
    }

    public void removeItemDecorationAt(int index) {
        this.mRecyclerView.removeItemDecorationAt(index);
    }

    public void removeItemDecoration(@NonNull ItemDecoration decor) {
        this.mRecyclerView.removeItemDecoration(decor);
    }

    private abstract static class DataSetChangeObserver extends AdapterDataObserver {
        private DataSetChangeObserver() {
        }

        public abstract void onChanged();

        public final void onItemRangeChanged(int positionStart, int itemCount) {
            this.onChanged();
        }

        public final void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
            this.onChanged();
        }

        public final void onItemRangeInserted(int positionStart, int itemCount) {
            this.onChanged();
        }

        public final void onItemRangeRemoved(int positionStart, int itemCount) {
            this.onChanged();
        }

        public final void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            this.onChanged();
        }
    }

    class PageAwareAccessibilityProvider extends CustomViewPager2.AccessibilityProvider {
        private final AccessibilityViewCommand mActionPageForward = new AccessibilityViewCommand() {
            public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
                CustomViewPager2 viewPager = (CustomViewPager2)view;
                PageAwareAccessibilityProvider.this.setCurrentItemFromAccessibilityCommand(viewPager.getCurrentItem() + 1);
                return true;
            }
        };
        private final AccessibilityViewCommand mActionPageBackward = new AccessibilityViewCommand() {
            public boolean perform(@NonNull View view, @Nullable CommandArguments arguments) {
                CustomViewPager2 viewPager = (CustomViewPager2)view;
                PageAwareAccessibilityProvider.this.setCurrentItemFromAccessibilityCommand(viewPager.getCurrentItem() - 1);
                return true;
            }
        };
        private AdapterDataObserver mAdapterDataObserver;

        PageAwareAccessibilityProvider() {
            super(/*(NamelessClass_1)null*/);
        }

        public void onInitialize(@NonNull CompositeOnPageChangeCallback pageChangeEventDispatcher, @NonNull RecyclerView recyclerView) {
            ViewCompat.setImportantForAccessibility(recyclerView, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
            this.mAdapterDataObserver = new CustomViewPager2.DataSetChangeObserver() {
                public void onChanged() {
                    PageAwareAccessibilityProvider.this.updatePageAccessibilityActions();
                }
            };
            if (ViewCompat.getImportantForAccessibility(CustomViewPager2.this) == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                ViewCompat.setImportantForAccessibility(CustomViewPager2.this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }

        }

        public boolean handlesGetAccessibilityClassName() {
            return true;
        }

        public String onGetAccessibilityClassName() {
            if (!this.handlesGetAccessibilityClassName()) {
                throw new IllegalStateException();
            } else {
                return "androidx.viewpager.widget.ViewPager";
            }
        }

        public void onRestorePendingState() {
            this.updatePageAccessibilityActions();
        }

        public void onAttachAdapter(@Nullable Adapter<?> newAdapter) {
            this.updatePageAccessibilityActions();
            if (newAdapter != null) {
                newAdapter.registerAdapterDataObserver(this.mAdapterDataObserver);
            }

        }

        public void onDetachAdapter(@Nullable Adapter<?> oldAdapter) {
            if (oldAdapter != null) {
                oldAdapter.unregisterAdapterDataObserver(this.mAdapterDataObserver);
            }

        }

        public void onSetOrientation() {
            this.updatePageAccessibilityActions();
        }

        public void onSetNewCurrentItem() {
            this.updatePageAccessibilityActions();
        }

        public void onSetUserInputEnabled() {
            this.updatePageAccessibilityActions();
            if (VERSION.SDK_INT < 21) {
                CustomViewPager2.this.sendAccessibilityEvent(2048);
            }

        }

        public void onSetLayoutDirection() {
            this.updatePageAccessibilityActions();
        }

        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            this.addCollectionInfo(info);
            if (VERSION.SDK_INT >= 16) {
                this.addScrollActions(info);
            }

        }

        public boolean handlesPerformAccessibilityAction(int action, Bundle arguments) {
            return action == 8192 || action == 4096;
        }

        public boolean onPerformAccessibilityAction(int action, Bundle arguments) {
            if (!this.handlesPerformAccessibilityAction(action, arguments)) {
                throw new IllegalStateException();
            } else {
                int nextItem = action == 8192 ? CustomViewPager2.this.getCurrentItem() - 1 : CustomViewPager2.this.getCurrentItem() + 1;
                this.setCurrentItemFromAccessibilityCommand(nextItem);
                return true;
            }
        }

        public void onRvInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
            event.setSource(CustomViewPager2.this);
            event.setClassName(this.onGetAccessibilityClassName());
        }

        void setCurrentItemFromAccessibilityCommand(int item) {
            if (CustomViewPager2.this.isUserInputEnabled()) {
                CustomViewPager2.this.setCurrentItemInternal(item, true);
            }

        }

        void updatePageAccessibilityActions() {
            CustomViewPager2 viewPager = CustomViewPager2.this;
            int actionIdPageLeft = 16908360;
            int actionIdPageRight = 16908361;
            int actionIdPageUp = 16908358;
            int actionIdPageDown = 16908359;
            ViewCompat.removeAccessibilityAction(viewPager, 16908360);
            ViewCompat.removeAccessibilityAction(viewPager, 16908361);
            ViewCompat.removeAccessibilityAction(viewPager, 16908358);
            ViewCompat.removeAccessibilityAction(viewPager, 16908359);
            if (CustomViewPager2.this.getAdapter() != null) {
                int itemCount = CustomViewPager2.this.getAdapter().getItemCount();
                if (itemCount != 0) {
                    if (CustomViewPager2.this.isUserInputEnabled()) {
                        if (CustomViewPager2.this.getOrientation() == 0) {
                            boolean isLayoutRtl = CustomViewPager2.this.isRtl();
                            int actionIdPageForward = isLayoutRtl ? 16908360 : 16908361;
                            int actionIdPageBackward = isLayoutRtl ? 16908361 : 16908360;
                            if (CustomViewPager2.this.mCurrentItem < itemCount - 1) {
                                ViewCompat.replaceAccessibilityAction(viewPager, new AccessibilityActionCompat(actionIdPageForward, (CharSequence)null), (CharSequence)null, this.mActionPageForward);
                            }

                            if (CustomViewPager2.this.mCurrentItem > 0) {
                                ViewCompat.replaceAccessibilityAction(viewPager, new AccessibilityActionCompat(actionIdPageBackward, (CharSequence)null), (CharSequence)null, this.mActionPageBackward);
                            }
                        } else {
                            if (CustomViewPager2.this.mCurrentItem < itemCount - 1) {
                                ViewCompat.replaceAccessibilityAction(viewPager, new AccessibilityActionCompat(16908359, (CharSequence)null), (CharSequence)null, this.mActionPageForward);
                            }

                            if (CustomViewPager2.this.mCurrentItem > 0) {
                                ViewCompat.replaceAccessibilityAction(viewPager, new AccessibilityActionCompat(16908358, (CharSequence)null), (CharSequence)null, this.mActionPageBackward);
                            }
                        }

                    }
                }
            }
        }

        private void addCollectionInfo(AccessibilityNodeInfo info) {
            int rowCount = 0;
            int colCount = 0;
            if (CustomViewPager2.this.getAdapter() != null) {
                if (CustomViewPager2.this.getOrientation() == 1) {
                    rowCount = CustomViewPager2.this.getAdapter().getItemCount();
                } else {
                    colCount = CustomViewPager2.this.getAdapter().getItemCount();
                }
            }

            AccessibilityNodeInfoCompat nodeInfoCompat = AccessibilityNodeInfoCompat.wrap(info);
            CollectionInfoCompat collectionInfo = CollectionInfoCompat.obtain(rowCount, colCount, false, 0);
            nodeInfoCompat.setCollectionInfo(collectionInfo);
        }

        private void addScrollActions(AccessibilityNodeInfo info) {
            Adapter<?> adapter = CustomViewPager2.this.getAdapter();
            if (adapter != null) {
                int itemCount = adapter.getItemCount();
                if (itemCount != 0 && CustomViewPager2.this.isUserInputEnabled()) {
                    if (CustomViewPager2.this.mCurrentItem > 0) {
                        info.addAction(8192);
                    }

                    if (CustomViewPager2.this.mCurrentItem < itemCount - 1) {
                        info.addAction(4096);
                    }

                    info.setScrollable(true);
                }
            }
        }
    }

    class BasicAccessibilityProvider extends CustomViewPager2.AccessibilityProvider {
        BasicAccessibilityProvider() {
            super(/*(NamelessClass_1)null*/);
        }

        public boolean handlesLmPerformAccessibilityAction(int action) {
            return (action == 8192 || action == 4096) && !CustomViewPager2.this.isUserInputEnabled();
        }

        public boolean onLmPerformAccessibilityAction(int action) {
            if (!this.handlesLmPerformAccessibilityAction(action)) {
                throw new IllegalStateException();
            } else {
                return false;
            }
        }

        public void onLmInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfoCompat info) {
            if (!CustomViewPager2.this.isUserInputEnabled()) {
                info.removeAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
                info.removeAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
                info.setScrollable(false);
            }

        }

        public boolean handlesRvGetAccessibilityClassName() {
            return true;
        }

        public CharSequence onRvGetAccessibilityClassName() {
            if (!this.handlesRvGetAccessibilityClassName()) {
                throw new IllegalStateException();
            } else {
                return "androidx.viewpager.widget.ViewPager";
            }
        }
    }

    private abstract class AccessibilityProvider {
        private AccessibilityProvider() {
        }

        void onInitialize(@NonNull CompositeOnPageChangeCallback pageChangeEventDispatcher, @NonNull RecyclerView recyclerView) {
        }

        boolean handlesGetAccessibilityClassName() {
            return false;
        }

        String onGetAccessibilityClassName() {
            throw new IllegalStateException("Not implemented.");
        }

        void onRestorePendingState() {
        }

        void onAttachAdapter(@Nullable Adapter<?> newAdapter) {
        }

        void onDetachAdapter(@Nullable Adapter<?> oldAdapter) {
        }

        void onSetOrientation() {
        }

        void onSetNewCurrentItem() {
        }

        void onSetUserInputEnabled() {
        }

        void onSetLayoutDirection() {
        }

        void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        }

        boolean handlesPerformAccessibilityAction(int action, Bundle arguments) {
            return false;
        }

        boolean onPerformAccessibilityAction(int action, Bundle arguments) {
            throw new IllegalStateException("Not implemented.");
        }

        void onRvInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
        }

        boolean handlesLmPerformAccessibilityAction(int action) {
            return false;
        }

        boolean onLmPerformAccessibilityAction(int action) {
            throw new IllegalStateException("Not implemented.");
        }

        void onLmInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfoCompat info) {
        }

        boolean handlesRvGetAccessibilityClassName() {
            return false;
        }

        CharSequence onRvGetAccessibilityClassName() {
            throw new IllegalStateException("Not implemented.");
        }
    }

    public interface PageTransformer {
        void transformPage(@NonNull View var1, float var2);
    }

    public abstract static class OnPageChangeCallback {
        public OnPageChangeCallback() {
        }

        public void onPageScrolled(int position, float positionOffset, @Px int positionOffsetPixels) {
        }

        public void onPageSelected(int position) {
        }

        public void onPageScrollStateChanged(int state) {
        }
    }

    private static class SmoothScrollToPosition implements Runnable {
        private final int mPosition;
        private final RecyclerView mRecyclerView;

        SmoothScrollToPosition(int position, RecyclerView recyclerView) {
            this.mPosition = position;
            this.mRecyclerView = recyclerView;
        }

        public void run() {
            this.mRecyclerView.smoothScrollToPosition(this.mPosition);
        }
    }

    private class PagerSnapHelperImpl extends PagerSnapHelper {
        PagerSnapHelperImpl() {
        }

        @Nullable
        public View findSnapView(LayoutManager layoutManager) {
            return CustomViewPager2.this.isFakeDragging() ? null : super.findSnapView(layoutManager);
        }
    }

    private class LinearLayoutManagerImpl extends LinearLayoutManager {
        LinearLayoutManagerImpl(Context context) {
            super(context);
        }

        public boolean performAccessibilityAction(@NonNull Recycler recycler, @NonNull State state, int action, @Nullable Bundle args) {
            return CustomViewPager2.this.mAccessibilityProvider.handlesLmPerformAccessibilityAction(action) ? CustomViewPager2.this.mAccessibilityProvider.onLmPerformAccessibilityAction(action) : super.performAccessibilityAction(recycler, state, action, args);
        }

        public void onInitializeAccessibilityNodeInfo(@NonNull Recycler recycler, @NonNull State state, @NonNull AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(recycler, state, info);
            CustomViewPager2.this.mAccessibilityProvider.onLmInitializeAccessibilityNodeInfo(info);
        }

        protected void calculateExtraLayoutSpace(@NonNull State state, @NonNull int[] extraLayoutSpace) {
            int pageLimit = CustomViewPager2.this.getOffscreenPageLimit();
            if (pageLimit == -1) {
                super.calculateExtraLayoutSpace(state, extraLayoutSpace);
            } else {
                int offscreenSpace = CustomViewPager2.this.getPageSize() * pageLimit;
                extraLayoutSpace[0] = offscreenSpace;
                extraLayoutSpace[1] = offscreenSpace;
            }
        }

        public boolean requestChildRectangleOnScreen(@NonNull RecyclerView parent, @NonNull View child, @NonNull Rect rect, boolean immediate, boolean focusedChildVisible) {
            return false;
        }
    }

    private class RecyclerViewImpl extends RecyclerView {
        RecyclerViewImpl(@NonNull Context context) {
            super(context);
        }

        @RequiresApi(23)
        public CharSequence getAccessibilityClassName() {
            return CustomViewPager2.this.mAccessibilityProvider.handlesRvGetAccessibilityClassName() ? CustomViewPager2.this.mAccessibilityProvider.onRvGetAccessibilityClassName() : super.getAccessibilityClassName();
        }

        public void onInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            event.setFromIndex(CustomViewPager2.this.mCurrentItem);
            event.setToIndex(CustomViewPager2.this.mCurrentItem);
            CustomViewPager2.this.mAccessibilityProvider.onRvInitializeAccessibilityEvent(event);
        }

        @SuppressLint({"ClickableViewAccessibility"})
        public boolean onTouchEvent(MotionEvent event) {
            return CustomViewPager2.this.isUserInputEnabled() && super.onTouchEvent(event);
        }

        public boolean onInterceptTouchEvent(MotionEvent ev) {
            return CustomViewPager2.this.isUserInputEnabled() && super.onInterceptTouchEvent(ev);
        }
    }

    static class SavedState extends BaseSavedState {
        int mRecyclerViewId;
        int mCurrentItem;
        Parcelable mAdapterState;
        public static final Creator<CustomViewPager2.SavedState> CREATOR = new ClassLoaderCreator<CustomViewPager2.SavedState>() {
            public CustomViewPager2.SavedState createFromParcel(Parcel source, ClassLoader loader) {
                return VERSION.SDK_INT >= 24 ? new CustomViewPager2.SavedState(source, loader) : new CustomViewPager2.SavedState(source);
            }

            public CustomViewPager2.SavedState createFromParcel(Parcel source) {
                return this.createFromParcel(source, (ClassLoader)null);
            }

            public CustomViewPager2.SavedState[] newArray(int size) {
                return new CustomViewPager2.SavedState[size];
            }
        };

        @RequiresApi(24)
        SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            this.readValues(source, loader);
        }

        SavedState(Parcel source) {
            super(source);
            this.readValues(source, (ClassLoader)null);
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        private void readValues(Parcel source, ClassLoader loader) {
            this.mRecyclerViewId = source.readInt();
            this.mCurrentItem = source.readInt();
            this.mAdapterState = source.readParcelable(loader);
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.mRecyclerViewId);
            out.writeInt(this.mCurrentItem);
            out.writeParcelable(this.mAdapterState, flags);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({Scope.LIBRARY_GROUP_PREFIX})
    @IntRange(
            from = 1L
    )
    public @interface OffscreenPageLimit {
    }

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({Scope.LIBRARY_GROUP_PREFIX})
    public @interface ScrollState {
    }

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({Scope.LIBRARY_GROUP_PREFIX})
    public @interface Orientation {
    }
}
