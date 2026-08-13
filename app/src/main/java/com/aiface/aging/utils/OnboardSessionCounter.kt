package com.aiface.aging.utils

import android.content.Context
import com.aiface.aging.AiFaceApp

object OnboardSessionCounter {

    private const val PREFS_NAME = "obsession_pref"
    private const val COUNT_KEY = "session_count"


    fun incrementCounter(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()


        var count = sharedPreferences.getInt(COUNT_KEY, 1)

        if (count <= AiFaceApp.onboardingSession){
            count++
            editor.putInt(COUNT_KEY, count)
            editor.apply()
        }


    }

    fun getCounter(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(COUNT_KEY, 0)
    }

}
