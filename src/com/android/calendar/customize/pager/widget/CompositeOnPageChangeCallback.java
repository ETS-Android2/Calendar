//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.android.calendar.customize.pager.widget;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import com.android.calendar.customize.pager.widget.CustomViewPager2.OnPageChangeCallback;

final class CompositeOnPageChangeCallback extends OnPageChangeCallback {
    @NonNull
    private final List<OnPageChangeCallback> mCallbacks;

    CompositeOnPageChangeCallback(int initialCapacity) {
        this.mCallbacks = new ArrayList<>(initialCapacity);
    }

    void addOnPageChangeCallback(OnPageChangeCallback callback) {
        this.mCallbacks.add(callback);
    }

    void removeOnPageChangeCallback(OnPageChangeCallback callback) {
        this.mCallbacks.remove(callback);
    }

    public void onPageScrolled(int position, float positionOffset, @Px int positionOffsetPixels) {
        try {
            Iterator var4 = this.mCallbacks.iterator();

            while(var4.hasNext()) {
                OnPageChangeCallback callback = (OnPageChangeCallback)var4.next();
                callback.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        } catch (ConcurrentModificationException var6) {
            this.throwCallbackListModifiedWhileInUse(var6);
        }

    }

    public void onPageSelected(int position) {
        try {
            Iterator var2 = this.mCallbacks.iterator();

            while(var2.hasNext()) {
                OnPageChangeCallback callback = (OnPageChangeCallback)var2.next();
                callback.onPageSelected(position);
            }
        } catch (ConcurrentModificationException var4) {
            this.throwCallbackListModifiedWhileInUse(var4);
        }

    }

    public void onPageScrollStateChanged(int state) {
        try {
            Iterator var2 = this.mCallbacks.iterator();

            while(var2.hasNext()) {
                OnPageChangeCallback callback = (OnPageChangeCallback)var2.next();
                callback.onPageScrollStateChanged(state);
            }
        } catch (ConcurrentModificationException var4) {
            this.throwCallbackListModifiedWhileInUse(var4);
        }

    }

    private void throwCallbackListModifiedWhileInUse(ConcurrentModificationException parent) {
        throw new IllegalStateException("Adding and removing callbacks during dispatch to callbacks is not supported", parent);
    }
}
