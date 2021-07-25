package com.android.calendar.widgets;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

import com.android.calendar.event.EventInfoActivity;
import com.android.calendar.event.EventTypeManager;
import com.android.calendar.utils.Utils;
import com.android.calendar.event.EditEventActivity;
import com.android.calendar.event.EventManager;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Objects;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import com.android.krcalendar.R;

/**
 * 일정 Widget을 위한 AppWidgetProvider
 */
public class EventViewWidgetProvider extends AppWidgetProvider {

    public static final String WIDGET_IDS_KEY ="event_widget_provider_widget_ids";
    public static final String NEXT_NOT_UPDATE ="next_not_update";
    private static final String CREATE_EVENT = "create_event";
    private static final String VIEW_EVENT = "edit_event";

    //앞으로의 첫 일정정보를 보관하기 위한 Preference Key 값
    public static final String KEY_EVENT_ID = "key-event-id";
    public static final String KEY_EVENT_START_MILLIS = "key-event-start-millis";
    public static final String KEY_EVENT_END_MILLIS = "key-event-end-millis";

    @SuppressLint("DefaultLocale")
    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId) {

        //RemoteViews 구축
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.event_view_widget);

        setupIntent(context, views, CREATE_EVENT, R.id.create_event);
        setupIntent(context, views, VIEW_EVENT, R.id.result_events);
        setupIntent(context, views, VIEW_EVENT, R.id.event_image);

        //앞으로의 일정을 얻는다.(최대 1개)
        List<EventManager.OneEvent> upcomingEvents = EventManager.getUpcomingEvents(context, 1);
        if(upcomingEvents.size() == 0){ //앞으로의 일정이 없을때
            //`일정없음`을 보여준다
            views.setTextViewText(R.id.event_day, getDayString(context, DateTime.now()));
            views.setViewVisibility(R.id.no_upcoming_events, VISIBLE);
            views.setViewVisibility(R.id.result_events, GONE);
            views.setImageViewResource(R.id.event_image, R.drawable.ic_default_icon);
            views.setInt(R.id.event_image,"setColorFilter", Color.WHITE);
        } else{   //앞으로의 일정이 있을때
            //첫 일정을 보여준다.
            views.setViewVisibility(R.id.no_upcoming_events, GONE);
            views.setViewVisibility(R.id.result_events, VISIBLE);

            //일정을 얻는다.
            EventManager.OneEvent event = upcomingEvents.get(0);

            //제목 label 설정
            if(event.title == null || event.title.isEmpty())
                views.setTextViewText(R.id.event_title, context.getResources().getString(R.string.no_title_label));
            else
                views.setTextViewText(R.id.event_title, event.title);

            //날자 label 설정
            views.setTextViewText(R.id.event_day, getDayString(context, event.startTime));

            //일정시간 문자렬을 얻어서 설정한다.
            final String dateTimeString;
            if(event.allDay) { //`하루종일`일정일때
                dateTimeString = String.format("%d.%d - %d.%d",
                        event.startTime.getMonthOfYear(), event.startTime.getDayOfMonth(),
                        event.endTime.getMonthOfYear(), event.endTime.getDayOfMonth());
            }
            else {  //`하루종일`일정이 아닐때
                //시작시간, 마감시간이 같은 날에 있을때
                if(event.startTime.getYear() == event.endTime.getYear() && event.startTime.getMonthOfYear() == event.endTime.getMonthOfYear() &&
                        event.startTime.getDayOfMonth() == event.endTime.getDayOfMonth()) {
                    dateTimeString = String.format("%d.%d %02d:%02d - %02d:%02d",
                            event.startTime.getMonthOfYear(), event.startTime.getDayOfMonth(),
                            event.startTime.getHourOfDay(), event.startTime.getMinuteOfHour(),
                            event.endTime.getHourOfDay(), event.endTime.getMinuteOfHour());
                }

                //시작시간, 마감시간이 다른 날에 있을때
                else {
                    dateTimeString = String.format("%d.%d %02d:%02d - %d.%d %02d:%02d",
                            event.startTime.getMonthOfYear(), event.startTime.getDayOfMonth(),
                            event.startTime.getHourOfDay(), event.startTime.getMinuteOfHour(),
                            event.endTime.getMonthOfYear(), event.endTime.getDayOfMonth(),
                            event.endTime.getHourOfDay(), event.endTime.getMinuteOfHour());
                }
            }
            views.setTextViewText(R.id.event_time, dateTimeString);

            //일정형식 얻기
            int eventTypeValue = event.type;
            EventTypeManager.OneEventType eventType = EventTypeManager.getEventTypeFromId(eventTypeValue);

            //일정화상 설정
            int eventImage = eventType.imageResource;
            int eventColor = eventType.color;
            views.setImageViewResource(R.id.event_image, eventImage);
            views.setInt(R.id.event_image,"setColorFilter", context.getColor(eventColor));

            //첫 일정을 Preference에 보관한다.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_EVENT_ID, event.id);
            editor.putLong(KEY_EVENT_START_MILLIS, event.startTime.getMillis());
            editor.putLong(KEY_EVENT_END_MILLIS, event.endTime.getMillis());
            editor.apply();
        }

        //Widget을 갱신한다.
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Widget이 여러개 있을수 있다. 모든 widget들을 다 갱신한다.
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        scheduleNextUpdate(context, appWidgetIds);
    }

    public void onNextUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, boolean nextUpdate) {
        //Widget이 여러개 있을수 있다. 모든 widget들을 다 갱신한다.
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        if(nextUpdate)
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
        Intent intent = new Intent(context, EventViewWidgetProvider.class);
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
        Intent intent = new Intent(context, EventViewWidgetProvider.class);
        intent.setAction(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        views.setOnClickPendingIntent(id, pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        assert intent.getAction() != null;

        switch (intent.getAction()) {
            case CREATE_EVENT:  //`+`단추를 눌렀을때
                //일정창조화면을 펼친다.
                Intent createIntent = new Intent(Intent.ACTION_VIEW);
                createIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                createIntent.setClass(context, EditEventActivity.class);
                context.getApplicationContext().startActivity(createIntent);
                break;

            case VIEW_EVENT:    //일정을 눌렀을때
                //SharedPreferences 에 보관했던 일정정보(Id, 시작, 마감시간)을 불러온다.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                int eventId = prefs.getInt(KEY_EVENT_ID, -1);
                long startMillis = prefs.getLong(KEY_EVENT_START_MILLIS, 0);
                long endMillis = prefs.getLong(KEY_EVENT_END_MILLIS, 0);

                //일정보기화면을 펼친다.
                if(eventId != -1) {
                    Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
                    Intent infoIntent = new Intent(Intent.ACTION_VIEW, uri);
                    infoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    infoIntent.putExtra(EXTRA_EVENT_BEGIN_TIME, startMillis);
                    infoIntent.putExtra(EXTRA_EVENT_END_TIME, endMillis);
                    infoIntent.setClass(context, EventInfoActivity.class);
                    context.getApplicationContext().startActivity(infoIntent);
                }
                break;

            case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
                if(intent.hasExtra(WIDGET_IDS_KEY)) {
                    int[] ids = Objects.requireNonNull(intent.getExtras()).getIntArray(WIDGET_IDS_KEY);
                    assert ids != null;
                    boolean nextUpdate = !intent.hasExtra(NEXT_NOT_UPDATE);
                    this.onNextUpdate(context, AppWidgetManager.getInstance(context), ids, nextUpdate);
                } else {
                    super.onReceive(context, intent);
                }
                break;
            default:
                super.onReceive(context, intent);
                break;
        }
    }

    /**
     * `년.월.일` 형식의 날자문자렬을 돌려준다
     * @param context Context
     * @param dateTime 날자
     */
    private static String getDayString(Context context, DateTime dateTime){
        @SuppressLint("DefaultLocale") String dateString = String.format("%1$d.%2$d.%3$d", dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
        String weekDayString = Utils.getWeekDayString(context, dateTime.getDayOfWeek(), false);
        return dateString + "   " + weekDayString;
    }
}