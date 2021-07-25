//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.calendar.customize.pager.widget;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2.PageTransformer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CompositePageTransformer implements PageTransformer {
    private final List<PageTransformer> mTransformers = new ArrayList<>();

    public CompositePageTransformer() {
    }

    public void addTransformer(@NonNull PageTransformer transformer) {
        this.mTransformers.add(transformer);
    }

    public void removeTransformer(@NonNull PageTransformer transformer) {
        this.mTransformers.remove(transformer);
    }

    public void transformPage(@NonNull View page, float position) {
        Iterator var3 = this.mTransformers.iterator();

        while(var3.hasNext()) {
            PageTransformer transformer = (PageTransformer)var3.next();
            transformer.transformPage(page, position);
        }

    }
}
