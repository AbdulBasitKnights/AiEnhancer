package com.aiface.aging.utils


import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.aiface.aging.R
import com.aiface.aging.SplashActivity


object ShortcutUtils {

    private val DROPPED_SHORTCUT_IDS = listOf(
        "aging_shortcut",
        "collage_shortcut",
        "body_shortcut",
    )

    fun createDynamicShortcut(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)

            // Clear dropped feature shortcuts so they cannot relaunch aging/collage/body.
            shortcutManager?.removeDynamicShortcuts(DROPPED_SHORTCUT_IDS)

            val uninstallShortcut = ShortcutInfo.Builder(context, "uninstall_shortcut")
                .setShortLabel(context.getString(R.string.uninstall))
                .setLongLabel(context.getString(R.string.uninstall))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_uninstall_36))
                .setIntent(
                    Intent(context, SplashActivity::class.java).apply {
                        action = "my_shortcut.ACTION_UNINSTALL_SHORTCUT"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                .build()

            shortcutManager?.addDynamicShortcuts(listOf(uninstallShortcut))
        }
    }

/*    fun removeEditShortcut(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager?.removeDynamicShortcuts(listOf("edit_shortcut"))
        }
    }*/

    fun removeAllShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager?.removeAllDynamicShortcuts()
        }
    }
}
