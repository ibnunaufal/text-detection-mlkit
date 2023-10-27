package com.naufall.textdetection.preferences

import android.content.Context

object SharedPref {
    private const val PREFS_NAME = "my_shared_prefs"
    private const val KEY_TEXT = "key_text"

    //    ==============================================================================================
    fun saveTextResult(context: Context, text: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(KEY_TEXT, text).apply()
    }

    fun getTextResult(context: Context): String? {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getString(KEY_TEXT, null)
    }

}