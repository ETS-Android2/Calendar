package com.android.calendar.event;

import com.android.krcalendar.R;

/**
 * 일정형식관리를 위한 helper 클라스
 */
public class EventTypeManager {
    public static final int APP_EVENT_TYPE_COUNT = 8;
    public static final int EVENT_TYPES_PER_ROW = 4;

    //일정형식들
    public static final int EVENT_TYPE_DEFAULT = 1;     //일반
    public static final int EVENT_TYPE_BIRTHDAY = 2;    //생일
    public static final int EVENT_TYPE_CONFERENCE = 3;  //회의
    public static final int EVENT_TYPE_HOLIDAY = 4;     //명절
    public static final int EVENT_TYPE_COMPETITION = 5; //경기
    public static final int EVENT_TYPE_TRAVEL =  6;     //려행
    public static final int EVENT_TYPE_CURE = 7;        //치료
    public static final int EVENT_TYPE_EXAMINATION = 8; //시험

    //일정형식들을 정적변수로 창조한다.
    public static final OneEventType[] APP_EVENT_TYPES = {
            new OneEventType(EVENT_TYPE_DEFAULT, R.string.event_type_default, R.color.event_type_default_color, R.drawable.ic_default_icon),
            new OneEventType(EVENT_TYPE_BIRTHDAY, R.string.event_type_birthday, R.color.event_type_birthday_color, R.drawable.ic_birthday_icon),
            new OneEventType(EVENT_TYPE_CONFERENCE, R.string.event_type_conference, R.color.event_type_conference_color, R.drawable.ic_conference_icon),
            new OneEventType(EVENT_TYPE_HOLIDAY, R.string.event_type_holiday, R.color.event_type_holiday_color, R.drawable.ic_holiday_icon),
            new OneEventType(EVENT_TYPE_COMPETITION, R.string.event_type_competition, R.color.event_type_competition_color, R.drawable.ic_competition_icon),
            new OneEventType(EVENT_TYPE_TRAVEL, R.string.event_type_travel, R.color.event_type_travel_color, R.drawable.ic_travel_icon),
            new OneEventType(EVENT_TYPE_CURE, R.string.event_type_cure, R.color.event_type_cure_color, R.drawable.ic_cure_icon),
            new OneEventType(EVENT_TYPE_EXAMINATION, R.string.event_type_examination, R.color.event_type_examination_color, R.drawable.ic_examination_icon),
    };

    /**
     * 일정형식을 찾아서 돌려준다
     * @param eventTypeId 일정형식 식별자
     */
    public static OneEventType getEventTypeFromId(int eventTypeId){
        //식별자에 해당한 일정형식을 검색한다.
        for (int i = 0; i < APP_EVENT_TYPE_COUNT; i++) {
            if (APP_EVENT_TYPES[i].id == eventTypeId) {
                //그것을 돌려준다.
                return APP_EVENT_TYPES[i];
            }
        }

        //찾지 못하면 기정의 일정형식을 돌려준다.
        return APP_EVENT_TYPES[0];
    }

    /**
     * 한개 일정형식을 구성하는 식별자, 제목, 색, 화상을 가진 model 클라스
     */
    public static class OneEventType {
        public int id;      //식별자
        public int title;   //제목
        public int color;   //색
        public int imageResource;   //화상

        //구성자
        public OneEventType(int evType, int evTitle, int evColor, int evImage) {
            id = evType;
            title = evTitle;
            color = evColor;
            imageResource = evImage;
        }
    }
}
