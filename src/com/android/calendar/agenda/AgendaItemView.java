package com.android.calendar.agenda;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.android.calendar.event.EventTypeManager;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EventManager;
import com.android.krcalendar.R;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

/**
 * 일정목록화면에서 한개 일정현시에 해당한 view
 */
public class AgendaItemView extends LinearLayout {
    //자식 view들
    ImageView mImageView;
    TextView mTitleView, mTimeView;

    //일정
    EventManager.OneEvent mEvent;

    //검색어부분을 강조해주기 위한 색갈
    public int mHighlightColor;

    public AgendaItemView(Context context) {
        this(context, null);
    }

    public AgendaItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AgendaItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mHighlightColor = getContext().getColor(R.color.search_keyword_highlight_color);
    }

    @Override
    public void onFinishInflate(){
        super.onFinishInflate();

        //자식 view들을 얻는다.
        mImageView = findViewById(R.id.event_image);
        mTitleView = findViewById(R.id.event_title);
        mTimeView = findViewById(R.id.event_time);
    }

    /**
     * 일정정보에 기초하여 한개의 일정항목 View를 설정한다.
     * @param event 일정정보
     * @param todayEvent 오늘일정인가?
     */
    public void applyFromEventInfo(EventManager.OneEvent event, Boolean todayEvent){
        mEvent = event;

        int eventTypeId = event.type;
        EventTypeManager.OneEventType eventType = EventTypeManager.getEventTypeFromId(eventTypeId);

        /*-- 일정화상 현시 --*/
        Resources resources = getResources();
        Drawable drawable = ResourcesCompat.getDrawable(resources, eventType.imageResource, null).mutate();
        drawable.setTint(resources.getColor(eventType.color, null));
        mImageView.setImageDrawable(drawable);

        //날자 textview 설정
        String timeString = AgendaFragment.getDateString(event.startTime) + " "
                + Utils.getWeekDayString(getContext(), event.startTime.getDayOfWeek(), false);
        mTimeView.setText(timeString);

        final int timeViewColor;
        //오늘날자일정인 경우에 날자색을 푸른색으로 설정한다
        if(todayEvent) {
            timeViewColor = getResources().getColor(R.color.today_item_time_color, null);
        }
        else {
            timeViewColor = Utils.getThemeAttribute(getContext(), R.attr.common_text_color_secondary);
        }
        mTimeView.setTextColor(timeViewColor);
    }

    /**
     * 검색어부분을 특정한 색으로 강조해준다
     * @param query 검색어
     */
    public void highLightQueryText(String query) {
        if(mEvent.title == null || mEvent.title.isEmpty())
            mTitleView.setText(R.string.no_title_label);
        else {
            mTitleView.setText(
                    Html.fromHtml(convertToHighlightedHtmlString(mEvent.title, query),
                            FROM_HTML_MODE_LEGACY));
        }
    }

    /**
     * @param query 검색어
     * @return 일정제목이 검색어를 포함하고 있는가를 돌려준다
     */
    public boolean containsQuery(String query) {
        String title;
        if(mEvent.title != null)
            title = mEvent.title;
        else
            title = "";

        //대소문자를 구별하지 않고 검색을 진행한다.
        return title.toLowerCase().contains(query.toLowerCase());
    }

    /**
     * @param oldString 원본문자렬
     * @param highlight 강조하려는 문자렬
     * @return 강조하려는 문자렬들을 다른 색으로 형식화한 Html문자렬을 돌려준다.
     */
    public String convertToHighlightedHtmlString(String oldString, String highlight){
        //문자렬교체를 위한 정규표현식 작성
        String regexInput = "(?i)(" + highlight + ")";

        //Html형식 문자렬을 추출한다.
        String hexString = Integer.toHexString(mHighlightColor);
        String color = "#" + hexString.substring(2);
        String replaceString = "<font color='"+ color + "'>$1</font>";

        //교체를 진행하여 돌려준다.
        return oldString.replaceAll(regexInput, replaceString);
    }
}
