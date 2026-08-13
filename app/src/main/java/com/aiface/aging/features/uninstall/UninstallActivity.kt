package com.aiface.aging.features.uninstall

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.aiface.aging.MainActivity
import com.aiface.aging.R
import com.aiface.aging.shared.DataStoreManager
import com.aiface.aging.shared.IS_LANGUAGE
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.shared.safeFinish
import com.aiface.aging.shared.safePopBackStack
import com.aiface.aging.databinding.ActivityUninstallBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class UninstallActivity : AppCompatActivity() {
    private var binding: ActivityUninstallBinding? = null
    private var navController: NavController? = null

    @Inject
    lateinit var dataStoreManager: DataStoreManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLocate(this)
        binding = ActivityUninstallBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        hideNavigationBar()
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_uninstall) as NavHostFragment
        navController = navHostFragment.navController


    }


    fun setLocate(activity: Activity) {
        var lang = Locale.getDefault().language //System Default Language
        dataStoreManager.readDataStoreValue(IS_LANGUAGE, "") {
            Log.e("Languageset", this.toString())
            val langnew = this
            if (langnew == "") {
                val supportedLangs = listOf(
                    "ja",
                    "es",
                    "in",
                    "hi",
                    "de",
                    "it",
                    "pt",
                    "ko",
                    "fr",
                    "ar",
                    "vi",
                    "ta",
                )

                // Check if the system language is in the list of supported languages, else default to English
                var lange = if (lang in supportedLangs) lang else "en"
                lang = lange
            } else {
                lang = langnew
            }
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            activity.baseContext.resources.updateConfiguration(
                config,
                activity.baseContext.resources.displayMetrics
            )
        }
    }

    override fun onBackPressed() {
        if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
        try {
            val navController = findNavController(R.id.nav_host_uninstall)
            if (!navController.safePopBackStack()) {
                startActivity(Intent(this, MainActivity::class.java))
                safeFinish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            safeFinish()
        }
    }
    companion object{
        var isUninstall = false
    }


    private fun forceImmersiveMode() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            // Make the content appear under system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val controller = window.insetsController ?: return
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Hide both system bars
            controller.hide(WindowInsets.Type.systemBars())

            // Add an additional attempt with delay to handle race conditions
            window.decorView.post {
                controller.hide(WindowInsets.Type.systemBars())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        forceImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus){
            lifecycleScope.launch {
                delay(3000)
                forceImmersiveMode()
            }
        }
    }
}