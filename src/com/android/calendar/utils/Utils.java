/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.calendar.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;

import com.android.calendar.activities.AllInOneActivity;
import com.android.kr_common.Time;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.android.calendar.utils.CalendarUtils.TimeZoneUtils;
import com.android.calendar.settings.GeneralPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.krcalendar.R;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static com.android.calendar.kr.common.CalendarUtil.ONE_DAY;

/**
 * 모든 클라스들에서 많이 리용되는 변수, 함수들을 정적성원으로 가지고 있는 클라스
 */
public class Utils {
    //지연시간 0 미리초
    public static final long NO_DELAY = 0;

    /* 일정편집 형태 */
    public static final int MODIFY_UNINITIALIZED = 0;   //정해지지 않음
    public static final int MODIFY_SELECTED = 1;        //이 일정만
    public static final int MODIFY_ALL_FOLLOWING = 2;   //현재 및 향후 일정
    public static final int MODIFY_ALL = 3;             //전체 일정

    //Done Runnable에 코드로 들어가는 flag값들
    public static final int DONE_EXIT = 1;      //끝내기
    public static final int DONE_SAVE = 1 << 1; //보관

    public static final String INTENT_KEY_HOME = "KEY_HOME";
    public static final int DECLINED_EVENT_ALPHA = 0x66;

    public static final String APPWIDGET_DATA_TYPE = "vnd.android.data/update";
    public static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";
    public static final String MACHINE_GENERATED_ADDRESS = "calendar.google.com";
    private static final String TAG = "CalUtils";
    private static final float SATURATION_ADJUST = 1.3f;
    private static final float INTENSITY_ADJUST = 0.8f;
    private static final TimeZoneUtils mTZUtils = new TimeZoneUtils(SHARED_PREFS_NAME);
    private static final Pattern mWildcardPattern = Pattern.compile("^.*$");

    //Calendar type preference
    public static final String CALENDAR_TYPE_PREF = "pref_display_type";
    public static final int CALENDAR_TYPE1 = 1;
    public static final int CALENDAR_TYPE2 = 2;
    public static final int CALENDAR_TYPE3 = 3;
    public static final int CALENDAR_TYPE4 = 4;

    private static boolean DAY_TO_MONTH_TRANSITION = false;
    private static boolean MONTH_TO_DAY_TRANSITION = false;
    private static boolean MONTH_TO_YEAR_TRANSITION = false;
    private static boolean YEAR_TO_MONTH_TRANSITION = false;
    private static boolean TO_AGENDA_TRANSITION = false;
    private static boolean FROM_AGENDA_TRANSITION = false;
    private static boolean TODAY_BOTH_VISIBLE = false;

    private static boolean IS_ON_TOUCH = false;
    private static View TOUCH_VIEW = null;

    public static final float CUSTOM_TOUCH_DRAG_MOVE_RATIO_FWD = 10f;

    /**
    * A coordinate must be of the following form for Google Maps to correctly use it:
    * Latitude, Longitude
    *
    * This may be in decimal form:
    * Latitude: {-90 to 90}
    * Longitude: {-180 to 180}
    *
    * Or, in degrees, minutes, and seconds:
    * Latitude: {-90 to 90}° {0 to 59}' {0 to 59}"
    * Latitude: {-180 to 180}° {0 to 59}' {0 to 59}"
    * + or - degrees may also be represented with N or n, S or s for latitude, and with
    * E or e, W or w for longitude, where the direction may either precede or follow the value.
    *
    * Some examples of coordinates that will be accepted by the regex:
    * 37.422081°, -122.084576°
    * 37.422081,-122.084576
    * +37°25'19.49", -122°5'4.47"
    * 37°25'19.49"N, 122°5'4.47"W
    * N 37° 25' 19.49",  W 122° 5' 4.47"
    **/
    private static final String COORD_DEGREES_LATITUDE =
            "([-+NnSs]" + "(\\s)*)?"
            + "[1-9]?[0-9](\u00B0)" + "(\\s)*"
            + "([1-5]?[0-9]\')?" + "(\\s)*"
            + "([1-5]?[0-9]" + "(\\.[0-9]+)?\")?"
            + "((\\s)*" + "[NnSs])?";
    private static final String COORD_DEGREES_LONGITUDE =
            "([-+EeWw]" + "(\\s)*)?"
            + "(1)?[0-9]?[0-9](\u00B0)" + "(\\s)*"
            + "([1-5]?[0-9]\')?" + "(\\s)*"
            + "([1-5]?[0-9]" + "(\\.[0-9]+)?\")?"
            + "((\\s)*" + "[EeWw])?";
    private static final String COORD_DEGREES_PATTERN =
            COORD_DEGREES_LATITUDE
            + "(\\s)*" + "," + "(\\s)*"
            + COORD_DEGREES_LONGITUDE;
    private static final String COORD_DECIMAL_LATITUDE =
            "[+-]?"
            + "[1-9]?[0-9]" + "(\\.[0-9]+)"
            + "(\u00B0)?";
    private static final String COORD_DECIMAL_LONGITUDE =
            "[+-]?"
            + "(1)?[0-9]?[0-9]" + "(\\.[0-9]+)"
            + "(\u00B0)?";
    private static final String COORD_DECIMAL_PATTERN =
            COORD_DECIMAL_LATITUDE
            + "(\\s)*" + "," + "(\\s)*"
            + COORD_DECIMAL_LONGITUDE;
    private static final Pattern COORD_PATTERN =
            Pattern.compile(COORD_DEGREES_PATTERN + "|" + COORD_DECIMAL_PATTERN);
    private static final String NANP_ALLOWED_SYMBOLS = "()+-*#.";
    private static final int NANP_MIN_DIGITS = 7;
    private static final int NANP_MAX_DIGITS = 11;
    private static String sVersion = null;

    /**
     * Returns whether the SDK is the Oreo release or later.
     */
    public static boolean isOreoOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * Gets the intent action for telling the widget to update.
     */
    public static String getWidgetUpdateAction(Context context) {
        return "com.android.calendar.APPWIDGET_UPDATE";
    }

    /**
     * Gets the intent action for telling the widget to update.
     */
    public static String getWidgetScheduledUpdateAction(Context context) {
        return "com.android.calendar.APPWIDGET_SCHEDULED_UPDATE";
    }

    /**
     * Gets the intent action for telling the widget to update.
     */
    public static String getSearchAuthority(Context context) {
        return "com.android.calendar.provider.CalendarRecentSuggestionsProvider";
    }

    /**
     * Gets the time zone that Calendar should be displayed in This is a helper
     * method to get the appropriate time zone for Calendar. If this is the
     * first time this method has been called it will initiate an asynchronous
     * query to verify that the data in preferences is correct. The callback
     * supplied will only be called if this query returns a value other than
     * what is stored in preferences and should cause the calling activity to
     * refresh anything that depends on calling this method.
     *
     * @param context The calling activity
     * @param callback The runnable that should execute if a query returns new
     *            values
     * @return The string value representing the time zone Calendar should
     *         display
     */
    public static String getTimeZone(Context context, Runnable callback) {
        return mTZUtils.getTimeZone(context, callback);
    }

    /**
     * Formats a date or a time range according to the local conventions.
     *
     * @param context the context is required only if the time is shown
     * @param startMillis the start time in UTC milliseconds
     * @param endMillis the end time in UTC milliseconds
     * @param flags a bit mask of options See {@link DateUtils#formatDateRange(Context, Formatter,
     * long, long, int, String) formatDateRange}
     * @return a string containing the formatted date/time range.
     */
    public static String formatDateRange(
            Context context, long startMillis, long endMillis, int flags) {
        return mTZUtils.formatDateRange(context, startMillis, endMillis, flags);
    }

    public static boolean getDefaultVibrate(SharedPreferences prefs) {
        boolean vibrate;
        vibrate = prefs.getBoolean(GeneralPreferences.KEY_ALERTS_VIBRATE, false);
        return vibrate;
    }

    /*-- Check permission with list of permissions --*/
    public static boolean isPermissionGranted(Context context, String[] permissionList) {
        for (String permission : permissionList) {
            if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        //If all permissions granted, then return true
        return true;
    }

    /*-- Check shouldShowRequestPermissionRationale with list of permissions --*/
    public static boolean shouldShowRequestPermissionsRationale(Activity activity, String[] permissionList) {
        for (String permission : permissionList) {
            if(activity.shouldShowRequestPermissionRationale(permission))
                return true;
        }

        //If all permissions not requiring showRequestPermission, then return false
        return false;
    }

    public static String getSharedPreference(Context context, String key, String defaultValue) {
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static int getSharedPreference(Context context, String key, int defaultValue) {
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        return prefs.getInt(key, defaultValue);
    }

    public static boolean getSharedPreference(Context context, String key, boolean defaultValue) {
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        return prefs.getBoolean(key, defaultValue);
    }

    public static int getCalendarTypePreference(Context context){
        return getSharedPreference(context, CALENDAR_TYPE_PREF, CALENDAR_TYPE1);
    }

    /**
     * Asynchronously sets the preference with the given key to the given value
     *
     * @param context the context to use to get preferences from
     * @param key the key of the preference to set
     * @param value the value to set
     */
    public static void setSharedPreference(Context context, String key, String value) {
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        prefs.edit().putString(key, value).apply();
    }

    public static void setSharedPreference(Context context, String key, String[] values) {
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        Collections.addAll(set, values);
        prefs.edit().putStringSet(key, set).apply();
    }

    public static void setSharedPreference(Context context, String key, boolean value) {
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void setSharedPreference(Context context, String key, int value) {
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static boolean getAlertIsEnabled(Context context){
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        return prefs.getBoolean(GeneralPreferences.KEY_ALERTS, false);
    }

    // The backed up ring tone preference should not used because it is a device
    // specific Uri. The preference now lives in a separate non-backed-up
    // shared_pref file (SHARED_PREFS_NAME_NO_BACKUP). The preference in the old
    // backed-up shared_pref file (SHARED_PREFS_NAME) is used only to control the
    // default value when the ringtone dialog opens up.
    //
    // At backup manager "restore" time (which should happen before launcher
    // comes up for the first time), the value will be set/reset to default
    // ringtone.

    public static String getRingtonePreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                GeneralPreferences.SHARED_PREFS_NAME_NO_BACKUP, Context.MODE_PRIVATE);
        String ringtone = prefs.getString(GeneralPreferences.KEY_ALERTS_RINGTONE, null);

        // If it hasn't been populated yet, that means new code is running for
        // the first time and restore hasn't happened. Migrate value from
        // backed-up shared_pref to non-shared_pref.
        if (ringtone == null) {
            // Read from the old place with a default of DEFAULT_RINGTONE
            ringtone = getSharedPreference(context, GeneralPreferences.KEY_ALERTS_RINGTONE,
                    GeneralPreferences.DEFAULT_RINGTONE);

            // Write it to the new place
            setRingtonePreference(context, ringtone);
        }

        return ringtone;
    }

    public static void setRingtonePreference(Context context, String value) {
        SharedPreferences prefs = context.getSharedPreferences(
                GeneralPreferences.SHARED_PREFS_NAME_NO_BACKUP, Context.MODE_PRIVATE);
        prefs.edit().putString(GeneralPreferences.KEY_ALERTS_RINGTONE, value).apply();
    }

    public static MatrixCursor matrixCursorFromCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        String[] columnNames = cursor.getColumnNames();
        if (columnNames == null) {
            columnNames = new String[] {};
        }
        MatrixCursor newCursor = new MatrixCursor(columnNames);
        int numColumns = cursor.getColumnCount();
        String data[] = new String[numColumns];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                data[i] = cursor.getString(i);
            }
            newCursor.addRow(data);
        }
        return newCursor;
    }

    /**
     * If the given intent specifies a time (in milliseconds since the epoch),
     * then that time is returned. Otherwise, the current time is returned.
     */
    public static final long timeFromIntentInMillis(Intent intent) {
        // If the time was specified, then use that. Otherwise, use the current
        // time.
        Uri data = intent.getData();
        long millis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
        if (millis == -1 && data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("time")) {
                try {
                    millis = Long.valueOf(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Log.i("Calendar", "timeFromIntentInMillis: Data existed but no valid time "
                            + "found. Using current time.");
                }
            }
        }
        if (millis <= 0) {
            millis = System.currentTimeMillis();
        }
        return millis;
    }

    /**
     * Get first day of week as android.text.format.Time constant.
     *
     * @return the first day of week in android.text.format.Time
     */
    public static int getFirstDayOfWeek() {
        return Time.SUNDAY;
    }

    /**
     * Get the default length for the duration of an event, in milliseconds.
     *
     * @return the default event length, in milliseconds
     */
    public static long getDefaultEventDurationInMillis(Context context) {
        SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        String pref = prefs.getString(GeneralPreferences.KEY_DEFAULT_EVENT_DURATION,
                GeneralPreferences.EVENT_DURATION_DEFAULT);
        final int defaultDurationInMins = Integer.parseInt(pref);
        return defaultDurationInMins * DateUtils.MINUTE_IN_MILLIS;
    }

    public static boolean useCustomSnoozeDelay(Context context) {
        final SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        return prefs.getBoolean(GeneralPreferences.KEY_USE_CUSTOM_SNOOZE_DELAY, false);
    }

    /**
     * 기정의 선잠지연시간(미리초)을 돌려준다.
     * @param context Context
     */
    public static long getDefaultSnoozeDelayMs(Context context) {
        final SharedPreferences prefs = GeneralPreferences.Companion.getSharedPreferences(context);
        final String value = prefs.getString(GeneralPreferences.KEY_DEFAULT_SNOOZE_DELAY, null);
        final long intValue = value != null
                ? Long.parseLong(value)
                : GeneralPreferences.SNOOZE_DELAY_DEFAULT_TIME;

        return intValue * 60L * 1000L; // 분 -> 미리초
    }

    /**
     * Convert given UTC time into current local time. This assumes it is for an
     * allday event and will adjust the time to be on a midnight boundary.
     *
     * @param recycle Time object to recycle, otherwise null.
     * @param utcTime Time to convert, in UTC.
     * @param tz The time zone to convert this time to.
     */
    public static long convertAlldayUtcToLocal(Time recycle, long utcTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = Time.TIMEZONE_UTC;
        recycle.set(utcTime);
        recycle.timezone = tz;
        return recycle.normalize(true);
    }

    public static long convertAlldayLocalToUTC(Time recycle, long localTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = tz;
        recycle.set(localTime);
        recycle.timezone = Time.TIMEZONE_UTC;
        return recycle.normalize(true);
    }

    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static boolean getConfigBool(Context c, int key) {
        return c.getResources().getBoolean(key);
    }

    public static String getUpperString(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String getLowerString(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * For devices with Jellybean or later, darkens the given color to ensure that white text is
     * clearly visible on top of it.  For devices prior to Jellybean, does nothing, as the
     * sync adapter handles the color change.
     *
     * @param color
     */
    public static int getDisplayColorFromColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(hsv[1] * SATURATION_ADJUST, 1.0f);
        hsv[2] = hsv[2] * INTENSITY_ADJUST;
        return Color.HSVToColor(hsv);
    }

    // This takes a color and computes what it would look like blended with
    // white. The result is the color that should be used for declined events.
    public static int getDeclinedColorFromColor(int color) {
        int bg = 0xffffffff;
        int a = DECLINED_EVENT_ALPHA;
        int r = (((color & 0x00ff0000) * a) + ((bg & 0x00ff0000) * (0xff - a))) & 0xff000000;
        int g = (((color & 0x0000ff00) * a) + ((bg & 0x0000ff00) * (0xff - a))) & 0x00ff0000;
        int b = (((color & 0x000000ff) * a) + ((bg & 0x000000ff) * (0xff - a))) & 0x0000ff00;
        return (0xff000000) | ((r | g | b) >> 8);
    }

    /**
     * Sends an intent to launch the top level Calendar view.
     *
     * @param context
     */
    public static void returnToCalendarHome(Context context) {
        Intent launchIntent = new Intent(context, AllInOneActivity.class);
        launchIntent.setAction(Intent.ACTION_DEFAULT);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntent.putExtra(INTENT_KEY_HOME, true);
        context.startActivity(launchIntent);
    }

    /**
     * Given a context and a time in millis since unix epoch figures out the
     * correct week of the year for that time.
     *
     * @param millisSinceEpoch
     * @return
     */
    public static int getWeekNumberFromTime(long millisSinceEpoch, Context context) {
        Time weekTime = new Time(getTimeZone(context, null));
        weekTime.set(millisSinceEpoch);
        weekTime.normalize(true);
        int firstDayOfWeek = getFirstDayOfWeek();
        // if the date is on Saturday or Sunday and the start of the week
        // isn't Monday we may need to shift the date to be in the correct
        // week
        if (weekTime.weekDay == Time.SUNDAY
                && (firstDayOfWeek == Time.SUNDAY || firstDayOfWeek == Time.SATURDAY)) {
            weekTime.plusDays(1);
            //weekTime.monthDay++;
            //weekTime.normalize(true);
        } else if (weekTime.weekDay == Time.SATURDAY && firstDayOfWeek == Time.SATURDAY) {
            weekTime.plusDays(2);
            //weekTime.monthDay += 2;
            //weekTime.normalize(true);
        }
        return weekTime.getWeekNumber();
    }

    /**
     * Example fake email addresses used as attendee emails are resources like conference rooms,
     * or another calendar, etc.  These all end in "calendar.google.com".
     */
    public static boolean isValidEmail(String email) {
        return email != null && !email.endsWith(MACHINE_GENERATED_ADDRESS);
    }

    /**
     * Return the app version code.
     */
    public static String getVersionCode(Context context) {
        if (sVersion == null) {
            try {
                sVersion = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                // Can't find version; just leave it blank.
                Log.e(TAG, "Error finding package " + context.getApplicationInfo().packageName);
            }
        }
        return sVersion;
    }

    /**
     * Replaces stretches of text that look like addresses and phone numbers with clickable
     * links. If lastDitchGeo is true, then if no links are found in the textview, the entire
     * string will be converted to a single geo link. Any spans that may have previously been
     * in the text will be cleared out.
     * <p>
     * This is really just an enhanced version of Linkify.addLinks().
     *
     * @param text - The string to search for links.
     * @param lastDitchGeo - If no links are found, turn the entire string into one geo link.
     * @return Spannable object containing the list of URL spans found.
     */
    public static Spannable extendedLinkify(String text, boolean lastDitchGeo) {
        // We use a copy of the string argument so it's available for later if necessary.
        Spannable spanText = SpannableString.valueOf(text);

        /*
         * If the text includes a street address like "1600 Amphitheater Parkway, 94043",
         * the current Linkify code will identify "94043" as a phone number and invite
         * you to dial it (and not provide a map link for the address).  For outside US,
         * use Linkify result iff it spans the entire text.  Otherwise send the user to maps.
         */
        String defaultPhoneRegion = System.getProperty("user.region", "US");
        if (!defaultPhoneRegion.equals("US")) {
            Linkify.addLinks(spanText, Linkify.ALL);

            // If Linkify links the entire text, use that result.
            URLSpan[] spans = spanText.getSpans(0, spanText.length(), URLSpan.class);
            if (spans.length == 1) {
                int linkStart = spanText.getSpanStart(spans[0]);
                int linkEnd = spanText.getSpanEnd(spans[0]);
                if (linkStart <= indexFirstNonWhitespaceChar(spanText) &&
                        linkEnd >= indexLastNonWhitespaceChar(spanText) + 1) {
                    return spanText;
                }
            }

            // Otherwise, to be cautious and to try to prevent false positives, reset the spannable.
            spanText = SpannableString.valueOf(text);
            // If lastDitchGeo is true, default the entire string to geo.
            if (lastDitchGeo && !text.isEmpty()) {
                Linkify.addLinks(spanText, mWildcardPattern, "geo:0,0?q=");
            }
            return spanText;
        }

        /*
         * For within US, we want to have better recognition of phone numbers without losing
         * any of the existing annotations.  Ideally this would be addressed by improving Linkify.
         * For now we manage it as a second pass over the text.
         *
         * URIs and e-mail addresses are pretty easy to pick out of text.  Phone numbers
         * are a bit tricky because they have radically different formats in different
         * countries, in terms of both the digits and the way in which they are commonly
         * written or presented (e.g. the punctuation and spaces in "(650) 555-1212").
         * The expected format of a street address is defined in WebView.findAddress().  It's
         * pretty narrowly defined, so it won't often match.
         *
         * The RFC 3966 specification defines the format of a "tel:" URI.
         *
         * Start by letting Linkify find anything that isn't a phone number.  We have to let it
         * run first because every invocation removes all previous URLSpan annotations.
         *
         * Ideally we'd use the external/libphonenumber routines, but those aren't available
         * to unbundled applications.
         */
        boolean linkifyFoundLinks = Linkify.addLinks(spanText,
                Linkify.ALL & ~(Linkify.PHONE_NUMBERS));

        /*
         * Get a list of any spans created by Linkify, for the coordinate overlapping span check.
         */
        URLSpan[] existingSpans = spanText.getSpans(0, spanText.length(), URLSpan.class);

        /*
         * Check for coordinates.
         * This must be done before phone numbers because longitude may look like a phone number.
         */
        Matcher coordMatcher = COORD_PATTERN.matcher(spanText);
        int coordCount = 0;
        while (coordMatcher.find()) {
            int start = coordMatcher.start();
            int end = coordMatcher.end();
            if (spanWillOverlap(spanText, existingSpans, start, end)) {
                continue;
            }

            URLSpan span = new URLSpan("geo:0,0?q=" + coordMatcher.group());
            spanText.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            coordCount++;
        }

        /*
         * Update the list of existing spans, for the phone number overlapping span check.
         */
        existingSpans = spanText.getSpans(0, spanText.length(), URLSpan.class);

        /*
         * Search for phone numbers.
         *
         * Some URIs contain strings of digits that look like phone numbers.  If both the URI
         * scanner and the phone number scanner find them, we want the URI link to win.  Since
         * the URI scanner runs first, we just need to avoid creating overlapping spans.
         */
        int[] phoneSequences = findNanpPhoneNumbers(text);

        /*
         * Insert spans for the numbers we found.  We generate "tel:" URIs.
         */
        int phoneCount = 0;
        for (int match = 0; match < phoneSequences.length / 2; match++) {
            int start = phoneSequences[match*2];
            int end = phoneSequences[match*2 + 1];

            if (spanWillOverlap(spanText, existingSpans, start, end)) {
                continue;
            }

            /*
             * The Linkify code takes the matching span and strips out everything that isn't a
             * digit or '+' sign.  We do the same here.  Extension numbers will get appended
             * without a separator, but the dialer wasn't doing anything useful with ";ext="
             * anyway.
             */

            //String dialStr = phoneUtil.format(match.number(),
            //        PhoneNumberUtil.PhoneNumberFormat.RFC3966);
            StringBuilder dialBuilder = new StringBuilder();
            for (int i = start; i < end; i++) {
                char ch = spanText.charAt(i);
                if (ch == '+' || Character.isDigit(ch)) {
                    dialBuilder.append(ch);
                }
            }
            URLSpan span = new URLSpan("tel:" + dialBuilder.toString());

            spanText.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            phoneCount++;
        }

        /*
         * If lastDitchGeo, and no other links have been found, set the entire string as a geo link.
         */
        if (lastDitchGeo && !text.isEmpty() &&
                !linkifyFoundLinks && phoneCount == 0 && coordCount == 0) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "No linkification matches, using geo default");
            }
            Linkify.addLinks(spanText, mWildcardPattern, "geo:0,0?q=");
        }

        return spanText;
    }

    private static int indexFirstNonWhitespaceChar(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int indexLastNonWhitespaceChar(CharSequence str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds North American Numbering Plan (NANP) phone numbers in the input text.
     *
     * @param text The text to scan.
     * @return A list of [start, end) pairs indicating the positions of phone numbers in the input.
     */
    // @VisibleForTesting
    static int[] findNanpPhoneNumbers(CharSequence text) {
        ArrayList<Integer> list = new ArrayList<Integer>();

        int startPos = 0;
        int endPos = text.length() - NANP_MIN_DIGITS + 1;
        if (endPos < 0) {
            return new int[] {};
        }

        /*
         * We can't just strip the whitespace out and crunch it down, because the whitespace
         * is significant.  March through, trying to figure out where numbers start and end.
         */
        while (startPos < endPos) {
            // skip whitespace
            while (Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                startPos++;
            }
            if (startPos == endPos) {
                break;
            }

            // check for a match at this position
            int matchEnd = findNanpMatchEnd(text, startPos);
            if (matchEnd > startPos) {
                list.add(startPos);
                list.add(matchEnd);
                startPos = matchEnd;    // skip past match
            } else {
                // skip to next whitespace char
                while (!Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                    startPos++;
                }
            }
        }

        int[] result = new int[list.size()];
        for (int i = list.size() - 1; i >= 0; i--) {
            result[i] = list.get(i);
        }
        return result;
    }

    /**
     * Checks to see if there is a valid phone number in the input, starting at the specified
     * offset.  If so, the index of the last character + 1 is returned.  The input is assumed
     * to begin with a non-whitespace character.
     *
     * @return Exclusive end position, or -1 if not a match.
     */
    private static int findNanpMatchEnd(CharSequence text, int startPos) {
        /*
         * A few interesting cases:
         *   94043                              # too short, ignore
         *   123456789012                       # too long, ignore
         *   +1 (650) 555-1212                  # 11 digits, spaces
         *   (650) 555 5555                     # Second space, only when first is present.
         *   (650) 555-1212, (650) 555-1213     # two numbers, return first
         *   1-650-555-1212                     # 11 digits with leading '1'
         *   *#650.555.1212#*!                  # 10 digits, include #*, ignore trailing '!'
         *   555.1212                           # 7 digits
         *
         * For the most part we want to break on whitespace, but it's common to leave a space
         * between the initial '1' and/or after the area code.
         */

        // Check for "tel:" URI prefix.
        if (text.length() > startPos+4
                && text.subSequence(startPos, startPos+4).toString().equalsIgnoreCase("tel:")) {
            startPos += 4;
        }

        int endPos = text.length();
        int curPos = startPos;
        int foundDigits = 0;
        char firstDigit = 'x';
        boolean foundWhiteSpaceAfterAreaCode = false;

        while (curPos <= endPos) {
            char ch;
            if (curPos < endPos) {
                ch = text.charAt(curPos);
            } else {
                ch = 27;    // fake invalid symbol at end to trigger loop break
            }

            if (Character.isDigit(ch)) {
                if (foundDigits == 0) {
                    firstDigit = ch;
                }
                foundDigits++;
                if (foundDigits > NANP_MAX_DIGITS) {
                    // too many digits, stop early
                    return -1;
                }
            } else if (Character.isWhitespace(ch)) {
                if ( (firstDigit == '1' && foundDigits == 4) ||
                        (foundDigits == 3)) {
                    foundWhiteSpaceAfterAreaCode = true;
                } else if (firstDigit == '1' && foundDigits == 1) {
                } else if (foundWhiteSpaceAfterAreaCode
                        && ( (firstDigit == '1' && (foundDigits == 7)) || (foundDigits == 6))) {
                } else {
                    break;
                }
            } else if (NANP_ALLOWED_SYMBOLS.indexOf(ch) == -1) {
                break;
            }
            // else it's an allowed symbol

            curPos++;
        }

        if ((firstDigit != '1' && (foundDigits == 7 || foundDigits == 10)) ||
                (firstDigit == '1' && foundDigits == 11)) {
            // match
            return curPos;
        }

        return -1;
    }
    /* Common theme attributes including color */

    public static int getCommonBackgroundColor(Context context){
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.common_background_color, typedValue, true);
        return typedValue.data;
    }

    public static int getThemeAttribute(Context context, int attr){
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static int getThemeDrawableAttribute(Context context, int attr){
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }

    public static int applyAlpha(int color, int alpha){
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    //Days difference between two dates
    public static long daysDiff(int startYear, int startMonth, int startDay,
                               int endYear, int endMonth, int endDay){
        DateTime minDate = new DateTime(DateTimeZone.UTC).withDate(startYear, startMonth, startDay).withMillisOfDay(0);
        DateTime curDate = new DateTime(DateTimeZone.UTC).withDate(endYear, endMonth, endDay).withMillisOfDay(0);
        return (curDate.getMillis() - minDate.getMillis()) / ONE_DAY;
    }

    public static String getWeekDayString(Context context, int weekday, boolean shortText){
        switch (weekday){
            case 1:
                return shortText?context.getResources().getString(R.string.mon):context.getResources().getString(R.string.monday);
            case 2:
                return shortText?context.getResources().getString(R.string.tue):context.getResources().getString(R.string.tuesday);
            case 3:
                return shortText?context.getResources().getString(R.string.wed):context.getResources().getString(R.string.wednesday);
            case 4:
                return shortText?context.getResources().getString(R.string.thu):context.getResources().getString(R.string.thursday);
            case 5:
                return shortText?context.getResources().getString(R.string.fri):context.getResources().getString(R.string.friday);
            case 6:
                return shortText?context.getResources().getString(R.string.sat):context.getResources().getString(R.string.saturday);
            case 7:
            default:
                return shortText?context.getResources().getString(R.string.sun):context.getResources().getString(R.string.sunday);
        }
    }

    public static void drawStrokedText(String string, Canvas canvas, Paint paint, float strokeWidth, int fillColor, int strokeColor, float fadeAlpha,
                                float x, float y){
        paint.setColor(fillColor);
        paint.setAlpha((int) (fadeAlpha * 255));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(string, x, y, paint);

        paint.setColor(strokeColor);
        paint.setAlpha((int) (fadeAlpha * 255));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        canvas.drawText(string, x, y, paint);

    }

    //Dark/Light Theme인가를 돌려준다.(현재는 Dark Theme)
    public static boolean isDayTheme(){
        return false;
    }

    public static void makeBottomDialog(Dialog dialog){
        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams windowLayoutParams = window.getAttributes();

        Resources resources = dialog.getContext().getResources();
        windowLayoutParams.y = (int) resources.getDimension(R.dimen.common_dialog_bottom_margin);
        window.setGravity(Gravity.BOTTOM);
        window.setAttributes(windowLayoutParams);
    }

    //Convert dp to pixel
    public static float convertDpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    //Convert sp to pixel
    public static float convertSpToPixel(float sp, Context context) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static int getMonthImageResource(int month) {
        switch (month){
            case 1:
                return R.drawable.image_january;
            case 2:
                return R.drawable.image_february;
            case 3:
                return R.drawable.image_march;
            case 4:
                return R.drawable.image_april;
            case 5:
                return R.drawable.image_may;
            case 6:
                return R.drawable.image_june;
            case 7:
                return R.drawable.image_july;
            case 8:
                return R.drawable.image_august;
            case 9:
                return R.drawable.image_september;
            case 10:
                return R.drawable.image_october;
            case 11:
                return R.drawable.image_november;
            case 12:
            default:
                return R.drawable.image_december;
        }
    }

    //Get month string
    public static String getMonthString(int month, Context context) {
        Resources resources = context.getResources();
        switch (month) {
            case 1:
                return resources.getString(R.string.january_label);
            case 2:
                return resources.getString(R.string.february_label);
            case 3:
                return resources.getString(R.string.march_label);
            case 4:
                return resources.getString(R.string.april_label);
            case 5:
                return resources.getString(R.string.may_label);
            case 6:
                return resources.getString(R.string.june_label);
            case 7:
                return resources.getString(R.string.july_label);
            case 8:
                return resources.getString(R.string.august_label);
            case 9:
                return resources.getString(R.string.september_label);
            case 10:
                return resources.getString(R.string.october_label);
            case 11:
                return resources.getString(R.string.november_label);
            case 12:
            default:
                return resources.getString(R.string.december_label);
        }
    }

    /**
     * Statusbar를 제외한 화면의 너비, 높이를 돌려준다
     * @param context
     * @return Point(너비, 높이)
     */
    @NonNull
    public static Point getDisplayDimensions(Context context )
    {
        WindowManager wm = ( WindowManager ) context.getSystemService( Context.WINDOW_SERVICE );
        Display display = wm.getDefaultDisplay();

        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics( metrics );
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        //Statusbar의 높이가 이미 제외되서 계산되였을수 있기때문에 그것을 검사한다.
        display.getRealMetrics( metrics );
        int physicalHeight = metrics.heightPixels;
        int statusBarHeight = getStatusBarHeight( context );
        int navigationBarHeight = getNavigationBarHeight( context );
        int heightDelta = physicalHeight - screenHeight;
        if ( heightDelta == 0 || heightDelta == navigationBarHeight )
        {
            screenHeight -= statusBarHeight;
        }

        return new Point( screenWidth, screenHeight );
    }

    /**
     * @param context
     * @return Statusbar의 높이를 돌려준다.
     */
    public static int getStatusBarHeight( Context context )
    {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier( "status_bar_height", "dimen", "android" );
        return ( resourceId > 0 ) ? resources.getDimensionPixelSize( resourceId ) : 0;
    }

    /**
     * @param context
     * @return Navigation bar의 높이를 돌려준다.
     */
    public static int getNavigationBarHeight( Context context )
    {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier( "navigation_bar_height", "dimen", "android" );
        return ( resourceId > 0 ) ? resources.getDimensionPixelSize( resourceId ) : 0;
    }

    /**
     * Determines whether a new span at [start,end) will overlap with any existing span.
     */
    private static boolean spanWillOverlap(Spannable spanText, URLSpan[] spanList, int start,
            int end) {
        if (start == end) {
            // empty span, ignore
            return false;
        }
        for (URLSpan span : spanList) {
            int existingStart = spanText.getSpanStart(span);
            int existingEnd = spanText.getSpanEnd(span);
            if ((start >= existingStart && start < existingEnd) ||
                    end > existingStart && end <= existingEnd) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    CharSequence seq = spanText.subSequence(start, end);
                    Log.v(TAG, "Not linkifying " + seq + " as phone number due to overlap");
                }
                return true;
            }
        }

        return false;
    }

    public static void setDayToMonthTransition(boolean value) {
        DAY_TO_MONTH_TRANSITION = value;
    }

    public static boolean isDayToMonthTransition(){
        return DAY_TO_MONTH_TRANSITION;
    }

    public static void setMonthToDayTransition(boolean value) {
        MONTH_TO_DAY_TRANSITION = value;
    }

    public static boolean isMonthToDayTransition(){
        return MONTH_TO_DAY_TRANSITION;
    }

    public static void setMonthToYearTransition(boolean value) {
        MONTH_TO_YEAR_TRANSITION = value;
    }

    public static boolean isMonthToYearTransition() {
        return MONTH_TO_YEAR_TRANSITION;
    }

    public static void setYearToMonthTransition(boolean value) {
        YEAR_TO_MONTH_TRANSITION = value;
    }

    public static boolean isYearToMonthTransition() {
        return YEAR_TO_MONTH_TRANSITION;
    }

    public static void setToAgendaTransition(boolean value) {
        TO_AGENDA_TRANSITION = value;
    }

    public static boolean isToAgendaTransition() {
        return TO_AGENDA_TRANSITION;
    }

    public static void setFromAgendaTransition(boolean value) {
        FROM_AGENDA_TRANSITION = value;
    }

    public static boolean isFromAgendaTransition() {
        return FROM_AGENDA_TRANSITION;
    }

    public static void setTodayBothVisible(boolean value) {
        TODAY_BOTH_VISIBLE = value;
    }

    public static boolean isTodayBothVisible() {
        return TODAY_BOTH_VISIBLE;
    }

    public static boolean isOnTransition() {
        return Utils.isMonthToDayTransition() || Utils.isDayToMonthTransition() ||
                Utils.isMonthToYearTransition() || Utils.isYearToMonthTransition() ||
                Utils.isToAgendaTransition() || Utils.isFromAgendaTransition();
    }

    /**
     * 여러개의 단추들이 동시에 여러개 눌리우는것을 방지하기 위해 Touch사건을 감지하여 처리한다.(Multi touch)
     * @param view Touch사건을 처리할 View
     */
    public static void addCommonTouchListener(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        if(IS_ON_TOUCH) {
                            return TOUCH_VIEW != view; //다른 View에서 touch사건이 일어났을때는 동작을 막아버린다.
                        }

                        IS_ON_TOUCH = true;
                        TOUCH_VIEW = view;  //Down사건이 일어났을때 눌리운 View를 보관한다.
                        return false;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        IS_ON_TOUCH = false;
                        TOUCH_VIEW = null;
                        return false;
                }

                return false;
            }
        });
    }

}
