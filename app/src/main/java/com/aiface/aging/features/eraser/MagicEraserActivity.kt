package com.aiface.aging.features.eraser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aiface.aging.R
import com.aiface.aging.features.imgpicker.util.Extras
import com.aiface.aging.features.result.ResultLauncher
import com.aiface.aging.features.result.ResultSource
import com.aiface.aging.shared.ads.showHomeInterstitialThen
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MagicEraserActivity : AppCompatActivity(), EraserFragment.OnFragmentInteractionListener {

    private var imagePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_magic_eraser)
        imagePath = intent.getStringExtra(Extras.ERASED_BITMAP_PATH).orEmpty()
        if (imagePath.isBlank()) {
            ToastUtils.showErrorToast(this)
            finish()
            return
        }
        openEraser()
    }

    private fun openEraser() {
        if (isFinishing || isDestroyed) return
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view_eraser, EraserFragment.newInstance(imagePath))
            .commit()
    }

    override fun onErasedImage(path: String?) {
        if (path.isNullOrBlank()) {
            finish()
            return
        }
        showHomeInterstitialThen {
            if (!isFinishing && !isDestroyed) {
                ResultLauncher.openLocalPreview(
                    activity = this,
                    source = ResultSource.PHOTO_EDITOR,
                    path = path,
                )
                finish()
            }
        }
    }
}
