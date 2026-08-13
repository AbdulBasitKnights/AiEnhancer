package com.aiface.aging.features.share

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aiface.aging.features.result.ResultArgs
import com.aiface.aging.features.result.ResultHostActivity
import com.aiface.aging.features.result.ResultSource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            return
        }

        val extras = readExtras()
        if (extras == null) {
            finish()
            return
        }

        startActivity(
            Intent(this, ResultHostActivity::class.java).apply {
                putExtras(ResultArgs.fromShareExtras(extras, resolveSource(extras)))
            },
        )
        finish()
    }

    private fun readExtras(): ExtrasShareImageActivity? {
        val key = ShareImageActivity::class.java.simpleName
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.extras?.getParcelable(key, ExtrasShareImageActivity::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.extras?.getParcelable(key)
        }
    }

    private fun resolveSource(extras: ExtrasShareImageActivity): ResultSource {
        intent.getStringExtra(ResultArgs.SOURCE)?.let { raw ->
            runCatching { ResultSource.valueOf(raw) }.getOrNull()?.let { return it }
        }
        if (extras.fromMyWork) return ResultSource.MY_WORK
        return ResultSource.PHOTO_EDITOR
    }
}
