package com.aiface.aging.features.look

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import com.aiface.aging.R
import com.aiface.aging.databinding.ActivityLookFeatureBinding
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.applyWindowInsets
import com.aiface.aging.shared.hideNavigationBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LookFeatureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLookFeatureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyLightSystemBars()
        hideNavigationBar()
//        applyWindowInsets()
        val featureType = intent.getStringExtra(LookConstants.EXTRA_FEATURE_TYPE)
            ?: LookConstants.SCREEN_MAKEUP

        val navHost = supportFragmentManager
            .findFragmentById(R.id.look_nav_host) as NavHostFragment
        val navController = navHost.navController
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.nav_look_features)
        navController.setGraph(graph, bundleOf("type" to featureType))

    }
}
