/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.calendar.event;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.android.calendar.event.CalendarEventModel.ReminderEntry;

import java.util.ArrayList;

import com.android.krcalendar.R;

/**
 * 미리알림설정과 관련한 함수들을 가지고 있는 helper클라스
 */
public class EventViewUtils {
    //미리알림최대개수를 1개로 정하였다.
    //다시말하여 미리알림이 1개만 있거나 없다.
    public static final int DEFAULT_MAX_REMINDERS = 1;
    private static final String TAG = "EventViewUtils";

    private EventViewUtils() {
    }

    /**
     * 미리알림분으로부터 문자렬계산하여 돌려준다.
     * @param context Context
     * @param minutes 분수
     * @param abbrev 긴 문자렬/짥은 문자렬? true: 1 min, false; 1 minute 형식
     */
    public static String constructReminderLabel(Context context, int minutes, boolean abbrev) {
        Resources resources = context.getResources();
        int value, resId;

        if (minutes % 60 != 0) {
            value = minutes;
            if (abbrev) {
                resId = R.plurals.Nmins;
            } else {
                resId = R.plurals.Nminutes;
            }
        } else if (minutes % (24 * 60) != 0) {
            value = minutes / 60;
            resId = R.plurals.Nhours;
        } else {
            value = minutes / (24 * 60);
            resId = R.plurals.Ndays;
        }

        String format = resources.getQuantityString(resId, value);
        return String.format(format, value);
    }

    /**
     * 목록에서 분수값을 찾아서 인수를 돌려준다
     * @param values 목록
     * @param minutes 분수
     */
    public static int findMinutesInReminderList(ArrayList<Integer> values, int minutes) {
        int index = values.indexOf(minutes);
        if (index == -1) {
            Log.e(TAG, "Cannot find minutes (" + minutes + ") in list");
            return 0;
        }
        return index;
    }

    /**
     * 우의 함수와 같은 동작
     */
    public static int findMethodInReminderList(ArrayList<Integer> values, int method) {
        int index = values.indexOf(method);
        if (index == -1) {
            index = 0;
        }
        return index;
    }

    /**
     * ReminderEntry객체 목록으로부터 Reminder목록 얻어서 돌려준다
     * @param reminderItems 미리알림들을 포함하고 있는 LinearLayout
     * @param reminderMinuteValues 분목록
     * @param reminderMethodValues 방식목록
     */
    public static ArrayList<ReminderEntry> reminderItemsToReminders(
            ArrayList<LinearLayout> reminderItems, ArrayList<Integer> reminderMinuteValues,
            ArrayList<Integer> reminderMethodValues) {
        int len = reminderItems.size();
        ArrayList<ReminderEntry> reminders = new ArrayList<ReminderEntry>(len);
        for (int index = 0; index < len; index++) {
            LinearLayout layout = reminderItems.get(index);
            Spinner minuteSpinner = (Spinner) layout.findViewById(R.id.reminder_minutes_value);
            Spinner methodSpinner = (Spinner) layout.findViewById(R.id.reminder_method_value);
            int minutes = reminderMinuteValues.get(minuteSpinner.getSelectedItemPosition());
            int method = reminderMethodValues.get(methodSpinner.getSelectedItemPosition());
            reminders.add(ReminderEntry.valueOf(minutes, method));
        }
        return reminders;
    }

    /**
     * 미리알림목록에 분추가
     * @param context
     * @param values
     * @param labels
     * @param minutes
     */
    public static void addMinutesToList(Context context, ArrayList<Integer> values,
            ArrayList<String> labels, int minutes) {
        //이미 있으면 추가동작을 안한다.
        int index = values.indexOf(minutes);
        if (index != -1) {
            return;
        }

        //Label을 얻는다.
        String label = constructReminderLabel(context, minutes, false);
        int len = values.size();
        for (int i = 0; i < len; i++) {
            if (minutes < values.get(i)) {
                values.add(i, minutes);
                labels.add(i, label);
                return;
            }
        }

        //추가한다.
        values.add(minutes);
        labels.add(len, label);
    }

    /**
     * 허용안된 미리알림방식들을 없앤다.
     * @param values ArrayList<Integer>
     * @param labels ArrayList<String>
     * @param allowedMethods 허용된 미리알림방식
     */
    public static void reduceMethodList(ArrayList<Integer> values, ArrayList<String> labels,
            String allowedMethods)
    {
        String[] allowedStrings = allowedMethods.split(",");
        int[] allowedValues = new int[allowedStrings.length];

        for (int i = 0; i < allowedValues.length; i++) {
            try {
                allowedValues[i] = Integer.parseInt(allowedStrings[i], 10);
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "Bad allowed-strings list: '" + allowedStrings[i] +
                        "' in '" + allowedMethods + "'");
                return;
            }
        }

        //허용안된 항목들을 목록에서 없애기
        for (int i = values.size() - 1; i >= 0; i--) {
            int val = values.get(i);
            int j;

            for (j = allowedValues.length - 1; j >= 0; j--) {
                if (val == allowedValues[j]) {
                    break;
                }
            }
            if (j < 0) {
                values.remove(i);
                labels.remove(i);
            }
        }
    }

    /**
     * 미리알림목록을 Spinner View에 반영
     * @param activity
     * @param spinner
     * @param labels
     * @param fromEventView
     */
    private static void setReminderSpinnerLabels(Activity activity, Spinner spinner,
            ArrayList<String> labels, boolean fromEventView) {
        Resources res = activity.getResources();
        spinner.setPrompt(res.getString(R.string.reminders_label));

        final int resource;

        if(fromEventView)
            resource = R.layout.spinner_item_for_view;
        else
            resource = R.layout.spinner_item_for_edit;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, resource, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     * 미리알림추가(`미리알림추가`단추를 눌렀을때 호출된다)
     * @param activity
     * @param view
     * @param listener
     * @param items
     * @param minuteValues
     * @param minuteLabels
     * @param methodValues
     * @param methodLabels
     * @param newReminder
     * @param maxReminders
     * @param fromEventView
     * @param onItemSelected
     * @return
     */
    public static boolean addReminder(Activity activity, View view, View.OnClickListener listener,
            ArrayList<LinearLayout> items, ArrayList<Integer> minuteValues,
            ArrayList<String> minuteLabels, ArrayList<Integer> methodValues,
            ArrayList<String> methodLabels, ReminderEntry newReminder, int maxReminders,
            boolean fromEventView,
            OnItemSelectedListener onItemSelected) {

        if (items.size() >= maxReminders) {
            return false;
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        LinearLayout parent = (LinearLayout) view.findViewById(R.id.reminder_items_container);
        LinearLayout reminderItem = (LinearLayout) inflater.inflate(R.layout.edit_reminder_item,
                null);
        parent.addView(reminderItem);

        ImageButton reminderRemoveButton;
        reminderRemoveButton = (ImageButton) reminderItem.findViewById(R.id.reminder_remove);
        reminderRemoveButton.setOnClickListener(listener);

        Spinner spinner = (Spinner) reminderItem.findViewById(R.id.reminder_minutes_value);
        setReminderSpinnerLabels(activity, spinner, minuteLabels, fromEventView);

        int index = findMinutesInReminderList(minuteValues, newReminder.getMinutes());
        spinner.setSelection(index);

        if (onItemSelected != null) {
            spinner.setTag(index);
            spinner.setOnItemSelectedListener(onItemSelected);
        }

        spinner = (Spinner) reminderItem.findViewById(R.id.reminder_method_value);
        setReminderSpinnerLabels(activity, spinner, methodLabels, fromEventView);

        index = findMethodInReminderList(methodValues, newReminder.getMethod());
        spinner.setSelection(index);

        if (onItemSelected != null) {
            spinner.setTag(index);
            spinner.setOnItemSelectedListener(onItemSelected);
        }

        items.add(reminderItem);

        return true;
    }

    /**
     * 미리알림개수가 최대개수를 넘어서면 `미리알림추가`를 숨긴다.
     * @param view Root View
     * @param reminders 미리알림목록
     * @param maxReminders 미리알림최대개수
     */
    public static void updateAddReminderButton(View view, ArrayList<LinearLayout> reminders,
            int maxReminders) {
        View reminderAddButton = view.findViewById(R.id.reminder_add);
        if (reminderAddButton != null) {
            if (reminders.size() >= maxReminders) {
                reminderAddButton.setVisibility(View.GONE);
            } else {
                reminderAddButton.setVisibility(View.VISIBLE);
            }
        }
    }
}
