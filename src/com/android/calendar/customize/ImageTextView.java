package com.android.calendar.customize;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import androidx.annotation.Nullable;

import com.android.krcalendar.R;

/**
 * 화면아래 bottom bar에 놓이는 화상과 문자렬을 가진 단추
 */
public class ImageTextView extends AppCompatTextView {

    //초점이 갔을때/해제되였을때의 글자 및 화상색갈
    private final int mFocusedColor;
    private final int mUnFocusedColor;

    public ImageTextView(Context context) {
        this(context, null);
    }

    public ImageTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        @SuppressLint("Recycle") TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ImageTextView);
        if(typedArray != null){
            //Get colors
            mFocusedColor = typedArray.getColor(R.styleable.ImageTextView_focused_color, Color.BLUE);
            mUnFocusedColor = typedArray.getColor(R.styleable.ImageTextView_unfocused_color, Color.WHITE);
        }
        else{
            mFocusedColor = Color.BLUE;
            mUnFocusedColor = Color.WHITE;
        }
    }

    @Override
    public void drawableStateChanged() {
        super.drawableStateChanged();

        //단추가 눌리웠는가를 검사한다.
        boolean statePressed = false;
        int[] states = getDrawableState();
        for (int state : states)
        {
            if (state == android.R.attr.state_pressed) {
                statePressed = true;
                break;
            }
        }

        Drawable[] drawables = getCompoundDrawables();
        //눌리웠을때
        if(statePressed){
            //글자색, 화상색을 mFocusedColor를 설정한다.
            for (Drawable drawable:drawables){
                if(drawable != null){
                    drawable.setTint(mFocusedColor);
                }
            }
            setTextColor(mFocusedColor);

            //서체를 Bold체로 설정한다.
            setTypeface(null, Typeface.BOLD);
        }

        //눌리우지 않았을때
        else{
            //글자색, 화상색을 mUnFocusedColor 설정한다.
            for (Drawable drawable:drawables){
                if(drawable != null){
                    drawable.setTint(mUnFocusedColor);
                }
            }
            setTextColor(mUnFocusedColor);

            //Bold체로 설정되였던 서체를 기정서체로 바꾼다.
            setTypeface(null, Typeface.NORMAL);
        }
    }
}
