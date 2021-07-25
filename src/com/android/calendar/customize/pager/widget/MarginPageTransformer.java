//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.calendar.customize.pager.widget;

import android.view.View;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.core.util.Preconditions;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2.PageTransformer;

public final class MarginPageTransformer implements PageTransformer {
    private final int mMarginPx;

    public MarginPageTransformer(@Px int marginPx) {
        Preconditions.checkArgumentNonnegative(marginPx, "Margin must be non-negative");
        this.mMarginPx = marginPx;
    }

    public void transformPage(@NonNull View page, float position) {
        CustomViewPager2 viewPager = this.requireViewPager(page);
        float offset = (float)this.mMarginPx * position;
        if (viewPager.getOrientation() == 0) {
            page.setTranslationX(viewPager.isRtl() ? -offset : offset);
        } else {
            page.setTranslationY(offset);
        }

    }

    private CustomViewPager2 requireViewPager(@NonNull View page) {
        ViewParent parent = page.getParent();
        ViewParent parentParent = parent.getParent();
        if (parent instanceof RecyclerView && parentParent instanceof CustomViewPager2) {
            return (CustomViewPager2)parentParent;
        } else {
            throw new IllegalStateException("Expected the page view to be managed by a ViewPager2 instance.");
        }
    }
}
