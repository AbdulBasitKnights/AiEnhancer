package com.aiface.aging.features.result

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.aiface.aging.features.share.ExtrasShareImageActivity

object ResultLauncher {

    fun open(
        activity: Activity,
        bundle: Bundle,
    ) {
        activity.startActivity(
            Intent(activity, ResultHostActivity::class.java).apply {
                putExtras(bundle)
            },
        )
    }

    fun openLocalSaved(
        activity: Activity,
        source: ResultSource,
        path: String? = null,
        uri: Uri? = null,
        fromMyWork: Boolean = false,
        mediaId: Long? = null,
        displayName: String? = null,
    ) {
        open(
            activity,
            ResultArgs.localSaved(
                source = source,
                path = path,
                uri = uri,
                fromMyWork = fromMyWork,
                mediaId = mediaId,
                displayName = displayName,
            ),
        )
    }

    /**
     * Editor Next: open Preview with unsaved cache/local file.
     * Gallery save happens later on Preview "Save to Gallery".
     *
     * When opening from [showHomeInterstitialThen] (activity hop), pass [finishHost]=false —
     * the inter helper finishes the editor after the ad so nav + ad run in parallel.
     */
    @JvmOverloads
    fun openLocalPreview(
        activity: Activity,
        source: ResultSource,
        path: String? = null,
        uri: Uri? = null,
        finishHost: Boolean = false,
    ) {
        open(
            activity,
            ResultArgs.localPending(
                source = source,
                path = path,
                uri = uri,
            ),
        )
        if (finishHost && !activity.isFinishing) {
            activity.finish()
        }
    }

    fun openFromShareExtras(
        activity: Activity,
        extras: ExtrasShareImageActivity,
        source: ResultSource,
    ) {
        open(activity, ResultArgs.fromShareExtras(extras, source))
    }
}
