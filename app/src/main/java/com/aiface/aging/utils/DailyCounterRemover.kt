package com.aiface.aging.utils

import android.content.Context
import android.content.SharedPreferences

/** Simple daily free-use counter (HD-Camera parity stub). */
object DailyCounterRemover {
    private const val PREFS = "daily_counter_prefs"
    private const val KEY_COUNT = "count"
    private const val KEY_DATE = "date"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getCounter(context: Context): Int = prefs(context).getInt(KEY_COUNT, 0)

    fun incrementCounter(context: Context) {
        val p = prefs(context)
        p.edit().putInt(KEY_COUNT, p.getInt(KEY_COUNT, 0) + 1).apply()
    }

    fun resetCounter(context: Context) {
        prefs(context).edit().putInt(KEY_COUNT, 0).putString(KEY_DATE, today()).apply()
    }

    fun getLastSavedDate(context: Context): String =
        prefs(context).getString(KEY_DATE, "").orEmpty()

    private fun today(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
}
