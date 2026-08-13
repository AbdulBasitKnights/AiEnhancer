package com.aiface.aging.features.result

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.MainActivity
import com.aiface.aging.R
import com.aiface.aging.features.home.HomeAiTemplateResolver

/**
 * Navigation + UI helpers for result / share screens.
 * All public entry points are crash-safe (detached fragment, missing NavHost, finishing Activity).
 */
object ResultFeatureNavigator {

    private const val TAG = "ResultFeatureNav"

    const val EXTRA_OPEN_PREVIEW = "extra_open_preview"
    const val EXTRA_PREVIEW_ARGS = "extra_preview_args"
    /** Kept for binary/intent compat; collage entry from result is disabled. */
    const val EXTRA_OPEN_COLLAGE = "extra_open_collage"
    /** Kept for binary/intent compat; photo-edit entry from result is disabled. */
    const val EXTRA_OPEN_PHOTO_EDIT = "extra_open_photo_edit"
    /** Feature opened from [ResultHostActivity]; back should return to the share screen. */
    const val EXTRA_LAUNCHED_FROM_RESULT = "extra_launched_from_result"

    fun isLaunchedFromResult(intent: Intent?): Boolean =
        intent?.getBooleanExtra(EXTRA_LAUNCHED_FROM_RESULT, false) == true

    /** Share screen host, or [MainActivity] opened from it — back should return to share. */
    fun shouldReturnToShareScreen(activity: FragmentActivity?): Boolean {
        if (activity == null) return false
        return activity is ResultHostActivity ||
            (activity is MainActivity && activity.shouldReturnToResultHostOnExit())
    }

    fun shouldFinishMainToRevealShareHost(activity: FragmentActivity?): Boolean =
        activity is MainActivity && activity.shouldReturnToResultHostOnExit()

    fun openFeature(fragment: Fragment, feature: ResultTryMoreFeature) {
        runCatching {
            if (!fragment.isAdded || fragment.view == null) {
                Log.w(TAG, "openFeature skipped — fragment not ready")
                return
            }
            val activity = fragment.activity
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                Log.w(TAG, "openFeature skipped — activity not ready")
                return
            }
            when (feature) {
                ResultTryMoreFeature.ENHANCER -> openPreview(fragment, feature)
            }
        }.onFailure { Log.e(TAG, "openFeature(fragment) failed", it) }
    }

    fun openFeature(activity: AppCompatActivity, feature: ResultTryMoreFeature) {
        runCatching {
            if (activity.isFinishing || activity.isDestroyed) {
                Log.w(TAG, "openFeature skipped — activity finishing")
                return
            }
            when (feature) {
                ResultTryMoreFeature.ENHANCER -> openPreviewFromActivity(activity, feature)
            }
        }.onFailure { Log.e(TAG, "openFeature(activity) failed", it) }
    }

    fun launchPhotoEditor(activity: FragmentActivity) {
        // Photo Editor home/result entry removed; keep method for any residual callers as no-op.
        Log.w(TAG, "launchPhotoEditor ignored — feature entry removed")
    }

    private fun openPreviewFromActivity(activity: AppCompatActivity, feature: ResultTryMoreFeature) {
        runCatching {
            if (activity.isFinishing || activity.isDestroyed) return
            val bundle = feature.toPreviewBundle(activity)
            if (activity is MainActivity) {
                safeFindNavController(activity, R.id.nav_host_main)
                    ?.let { safeNavigate(it, R.id.previewFragment, bundle) }
                return
            }
            if (activity is ResultHostActivity) {
                safeFindNavController(activity, R.id.result_host_nav)
                    ?.let { safeNavigate(it, R.id.action_resultFragment_to_previewFragment, bundle) }
                return
            }
            launchMainForResultFeature(activity) {
                putExtra(EXTRA_OPEN_PREVIEW, true)
                putExtra(EXTRA_PREVIEW_ARGS, bundle)
            }
        }.onFailure { Log.e(TAG, "openPreviewFromActivity failed", it) }
    }

    private fun openPreview(fragment: Fragment, feature: ResultTryMoreFeature) {
        runCatching {
            if (!fragment.isAdded) return
            val context = fragment.context ?: return
            val bundle = feature.toPreviewBundle(context)
            val navController = safeNavController(fragment)
            val canNavigateFromResult =
                navController?.currentDestination?.id == R.id.resultFragment &&
                    navController.graph.findNode(R.id.previewFragment) != null

            if (canNavigateFromResult && navController != null) {
                safeNavigate(navController, R.id.action_resultFragment_to_previewFragment, bundle)
                return
            }
            val activity = fragment.activity as? AppCompatActivity ?: return
            launchMainForResultFeature(activity) {
                putExtra(EXTRA_OPEN_PREVIEW, true)
                putExtra(EXTRA_PREVIEW_ARGS, bundle)
            }
        }.onFailure { Log.e(TAG, "openPreview failed", it) }
    }

    private fun launchMainForResultFeature(activity: AppCompatActivity, configure: Intent.() -> Unit) {
        runCatching {
            if (activity.isFinishing || activity.isDestroyed) return
            activity.startActivity(
                Intent(activity, MainActivity::class.java).apply {
                    putExtra(EXTRA_LAUNCHED_FROM_RESULT, true)
                    configure()
                },
            )
        }.onFailure { Log.e(TAG, "launchMainForResultFeature failed", it) }
    }

    private fun ResultTryMoreFeature.toPreviewBundle(context: android.content.Context): Bundle {
        return when (this) {
            ResultTryMoreFeature.ENHANCER -> Bundle().apply {
                putString("item_id", "")
                putString("prompt", HomeAiTemplateResolver.DEFAULT_ENHANCER_PROMPT)
                putString("url", "enhancer")
                putString("category_name", context.getString(R.string.photo_enhancer))
                putString("title", context.getString(R.string.photo_enhancer))
            }
        }
    }

    private fun safeNavController(fragment: Fragment): NavController? {
        if (!fragment.isAdded || fragment.view == null) return null
        return runCatching { fragment.findNavController() }.getOrNull()
    }

    private fun safeFindNavController(activity: AppCompatActivity, viewId: Int): NavController? {
        if (activity.isFinishing || activity.isDestroyed) return null
        return runCatching { activity.findNavController(viewId) }.getOrNull()
    }

    private fun safeNavigate(navController: NavController, resId: Int, args: Bundle? = null) {
        runCatching {
            val current = navController.currentDestination
            val action = current?.getAction(resId)
            val node = navController.graph.findNode(resId)
            when {
                action != null -> navController.navigate(resId, args)
                node != null -> navController.navigate(resId, args)
                else -> Log.w(TAG, "safeNavigate skipped — no action/dest for $resId")
            }
        }.onFailure { Log.e(TAG, "safeNavigate($resId) failed", it) }
    }
}

object ResultScreenHelper {

    private const val TAG = "ResultScreenHelper"

    fun setupTryMoreFeatures(
        recyclerView: RecyclerView,
        fragment: Fragment,
    ) {
        runCatching {
            if (!fragment.isAdded || fragment.view == null) return
            setupTryMoreFeatures(recyclerView) { feature ->
                // Re-check on click — fragment may be destroyed after bind.
                if (!fragment.isAdded || fragment.view == null) {
                    Log.w(TAG, "try-more click ignored — fragment gone")
                    return@setupTryMoreFeatures
                }
                ResultFeatureNavigator.openFeature(fragment, feature)
            }
        }.onFailure { Log.e(TAG, "setupTryMoreFeatures(fragment) failed", it) }
    }

    fun setupTryMoreFeatures(
        recyclerView: RecyclerView,
        activity: AppCompatActivity,
    ) {
        runCatching {
            if (activity.isFinishing || activity.isDestroyed) return
            setupTryMoreFeatures(recyclerView) { feature ->
                if (activity.isFinishing || activity.isDestroyed) {
                    Log.w(TAG, "try-more click ignored — activity gone")
                    return@setupTryMoreFeatures
                }
                ResultFeatureNavigator.openFeature(activity, feature)
            }
        }.onFailure { Log.e(TAG, "setupTryMoreFeatures(activity) failed", it) }
    }

    private fun setupTryMoreFeatures(
        recyclerView: RecyclerView,
        onFeatureClick: (ResultTryMoreFeature) -> Unit,
    ) {
        runCatching {
            if (recyclerView.layoutManager == null) {
                recyclerView.layoutManager =
                    LinearLayoutManager(recyclerView.context, LinearLayoutManager.HORIZONTAL, false)
            }
            recyclerView.adapter = ResultTryMoreFeaturesAdapter { feature ->
                runCatching { onFeatureClick(feature) }
                    .onFailure { Log.e(TAG, "try-more feature click failed", it) }
            }
        }.onFailure { Log.e(TAG, "setupTryMoreFeatures internal failed", it) }
    }

    fun showSavedBadge(savedBadge: View?) {
        runCatching {
            if (savedBadge == null) return
            savedBadge.visibility = View.VISIBLE
        }.onFailure { Log.e(TAG, "showSavedBadge failed", it) }
    }
}
