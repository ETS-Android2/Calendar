package com.android.calendar.kr.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.calendar.event.EventTypeManager;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;
import com.android.krcalendar.R;

/**
 * 양식 2,3,4
 * 한개 날자 event현시를 위한 layout
 * 일정화상, 일정이름, 일정시간들을 현시한다.
 */
public class CommonDayEventItemView extends LinearLayout {
    private final float BG_ROUND_RADIUS = Utils.convertDpToPixel(20, getContext());

    //자식 view들
    ImageView mImage;   //화상
    TextView mTitle;    //제목
    TextView mWhen;     //기간

    //일정정보
    EventManager.OneEvent mEvent;

    //년, 월, 일
    int mYear;
    int mMonth;
    int mDay;

    //둥근 4각형모양으로 잘라주겠는가?
    private boolean mClipRound = true;

    public CommonDayEventItemView(Context context) {
        this(context, null);
    }

    public CommonDayEventItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0  );
    }

    public CommonDayEventItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        @SuppressLint("Recycle") TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CommonDayEventItemView);
        if(array != null) {
            mClipRound = array.getBoolean(R.styleable.CommonDayEventItemView_clip_round, true);
        }
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        //자식 View들 얻기
        mImage = findViewById(R.id.event_image);
        mTitle = findViewById(R.id.event_title);
        mWhen = findViewById(R.id.event_time);
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        if(mClipRound) {    //둥근 4각형모양으로 잘라준다.
            Path path = new Path();
            path.addRoundRect(0, 0, getWidth(), getHeight(), BG_ROUND_RADIUS, BG_ROUND_RADIUS, Path.Direction.CCW);
            canvas.clipPath(path);
        }

        super.dispatchDraw(canvas);
    }

    public void setEventItemBackgroundColor(int color) {
        getBackground().mutate().setTint(color);
    }

    public void setDate(int year, int month, int day){
        mYear = year;
        mMonth = month;
        mDay = day;
    }

    public void applyFromEventInfo(EventManager.OneEvent event){
        mEvent = event;

        int eventTypeId = event.type;
        EventTypeManager.OneEventType eventType = EventTypeManager.getEventTypeFromId(eventTypeId);

        //일정화상현시
        Resources resources = getResources();
        Drawable drawable = ResourcesCompat.getDrawable(resources, eventType.imageResource, null).mutate();
        drawable.setTint(resources.getColor(eventType.color, null));
        mImage.setImageDrawable(drawable);

        //일정제목현시
        if(event.title == null || event.title.isEmpty())
            mTitle.setText(R.string.no_title_label);
        else
            mTitle.setText(event.title);

        //일정기간현시
        String whenString = event.getHourMinuteString(getContext(), mYear, mMonth, mDay, true);
        mWhen.setText(whenString);
    }

    /**
     * 상하좌우 여백을 준다.
     * @param marginStartDp 왼쪽여백(dp)
     * @param marginEndDp 오른쪽여백(dp)
     * @param marginTopDp 웃쪽여백(dp)
     * @param marginBottomDp 아래쪽여백(dp)
     */
    public void setMargin(float marginStartDp, float marginEndDp, float marginTopDp, float marginBottomDp) {
        //dp -> pixel 변환
        Context context = getContext();
        int marginStart = (int) Utils.convertDpToPixel(marginStartDp, context);
        int marginEnd = (int) Utils.convertDpToPixel(marginEndDp, context);
        int marginTop = (int) Utils.convertDpToPixel(marginTopDp, context);
        int marginBottom = (int) Utils.convertDpToPixel(marginBottomDp, context);

        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)getLayoutParams();
        lp.leftMargin = marginStart;
        lp.rightMargin = marginEnd;
        lp.topMargin = marginTop;
        lp.bottomMargin = marginBottom;
    }

    /**
     * 왼쪽 padding을 설정한다.
     * @param paddingStartDP 왼쪽 padding(dp)
     */
    public void setPaddingStart(float paddingStartDP) {
        setPadding((int) Utils.convertDpToPixel(paddingStartDP, getContext()), getPaddingTop(), getPaddingRight(), getPaddingBottom());
    }
}
