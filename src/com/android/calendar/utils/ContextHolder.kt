package com.android.calendar.utils

import android.content.Context

/**
 * 아무데서나 context를 리용할수 있도록 하기 위해 만든 클라스
 */
class ContextHolder {
    companion object {
        private var applicationContext: Context? = null

        //Application이 창조될때 Application을 받는다.
        fun init(context: Context) {
            applicationContext = context
        }

        //Context를 돌려준다.
        fun getApplicationContext(): Context? {
            return applicationContext
        }
    }
}