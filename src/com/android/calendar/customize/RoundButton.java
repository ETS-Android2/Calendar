package com.android.calendar.customize;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;

/**
 * 등근4각형모양의 단추
 */
public class RoundButton extends androidx.appcompat.widget.AppCompatButton {
    public RoundButton(Context context) {
        super(context);
        setStateListAnimator(null);
    }

    public RoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStateListAnimator(null);
    }

    public RoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setStateListAnimator(null);
    }

    @Override
    public void draw(Canvas canvas){
        Path path = new Path();
        path.addRoundRect(0, 0, getWidth(), getHeight(), getHeight()/2f, getHeight()/2f, Path.Direction.CCW);
        canvas.clipPath(path);
        super.draw(canvas);
    }
}
