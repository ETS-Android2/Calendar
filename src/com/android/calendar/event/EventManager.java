package com.android.calendar.event;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.core.app.ActivityCompat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.android.kr_common.Time;
import com.android.krcalendar.R;

import static com.android.calendar.event.EventTypeManager.APP_EVENT_TYPE_COUNT;

/**
 * 일정관리와 관련한 함수들을 제공해주는 클라스
 */
public class EventManager {

    //날자범위
    public static final int YEAR = 1;   //한해
    public static final int MONTH = 2;  //한달
    public static final int WEEK = 3;   //한주
    public static final int DAY = 4;    //하루

    /**
     * 날자비교를 기본용도로 정의한 클라스
     */
    public static class OneDate {
        int year = 0;
        int month = 0;
        int day = 0;

        OneDate(int yy, int mm, int dd) {
            year = yy;
            month = mm;
            day = dd;
        }

        OneDate(DateTime dateTime) {
            year = dateTime.getYear();
            month = dateTime.getMonthOfYear();
            day = dateTime.dayOfMonth().get();
        }

        boolean isEqual(OneDate other) {
            return year == other.year && month == other.month && day == other.day;
        }

        boolean isEqualOrAfter(OneDate other){
            return isEqual(other) || compare(this, other) == DATE_AFTER;
        }
        boolean isBefore(OneDate other){
            return compare(this, other) == DATE_BEFORE;
        }
        boolean isAfter(OneDate other){
            return compare(this, other) == DATE_AFTER;
        }

        private static final int DATE_AFTER = 0;
        private static final int DATE_SAME = 1;
        private static final int DATE_BEFORE = 2;
        int compare(OneDate date1, OneDate date2){
            if(date1.year > date2.year)
                return DATE_AFTER;
            if(date1.year < date2.year)
                return DATE_BEFORE;
            if(date1.month > date2.month)
                return DATE_AFTER;
            if(date1.month < date2.month)
                return DATE_BEFORE;
            if(date1.day > date2.day)
                return DATE_AFTER;
            if(date1.day < date2.day)
                return DATE_BEFORE;
            return DATE_SAME;
        }
    }

    /**
     * 한개 일정에서 실지 보여줄 항목들에 대한 column들만을 포함하고 있는 클라스
     */
    public static class OneEvent{
        public int id;
        public int type;
        public String title;
        public String description;
        public String location;
        public DateTime startTime;
        public DateTime endTime;
        public DateTime realStartTimeRecurrence;
        public boolean allDay;

        public OneEvent(int _id, int _type, String _title, String _location, DateTime _startTime, DateTime _endTime,
                        String _description, boolean _allDay, DateTime _realStartTime){
            id = _id;
            type = _type;
            title = _title;
            description = _description;
            location = _location;
            startTime = _startTime;
            endTime = _endTime;
            allDay = _allDay;
            realStartTimeRecurrence = _realStartTime;
        }

        /**
         * 날자에 해당한 일정의 시간문자렬(`5.30 ~ 13.30` 혹은 `시작: 5.30` 혹은 `종료: 13.30` 혹은 `하루종일`)
         * @param context
         * @param year 년
         * @param month 월
         * @param day 일
         * @param is24Hours true: 24시간 방식, false: 12시간방식
         * @return 결과문자렬을 돌려준다.
         */
        public String getHourMinuteString(Context context, int year, int month, int day, boolean is24Hours){
            if(allDay)
                return context.getResources().getString(R.string.all_day_label);

            OneDate curDate = new OneDate(year, month, day);
            String start, end;          //시작시간, 마감시간에 해당한 문자렬
            boolean fromStart = false;  //전날부터 시작한 일정인가?

            if(curDate.isAfter(getStartDate()))
                fromStart = true;

            boolean toEnd = false;      //다음날 혹은 그이후까지 지속되는 일정인가?
            DateTime beforeDay = endTime.minusDays(1);
            OneDate beforeDate = new OneDate(beforeDay);
            if(curDate.isBefore(getEndDate())){
                if(curDate.isEqual(beforeDate)){
                    if(endTime.getMinuteOfDay() > 0){
                        toEnd = true;
                    }
                }
                else
                    toEnd = true;
            }

            if(fromStart){
                start = "";
            }
            else {
                @SuppressLint("DefaultLocale") String minuteString = String.format("%02d", startTime.getMinuteOfHour());
                if(is24Hours){
                    start = startTime.getHourOfDay() + ":" + minuteString;
                }
                else {
                    //오전|오후를 붙여준다.
                    if (startTime.getHourOfDay() < 12) {
                        if (startTime.getHourOfDay() == 0)
                            start = "12:" + minuteString;
                        else
                            start = startTime.getHourOfDay() + ":" + minuteString;

                        //"오전"
                        start = context.getString(R.string.am_label, start);
                    } else {
                        if (startTime.getHourOfDay() == 12)
                            start = "12:" + minuteString;
                        else
                            start = (startTime.getHourOfDay() - 12) + ":" + minuteString;

                        //"오후"
                        start = context.getString(R.string.pm_label, start);
                    }
                }
            }

            if(toEnd){
                end = "";
            }
            else
            {
                @SuppressLint("DefaultLocale") String minuteString = String.format("%02d", endTime.getMinuteOfHour());
                if(is24Hours){
                    end = endTime.getHourOfDay() + ":" + minuteString;
                }
                else {
                    if (endTime.getHourOfDay() < 12) {
                        if (endTime.getHourOfDay() == 0)
                            end = "12:" + minuteString;
                        else
                            end = endTime.getHourOfDay() + ":" + minuteString;

                        //"오전"
                        end = context.getString(R.string.am_label, end);
                    } else {
                        if (endTime.getHourOfDay() == 12)
                            end = "12:" + minuteString;
                        else
                            end = (endTime.getHourOfDay() - 12) + ":" + minuteString;

                        //"오후"
                        end = context.getString(R.string.pm_label, end);
                    }
                }
            }
            if(fromStart && toEnd){
                return context.getResources().getString(R.string.all_day_label);
            }

            if(fromStart){
                return context.getString(R.string.agenda_time_end) + ": " + end;
            }
            if(toEnd){
                return context.getString(R.string.agenda_time_start) + ": " + start;
            }
            if(startTime.getMillis() == endTime.getMillis())
                return start;
            return start + " ~ " + end;
        }

        /* 시작, 마감날자를 OneDate형으로 변환하여 돌려준다. */
        OneDate getStartDate(){
            return new OneDate(startTime.getYear(), startTime.getMonthOfYear(), startTime.getDayOfMonth());
        }
        OneDate getEndDate(){
            return new OneDate(endTime.getYear(), endTime.getMonthOfYear(), endTime.getDayOfMonth());
        }

        /**
         * 일정이 날자에 있는지를 돌려준다.
         * @param date 날자
         */
        boolean containsDate(OneDate date){
            final OneDate startDate = getStartDate();
            final OneDate endDate = getEndDate();

            if(allDay){
                return date.isEqualOrAfter(startDate) && date.isBefore(endDate);
            }

            if(date.isEqualOrAfter(startDate)){
                if(date.isBefore(endDate)){
                    return true;
                }
                if(date.isEqual(endDate)){
                    if(endTime.getMinuteOfDay() == 0) {
                        return endTime.getMillis() == startTime.getMillis();
                    }
                    return true;
                }
            }
            return false;
        }
        public boolean containsDate(int year, int month, int day){
            return containsDate(new OneDate(year, month, day));
        }

        /**
         * 일정의 시작날자와 입력된 날자를 비교하여 같으면 true를 돌려준다.
         * @param year 년
         * @param month 월
         * @param day 일
         */
        public boolean startDateEquals(int year, int month, int day){
            return startTime.getYear() == year && startTime.getMonthOfYear() == month && startTime.getDayOfMonth() == day;
        }

        /**
         * @return 지나간 일정이면 true, 오늘 혹은 오늘이후의 일정이면 false를 돌려준다.
         */
        public boolean pastOrFutureCurrent(){
            DateTime curDateTime = new DateTime().withSecondOfMinute(0).withMillisOfSecond(0);
            if(!allDay) {
                return endTime.getMillis() < curDateTime.getMillis();
            }

            long endMillis = endTime.minusDays(1).withMillisOfDay(0).getMillis();
            return endMillis < curDateTime.withMillisOfDay(0).getMillis();
        }
    }

    /**
     * 시작날자로부터 시작하여 하루|한주|한달|한해 동안의 일정목록을 돌려준다.
     * @param context
     * @param timeMillis 시작시간
     * @param selectRange 날자선택범위
     * @return 일정목록
     */
    public static List<OneEvent> getEvents(Context context, long timeMillis, int selectRange){
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return Collections.emptyList();
        }

        /* 시작날자, 마감날자들을 계산한다. */
        DateTime selectedTime = new DateTime(timeMillis);
        DateTime startTime;
        DateTime endTime;
        int startDay = 0, endDay = 0;

        //한해
        if(selectRange == YEAR){
            startTime = new DateTime().withDate(selectedTime.getYear(), 1, 1).withMillisOfDay(0);
            endTime = startTime.plusYears(1).minusDays(1);

            startDay = Time.getJulianDay(startTime);
            endDay = Time.getJulianDay(endTime);
        }

        //한달
        else if(selectRange == MONTH){
            startTime = new DateTime().withDate(selectedTime.getYear(), selectedTime.getMonthOfYear(), 1).withMillisOfDay(0);
            endTime = startTime.plusMonths(1).minusDays(1);

            startDay = Time.getJulianDay(startTime);
            endDay = Time.getJulianDay(endTime);
        }

        //한주
        else if(selectRange == WEEK){
            DateTime startWeek = selectedTime.minusDays(selectedTime.getDayOfWeek() % 7);
            startTime = new DateTime().withDate(startWeek.getYear(), startWeek.getMonthOfYear(), startWeek.getDayOfMonth()).withMillisOfDay(0);

            startDay = Time.getJulianDay(startTime);
            endDay = startDay + 7 - 1;
        }

        //하루
        else if(selectRange == DAY){
            startTime = new DateTime().withDate(selectedTime.getYear(), selectedTime.getMonthOfYear(), selectedTime.getDayOfMonth()).withMillisOfDay(0);

            startDay = Time.getJulianDay(startTime);
            endDay = startDay;
        }

        //시작날자 - 끝날자 사이의 일정들을 돌려준다.
        return getEventsInRange(context, startDay, endDay, "");
    }

    /**
     * 날자범위에 속하면서 제목에 검색어가 포함된 일정목록들을 돌려준다.
     * @param context
     * @param startDay 시작날자 (Julian day)
     * @param endDay 마감날자 (Julian day)
     * @param query 검색어
     * @return 일정목록
     */
    public static List<OneEvent> getEventsInRange(Context context, int startDay, int endDay, String query){
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return Collections.emptyList();
        }

        //시작, 마감날자(julian day)를 uri에 붙여준다.
        Uri.Builder builder = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon();
        ContentUris.appendId(builder, startDay);
        ContentUris.appendId(builder, endDay);

        //Query함수를 실행하기 위해 파라메터들 설정
        final String[] projection = new String[]{
                CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.EVENT_COLOR,
                CalendarContract.Instances.TITLE, CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.DTSTART
        };  //Id, 형식, 제목, 설명, 시작시간, 마감시간, 위치, 하루종일, 시작시간(반복일정일때 첫일정의 시작시간, 아닐때는 BEGIN과 같음)

        //selection, selectionArgs 설정
        final String selection;
        final String[] selectionArgs;
        if(query == null || query.isEmpty()){
            selection = null;
            selectionArgs = null;
        }
        else {
            //제목들에서 검색어를 검색한다.
            selection = CalendarContract.Instances.TITLE + " LIKE ? ";
            selectionArgs = new String[] { "%" + query + "%" };
        }

        //sortOrder: 시작시간, 마감시간, 제목순서로 정렬
        String sort = "begin ASC, end DESC, title ASC";

        Cursor cursor = context.getContentResolver().query(
                builder.build(), projection, selection, selectionArgs, sort);

        assert cursor != null;
        cursor.moveToFirst();

        //일정들을 하나씩 얻어서 목록에 추가한다.
        List<OneEvent> eventList = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            //일정의 Id
            int Id = cursor.getInt(0);

            //일정형식
            int type = EventTypeManager.EVENT_TYPE_DEFAULT;
            if(!cursor.isNull(1)){

                int temp = cursor.getInt(1);
                for (int j = 0; j < APP_EVENT_TYPE_COUNT; j ++){
                    if (EventTypeManager.APP_EVENT_TYPES[j].id == temp){
                        type = temp;
                        break;
                    }
                }
            }

            //제목, 설명, 하루종일
            String title = cursor.getString(2);
            String description = cursor.getString(3);
            boolean allDay = cursor.getInt(4) != 0;

            //시작시간(begin)
            long milliSeconds = Long.parseLong(cursor.getString(5));
            final DateTime startDateTime;
            if(allDay)
                startDateTime = new DateTime(DateTimeZone.UTC).withMillis(milliSeconds);
            else
                startDateTime = new DateTime(milliSeconds);

            //마감시간(end)
            milliSeconds = Long.parseLong(cursor.getString(6));
            final DateTime endDateTime;
            if(allDay)
                endDateTime = new DateTime(DateTimeZone.UTC).withMillis(milliSeconds);
            else
                endDateTime = new DateTime(milliSeconds);

            //위치
            String location = cursor.getString(7);
            milliSeconds = Long.parseLong(cursor.getString(8));

            //시작시간(dtStart)
            DateTime realStartTime = new DateTime(milliSeconds);

            //일정들을 목록에 추가한다.
            eventList.add(new OneEvent(Id, type, title, description, startDateTime, endDateTime, location, allDay, realStartTime));
            cursor.moveToNext();
        }
        cursor.close();

        //결과목록을 돌려준다.
        return eventList;
    }

    /**
     * 다가오는 일정목록을 돌려준다
     * @param context
     * @param count 최대개수
     * @return 일정목록
     */
    public static List<OneEvent> getUpcomingEvents(Context context, int count){
        //달력에 대한 읽기/쓰기 권한이 부여되지 않았으면 빈 배렬을 돌려준다.
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return Collections.emptyList();
        }

        //Uri 설정
        Uri.Builder builder = CalendarContract.Instances.CONTENT_BY_DAY_URI.buildUpon();
        DateTime startTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);
        int startDay = Time.getJulianDay(startTime);    //오늘의 julian day얻기
        ContentUris.appendId(builder, startDay);            //오늘부터의
        ContentUris.appendId(builder, Integer.MAX_VALUE);   //일정들을 얻는다.

        //Query를 실행하기 위한 파라메터들 설정
        String[] projection = new String[] {
                CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.EVENT_COLOR,
                CalendarContract.Instances.TITLE, CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
                CalendarContract.Instances.EVENT_LOCATION, CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DTSTART
        };  //Id, 형식, 제목, 설명, 시작시간, 마감시간, 위치, 하루종일, 시작시간(반복일정일때 첫일정의 시작시간, 아닐때는 BEGIN과 같음)
        String selection = "begin > " + startTime.getMillis();  //현재시간이후
        String sortOrder = " begin LIMIT " + count;             //시작시간으로 정렬, 개수제한

        //Query실행
        Cursor cursor = context.getContentResolver().query(
                builder.build(), projection, selection, null,  sortOrder);

        assert cursor != null;
        cursor.moveToFirst();

        //일정들을 하나씩 얻어서 목록에 추가한다.
        List<OneEvent> upcomingEvents = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            //일정의 Id
            int Id = cursor.getInt(0);

            //일정형식
            int type = EventTypeManager.EVENT_TYPE_DEFAULT;
            if(!cursor.isNull(1)){
                int temp = cursor.getInt(1);
                for (int j = 0; j < APP_EVENT_TYPE_COUNT; j ++){
                    if(EventTypeManager.APP_EVENT_TYPES[j].id == temp){
                        type = temp;
                        break;
                    }
                }
            }

            //제목
            String title = cursor.getString(2);

            //설명
            String description = cursor.getString(3);

            //시작, 마감시간
            long startMillis = Long.parseLong(cursor.getString(4));
            long endMillis = Long.parseLong(cursor.getString(5));

            //위치
            String location = cursor.getString(6);

            //하루종일
            boolean allDay = cursor.getInt(7) != 0;

            //시작시간
            long originStartMillis = Long.parseLong(cursor.getString(8));

            //시간(미리초)를 DateTime클라스로 변환하여 얻는다.
            final DateTime startDateTime, endDateTime, realStartTime;
            if(allDay) {
                startDateTime = new DateTime(DateTimeZone.UTC).withMillis(startMillis);
                endDateTime = new DateTime(DateTimeZone.UTC).withMillis(endMillis).minusDays(1);
                realStartTime = new DateTime(DateTimeZone.UTC).withMillis(originStartMillis);
            }
            else {
                startDateTime = new DateTime().withMillis(startMillis);
                endDateTime = new DateTime().withMillis(endMillis);
                realStartTime = new DateTime().withMillis(originStartMillis);
            }

            //OneEvent 객체를 구성하여 목록에 추가한다.
            upcomingEvents.add(new OneEvent(Id, type, title, description, startDateTime, endDateTime, location, allDay, realStartTime));
            cursor.moveToNext();
        }
        cursor.close();

        //결과목록을 돌려준다.
        return upcomingEvents;
    }

    /**
     * 입력으로 들어오는 일정들가운데서 그날자에 있는 일정들을 목록으로 돌려준다.
     * @param events 일정목록
     * @param year   년
     * @param month  월
     * @param date   일
     * @return 일정목록
     */
    public static ArrayList<OneEvent> getEventsFromDate(List<OneEvent> events, int year, int month, int date){
        ArrayList<OneEvent> result = new ArrayList<>();
        for (OneEvent oneEvent : events){
            if(oneEvent.containsDate(year, month, date)){
                result.add(oneEvent);
            }
        }

        return result;
    }
}