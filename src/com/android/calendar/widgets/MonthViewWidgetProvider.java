package com.android.calendar.widgets;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.RemoteViews;

import com.android.calendar.event.EventManager;

import org.joda.time.DateTime;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import com.android.krcalendar.R;

import static com.android.calendar.widgets.EventViewWidgetProvider.NEXT_NOT_UPDATE;

/**
 * 월 Widget을 위한 AppWidgetProvider
 */
public class MonthViewWidgetProvider extends AppWidgetProvider {

    public static final String WIDGET_IDS_KEY ="monthwidgetproviderwidgetids";

    //가로, 세로 날자수
    private static final int DAYS_PER_ROW = 7;
    private static final int DAYS_ROW_COUNT = 6;

    //`뒤로`, `앞으로` 단추들의 action들
    private static final String PREV = "prev";
    private static final String NEXT = "next";

    private final SimpleMonthDate[] mMonthDateArray = new SimpleMonthDate[DAYS_PER_ROW * DAYS_ROW_COUNT];
    private static DateTime mTargetDate = DateTime.now().withDayOfMonth(1);

    @SuppressLint("DefaultLocale")
    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId)    {

        //Widget설정을 위한 RemoteViews 구축
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.month_view_widget);

        //`앞으로`, `뒤로`단추들의 동작들 추가
        setupIntent(context, views, PREV, R.id.go_prev);
        setupIntent(context, views, NEXT, R.id.go_next);

        //년.월 label 설정
        String monthString = String.format("%1$d.%2$02d", mTargetDate.getYear(), mTargetDate.getMonthOfYear());
        views.setTextViewText(R.id.widget_current_month, monthString);

        //한달일정 얻기
        List<EventManager.OneEvent> eventList = EventManager.getEvents(context, mTargetDate.getMillis(), EventManager.MONTH);

        //날자들을 얻기
        int i;
        for (i = 0; i < DAYS_PER_ROW * DAYS_ROW_COUNT; i ++){
            mMonthDateArray[i] = new SimpleMonthDate();
        }

        int start_date_week_day = mTargetDate.getDayOfWeek() % 7;
        YearMonth yearMonth = YearMonth.of(mTargetDate.getYear(), mTargetDate.getMonthOfYear());
        int month_day_count = yearMonth.lengthOfMonth();

        //전달의 날자들
        for (i = 0; i < start_date_week_day; i ++){
            mMonthDateArray[i].otherMonth = true;
        }

        //그달의 날자들
        for (i = start_date_week_day; i < start_date_week_day + month_day_count; i ++){
            mMonthDateArray[i].otherMonth = false;
            mMonthDateArray[i].dateTime = mTargetDate.plusDays(i - start_date_week_day);

            //날자에 해당한 일정들을 얻는다.
            List<EventManager.OneEvent> dayEvents = EventManager.getEventsFromDate(eventList, mMonthDateArray[i].dateTime.getYear(),
                    mMonthDateArray[i].dateTime.getMonthOfYear(), mMonthDateArray[i].dateTime.getDayOfMonth());

            if(dayEvents.size() == 0) { //일정이 없을때
                mMonthDateArray[i].hasEvent = false;
            } else {    //일정이 있을때
                mMonthDateArray[i].hasEvent = true;
                mMonthDateArray[i].allPastEvents = true;
                for (EventManager.OneEvent event:dayEvents){
                    //과거의 일정들만 포함하고 있는가를 검사하고 allPastEvents에 설정한다.
                    if(!event.pastOrFutureCurrent()){
                        mMonthDateArray[i].allPastEvents = false;
                        break;
                    }
                }
            }
        }

        //다음달의 날자들
        for (i = start_date_week_day + month_day_count; i < DAYS_PER_ROW * DAYS_ROW_COUNT; i ++){
            mMonthDateArray[i].otherMonth = true;
        }

        //일정, 날자들을 얻은다음 UI에 반영한다.
        DateTime dateTime = DateTime.now();
        for (i = 0; i < DAYS_PER_ROW * DAYS_ROW_COUNT; i ++){
            SimpleMonthDate monthDate = mMonthDateArray[i];
            int day_id = getDayIdentifier(i + 1, context);
            int event_id = getEventIdentifier(i + 1, context);

            if(monthDate.otherMonth){ //다른 달의 날자들은 현시해주지 않는다.
                views.setViewVisibility(day_id, View.INVISIBLE);
                views.setViewVisibility(event_id, View.INVISIBLE);
                continue;
            }

            views.setViewVisibility(day_id, View.VISIBLE);
            views.setTextViewText(day_id, monthDate.dateTime.getDayOfMonth() + "");

            //오늘 날자에는 동그라미배경을 않힌다.
            if(dateTime.getYear() == monthDate.dateTime.getYear() &&
                    dateTime.getMonthOfYear() == monthDate.dateTime.getMonthOfYear() &&
                    dateTime.getDayOfMonth() == monthDate.dateTime.getDayOfMonth()) {
                views.setInt(day_id, "setBackgroundResource", R.drawable.month_widget_today_bg);
            } else {
                views.setInt(day_id, "setBackgroundResource", 0);
            }

            //날자에 일정이 있는 경우에는 날자아래의 동그라미에 해당한 ImageView를 보여준다.(VISIBLE 설정)
            if(monthDate.hasEvent) {
                views.setViewVisibility(event_id, View.VISIBLE);
                if(monthDate.allPastEvents) {   //과거일정만 가지고 있을때
                    views.setInt(event_id, "setColorFilter", context.getColor(R.color.colorPastEvent));
                } else {    //현재 진행중, 혹은 앞으로의 일정을 하나라도 가지고 있을때
                    views.setInt(event_id, "setColorFilter", context.getColor(R.color.colorFutureEvent));
                }
            } else {
                views.setViewVisibility(event_id, View.INVISIBLE);
            }
        }

        //Widget 설정
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Widget이 여러개 있을수 있다. 모든 widget들을 다 갱신한다.
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        //다음번 갱신을 예정해놓는다.
        scheduleNextUpdate(context, appWidgetIds);
    }

    public void onNextUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, boolean nextUpdate) {
        //Widget이 여러개 있을수 있다. 모든 widget들을 다 갱신한다.
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        if(nextUpdate)  //다음번 갱신을 예정해놓는다.
            scheduleNextUpdate(context, appWidgetIds);
    }

    /**
     * 다음번의 Widget갱신을 예약
     * @param context Context
     * @param appWidgetIds Int[]
     */
    private static void scheduleNextUpdate(Context context, int[] appWidgetIds) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        //Widget을 갱신하는 intent창조
        Intent intent = new Intent(context, MonthViewWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(WIDGET_IDS_KEY, appWidgetIds);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        //다음 분에 해당한 시간 얻기
        DateTime dateTime = DateTime.now().withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime nextMinute = dateTime.plusMinutes(1);

        //다음 분에 widget이 갱신되도록 alarm 설정
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextMinute.getMillis(), pendingIntent);
    }

    /**
     * 단추를 비롯한 view의 click 동작을 위한 intent설정
     * @param context Context
     * @param views RemoteViews
     * @param action String
     * @param id Int
     */
    private static void setupIntent(Context context, RemoteViews views, String action, int id) {
        Intent intent = new Intent(context, MonthViewWidgetProvider.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        views.setOnClickPendingIntent(id, pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        assert intent.getAction() != null;

        //action 검사
        switch (intent.getAction()){
            case PREV:  //`이전`단추를 눌렀을때
                //이전달을 보여준다.
                mTargetDate = mTargetDate.minusMonths(1);
                performUpdate(context);
                break;
            case NEXT:  //`다음`단추를 눌렀을때
                //다음달을 보여준다.
                mTargetDate = mTargetDate.plusMonths(1);
                performUpdate(context);
                break;
            case AppWidgetManager.ACTION_APPWIDGET_UPDATE:  //Widget갱신
                if(intent.hasExtra(WIDGET_IDS_KEY)){    //App내부에서 프로그람적으로 보내온것
                    int[] ids = Objects.requireNonNull(intent.getExtras()).getIntArray(WIDGET_IDS_KEY);
                    assert ids != null;

                    //Widget을 갱신한다.
                    boolean nextUpdate = !intent.hasExtra(NEXT_NOT_UPDATE);
                    this.onNextUpdate(context, AppWidgetManager.getInstance(context), ids, nextUpdate);
                }
                else {  //체계에서 보내온것
                    super.onReceive(context, intent);
                }
                break;
            default:
                super.onReceive(context, intent);
        }
    }

    /**
     * 이전/다음 단추를 눌렀을때 호출되는 Widget갱신함수
     * @param context Context
     */
    private void performUpdate(Context context){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = getComponentName(context);

        for (int appWidgetId : appWidgetManager.getAppWidgetIds(componentName)) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * 날자를 보여주는 TextView의 Id를 돌려준다
     * @see R.id#day_01
     * @param dayNumber 날자(1-31)
     * @param context Context
     */
    int getDayIdentifier(int dayNumber, Context context){
        @SuppressLint("DefaultLocale") String idName = String.format("day_%1$02d", dayNumber);
        Resources resources = context.getResources();
        return resources.getIdentifier(idName, "id", context.getPackageName());
    }

    /**
     * 일정동그라미를 보여주는 ImageView의 Id를 돌려준다
     * @see R.id#event_01
     * @param dayNumber 날자(1-31)
     * @param context Context
     */
    int getEventIdentifier(int dayNumber, Context context){
        @SuppressLint("DefaultLocale") String idName = String.format("event_%1$02d", dayNumber);
        Resources resources = context.getResources();
        return resources.getIdentifier(idName, "id", context.getPackageName());
    }

    private ComponentName getComponentName(Context context){
        return new ComponentName(context, MonthViewWidgetProvider.class);
    }

    /**
     * 월 Widget에서 한개 날자를 그려주는데 리용되는 Model클라스
     */
    private static class SimpleMonthDate {
        boolean otherMonth = false;     //다른 달인가?
        boolean allPastEvents = false;  //과거의 일정만 가지고 있는가?
        boolean hasEvent = false;       //일정이 있는가?
        DateTime dateTime;              //날자
    }
}

