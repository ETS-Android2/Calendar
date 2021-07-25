//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.calendar.customize.pager.widget;

import android.view.View;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.android.calendar.customize.pager.widget.CustomViewPager2.OnPageChangeCallback;
import com.android.calendar.customize.pager.widget.CustomViewPager2.PageTransformer;
import java.util.Locale;

final class PageTransformerAdapter extends OnPageChangeCallback {
    private final LinearLayoutManager mLayoutManager;
    private PageTransformer mPageTransformer;

    PageTransformerAdapter(LinearLayoutManager layoutManager) {
        this.mLayoutManager = layoutManager;
    }

    PageTransformer getPageTransformer() {
        return this.mPageTransformer;
    }

    void setPageTransformer(@Nullable PageTransformer transformer) {
        this.mPageTransformer = transformer;
    }

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (this.mPageTransformer != null) {
            float transformOffset = -positionOffset;

            for(int i = 0; i < this.mLayoutManager.getChildCount(); ++i) {
                View view = this.mLayoutManager.getChildAt(i);
                if (view == null) {
                    throw new IllegalStateException(String.format(Locale.US, "LayoutManager returned a null child at pos %d/%d while transforming pages", i, this.mLayoutManager.getChildCount()));
                }

                int currPos = this.mLayoutManager.getPosition(view);
                float viewOffset = transformOffset + (float)(currPos - position);
                this.mPageTransformer.transformPage(view, viewOffset);
            }

        }
    }

    public void onPageSelected(int position) {
    }

    public void onPageScrollStateChanged(int state) {
    }
}
