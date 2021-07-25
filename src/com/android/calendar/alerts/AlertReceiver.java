/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar.alerts;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.provider.CalendarContract.Events;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.android.calendar.utils.Utils;
import com.android.calendar.alerts.AlertService.NotificationWrapper;

import java.util.ArrayList;
import java.util.regex.Pattern;

import com.android.krcalendar.R;

import static com.android.calendar.alerts.AlertService.ALERT_CHANNEL_ID;
import static com.android.calendar.alerts.AlertUtils.NOTIFICATION_ID_KEY;

/**
 * @see android.provider.CalendarContract#ACTION_EVENT_REMINDER
 * 미리알림동작 intent들을 받고 처리를 진행한다.
 * 실지 기본동작은 {@link AlertService}에서 진행한다.
 */
public class AlertReceiver extends BroadcastReceiver {

    // The broadcast for notification refreshes scheduled by the app. This is to
    // distinguish the EVENT_REMINDER broadcast sent by the provider.
    public static final String EVENT_REMINDER_APP_ACTION =
            "com.android.calendar.EVENT_REMINDER_APP";
    public static final String ACTION_DISMISS_OLD_REMINDERS = "removeOldReminders";
    static final Object mStartingServiceSync = new Object();
    private static final String TAG = "AlertReceiver";
    private static final String DELETE_ALL_ACTION = "com.android.calendar.DELETEALL";
    private static final Pattern mBlankLinePattern = Pattern.compile("^\\s*$[\n\r]",
            Pattern.MULTILINE);
    private static final int NOTIFICATION_DIGEST_MAX_LENGTH = 3;
    static PowerManager.WakeLock mStartingService;

    static {
        HandlerThread thr = new HandlerThread("AlertReceiver async");
        thr.start();
    }

    /**
     * Start the service to process the current event notifications, acquiring
     * the wake lock before returning to ensure that the service will run.
     */
    public static void beginStartingService(Context context, Intent intent) {
        synchronized (mStartingServiceSync) {
            if (mStartingService == null) {
                PowerManager pm =
                    (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "Etar:StartingAlertService");
                mStartingService.setReferenceCounted(false);
            }

            if(!Utils.getAlertIsEnabled(context))
                return;
            mStartingService.acquire();
            if (Utils.isOreoOrLater()) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }

        }
    }

    /**
     * Called back by the service when it has finished processing notifications,
     * releasing the wake lock if the service is now stopping.
     */
    public static void finishStartingService(Service service, int startId) {
        synchronized (mStartingServiceSync) {
            if (mStartingService != null) {
                if (service.stopSelfResult(startId)) {
                    mStartingService.release();
                }
            }
        }
    }

    private static PendingIntent createClickEventIntent(Context context, long eventId,
            long startMillis, long endMillis, int notificationId) {
        return createDismissAlarmsIntent(context, eventId, startMillis, endMillis, notificationId,
                "com.android.calendar.CLICK", true);
    }

    private static PendingIntent createDeleteEventIntent(Context context, long eventId,
            long startMillis, long endMillis, int notificationId) {
        return createDismissAlarmsIntent(context, eventId, startMillis, endMillis, notificationId,
                "com.android.calendar.DELETE", false);
    }

    private static PendingIntent createDismissAlarmsIntent(Context context, long eventId,
            long startMillis, long endMillis, int notificationId, String action,
            boolean showEvent) {
        Intent intent = new Intent();
        intent.setClass(context, DismissAlarmsService.class);
        intent.putExtra(AlertUtils.EVENT_ID_KEY, eventId);
        intent.putExtra(AlertUtils.EVENT_START_KEY, startMillis);
        intent.putExtra(AlertUtils.EVENT_END_KEY, endMillis);
        intent.putExtra(AlertUtils.SHOW_EVENT_KEY, showEvent);
        intent.putExtra(NOTIFICATION_ID_KEY, notificationId);

        // Must set a field that affects Intent.filterEquals so that the resulting
        // PendingIntent will be a unique instance (the 'extras' don't achieve this).
        // This must be unique for the click event across all reminders (so using
        // event ID + startTime should be unique).  This also must be unique from
        // the delete event (which also uses DismissAlarmsService).
        Uri.Builder builder = Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, eventId);
        ContentUris.appendId(builder, startMillis);
        intent.setData(builder.build());
        intent.setAction(action);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent createSnoozeIntent(Context context, long eventId,
            long startMillis, long endMillis, int notificationId) {
        Intent intent = new Intent();
        intent.putExtra(AlertUtils.EVENT_ID_KEY, eventId);
        intent.putExtra(AlertUtils.EVENT_START_KEY, startMillis);
        intent.putExtra(AlertUtils.EVENT_END_KEY, endMillis);
        intent.putExtra(NOTIFICATION_ID_KEY, notificationId);

        Uri.Builder builder = Events.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, eventId);
        ContentUris.appendId(builder, startMillis);
        intent.setData(builder.build());

        if (Utils.useCustomSnoozeDelay(context)) {
            intent.setClass(context, SnoozeDelayActivity.class);
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            intent.setClass(context, SnoozeAlarmsService.class);
            return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    private static PendingIntent createAlertActivityIntent(Context context) {
        Intent clickIntent = new Intent();
        clickIntent.setClass(context, AlertActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, clickIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static NotificationWrapper makeBasicNotification(Context context, String title,
                                                            String summaryText, long startMillis, long endMillis, long eventId,
                                                            int notificationId) {
        Notification n = buildBasicNotification(new Notification.Builder(context),
                context, title, summaryText, startMillis, endMillis, eventId, notificationId,
                false);
        return new NotificationWrapper(n, notificationId, eventId, startMillis, endMillis);
    }

    /**
     * 일정알림 Notification을 생성한다.
     * @param notificationBuilder NotificationBuilder
     * @param context Context
     * @param title Notification 제목
     * @param summaryText Notification 내용
     * @param startMillis 시작시간(미리초)
     * @param endMillis 마감시간(미리초)
     * @param eventId 일정 Id
     * @param notificationId  Notification Id
     * @param addActionButtons Action단추 (불필요한것은 없애다보니 Snooze 단추밖에 없음)를 추가하겠는가?
     * @return 생성된 Notification을 돌려준다.
     */
    private static Notification buildBasicNotification(Notification.Builder notificationBuilder,
                                                       Context context, String title, String summaryText, long startMillis, long endMillis,
                                                       long eventId, int notificationId,
                                                       boolean addActionButtons) {

        //제목이 비였으면 "제목 없음"이라는 문자렬을 제목으로 설정한다.
        Resources resources = context.getResources();
        if (title == null || title.length() == 0) {
            title = resources.getString(R.string.no_title_label);
        }

        //Click 했을때의 동작 Intent
        PendingIntent clickIntent = createClickEventIntent(context, eventId, startMillis,
                endMillis, notificationId);

        //Notification 이 사라질때의 동작 Intent
        PendingIntent deleteIntent = createDeleteEventIntent(context, eventId, startMillis,
            endMillis, notificationId);

        //Notification구축을 위한 초기설정들을 진행한다.
        notificationBuilder.setContentTitle(title);         //제목
        notificationBuilder.setContentText(summaryText);    //내용
        notificationBuilder.setSmallIcon(R.drawable.stat_notify_calendar);  //아이콘
        int color = context.getColor(R.color.small_icon_color); //색갈얻기 (푸른색)
        notificationBuilder.setColor(color);    //색갈

        //Timestamp 없애기
        notificationBuilder.setWhen(0);

        //Intent 들을 추가
        notificationBuilder.setContentIntent(clickIntent);
        notificationBuilder.setDeleteIntent(deleteIntent);
        notificationBuilder.setFullScreenIntent(createAlertActivityIntent(context), true);

        PendingIntent snoozeIntent = null;
        if (addActionButtons) {
            //Snooze 단추 동작 intent 를 정의하고 Notification에 단추를 추가한다.
            snoozeIntent = createSnoozeIntent(context, eventId, startMillis, endMillis,
                    notificationId);
            notificationBuilder.addAction(new Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_alarm_holo_dark),
                    context.getString(R.string.snooze_label),
                    snoozeIntent
            ).build());
        }

        //닫기단추를 Notification에 추가한다.
        notificationBuilder.addAction(new Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_alarm_holo_dark),
                context.getString(R.string.close_label),
                deleteIntent    //닫기단추의 동작은 deleteIntent 를 같이 리용한다.
        ).build());

        return notificationBuilder.build();
    }

    /**
     * Creates an expanding notification.  The initial expanded state is decided by
     * the notification manager based on the priority.
     */
    public static NotificationWrapper makeExpandingNotification(Context context, String title,
                                                                String summaryText, String description, long startMillis, long endMillis, long eventId,
                                                                int notificationId) {

        //Make a basic notification builder
        Notification.Builder basicBuilder = new Notification.Builder(context, ALERT_CHANNEL_ID);
        buildBasicNotification(basicBuilder, context, title,
                summaryText, startMillis, endMillis, eventId, notificationId,
                true);

        // Create a new-style expanded notification
        Notification.BigTextStyle expandedBuilder = new Notification.BigTextStyle();
        expandedBuilder.setBuilder(basicBuilder);

        //Set text
        if (description != null) {
            description = mBlankLinePattern.matcher(description).replaceAll("");
            description = description.trim();
        }
        CharSequence text;
        if (TextUtils.isEmpty(description)) {
            text = summaryText;
        } else {
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append(summaryText);
            stringBuilder.append("\n\n");
            stringBuilder.setSpan(new RelativeSizeSpan(0.5f), summaryText.length(),
                    stringBuilder.length(), 0);
            stringBuilder.append(description);
            text = stringBuilder;
        }
        expandedBuilder.bigText(text);

        //Return builder's output
        Notification notification = expandedBuilder.build();

        return new NotificationWrapper(notification, notificationId, eventId, startMillis,
                endMillis);
    }

    /**
     * Creates an expanding digest notification for expired events.
     */
    public static NotificationWrapper makeDigestNotification(Context context,
            ArrayList<AlertService.NotificationInfo> notificationInfos, String digestTitle,
            boolean expandable) {
        if (notificationInfos == null || notificationInfos.size() < 1) {
            return null;
        }

        Resources res = context.getResources();
        int numEvents = notificationInfos.size();
        long[] eventIds = new long[notificationInfos.size()];
        long[] startMillis = new long[notificationInfos.size()];
        for (int i = 0; i < notificationInfos.size(); i++) {
            eventIds[i] = notificationInfos.get(i).eventId;
            startMillis[i] = notificationInfos.get(i).startMillis;
        }

        // Create an intent triggered by clicking on the status icon that shows the alerts list.
        PendingIntent pendingClickIntent = createAlertActivityIntent(context);

        // Create an intent triggered by dismissing the digest notification that clears all
        // expired events.
        Intent deleteIntent = new Intent();
        deleteIntent.setClass(context, DismissAlarmsService.class);
        deleteIntent.setAction(DELETE_ALL_ACTION);
        deleteIntent.putExtra(AlertUtils.EVENT_IDS_KEY, eventIds);
        deleteIntent.putExtra(AlertUtils.EVENT_STARTS_KEY, startMillis);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(context, 0, deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (digestTitle == null || digestTitle.length() == 0) {
            digestTitle = res.getString(R.string.no_title_label);
        }

        Notification.Builder notificationBuilder = new Notification.Builder(context);
        notificationBuilder.setContentText(digestTitle);
        notificationBuilder.setSmallIcon(R.drawable.stat_notify_calendar_multiple);
        notificationBuilder.setContentIntent(pendingClickIntent);
        notificationBuilder.setDeleteIntent(pendingDeleteIntent);
        String nEventsStr = res.getQuantityString(R.plurals.Nevents, numEvents, numEvents);
        notificationBuilder.setContentTitle(nEventsStr);

        Notification n;
        // New-style notification...

        // Set to min priority to encourage the notification manager to collapse it.
        notificationBuilder.setPriority(Notification.PRIORITY_MIN);

        if (expandable) {
            // Multiple reminders.  Combine into an expanded digest notification.
            Notification.InboxStyle expandedBuilder = new Notification.InboxStyle(
                    notificationBuilder);
            int i = 0;
            for (AlertService.NotificationInfo info : notificationInfos) {
                if (i < NOTIFICATION_DIGEST_MAX_LENGTH) {
                    String name = info.eventName;
                    if (TextUtils.isEmpty(name)) {
                        name = context.getResources().getString(R.string.no_title_label);
                    }
                    String timeLocation = AlertUtils.formatTimeLocation(context,
                            info.startMillis, info.allDay, info.location);

                    TextAppearanceSpan primaryTextSpan = new TextAppearanceSpan(context,
                            R.style.NotificationPrimaryText);
                    TextAppearanceSpan secondaryTextSpan = new TextAppearanceSpan(context,
                            R.style.NotificationSecondaryText);

                    // Event title in bold.
                    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                    stringBuilder.append(name);
                    stringBuilder.setSpan(primaryTextSpan, 0, stringBuilder.length(), 0);
                    stringBuilder.append("  ");

                    // Followed by time and location.
                    int secondaryIndex = stringBuilder.length();
                    stringBuilder.append(timeLocation);
                    stringBuilder.setSpan(secondaryTextSpan, secondaryIndex,
                            stringBuilder.length(), 0);
                    expandedBuilder.addLine(stringBuilder);
                    i++;
                } else {
                    break;
                }
            }

            // If there are too many to display, add "+X missed events" for the last line.
            int remaining = numEvents - i;
            if (remaining > 0) {
                String nMoreEventsStr = res.getQuantityString(R.plurals.N_remaining_events,
                            remaining, remaining);
                // TODO: Add highlighting and icon to this last entry once framework allows it.
                expandedBuilder.setSummaryText(nMoreEventsStr);
            }

            // Remove the title in the expanded form (redundant with the listed items).
            expandedBuilder.setBigContentTitle("");

            n = expandedBuilder.build();
        } else {
            n = notificationBuilder.build();
        }

        NotificationWrapper nw = new NotificationWrapper(n);
        if (AlertService.DEBUG) {
            for (AlertService.NotificationInfo info : notificationInfos) {
                nw.add(new NotificationWrapper(null, 0, info.eventId, info.startMillis,
                        info.endMillis));
            }
        }
        return nw;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (AlertService.DEBUG) {
            Log.d(TAG, "onReceive: a=" + intent.getAction() + " " + intent.toString());
        }

        String action = intent.getAction();
        if (DELETE_ALL_ACTION.equals(action)) {

            // The user has dismissed a digest notification.
            // TODO Grab a wake lock here?
            Intent serviceIntent = new Intent(context, DismissAlarmsService.class);
            context.startService(serviceIntent);
        }
        else {
            Intent i = new Intent();
            i.setClass(context, AlertService.class);
            i.putExtras(intent);
            i.putExtra("action", intent.getAction());
            Uri uri = intent.getData();

            if (uri != null) {
                i.putExtra("uri", uri.toString());
            }
            beginStartingService(context, i);
        }
    }

}
