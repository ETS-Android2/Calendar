package com.android.calendar.activities;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;

import static com.android.calendar.utils.Utils.CALENDAR_TYPE1;
import static com.android.calendar.utils.Utils.CALENDAR_TYPE2;
import static com.android.calendar.utils.Utils.CALENDAR_TYPE3;
import static com.android.calendar.utils.Utils.CALENDAR_TYPE4;

/**
 * 달력양식 선택화면
 * 4개의 양식중의 하나를 선택한다.
 */
public class CalendarTypeSelectActivity extends AppCompatActivity implements View.OnClickListener {
    //자식 view들
    View mBackButton;
    ImageView mImageCalendarType1, mImageCalendarType2, mImageCalendarType3, mImageCalendarType4;
    TextView mTextCalendarType1, mTextCalendarType2, mTextCalendarType3, mTextCalendarType4;

    //달력양식이 선택되였는가, 안되였는가에 따라 설정해줄 본문색갈, 테두리색갈
    int selected_text_color, selected_outline_color, unselected_text_color, unselected_outline_color;
    Toast mToast;

    @Override
    protected void onCreate(Bundle icicle) {
        //Dark/Light Theme 설정(현재는 dark theme 만 구현되여있음)
        if(Utils.isDayTheme())
            setTheme(R.style.CalendarAppThemeDay);
        else
            setTheme(R.style.CalendarAppThemeNight);

        super.onCreate(icicle);
        setContentView(R.layout.calendar_type_select);

        //색갈들 얻기
        selected_text_color = getColor(R.color.selected_calendar_text_color);
        selected_outline_color = getColor(R.color.selected_calendar_outline_color);
        unselected_text_color = getColor(R.color.unselected_calendar_text_color);
        unselected_outline_color = getColor(R.color.unselected_calendar_outline_color);

        initViews();
    }

    /**
     * 자식 view 들 초기화
     */
    public void initViews(){
        //자식 view들을 모두 얻는다.
        mBackButton = findViewById(R.id.back_button);
        mImageCalendarType1 = findViewById(R.id.image_calendar_type1);
        mImageCalendarType2 = findViewById(R.id.image_calendar_type2);
        mImageCalendarType3 = findViewById(R.id.image_calendar_type3);
        mImageCalendarType4 = findViewById(R.id.image_calendar_type4);
        mTextCalendarType1 = findViewById(R.id.text_calendar_type1);
        mTextCalendarType2 = findViewById(R.id.text_calendar_type2);
        mTextCalendarType3 = findViewById(R.id.text_calendar_type3);
        mTextCalendarType4 = findViewById(R.id.text_calendar_type4);

        //Click사건들을 추가한다.
        mBackButton.setOnClickListener(this);
        mImageCalendarType1.setOnClickListener(this);
        mImageCalendarType2.setOnClickListener(this);
        mImageCalendarType3.setOnClickListener(this);
        mImageCalendarType4.setOnClickListener(this);

        //Multi touch를 통해 한번에 2개의 view가 눌리우는것을 방지해주기 위하여 touch사건동작을 추가한다.
        Utils.addCommonTouchListener(mBackButton);
        Utils.addCommonTouchListener(mImageCalendarType1);
        Utils.addCommonTouchListener(mImageCalendarType2);
        Utils.addCommonTouchListener(mImageCalendarType3);
        Utils.addCommonTouchListener(mImageCalendarType4);

        updateViewColors();
    }

    /**
     * 양식선택이 변할때 선택된 양식의 테두리와 바깥선을 푸른색으로 설정한다.
     */
    public void updateViewColors() {
        int calendarType = Utils.getCalendarTypePreference(this);
        if(calendarType == CALENDAR_TYPE1) {
            mTextCalendarType1.setTextColor(selected_text_color);
            mImageCalendarType1.getBackground().setColorFilter(selected_outline_color, PorterDuff.Mode.SRC_IN);
        }
        else {
            mTextCalendarType1.setTextColor(unselected_text_color);
            mImageCalendarType1.getBackground().setColorFilter(unselected_outline_color, PorterDuff.Mode.SRC_IN);
        }

        if(calendarType == CALENDAR_TYPE2) {
            mTextCalendarType2.setTextColor(selected_text_color);
            mImageCalendarType2.getBackground().setColorFilter(selected_outline_color, PorterDuff.Mode.SRC_IN);
        }
        else {
            mTextCalendarType2.setTextColor(unselected_text_color);
            mImageCalendarType2.getBackground().setColorFilter(unselected_outline_color, PorterDuff.Mode.SRC_IN);
        }

        if(calendarType == CALENDAR_TYPE3) {
            mTextCalendarType3.setTextColor(selected_text_color);
            mImageCalendarType3.getBackground().setColorFilter(selected_outline_color, PorterDuff.Mode.SRC_IN);
        }
        else {
            mTextCalendarType3.setTextColor(unselected_text_color);
            mImageCalendarType3.getBackground().setColorFilter(unselected_outline_color, PorterDuff.Mode.SRC_IN);
        }

        if(calendarType == CALENDAR_TYPE4) {
            mTextCalendarType4.setTextColor(selected_text_color);
            mImageCalendarType4.getBackground().setColorFilter(selected_outline_color, PorterDuff.Mode.SRC_IN);
        }
        else {
            mTextCalendarType4.setTextColor(unselected_text_color);
            mImageCalendarType4.getBackground().setColorFilter(unselected_outline_color, PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public void onClick(View v) {
        //Memory leak을 방지하기 위하여 Activity가 아니라 ApplicationContext를 리용한다.
        Context context = getApplicationContext();

        if(v == mBackButton) {
            onBackPressed();
        }

        //양식을 하나 선택하면 선택된 달력양식을 preference를 통해 보관한다.
        else if(v == mImageCalendarType1){
            Utils.setSharedPreference(context, Utils.CALENDAR_TYPE_PREF, CALENDAR_TYPE1);
            if(mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(context, R.string.calendar_type1_set, Toast.LENGTH_SHORT);
            mToast.show();
        }
        else if(v == mImageCalendarType2){
            Utils.setSharedPreference(context, Utils.CALENDAR_TYPE_PREF, CALENDAR_TYPE2);
            if(mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(context, R.string.calendar_type2_set, Toast.LENGTH_SHORT);
            mToast.show();
        }
        else if(v == mImageCalendarType3){
            Utils.setSharedPreference(context, Utils.CALENDAR_TYPE_PREF, CALENDAR_TYPE3);
            if(mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(context, R.string.calendar_type3_set, Toast.LENGTH_SHORT);
            mToast.show();
        }
        else if(v == mImageCalendarType4){
            Utils.setSharedPreference(context, Utils.CALENDAR_TYPE_PREF, CALENDAR_TYPE4);
            if(mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(context, R.string.calendar_type4_set, Toast.LENGTH_SHORT);
            mToast.show();
        }

        updateViewColors();
    }

    /**
     * 둥근4각형모양의 ImageView를 현시하기 위하여 정의한 ImageView클라스
     */
    public static class CalendarTypeImage extends androidx.appcompat.widget.AppCompatImageView {

        //모서리반경과 바깥선두께
        public final float mRadius = Utils.convertDpToPixel(20, getContext());
        public final float mOutlineSize = Utils.convertDpToPixel(2.5f, getContext());

        //둥근4각형모양으로 잘라주는데 리용되는 Path
        Path mPath = new Path();

        public CalendarTypeImage(Context context) {
            this(context, null);
        }

        public CalendarTypeImage(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public CalendarTypeImage(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public void onDraw(Canvas canvas) {
            //그리기할때 보임령역을 설정해준다.
            mPath.reset();
            mPath.addRoundRect(mOutlineSize - 1, mOutlineSize - 1, getWidth() - mOutlineSize + 1,
                    getHeight() - mOutlineSize + 1, mRadius, mRadius, Path.Direction.CCW);
            canvas.clipPath(mPath);

            super.onDraw(canvas);
        }
    }
}
