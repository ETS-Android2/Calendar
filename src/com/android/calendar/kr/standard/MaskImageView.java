package com.android.calendar.kr.standard;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

import com.android.calendar.utils.Utils;

/**
 * 양식 1
 * Mask 색을 덧그려주는 ImageView
 */
public class MaskImageView extends AppCompatImageView {
    private static final int MASK_ALPHA = 0x30;
    private static final int MASK_COLOR = 0xff0000;
    private static final float VIEW_ALPHA_MIN = 0.6f;

    private float mProgress = 0;

    public MaskImageView(Context context) {
        this(context, null);
    }

    public MaskImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaskImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawColor(Utils.applyAlpha(MASK_COLOR, (int) (MASK_ALPHA * mProgress)));
    }

    public void setAlphaProgress(float value){
        mProgress = value;
        setAlpha(VIEW_ALPHA_MIN + (1 - VIEW_ALPHA_MIN) * (1 - value));
        invalidate();
    }
}
