package com.aiface.aging.features.result

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.aiface.aging.R
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.shared.safeFinish
import com.aiface.aging.shared.safePopBackStack
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ResultHostActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyLightSystemBars()
        supportActionBar?.hide()
        setContentView(R.layout.activity_result_host)
        hideNavigationBar()

        if (savedInstanceState == null) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.result_host_nav) as NavHostFragment
            val graph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph_result_host)
            val extras = intent.extras ?: Bundle()
            val fromMyWork = extras.getBoolean(ResultArgs.FROM_MY_WORK, false)
            graph.setStartDestination(
                if (fromMyWork) R.id.resultFragment else R.id.resultPreviewFragment,
            )
            // Fragments also read activity.intent.extras (Nav may filter undeclared keys).
            navHostFragment.navController.setGraph(graph, extras)
        }

        setupBackNavigation()
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
                    try {
                        val navController =
                            (supportFragmentManager.findFragmentById(R.id.result_host_nav) as? NavHostFragment)
                                ?.navController
                        if (navController == null) {
                            safeFinish()
                            return
                        }
                        if (!navController.safePopBackStack()) {
                            safeFinish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        )
    }
}
