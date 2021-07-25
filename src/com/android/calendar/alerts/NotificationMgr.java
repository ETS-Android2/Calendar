/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.NotificationChannel;

import com.android.calendar.alerts.AlertService.NotificationWrapper;

/**
 * 알림 띄우기, 취소를 진행하는 추상 클라스
 * @see AlertService.NotificationMgrWrapper
 */
public abstract class NotificationMgr {
    //알림 띄우기
    public abstract void notify(int id, NotificationWrapper notification);

    //알림 취소
    public abstract void cancel(int id);

    //알림통로 창조
    public abstract void createNotificationChannel(NotificationChannel channel);

    /**
     * 모든 알림들을 취소한다.
     */
    public void cancelAll() {
        cancelAllBetween(0, AlertService.MAX_NOTIFICATIONS);
    }

    /**
     * 수값범위에 있는 Id들에 해당한 알림들을 취소한다.
     * @param from 시작
     * @param to 마감
     */
    public void cancelAllBetween(int from, int to) {
        for (int i = from; i <= to; i++) {
            cancel(i);
        }
    }
}
