//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.calendar.customize.pager.widget;

import android.animation.LayoutTransition;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.Arrays;
import java.util.Comparator;

final class AnimateLayoutChangeDetector {
    private static final MarginLayoutParams ZERO_MARGIN_LAYOUT_PARAMS = new MarginLayoutParams(-1, -1);
    private LinearLayoutManager mLayoutManager;

    AnimateLayoutChangeDetector(@NonNull LinearLayoutManager llm) {
        this.mLayoutManager = llm;
    }

    boolean mayHaveInterferingAnimations() {
        return (!this.arePagesLaidOutContiguously() || this.mLayoutManager.getChildCount() <= 1) && this.hasRunningChangingLayoutTransition();
    }

    private boolean arePagesLaidOutContiguously() {
        int childCount = this.mLayoutManager.getChildCount();
        if (childCount == 0) {
            return true;
        } else {
            boolean isHorizontal = this.mLayoutManager.getOrientation() == 0;
            int[][] bounds = new int[childCount][2];

            int i;
            for(i = 0; i < childCount; ++i) {
                View view = this.mLayoutManager.getChildAt(i);
                if (view == null) {
                    throw new IllegalStateException("null view contained in the view hierarchy");
                }

                LayoutParams layoutParams = view.getLayoutParams();
                MarginLayoutParams margin;
                if (layoutParams instanceof MarginLayoutParams) {
                    margin = (MarginLayoutParams)layoutParams;
                } else {
                    margin = ZERO_MARGIN_LAYOUT_PARAMS;
                }

                bounds[i][0] = isHorizontal ? view.getLeft() - margin.leftMargin : view.getTop() - margin.topMargin;
                bounds[i][1] = isHorizontal ? view.getRight() + margin.rightMargin : view.getBottom() + margin.bottomMargin;
            }

            Arrays.sort(bounds, new Comparator<int[]>() {
                public int compare(int[] lhs, int[] rhs) {
                    return lhs[0] - rhs[0];
                }
            });

            for(i = 1; i < childCount; ++i) {
                if (bounds[i - 1][1] != bounds[i][0]) {
                    return false;
                }
            }

            i = bounds[0][1] - bounds[0][0];
            if (bounds[0][0] <= 0 && bounds[childCount - 1][1] >= i) {
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean hasRunningChangingLayoutTransition() {
        int childCount = this.mLayoutManager.getChildCount();

        for(int i = 0; i < childCount; ++i) {
            if (hasRunningChangingLayoutTransition(this.mLayoutManager.getChildAt(i))) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasRunningChangingLayoutTransition(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup)view;
            LayoutTransition layoutTransition = viewGroup.getLayoutTransition();
            if (layoutTransition != null && layoutTransition.isChangingLayout()) {
                return true;
            }

            int childCount = viewGroup.getChildCount();

            for(int i = 0; i < childCount; ++i) {
                if (hasRunningChangingLayoutTransition(viewGroup.getChildAt(i))) {
                    return true;
                }
            }
        }

        return false;
    }

    static {
        ZERO_MARGIN_LAYOUT_PARAMS.setMargins(0, 0, 0, 0);
    }
}
