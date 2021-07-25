package com.android.calendar.kr.year;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.viewpager2.widget.ViewPager2;

/**
 * 년보기
 * Page Transition을 진행할때 페지간 여백을 주기 위한 PageTransformer클라스
 */
public final class MarginPageTransformer2 implements ViewPager2.PageTransformer {
    private final int mMarginPx;    //여백 pixel

    public MarginPageTransformer2(@Px int marginPx) {
        this.mMarginPx = marginPx;
    }

    @Override
    public void transformPage(@NonNull View page, float position) {
        float offset = (float)this.mMarginPx * position;
        page.setTranslationX(offset);
    }
}
