package com.android.calendar.event.custom;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.android.calendar.event.EventTypeManager;
import com.android.krcalendar.R;

/**
 * DrawableTop 으로서 일정화상을 앉힌 TextView
 */
public class CustomTextView extends androidx.appcompat.widget.AppCompatTextView {
    public CustomTextView(Context context) {
        this(context, null);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void applyFromEventType(EventTypeManager.OneEventType eventType){
        int imageSize = (int) getResources().getDimension(R.dimen.event_item_image_size);

        //일정화상을 DrawableTop으로 설정한다.
        setText(eventType.title);
        Drawable drawable = ContextCompat.getDrawable(getContext(), eventType.imageResource).mutate();
        drawable.setBounds(0, 0, imageSize, imageSize);
        drawable.setTint(getContext().getColor(eventType.color));
        setCompoundDrawables(null, drawable, null, null);
    }
}
