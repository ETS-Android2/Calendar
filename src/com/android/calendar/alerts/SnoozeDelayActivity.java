/*
 * Copyright (C) 2013 The CyanogenMod Project
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

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.TimePicker;

import com.android.calendar.utils.Utils;

import com.android.krcalendar.R;

/**
 * 선잠지연화면
 */
public class SnoozeDelayActivity extends Activity implements
        TimePickerDialog.OnTimeSetListener, DialogInterface.OnCancelListener {
    private static final int DIALOG_DELAY = 1;  //Dialog Id

    @Override
    protected void onResume() {
        super.onResume();
        showDialog(DIALOG_DELAY);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_DELAY) {
            //시간선택대화창(시, 분) 현시
            TimePickerDialog d = new TimePickerDialog(this, this, 0, 0, true);
            d.setTitle(R.string.snooze_delay_dialog_title);
            d.setCancelable(true);
            d.setOnCancelListener(this);
            return d;
        }

        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d) {
        if (id == DIALOG_DELAY) {
            //Preference 에서 선잠지연시간을 불러들여서 시간선택대화창에 현시된다.
            TimePickerDialog tpd = (TimePickerDialog) d;
            int delayMinutes = (int) (Utils.getDefaultSnoozeDelayMs(this) / (60L * 1000L));
            int hours = delayMinutes / 60;
            int minutes = delayMinutes % 60;
            tpd.updateTime(hours, minutes);
        }
        super.onPrepareDialog(id, d);
    }

    /**
     * 시간선택대화창에서 `취소`단추를 눌렀을때
     * @param d
     */
    @Override
    public void onCancel(DialogInterface d) {
        //Activity 를 종료한다.
        finish();
    }

    /**
     * 시간선택대화창에서 시간을 변경하고 `확인`단추를 눌렀을때
     * @param view
     * @param hour 시
     * @param minute 분
     */
    @Override
    public void onTimeSet(TimePicker view, int hour, int minute) {
        //지연시간을 얻는다.(미리초)
        long delay = (hour * 60 + minute) * 60L * 1000L;

        //SnoozeAlarmService 를 기동시킨다.
        Intent intent = getIntent();
        intent.setClass(this, SnoozeAlarmsService.class);
        intent.putExtra(AlertUtils.SNOOZE_DELAY_KEY, delay);
        startService(intent);

        //Activity 를 종료한다.
        finish();
    }
}
