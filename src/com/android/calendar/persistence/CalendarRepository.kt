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

package com.android.calendar.persistence

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.provider.CalendarContract
import com.android.krcalendar.R

/**
 * 달력계정을 추가하기 위해 리용되는 클라스
 */
@SuppressLint("MissingPermission")
internal class CalendarRepository(val application: Application) {

    private var contentResolver = application.contentResolver

    /**
     * 달력계정을 만들기 위해 ContentValue에 써넣기한다.
     * @param accountName
     * @param displayName
     */
    private fun buildLocalCalendarContentValues(accountName: String, displayName: String): ContentValues {
        val internalName = "kr_local_" + displayName.replace("[^a-zA-Z0-9]".toRegex(), "")
        return ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
            put(CalendarContract.Calendars.NAME, internalName)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, displayName)
            put(CalendarContract.Calendars.CALENDAR_COLOR_KEY, DEFAULT_COLOR_KEY)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_ROOT)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.IS_PRIMARY, 0)
            put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0)
            put(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE, 1)
            // from Android docs: "the device will only process METHOD_DEFAULT and METHOD_ALERT reminders"
            put(CalendarContract.Calendars.ALLOWED_REMINDERS, CalendarContract.Reminders.METHOD_ALERT.toString())
            put(CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES, CalendarContract.Attendees.TYPE_NONE.toString())
        }
    }

    /**
     * 달력계정에 색을 추가하기 위해 ContentValue에 써넣기한다.
     * @param accountName
     * @param displayName
     */
    private fun buildLocalCalendarColorsContentValues(accountName: String, colorType: Int, colorKey: String, color: Int): ContentValues {
        return ContentValues().apply {
            put(CalendarContract.Colors.ACCOUNT_NAME, accountName)
            put(CalendarContract.Colors.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Colors.COLOR_TYPE, colorType)
            put(CalendarContract.Colors.COLOR_KEY, colorKey)
            put(CalendarContract.Colors.COLOR, color)
        }
    }

    /**
     * Offline 달력계정 창조
     * @param accountName [CalendarContract.Calendars.ACCOUNT_NAME]
     * @param displayName [CalendarContract.Calendars.CALENDAR_DISPLAY_NAME]
     */
    fun addLocalCalendar(accountName: String, displayName: String): Uri {
        maybeAddCalendarAndEventColors(accountName)

        val cv = buildLocalCalendarContentValues(accountName, displayName)
        return contentResolver.insert(asLocalCalendarSyncAdapter(accountName, CalendarContract.Calendars.CONTENT_URI), cv)
                ?: throw IllegalArgumentException()
    }

    /**
     * Color Table에 달력색갈 추가
     */
    private fun maybeAddCalendarAndEventColors(accountName: String) {
        //달력색갈이 table에 존재하면 여기서 끝낸다.
        if (areCalendarColorsExisting(accountName)) {
            return
        }

        //색갈배렬을 얻는다. (24개 색)
        val defaultColors: IntArray = application.resources.getIntArray(R.array.defaultCalendarColors)

        val insertBulk = mutableListOf<ContentValues>()
        for ((i, color) in defaultColors.withIndex()) {
            val colorKey = i.toString()
            val colorCvCalendar = buildLocalCalendarColorsContentValues(accountName, CalendarContract.Colors.TYPE_CALENDAR, colorKey, color)
            val colorCvEvent = buildLocalCalendarColorsContentValues(accountName, CalendarContract.Colors.TYPE_EVENT, colorKey, color)
            insertBulk.add(colorCvCalendar) //달력(Calendar)의 색추가
            insertBulk.add(colorCvEvent)    //달력일정(Event)의 색추가
        }
        contentResolver.bulkInsert(asLocalCalendarSyncAdapter(accountName, CalendarContract.Colors.CONTENT_URI), insertBulk.toTypedArray())
    }

    /**
     * 계정에 대한 달력색갈이 Table에 있는가를 돌려준다.
     */
    private fun areCalendarColorsExisting(accountName: String): Boolean {
        contentResolver.query(CalendarContract.Colors.CONTENT_URI,
                null,
                CalendarContract.Colors.ACCOUNT_NAME + "=? AND " + CalendarContract.Colors.ACCOUNT_TYPE + "=?",
                arrayOf(accountName, CalendarContract.ACCOUNT_TYPE_LOCAL),
                null).use {
            if (it!!.moveToFirst()) {
                return true
            }
        }
        return false
    }

    companion object {
        const val DEFAULT_COLOR_KEY = "1"

        /**
         * 본래의 uri에 달력계정정보를 붙여서 돌려준다.
         */
        @JvmStatic
        fun asLocalCalendarSyncAdapter(accountName: String, uri: Uri): Uri {
            return uri.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL).build()
        }
    }
}