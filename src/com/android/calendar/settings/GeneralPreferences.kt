/*
 * Copyright (C) 2020 Dominik Schürmann <dominik@schuermann.eu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.calendar.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.android.krcalendar.R

/**
 * Preference key들을 가지고 있는 클라스
 */
class GeneralPreferences {
    companion object {
        //알림을 띄워주겠는가? (Boolean)
        const val KEY_ALERTS = "preferences_alerts"

        //알림을 띄울때 진동을 주겠는가? (Boolean)
        const val KEY_ALERTS_VIBRATE = "preferences_alerts_vibrate"

        //알림음(String)
        const val KEY_ALERTS_RINGTONE = "preferences_alerts_ringtone"

        //기정의 미리알림시간(10분)
        const val KEY_DEFAULT_REMINDER = "preferences_default_reminder"
        const val NO_REMINDER = -1          //미리알림없음
        const val NO_REMINDER_STRING = "-1" //미리알림없음

        //사용자에게 snooze하겠는가를 문의하겠는가?
        const val KEY_USE_CUSTOM_SNOOZE_DELAY = "preferences_custom_snooze_delay"

        //Snooze 지연시간
        const val KEY_DEFAULT_SNOOZE_DELAY = "preferences_default_snooze_delay"
        const val SNOOZE_DELAY_DEFAULT_TIME = 5 // in minutes

        const val KEY_VERSION = "preferences_version"

        //기정의 일정 지속시간
        const val KEY_DEFAULT_EVENT_DURATION = "preferences_default_event_duration"
        const val EVENT_DURATION_DEFAULT = "60"

        //기정의 알림음
        const val DEFAULT_RINGTONE = "content://settings/system/notification_sound"

        //App 의 Preference 이름
        private const val SHARED_PREFS_NAME = "com.android.calendar_preferences"
        const val SHARED_PREFS_NAME_NO_BACKUP = "com.android.calendar_preferences_no_backup"

        /**
         * SharedPreference객체를 돌려준다.
         */
        fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        }

        /**
         * xml파일로부터 preference 기정값들을 불러들여서 그것들을 설정한다.
         */
        fun setDefaultValues(context: Context) {
            PreferenceManager.setDefaultValues(context, SHARED_PREFS_NAME, Context.MODE_PRIVATE,
                    R.xml.general_preferences, true)
        }
    }
}
