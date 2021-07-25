package com.android.calendar.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.calendar.utils.CalendarUtils;
import com.android.calendar.utils.Utils;
import com.android.krcalendar.R;
import com.kevalpatel.ringtonepicker.RingtonePickerDialog;
import com.kevalpatel.ringtonepicker.RingtonePickerListener;

import org.jetbrains.annotations.NotNull;

import static com.android.calendar.settings.GeneralPreferences.KEY_ALERTS_RINGTONE;

/**
 * 설정화면의 Preference들을 가지고 있는 Fragment
 */
public class SettingsPreference extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    private static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";

    private Preference ringtonePref;

    //대화창이 두번이상 켜지는것을 방지하기 위해 리용되는 flag변수
    private boolean mDialogOpened = false;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        setPreferencesFromResource(R.xml.settings_preference, s);
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        //Preference로부터 설정된 알림음을 얻는다.
        Activity activity = requireActivity();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences prefs = CalendarUtils.getSharedPreferences(activity,
                Utils.SHARED_PREFS_NAME);

        ringtonePref = preferenceScreen.findPreference(KEY_ALERTS_RINGTONE);
        String ringtoneUriString = Utils.getRingtonePreference(activity);

        //Preference에 잘못된 ringtone값이 들어가있을수 있으므로 다시 ringtone을 설정한다.
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ALERTS_RINGTONE, ringtoneUriString).apply();

        //Summary를 얻어서 보여준다.
        String ringtoneDisplayString = getRingtoneTitleFromUri(activity, ringtoneUriString);
        ringtonePref.setSummary(ringtoneDisplayString);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setDivider(null);
        setDividerHeight(0);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        //`알림음`을 눌렀을때
        if(key.equals(KEY_ALERTS_RINGTONE)){
            if(!mDialogOpened)
                showRingtoneManager();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        return false;
    }

    /**
     * 알림음선택대화창을 띄워준다.
     */
    private void showRingtoneManager() {
        RingtonePickerDialog.Builder ringtonePickerBuilder =
                new RingtonePickerDialog.Builder(requireActivity(), requireActivity().getSupportFragmentManager())
                .displayDefaultRingtone(true)
                .displaySilentRingtone(true)
                .setPlaySampleWhileSelection(true);

        //알림음을 얻는다.
        String existingValue = Utils.getRingtonePreference(requireActivity());
        if (existingValue != null) {
            if (existingValue.isEmpty()) {
                //"무음"을 설정한다.
                ringtonePickerBuilder.setCurrentRingtoneUri(Uri.EMPTY);
            } else {
                ringtonePickerBuilder.setCurrentRingtoneUri(Uri.parse(existingValue));
            }
        } else {
            //설정된 알림음이 없을때는 "기정알림음"을 설정한다.
            ringtonePickerBuilder.setCurrentRingtoneUri(Settings.System.DEFAULT_NOTIFICATION_URI);
        }

        ringtonePickerBuilder.addRingtoneType(RingtonePickerDialog.Builder.TYPE_NOTIFICATION);
        ringtonePickerBuilder.setListener(new RingtonePickerListener() {
                @Override
                public void OnRingtoneSelected(@NotNull String ringtoneName, Uri ringtoneUri) {
                    //알림음이 선택되면 그것을 preference에 보관한다.
                    String ringtoneString = ringtoneUri.toString();

                    Utils.setRingtonePreference(requireActivity(), ringtoneString);
                    String ringtoneDisplayString = getRingtoneTitleFromUri(requireActivity(), ringtoneString);
                    ringtonePref.setSummary(ringtoneDisplayString);
                }
            }
        );

        final RingtonePickerDialog ringtoneDialog = ringtonePickerBuilder.show();
        ringtoneDialog.setOnDialogShowListener((positive, negative, dialog) -> {
            Utils.addCommonTouchListener(positive);
            Utils.addCommonTouchListener(negative);
        });
        ringtoneDialog.setOnDialogDismissListener(() -> mDialogOpened = false);

        mDialogOpened = true;
    }

    /**
     * Ringtone의 제목을 돌려준다
     * @param context Context
     * @param uri Ringtone uri
     */
    private String getRingtoneTitleFromUri(Context context, String uri) {
        if (TextUtils.isEmpty(uri)) {
            return null;
        }

        Ringtone ring = RingtoneManager.getRingtone(requireActivity(), Uri.parse(uri));
        return ring.getTitle(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //OverScroll 효과를 없앤다.
        if(getListView() != null)
            getListView().setOverScrollMode(View.OVER_SCROLL_NEVER);
    }
}
