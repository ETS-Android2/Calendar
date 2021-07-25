
package com.android.calendar.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.calendar.utils.Utils
import com.android.krcalendar.R

const val EXTRA_SHOW_FRAGMENT = "settingsShowFragment"

/**
 * 설정화면
 */
class SettingsActivity : AppCompatActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Utils.isDayTheme())
            setTheme(R.style.CalendarAppThemeDay)
        else
            setTheme(R.style.CalendarAppThemeNight)
        val fragment = if (intent.hasExtra(EXTRA_SHOW_FRAGMENT)) {
            supportFragmentManager.fragmentFactory.instantiate(
                    classLoader,
                    intent.getStringExtra(EXTRA_SHOW_FRAGMENT)!!
            )
        } else {
            SettingsPreference()
        }

        setContentView(R.layout.settings)

        //`뒤로가기`단추를 눌렀을때 Activity의 onBackPressed를 호출한다.
        val backButton = findViewById<View>(R.id.back_button)
        backButton?.setOnClickListener { onBackPressed() }

        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.body_frame, fragment)
                    .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {
        //Fragment생성
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                pref.fragment
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }

        //Fragment를 추가/교체한다.
        supportFragmentManager.beginTransaction()
                .replace(R.id.body_frame, fragment)
                .addToBackStack(null)
                .commit()
        title = pref.title
        return true
    }
}
